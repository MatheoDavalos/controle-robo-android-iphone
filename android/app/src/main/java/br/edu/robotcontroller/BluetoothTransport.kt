package br.edu.robotcontroller

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executors

/** Owns only the RFCOMM session. No joystick, protocol or periodic transmission logic. */
@SuppressLint("MissingPermission") // Activity grants CONNECT and SCAN before use.
class BluetoothTransport(private val report: (State, String) -> Unit) {
    enum class State { DISCONNECTED, CONNECTING, CONNECTED }
    private val main = Handler(Looper.getMainLooper())
    private val writer = Executors.newSingleThreadExecutor()
    private var socket: BluetoothSocket? = null
    private var generation = 0
    private var state = State.DISCONNECTED
    private var timeout: Runnable? = null
    private var closed = false
    private var writing = false

    @Synchronized private fun emit(message: String) {
        val snapshot = state
        val token = generation
        main.post { synchronized(this) { if (!closed && token == generation) report(snapshot, message) } }
    }

    @Synchronized fun connect(adapter: BluetoothAdapter, device: BluetoothDevice) {
        if (closed || state != State.DISCONNECTED) return
        generation++
        val token = generation
        state = State.CONNECTING
        emit("Conectando a ${device.address} via SPP…")
        timeout = Runnable { fail(token, "Tempo de conexão excedido (15 s). Confira o ESP32 e tente novamente.") }
        main.postDelayed(timeout!!, 15_000)
        Thread({
            try {
                val candidate = device.createRfcommSocketToServiceRecord(SPP_UUID)
                synchronized(this) {
                    if (token != generation || closed) { candidate.close(); return@Thread }
                    socket = candidate
                }
                adapter.cancelDiscovery()
                candidate.connect()
                synchronized(this) {
                    if (token != generation || closed) return@Thread
                    timeout?.let(main::removeCallbacks)
                    timeout = null
                    state = State.CONNECTED
                    emit("Canal SPP conectado.")
                }
                val buffer = ByteArray(256)
                while (true) {
                    val count = candidate.inputStream.read(buffer)
                    if (count < 0) throw IOException("O dispositivo encerrou o canal")
                    synchronized(this) {
                        if (token != generation || closed) return@Thread
                        if (count > 0) emit("RX: " + buffer.take(count).joinToString(" ") { "%02X".format(it.toInt() and 255) })
                    }
                }
            } catch (error: Exception) {
                fail(token, "Falha na conexão/leitura: ${error.javaClass.simpleName}: ${error.message}")
            }
        }, "robot-rfcomm").start()
    }

    @Synchronized fun send(value: Int, onComplete: (Boolean) -> Unit = {}) {
        require(value in 0..255)
        if (closed || state != State.CONNECTED || writing) { onComplete(false); return }
        val active = socket ?: run { onComplete(false); return }
        val token = generation
        writing = true
        val writeTimeout = Runnable { fail(token, "Envio bloqueado por mais de 2 s; conexão encerrada.") }
        main.postDelayed(writeTimeout, 2_000)
        writer.execute {
            var success = false
            try {
                synchronized(this) { if (token != generation || closed) return@execute }
                active.outputStream.write(value) // Exactly one binary byte, including NUL.
                success = true
                synchronized(this) {
                    if (token == generation && !closed) emit("TX: 0x%02X — 1 byte escrito; recebimento no ESP32 ainda não confirmado.".format(value))
                }
            } catch (error: Exception) {
                fail(token, "Falha no envio: ${error.javaClass.simpleName}: ${error.message}")
            } finally {
                main.removeCallbacks(writeTimeout)
                synchronized(this) { if (token == generation) writing = false }
                main.post {
                    synchronized(this) {
                        if (!closed && token == generation) onComplete(success)
                    }
                }
            }
        }
    }

    @Synchronized private fun fail(token: Int, message: String) {
        if (token == generation && !closed) disconnect(message)
    }

    @Synchronized fun disconnect(message: String = "Desconectado pelo usuário.") {
        generation++ // Invalidates late connections and queued writes from previous sessions.
        timeout?.let(main::removeCallbacks)
        timeout = null
        try { socket?.close() } catch (_: IOException) { }
        socket = null
        writing = false
        state = State.DISCONNECTED
        emit(message)
    }

    @Synchronized fun close() {
        disconnect()
        closed = true
        writer.shutdownNow()
        main.removeCallbacksAndMessages(null)
    }

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
