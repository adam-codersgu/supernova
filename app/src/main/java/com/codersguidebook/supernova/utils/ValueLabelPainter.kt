package com.codersguidebook.supernova.utils

import android.graphics.Canvas
import androidx.annotation.ColorInt
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter

class ValueLabelPainter(
    @ColorInt private val color: Int,
    override val intrinsicSize: Size
) : Painter() {

    override fun DrawScope.onDraw() {
        TODO("Not yet implemented")
    }

    fun paint(canvas: Canvas) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()

        val shapeBounds = RectFFactory.fromLTWH(0f, 0f, width, height)

        val paint = Paint()
        paint.color = color

        canvas.drawRect(shapeBounds, paint)
    }
}