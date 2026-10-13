package br.edu.robotcontroller

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import android.widget.FrameLayout
import android.view.Gravity
import android.view.View

class ControlPanel @JvmOverloads constructor(context: Context, fillSpace: Boolean = false, private val diagnostics: Boolean = true) : LinearLayout(context) {
    var onInput: (Float, Float) -> Unit = { _, _ -> }
    private var movement = 0f
    private var steering = 0f
    private val info = TextView(context)
    private val move = JoystickView(context, true, "Frente / Trás")
    private val turn = JoystickView(context, false, "Direita / Esquerda")
    private var moveEngaged = false
    private var turnEngaged = false
    init {
        orientation = VERTICAL
        setBackgroundColor(context.getColor(R.color.robot_background))
        val joysticks = LinearLayout(context).apply {
            isMotionEventSplittingEnabled = true
            addView(move, LayoutParams(0, LayoutParams.MATCH_PARENT, 3f))
            val emblem = FrameLayout(context).apply {
                addView(ImageView(context).apply {
                    setImageResource(R.drawable.ic_control_robot)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    val inset = RobotUi.dp(context, 8)
                    setPadding(inset, inset, inset, inset)
                }, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, RobotUi.dp(context, 128), Gravity.CENTER))
            }
            addView(emblem, LayoutParams(0, LayoutParams.MATCH_PARENT, 1.4f))
            addView(turn, LayoutParams(0, LayoutParams.MATCH_PARENT, 3f))
        }
        addView(joysticks, if (fillSpace) LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
            else LayoutParams(LayoutParams.MATCH_PARENT, (260 * resources.displayMetrics.density).toInt()))
        info.gravity = android.view.Gravity.CENTER
        info.setTextColor(context.getColor(R.color.robot_muted))
        info.textSize = 12f
        if (diagnostics) addView(info)
        move.onEngaged = { moveEngaged = it; updateInterception() }
        turn.onEngaged = { turnEngaged = it; updateInterception() }
        move.onValue = { movement = it; changed() }
        turn.onValue = { steering = it; changed() }
        changed()
    }
    private fun updateInterception() { requestDisallowInterceptTouchEvent(moveEngaged || turnEngaged) }
    private fun changed() {
        if (diagnostics) info.text = RobotProtocol.describe(movement, steering)
        onInput(movement, steering)
    }
    fun reset() { movement = 0f; steering = 0f; move.reset(); turn.reset(); changed() }
    fun controlsEnabled(enabled: Boolean) { move.isEnabled = enabled; turn.isEnabled = enabled; if (!enabled) reset(); move.invalidate(); turn.invalidate() }
}
