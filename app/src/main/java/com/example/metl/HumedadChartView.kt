package com.example.metl

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class HumedadChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private var valores: List<Int> = emptyList()
    private val dp = resources.displayMetrics.density
    private val path = Path()

    private val linea = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.green)
        style = Paint.Style.STROKE
        strokeWidth = 3 * dp
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val base = Paint().apply {
        color = context.getColor(R.color.line)
        strokeWidth = 1 * dp
    }

    fun setValores(v: List<Int>) {
        valores = v
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (valores.size < 2) return
        val w = width.toFloat()
        val h = height.toFloat()
        val paso = w / (valores.size - 1)
        val alto = h * 0.9f
        path.reset()
        valores.forEachIndexed { i, v ->
            val x = i * paso
            val y = alto * (1f - v.coerceIn(0, 100) / 100f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, linea)
        canvas.drawLine(0f, h - 2f, w, h - 2f, base)
    }
}