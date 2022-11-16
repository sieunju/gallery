package com.gallery.edit.internal

import android.content.res.Resources
import android.graphics.Rect
import android.graphics.RectF
import android.util.TypedValue
import kotlin.math.max
import kotlin.math.min

/**
 * Convert Dp to Float
 * ex. 5F.sp
 */
internal val Float.dp: Float
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        Resources.getSystem().displayMetrics
    )

/**
 * Convert Dp to Int
 * ex. 5.dp
 */
internal val Int.dp: Int
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    ).toInt()

/**
 * Reusable rectangle for general internal usage
 */
internal val RECT = RectF()

/**
 * Reusable point for general internal usage
 */
internal val POINTS = FloatArray(6)

/**
 * Reusable point for general internal usage
 */
internal val POINTS2 = FloatArray(6)

internal val EMPTY_RECT = Rect()

internal val EMPTY_RECT_F = RectF()

/**
 * Get left value of the bounding rectangle of the given points.
 */
internal fun getRectLeft(points: FloatArray): Float {
    return min(min(min(points[0], points[2]), points[4]), points[6])
}

/**
 * Get top value of the bounding rectangle of the given points.
 */
internal fun getRectTop(points: FloatArray): Float {
    return min(min(min(points[1], points[3]), points[5]), points[7])
}

/**
 * Get right value of the bounding rectangle of the given points.
 */
internal fun getRectRight(points: FloatArray): Float {
    return max(max(max(points[0], points[2]), points[4]), points[6])
}

/**
 * Get bottom value of the bounding rectangle of the given points.
 */
internal fun getRectBottom(points: FloatArray): Float {
    return max(max(max(points[1], points[3]), points[5]), points[7])
}

/**
 * Get width of the bounding rectangle of the given points.
 */
internal fun getRectWidth(points: FloatArray): Float {
    return getRectRight(points) - getRectLeft(points)
}

/**
 * Get height of the bounding rectangle of the given points.
 */
internal fun getRectHeight(points: FloatArray): Float {
    return getRectBottom(points) - getRectTop(points)
}

/**
 * Get horizontal center value of the bounding rectangle of the given points.
 */
internal fun getRectCenterX(points: FloatArray): Float {
    return (getRectRight(points) + getRectLeft(points)) / 2f
}

/**
 * Get vertical center value of the bounding rectangle of the given points.
 */
internal fun getRectCenterY(points: FloatArray): Float {
    return (getRectBottom(points) + getRectTop(points)) / 2f
}
