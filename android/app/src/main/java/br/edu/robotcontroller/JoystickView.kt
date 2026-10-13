package br.edu.robotcontroller

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View

/** Each view captures its own pointer id, allowing two simultaneous independent fingers. */
class JoystickView @JvmOverloads constructor(context: Context, private val vertical: Boolean = true, private val title: String = "Movimento") : View(context) {
    var onValue: (Float) -> Unit = {}
    var onEngaged: (Boolean) -> Unit = {}
    private var pointer = -1
    private var value = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    init { contentDescription = title; isClickable = true }
    fun reset() {
        val wasEngaged = pointer != -1
        pointer = -1; value = 0f; invalidate()
        if (wasEngaged) onEngaged(false)
    }
    private fun geometry(): JoystickGeometry {
        paint.textSize = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)
        val labelHeight = paint.fontMetrics.descent - paint.fontMetrics.ascent + 12 * resources.displayMetrics.density
        return JoystickGeometry.fit(width.toFloat(), height.toFloat(), labelHeight, 8 * resources.displayMetrics.density)
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val geometry = geometry()
        val cx = geometry.centerX; val cy = geometry.centerY; val r = geometry.radius
        paint.style = Paint.Style.FILL
        paint.color = context.getColor(R.color.robot_surface)
        canvas.drawCircle(cx, cy, r, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = resources.getDimension(R.dimen.robot_joystick_border)
        paint.color = context.getColor(if (isEnabled) R.color.robot_ring else R.color.robot_secondary)
        canvas.drawCircle(cx, cy, (r - paint.strokeWidth / 2f).coerceAtLeast(0f), paint)
        paint.color = context.getColor(if (isEnabled) R.color.robot_blue else R.color.robot_secondary)
        paint.strokeWidth = resources.getDimension(R.dimen.robot_axis_width)
        val axisRadius = (r - resources.getDimension(R.dimen.robot_joystick_border)).coerceAtLeast(0f)
        if (vertical) canvas.drawLine(cx, cy-axisRadius, cx, cy+axisRadius, paint)
        else canvas.drawLine(cx-axisRadius, cy, cx+axisRadius, cy, paint)
        paint.style = Paint.Style.FILL
        paint.color = context.getColor(if (isEnabled) R.color.robot_blue else R.color.robot_secondary)
        canvas.drawCircle(cx + if (vertical) 0f else value*geometry.travel,
            cy - if (vertical) value*geometry.travel else 0f, geometry.thumbRadius, paint)
        paint.color = context.getColor(R.color.robot_muted)
        paint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        paint.textSize = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics); paint.textAlign = Paint.Align.CENTER
        canvas.drawText(title, cx, height - 8 * resources.displayMetrics.density, paint)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { pointer = event.getPointerId(0); onEngaged(true); update(event, 0) }
            MotionEvent.ACTION_MOVE -> { val index = event.findPointerIndex(pointer); if (index >= 0) update(event, index) }
            MotionEvent.ACTION_UP -> { release(); performClick() }
            MotionEvent.ACTION_CANCEL -> release()
            MotionEvent.ACTION_POINTER_UP -> if (event.getPointerId(event.actionIndex) == pointer) release()
        }
        return true
    }
    private fun update(event: MotionEvent, index: Int) {
        val geometry = geometry()
        val travel = geometry.travel.coerceAtLeast(1f)
        value = (if (vertical) (geometry.centerY-event.getY(index))/travel
            else (event.getX(index)-geometry.centerX)/travel).coerceIn(-1f, 1f)
        invalidate(); onValue(value)
    }
    private fun release() { reset(); onValue(0f) }
    override fun performClick(): Boolean { super.performClick(); return true }
}
