package br.edu.robotcontroller

import android.content.Context
import android.view.View
import android.view.ViewGroup
import kotlin.math.min
import kotlin.math.roundToInt

/** Fits the complete setup step into the available window, without a ScrollView. */
class ConnectionStepLayout(context: Context) : ViewGroup(context) {
    private data class Row(val view: View, val heightDp: Int, val gapDp: Int)
    private val rows = mutableListOf<Row>()
    private var inset = 0
    private var top = 0
    private var scale = 1f

    fun row(view: View, heightDp: Int, gapDp: Int = 0) {
        rows += Row(view, heightDp, gapDp)
        addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val visible = rows.filter { it.view.visibility != GONE }
        val ideal = RobotUi.dp(context, 36 + visible.sumOf { it.heightDp + it.gapDp })
        val height = resolveSize(ideal, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        scale = min(1f, height.toFloat() / ideal.coerceAtLeast(1))
        inset = (width * 0.05f).roundToInt()
        top = (RobotUi.dp(context, 24) * scale).roundToInt()
        visible.forEach { row ->
            row.view.measure(MeasureSpec.makeMeasureSpec((width - 2 * inset).coerceAtLeast(0), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec((RobotUi.dp(context, row.heightDp) * scale).toInt(), MeasureSpec.EXACTLY))
        }
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var y = top
        rows.filter { it.view.visibility != GONE }.forEach { row ->
            y += (RobotUi.dp(context, row.gapDp) * scale).toInt()
            row.view.layout(inset, y, width - inset, y + row.view.measuredHeight)
            y += row.view.measuredHeight
        }
    }
}
