package com.codersguidebook.supernova.utils

import android.content.Context
import android.util.TypedValue

/** A helper for dimension conversions and calculations */
object DimensionsHelper {

    /**
     * Convert a given numerical value to density-independent pixels.
     *
     * @param context The context in which the display metrics should be retrieved.
     * @param width The number to be converted to dp.
     * @return The number as an Integer in dp format.
     */
    fun convertToDp(context: Context, width: Float): Int {
        val widthDp = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            width, context.resources.displayMetrics)
        return widthDp.toInt()
    }
}