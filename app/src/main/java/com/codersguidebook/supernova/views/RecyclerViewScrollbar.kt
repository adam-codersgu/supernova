package com.codersguidebook.supernova.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.codersguidebook.supernova.R

class RecyclerViewScrollbar(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val thumb = ContextCompat.getDrawable(context, R.drawable.thumb_drawable)
    private val track = ContextCompat.getDrawable(context, R.drawable.line_drawable)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply { track?.draw(this) }
    }

    override fun getLayoutParams(): ViewGroup.LayoutParams {
        return super.getLayoutParams().apply {
            width = 50
        }
    }
}