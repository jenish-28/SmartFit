package com.example.smartfit.view

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * A radial arc gauge (e.g. "115 / 150 min" weekly goal, or a live sensor
 * magnitude dial). Draws a background track and a gradient progress arc
 * with Canvas/Paint, in the spirit of [MovementGraphView].
 */
class GaugeArcView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** 0f..1f */
    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var startAngle: Float = 180f
        set(value) { field = value; invalidate() }

    var sweepAngle: Float = 180f
        set(value) { field = value; invalidate() }

    var trackColor: Int = Color.DKGRAY
        set(value) { field = value; trackPaint.color = value; invalidate() }

    var gradientStartColor: Int = Color.CYAN
        set(value) { field = value; requestLayout(); invalidate() }

    var gradientEndColor: Int = Color.GREEN
        set(value) { field = value; requestLayout(); invalidate() }

    private val strokeWidthPx = 14f * resources.displayMetrics.density

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
        color = trackColor
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val arcBounds = RectF()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val inset = strokeWidthPx / 2f + dotPaint.strokeWidth
        arcBounds.set(inset, inset, w - inset, h - inset)
        updateGradient()
    }

    private fun updateGradient() {
        if (arcBounds.width() <= 0f || arcBounds.height() <= 0f) return
        val startRad = Math.toRadians(startAngle.toDouble())
        val endRad = Math.toRadians((startAngle + sweepAngle).toDouble())
        val cx = arcBounds.centerX()
        val cy = arcBounds.centerY()
        val rx = arcBounds.width() / 2f
        val ry = arcBounds.height() / 2f
        val x0 = cx + rx * cos(startRad).toFloat()
        val y0 = cy + ry * sin(startRad).toFloat()
        val x1 = cx + rx * cos(endRad).toFloat()
        val y1 = cy + ry * sin(endRad).toFloat()
        progressPaint.shader = LinearGradient(
            x0, y0, x1, y1,
            gradientStartColor, gradientEndColor,
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        if (arcBounds.width() <= 0f) return

        canvas.drawArc(arcBounds, startAngle, sweepAngle, false, trackPaint)

        val sweep = sweepAngle * progress
        if (sweep > 0f) {
            canvas.drawArc(arcBounds, startAngle, sweep, false, progressPaint)

            val endAngleRad = Math.toRadians((startAngle + sweep).toDouble())
            val dotX = arcBounds.centerX() + (arcBounds.width() / 2f) * cos(endAngleRad).toFloat()
            val dotY = arcBounds.centerY() + (arcBounds.height() / 2f) * sin(endAngleRad).toFloat()
            dotPaint.setShadowLayer(16f, 0f, 0f, gradientEndColor)
            canvas.drawCircle(dotX, dotY, strokeWidthPx / 2.6f, dotPaint)
        }
    }
}
