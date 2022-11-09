package com.gallery.edit

import android.content.res.Resources
import android.util.TypedValue

/**
 * Convert Dp to Float
 * ex. 5F.sp
 */
val Float.dp: Float
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        Resources.getSystem().displayMetrics
    )

/**
 * Convert Dp to Int
 * ex. 5.dp
 */
val Int.dp: Int
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    ).toInt()
