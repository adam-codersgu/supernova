package com.codersguidebook.supernova.utils

import android.content.Context
import android.util.TypedValue

/** A helper for dimension conversions and calculations */
object DimensionsHelper {

    /**
     * Convert a given width value to density-independent pixels.
     *
     * @param context The context in which the display metrics should be retrieved.
     * @param width The width to be converted to dp.
     * @return The width as an Integer in dp format.
     */
    fun convertWidthToDp(context: Context, width: Float): Int {
        val widthDp = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            width, context.resources.displayMetrics)
        return widthDp.toInt()
    }
}