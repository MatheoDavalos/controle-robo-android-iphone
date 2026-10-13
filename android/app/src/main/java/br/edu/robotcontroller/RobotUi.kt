package br.edu.robotcontroller

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.util.TypedValue
import android.widget.Button
import android.widget.TextView

/** Shared visual styles only; no connection or control state is owned here. */
object RobotUi {
    enum class Action { PRIMARY, CONNECT, DISCONNECT, SECONDARY }
    fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density).toInt()
    fun fitText(view: TextView, maxSp: Int = 14, lines: Int = 1) {
        view.maxLines = lines
        view.ellipsize = null
        view.setHorizontallyScrolling(false)
        view.includeFontPadding = false
        val metrics = view.resources.displayMetrics
        view.setAutoSizeTextTypeUniformWithConfiguration(
            (8 * metrics.density).toInt().coerceAtLeast(1),
            (maxSp * metrics.scaledDensity).toInt().coerceAtLeast((8 * metrics.density).toInt() + 1),
            1, TypedValue.COMPLEX_UNIT_PX)
    }
    fun label(context: Context, text: String, size: Float = 14f, muted: Boolean = false) = TextView(context).apply {
        this.text = text
        textSize = size
        setTextColor(context.getColor(if (muted) R.color.robot_muted else R.color.robot_text))
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    fun surface(context: Context) = shape(context, R.color.robot_surface, R.color.robot_surface, R.color.robot_outline)
    private fun shape(context: Context, start: Int, end: Int, border: Int) = GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(context.getColor(start), context.getColor(end))
    ).apply {
        cornerRadius = context.resources.getDimension(R.dimen.robot_corner)
        setStroke(dp(context, 1).coerceAtLeast(1), context.getColor(border))
    }
    fun style(button: Button, action: Action) {
        val context = button.context
        val (start, end, border) = when (action) {
            Action.PRIMARY -> Triple(R.color.robot_blue_start, R.color.robot_blue, R.color.robot_outline)
            Action.CONNECT -> Triple(R.color.robot_green_start, R.color.robot_green_end, R.color.robot_outline)
            Action.DISCONNECT -> Triple(R.color.robot_red_start, R.color.robot_red_end, R.color.robot_disconnected)
            Action.SECONDARY -> Triple(R.color.robot_secondary, R.color.robot_secondary, R.color.robot_secondary)
        }
        val states = StateListDrawable().apply {
            addState(intArrayOf(-android.R.attr.state_enabled), shape(context, R.color.robot_secondary, R.color.robot_secondary, R.color.robot_secondary))
            addState(intArrayOf(), shape(context, start, end, border))
        }
        button.backgroundTintList = null
        button.background = RippleDrawable(ColorStateList.valueOf(context.getColor(R.color.robot_ripple)), states, null)
        button.setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(context.getColor(R.color.robot_disabled_text), context.getColor(R.color.robot_text))))
        button.textSize = 14f
        button.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        button.isAllCaps = true
        button.gravity = Gravity.CENTER
        button.minWidth = 0
        button.minimumWidth = 0
        button.minHeight = 0
        button.minimumHeight = 0
        button.setPadding(dp(context, 10), 0, dp(context, 10), 0)
        button.stateListAnimator = null
        fitText(button)
    }
    fun button(context: Context, text: String, action: Action, click: () -> Unit) = Button(context).apply {
        this.text = text
        style(this, action)
        setOnClickListener { click() }
    }
}
