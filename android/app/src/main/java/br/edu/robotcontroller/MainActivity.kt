package br.edu.robotcontroller

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val production = BuildConfig.FINAL_APP
    private val manual = BuildConfig.FLAVOR == "bluetooth"
    private lateinit var status: TextView
    private lateinit var notice: TextView
    private var log: TextView? = null
    private lateinit var picker: Spinner
    private lateinit var connect: Button
    private lateinit var disconnect: Button
    private lateinit var stop: Button
    private lateinit var refresh: Button
    private lateinit var transport: BluetoothTransport
    private var panel: ControlPanel? = null
    private var connectionPage: View? = null
    private var controlTab: Button? = null
    private var settingsTab: Button? = null
    private lateinit var heading: TextView
    private lateinit var connectionIcon: ImageView
    private lateinit var stepOne: View
    private lateinit var stepTwo: View
    private lateinit var settingsShortcut: View
    private var flow = ConnectionFlow()
    private var showingConnection = true
    private var resumed = false
    private val timer = Handler(Looper.getMainLooper())
    private val session = ControlSession(
        send = { value, done -> transport.send(value, done) },
        trace = { if (BuildConfig.DEBUG) android.util.Log.d("RobotControl", it) }
    )
    private val tick = object : Runnable {
        override fun run() { session.tick(); if (session.connected) timer.postDelayed(this, 200) }
    }
    private var devices = emptyList<BluetoothDevice>()
    private var state = BluetoothTransport.State.DISCONNECTED
    private val history = ArrayDeque<String>()
    private val adapter get() = getSystemService(BluetoothManager::class.java)?.adapter
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        flow = ConnectionFlow(savedInstanceState?.getBoolean("selectionReached") ?: false,
            savedInstanceState?.getBoolean("refreshPending") ?: false)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(4))
            setBackgroundColor(getColor(R.color.robot_background))
        }
        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; isBaselineAligned = false }
        connectionIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_connection)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        bar.addView(connectionIcon, LinearLayout.LayoutParams(dp(36), dp(36)).apply { marginEnd = dp(8) })
        val identity = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        heading = RobotUi.label(this, "Conectar ao robô").apply { RobotUi.fitText(this) }
        status = RobotUi.label(this, "• DESCONECTADO", 12f).apply {
            setPadding(0, dp(4), 0, 0)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            RobotUi.fitText(this, 12)
        }
        identity.addView(heading, LinearLayout.LayoutParams(-1, 0, 1f))
        identity.addView(status, LinearLayout.LayoutParams(-1, 0, 1f))
        bar.addView(identity, LinearLayout.LayoutParams(0, dp(48), 1f))
        stop = RobotUi.button(this, if (manual) "Enviar 0x00" else "PARAR", RobotUi.Action.DISCONNECT) {
            if (manual) transport.send(0) else { panel?.reset(); session.stop() }
        }.apply {
            contentDescription = if (manual) "Enviar um byte de parada" else "Parar e centralizar controles"
            if (production) visibility = View.GONE
        }
        bar.addView(stop, LinearLayout.LayoutParams(dp(if (manual) 112 else 72), dp(48)).apply { marginStart = dp(8) })
        val tabs = LinearLayout(this).apply { isBaselineAligned = false }
        if (!manual) {
            controlTab = RobotUi.button(this, "CONTROLE", RobotUi.Action.SECONDARY) { showConnection(false) }
            settingsTab = RobotUi.button(this, "CONFIGURAÇÃO", RobotUi.Action.PRIMARY) {
                if (showingConnection && flow.selectionReached) flow.restart()
                showConnection(true)
                updatePresentation()
            }
            tabs.addView(controlTab, LinearLayout.LayoutParams(0, dp(48), 1f))
            tabs.addView(settingsTab, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(10) })
            if (resources.configuration.screenWidthDp >= 480) {
                bar.addView(tabs, LinearLayout.LayoutParams(0, dp(48), 1.4f).apply { marginStart = dp(12) })
            }
        }
        root.addView(bar, LinearLayout.LayoutParams(-1, -2))
        if (!manual && tabs.parent == null) root.addView(tabs, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) })

        val connection = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun section(title: String) = ConnectionStepLayout(this).apply {
            row(RobotUi.label(context, title, 22f).apply { RobotUi.fitText(this, 22); gravity = Gravity.CENTER_VERTICAL }, 32)
            connection.addView(this, LinearLayout.LayoutParams(-1, -2))
        }
        stepOne = section("PASSO 1").apply {
            row(RobotUi.label(context, "CLIQUE NO BOTÃO ABAIXO E ATIVE O BLUETOOTH", 12f, muted = true).apply {
                RobotUi.fitText(this, 12); gravity = Gravity.CENTER_VERTICAL
            }, 24, 24)
            row(RobotUi.button(context, "CONFIGURAÇÕES DE BLUETOOTH", RobotUi.Action.PRIMARY) { openBluetoothSettings() }, 48, 12)
        }
        notice = RobotUi.label(this, "", 12f, muted = true).apply {
            RobotUi.fitText(this, 12, 2); visibility = View.GONE
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    visibility = if (s.isNullOrBlank()) View.GONE else View.VISIBLE
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }
        stepTwo = section("PASSO 2").apply {
            row(RobotUi.label(context, "SELECIONE O ROBÔ E TOQUE EM CONECTAR", 12f, muted = true).apply {
                RobotUi.fitText(this, 12); gravity = Gravity.CENTER_VERTICAL
            }, 24, 24)
            val selector = FrameLayout(context).apply { background = RobotUi.surface(context) }
            picker = Spinner(context, Spinner.MODE_DROPDOWN).apply {
                background = null
                contentDescription = "Selecionar robô pareado"
                setPopupBackgroundDrawable(android.graphics.drawable.ColorDrawable(getColor(R.color.robot_surface)))
                setOnTouchListener { _, event ->
                    if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN && !permitted()) {
                        loadDevices(); true
                    } else false
                }
            }
            selector.addView(picker, FrameLayout.LayoutParams(-1, -1))
            selector.addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_dropdown)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, FrameLayout.LayoutParams(dp(24), dp(24), Gravity.END or Gravity.CENTER_VERTICAL).apply { marginEnd = dp(12) })
            row(selector, 48, 16)
            val actions = LinearLayout(context).apply { isBaselineAligned = false }
            connect = RobotUi.button(context, "CONECTAR", RobotUi.Action.CONNECT) { connectSelected() }
            disconnect = RobotUi.button(context, "DESCONECTAR", RobotUi.Action.DISCONNECT) { transport.disconnect() }
            actions.addView(connect, LinearLayout.LayoutParams(0, -1, 1f).apply { marginEnd = dp(8) })
            actions.addView(disconnect, LinearLayout.LayoutParams(0, -1, 1f).apply { marginStart = dp(8) })
            row(actions, 48, 20)
            row(notice, 32, 8)
        }
        val utilities = LinearLayout(this).apply {
            isBaselineAligned = false
            orientation = if (resources.configuration.screenWidthDp < 480) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        }
        refresh = RobotUi.button(this, "Atualizar dispositivos", RobotUi.Action.SECONDARY) { loadDevices() }
        settingsShortcut = RobotUi.button(this, "Configurações de Bluetooth", RobotUi.Action.SECONDARY) { openBluetoothSettings() }
        if (utilities.orientation == LinearLayout.HORIZONTAL) {
            utilities.addView(refresh, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(8) })
            utilities.addView(settingsShortcut, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(8) })
        } else {
            utilities.addView(refresh, LinearLayout.LayoutParams(-1, dp(48)))
            utilities.addView(settingsShortcut, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) })
        }
        if (!production) connection.addView(utilities)
        if (!production) {
            connection.addView(RobotUi.label(this, if (manual) "Envio manual de um byte, sem joysticks." else "Envio periódico de 200 ms; inversão passa por zero.", 12f, muted = true))
            connection.addView(RobotUi.label(this, "Histórico", 18f))
            log = RobotUi.label(this, "", 13f, muted = true).apply { setTextIsSelectable(true); connection.addView(this) }
        }
        connectionPage = if (production) connection else ScrollView(this).apply { isFillViewport = true; addView(connection) }
        val body = FrameLayout(this)
        if (!manual) {
            panel = ControlPanel(this, fillSpace = true, diagnostics = !production).also { controls ->
                controls.onInput = { movement, steering -> session.input(movement, steering) }
                controls.controlsEnabled(false)
                body.addView(controls, FrameLayout.LayoutParams(-1, -1))
            }
        }
        body.addView(connectionPage, FrameLayout.LayoutParams(-1, -1))
        root.addView(body, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        showConnection(true)
        transport = BluetoothTransport { newState, message ->
            val previous = state
            state = newState
            if (previous != state) {
                timer.removeCallbacks(tick)
                if (state == BluetoothTransport.State.CONNECTED) {
                    notice.text = ""
                    if (!manual) {
                        panel?.reset()
                        session.connected()
                        showConnection(false)
                        timer.postDelayed(tick, 200)
                    }
                } else {
                    session.disconnected()
                    panel?.controlsEnabled(false)
                    if (state == BluetoothTransport.State.DISCONNECTED) {
                        notice.text = if (message.startsWith("Falha") || message.startsWith("Tempo") || message.startsWith("Envio"))
                            "Conexão interrompida. Confira se o robô está ligado e tente conectar novamente."
                        else ""
                        showConnection(true)
                    }
                }
            }
            status.text = when (state) {
                BluetoothTransport.State.DISCONNECTED -> "Desconectado"
                BluetoothTransport.State.CONNECTING -> "Conectando…"
                BluetoothTransport.State.CONNECTED -> "Conectado"
            }
            append(message)
            updateButtons()
        }
        updateButtons()
    }

    private fun showConnection(show: Boolean) {
        showingConnection = show || manual
        if (showingConnection) { session.stop(); panel?.reset() }
        connectionPage?.visibility = if (showingConnection) View.VISIBLE else View.GONE
        panel?.visibility = if (showingConnection) View.GONE else View.VISIBLE
        controlTab?.let { it.isSelected = !showingConnection; RobotUi.style(it, if (it.isSelected) RobotUi.Action.PRIMARY else RobotUi.Action.SECONDARY) }
        settingsTab?.let { it.isSelected = showingConnection; RobotUi.style(it, if (it.isSelected) RobotUi.Action.PRIMARY else RobotUi.Action.SECONDARY) }
        heading.text = if (showingConnection) "Conectar ao robô" else "CONTROLE DO ROBÔ"
        connectionIcon.visibility = if (showingConnection) View.VISIBLE else View.GONE
        updateControls()
    }

    private fun updateControls() {
        panel?.controlsEnabled(session.connected && resumed && hasWindowFocus() && !showingConnection)
    }

    private fun disarmInputs() {
        if (manual) return
        session.stop()
        panel?.controlsEnabled(false)
    }

    private fun openBluetoothSettings() {
        flow.openSettings()
        updatePresentation()
        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    private fun permitted(): Boolean = Build.VERSION.SDK_INT < 31 ||
        listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN).all {
            checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }

    @SuppressLint("MissingPermission")
    private fun loadDevices(ask: Boolean = true) {
        if (state != BluetoothTransport.State.DISCONNECTED) return
        devices = emptyList()
        if (!permitted()) {
            setDeviceLabels(listOf("Permissão necessária"))
            notice.text = "Toque na lista de dispositivos para permitir o acesso ao Bluetooth."
            updateButtons()
            if (ask && Build.VERSION.SDK_INT >= 31)
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN), 10)
            return
        }
        try {
            val bt = adapter
            when {
                bt == null -> notice.text = "Este telefone não oferece Bluetooth."
                !bt.isEnabled -> notice.text = "Ative o Bluetooth nas configurações."
                else -> {
                    devices = bt.bondedDevices.sortedBy { it.name ?: it.address }
                    notice.text = if (devices.isEmpty()) "Nenhum robô pareado. Toque em CONFIGURAÇÃO para voltar ao PASSO 1."
                        else ""
                }
            }
            val names = devices.map { "${it.name ?: "Sem nome"} · ${it.address}" }
                .ifEmpty { listOf("Nenhum dispositivo disponível") }
            setDeviceLabels(names)
            append("${devices.size} dispositivo(s) pareado(s).")
        } catch (error: SecurityException) {
            notice.text = "Permita o acesso a dispositivos próximos nas configurações do aplicativo."
        }
        updateButtons()
    }

    @SuppressLint("MissingPermission")
    private fun connectSelected() {
        if (!permitted()) { loadDevices(); return }
        try {
            val bt = adapter ?: return
            if (!bt.isEnabled) { notice.text = "Ative o Bluetooth antes de conectar."; return }
            val device = devices.getOrNull(picker.selectedItemPosition) ?: return
            transport.connect(bt, device)
        } catch (error: SecurityException) {
            notice.text = "Permita o acesso a dispositivos próximos nas configurações do aplicativo."
        }
    }

    private fun updateButtons() {
        val idle = state == BluetoothTransport.State.DISCONNECTED
        connect.isEnabled = idle && devices.isNotEmpty() && permitted()
        refresh.isEnabled = idle
        picker.isEnabled = idle
        disconnect.isEnabled = !idle
        disconnect.text = if (state == BluetoothTransport.State.CONNECTING) "Cancelar" else "Desconectar"
        stop.isEnabled = state == BluetoothTransport.State.CONNECTED
        updatePresentation()
    }

    private fun setDeviceLabels(names: List<String>) {
        picker.adapter = ArrayAdapter(this, R.layout.robot_spinner_item, names).apply {
            setDropDownViewResource(R.layout.robot_spinner_dropdown)
        }
    }

    private fun updatePresentation() {
        val selectionVisible = flow.selectionReached
        stepOne.visibility = if (selectionVisible) View.GONE else View.VISIBLE
        stepTwo.visibility = if (selectionVisible) View.VISIBLE else View.GONE
        settingsShortcut.visibility = if (selectionVisible) View.VISIBLE else View.GONE
        if (production) stop.visibility = if (state == BluetoothTransport.State.CONNECTED) View.VISIBLE else View.GONE
        status.text = when (state) {
            BluetoothTransport.State.DISCONNECTED -> "• DESCONECTADO"
            BluetoothTransport.State.CONNECTING -> "• CONECTANDO…"
            BluetoothTransport.State.CONNECTED -> "• CONECTADO"
        }
        status.setTextColor(getColor(when (state) {
            BluetoothTransport.State.DISCONNECTED -> R.color.robot_disconnected
            BluetoothTransport.State.CONNECTING -> R.color.robot_icon
            BluetoothTransport.State.CONNECTED -> R.color.robot_connected
        }))
    }

    private fun append(message: String) {
        if (production) return
        history.addFirst("${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}  $message")
        while (history.size > 80) history.removeLast()
        log?.text = history.joinToString("\n\n")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10) {
            if (permitted()) loadDevices(false)
            else notice.text = "Acesso negado. Permita Dispositivos próximos nas configurações do aplicativo."
        }
    }
    override fun onResume() {
        super.onResume()
        resumed = true
        if (::transport.isInitialized && flow.selectionReached) loadDevices(flow.takeRefreshOnResume())
        updateControls()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("selectionReached", flow.selectionReached)
        outState.putBoolean("refreshPending", flow.hasPendingRefresh())
        super.onSaveInstanceState(outState)
    }
    override fun onPause() { resumed = false; disarmInputs(); super.onPause() }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) disarmInputs() else updateControls()
    }
    override fun onStop() {
        timer.removeCallbacks(tick)
        session.disconnected()
        panel?.controlsEnabled(false)
        transport.disconnect("Sessão encerrada ao sair da tela.")
        super.onStop()
    }
    override fun onDestroy() { transport.close(); super.onDestroy() }
}
