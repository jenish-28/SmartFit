package com.example.smartfit.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import java.util.LinkedList
import kotlin.math.max

/**
 * A lightweight custom view that draws a live line graph of recent
 * movement-magnitude values. Implemented manually with Canvas/Paint/Path
 * instead of a charting library, per the coursework requirement.
 */
class MovementGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val values = LinkedList<Float>()
    private var maxValues = 60

    /** The graph never scales below this ceiling; it auto-expands above it. */
    var minScaleCeiling = 5f

    private val density = resources.displayMetrics.density
    private val lineColor = Color.parseColor("#C3F400")

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = lineColor
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1FFFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }

    private val pipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val path = Path()
    private val fillPath = Path()

    private var maxObservedValue = minScaleCeiling
    private var gradientCachedHeight = -1

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    /** Adds a new movement-magnitude reading and redraws the graph. */
    fun addValue(value: Float) {
        values.addLast(value)
        while (values.size > maxValues) {
            values.removeFirst()
        }
        maxObservedValue = max(minScaleCeiling, max(maxObservedValue * 0.98f, values.maxOrNull() ?: minScaleCeiling))
        invalidate()
    }

    fun clear() {
        values.clear()
        maxObservedValue = minScaleCeiling
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        if (width <= 0f || height <= 0f) return

        if (gradientCachedHeight != height.toInt()) {
            fillPaint.shader = LinearGradient(
                0f, 0f, 0f, height,
                Color.argb(90, 195, 244, 0), Color.argb(0, 195, 244, 0),
                Shader.TileMode.CLAMP
            )
            gradientCachedHeight = height.toInt()
        }

        val rowCount = 4
        for (i in 0..rowCount) {
            val y = height / rowCount * i
            canvas.drawLine(0f, y, width, y, gridPaint)
        }

        if (values.size < 2) return

        path.reset()
        fillPath.reset()

        val step = width / (maxValues - 1).coerceAtLeast(1)
        val startIndex = maxValues - values.size

        values.forEachIndexed { index, value ->
            val x = (startIndex + index) * step
            val normalized = (value / maxObservedValue).coerceIn(0f, 1f)
            val y = height - (normalized * height)

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        val lastX = (startIndex + values.size - 1) * step
        fillPath.lineTo(lastX, height)
        fillPath.close()

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)

        val lastValue = values.last()
        val lastNormalized = (lastValue / maxObservedValue).coerceIn(0f, 1f)
        val lastY = height - (lastNormalized * height)
        pipPaint.setShadowLayer(10f, 0f, 0f, lineColor)
        canvas.drawCircle(lastX, lastY, 4.5f * density, pipPaint)
    }
}
