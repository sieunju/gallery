package com.gallery.core

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import com.gallery.model.RequestSizeOptions
import kotlin.math.*

/**
 * Description : CropImageEditView 관련 utils Class
 *
 * Created by juhongmin on 2023/04/03
 */
internal object CropImageEditExtensions {
    /**
     * Get a rectangle for the given 4 points (x0,y0,x1,y1,x2,y2,x3,y3) by finding the min/max 2
     * points that contains the given 4 points and is a straight rectangle.
     */
    private fun getRectFromPoints(
        points: FloatArray,
        imageWidth: Int,
        imageHeight: Int,
        fixAspectRatio: Boolean,
        aspectRatioX: Int,
        aspectRatioY: Int
    ): Rect {
        val left = max(0f, getRectLeft(points)).roundToInt()
        val top = max(0f, getRectTop(points)).roundToInt()
        val right = min(imageWidth.toFloat(), getRectRight(points)).roundToInt()
        val bottom = min(imageHeight.toFloat(), getRectBottom(points)).roundToInt()
        val rect = Rect(left, top, right, bottom)
        if (fixAspectRatio) {
            fixRectForAspectRatio(rect, aspectRatioX, aspectRatioY)
        }
        return rect
    }

    /**
     * Get left value of the bounding rectangle of the given points.
     */
    private fun getRectLeft(points: FloatArray): Float {
        return min(min(min(points[0], points[2]), points[4]), points[6])
    }

    /**
     * Get top value of the bounding rectangle of the given points.
     */
    private fun getRectTop(points: FloatArray): Float {
        return min(min(min(points[1], points[3]), points[5]), points[7])
    }

    /**
     * Get right value of the bounding rectangle of the given points.
     */
    private fun getRectRight(points: FloatArray): Float {
        return max(max(max(points[0], points[2]), points[4]), points[6])
    }

    /**
     * Get bottom value of the bounding rectangle of the given points.
     */
    private fun getRectBottom(points: FloatArray): Float {
        return max(max(max(points[1], points[3]), points[5]), points[7])
    }

    /**
     * Get width of the bounding rectangle of the given points.
     */
    private fun getRectWidth(points: FloatArray): Float {
        return getRectRight(points) - getRectLeft(points)
    }

    /**
     * Get height of the bounding rectangle of the given points.
     */
    private fun getRectHeight(points: FloatArray): Float {
        return getRectBottom(points) - getRectTop(points)
    }

    /**
     * Get horizontal center value of the bounding rectangle of the given points.
     */
    private fun getRectCenterX(points: FloatArray): Float {
        return (getRectRight(points) + getRectLeft(points)) / 2f
    }

    /**
     * Get vertical center value of the bounding rectangle of the given points.
     */
    private fun getRectCenterY(points: FloatArray): Float {
        return (getRectBottom(points) + getRectTop(points)) / 2f
    }

    /**
     * Fix the given rectangle if it doesn't confirm to aspect ration rule.<br></br>
     * Make sure that width and height are equal if 1:1 fixed aspect ratio is requested.
     */
    private fun fixRectForAspectRatio(rect: Rect, aspectRatioX: Int, aspectRatioY: Int) {
        if (aspectRatioX == aspectRatioY && rect.width() != rect.height()) {
            if (rect.height() > rect.width()) {
                rect.bottom -= rect.height() - rect.width()
            } else {
                rect.right -= rect.width() - rect.height()
            }
        }
    }

    /**
     * Special crop of bitmap rotated by not stright angle, in this case the original crop bitmap
     * contains parts beyond the required crop area, this method crops the already cropped and rotated
     * bitmap to the final rectangle.<br></br>
     * Note: rotating by 0, 90, 180 or 270 degrees doesn't require extra cropping.
     */
    private fun cropForRotatedImage(
        bitmap: Bitmap?,
        points: FloatArray,
        rect: Rect,
        degreesRotated: Int,
        fixAspectRatio: Boolean,
        aspectRatioX: Int,
        aspectRatioY: Int
    ): Bitmap? {
        var tempBitmap = bitmap
        if (degreesRotated % 90 != 0) {
            var adjLeft = 0
            var adjTop = 0
            var width = 0
            var height = 0
            val rads = Math.toRadians(degreesRotated.toDouble())
            val compareTo =
                if (degreesRotated < 90 || degreesRotated in 181..269) rect.left else rect.right
            var i = 0
            while (i < points.size) {
                if (points[i] >= compareTo - 1 && points[i] <= compareTo + 1) {
                    adjLeft = abs(sin(rads) * (rect.bottom - points[i + 1]))
                        .toInt()
                    adjTop = abs(cos(rads) * (points[i + 1] - rect.top))
                        .toInt()
                    width = abs((points[i + 1] - rect.top) / sin(rads))
                        .toInt()
                    height = abs((rect.bottom - points[i + 1]) / cos(rads))
                        .toInt()
                    break
                }
                i += 2
            }
            rect[adjLeft, adjTop, adjLeft + width] = adjTop + height
            if (fixAspectRatio) {
                fixRectForAspectRatio(rect, aspectRatioX, aspectRatioY)
            }
            val bitmapTmp = tempBitmap
            tempBitmap = Bitmap.createBitmap(
                bitmap!!,
                rect.left,
                rect.top,
                rect.width(),
                rect.height()
            )
            if (bitmapTmp != tempBitmap) {
                bitmapTmp?.recycle()
            }
        }
        return tempBitmap
    }

    /**
     * Crop image bitmap from given bitmap using the given points in the original bitmap and the given
     * rotation.<br></br>
     * if the rotation is not 0,90,180 or 270 degrees then we must first crop a larger area of the
     * image that contains the requires rectangle, rotate and then crop again a sub rectangle.<br></br>
     * If crop fails due to OOM we scale the cropping image by 0.5 every time it fails until it is
     * small enough.
     */
    fun cropBitmapObjectHandleOOM(
        bitmap: Bitmap,
        points: FloatArray,
        degreesRotated: Int,
        fixAspectRatio: Boolean,
        aspectRatioX: Int,
        aspectRatioY: Int,
        flipHorizontally: Boolean,
        flipVertically: Boolean
    ): Bitmap? {
        var scale = 1
        while (true) {
            try {
                return cropBitmapObjectWithScale(
                    bitmap,
                    points,
                    degreesRotated,
                    fixAspectRatio,
                    aspectRatioX,
                    aspectRatioY,
                    1 / scale.toFloat(),
                    flipHorizontally,
                    flipVertically
                )
            } catch (e: OutOfMemoryError) {
                scale *= 2
                if (scale > 8) {
                    throw e
                }
            }
        }
    }

    /**
     * Crop image bitmap from given bitmap using the given points in the original bitmap and the given
     * rotation.<br></br>
     * if the rotation is not 0,90,180 or 270 degrees then we must first crop a larger area of the
     * image that contains the requires rectangle, rotate and then crop again a sub rectangle.
     *
     * @param scale how much to scale the cropped image part, use 0.5 to lower the image by half (OOM
     * handling)
     */
    fun cropBitmapObjectWithScale(
        bitmap: Bitmap,
        points: FloatArray,
        degreesRotated: Int,
        fixAspectRatio: Boolean,
        aspectRatioX: Int,
        aspectRatioY: Int,
        scale: Float,
        flipHorizontally: Boolean,
        flipVertically: Boolean
    ): Bitmap? {
        // get the rectangle in original image that contains the required cropped area (larger for non
        // rectangular crop)
        val rect = getRectFromPoints(
            points,
            bitmap.width,
            bitmap.height,
            fixAspectRatio,
            aspectRatioX,
            aspectRatioY
        )
        // crop and rotate the cropped image in one operation
        val matrix = Matrix()
        matrix.setRotate(degreesRotated.toFloat(), bitmap.width / 2.0f, bitmap.height / 2.0f)
        matrix.postScale(
            if (flipHorizontally) -scale else scale,
            if (flipVertically) -scale else scale
        )
        var result = Bitmap.createBitmap(
            bitmap,
            rect.left,
            rect.top,
            rect.width(),
            rect.height(),
            matrix,
            true
        )
        if (result == bitmap) {
            // corner case when all bitmap is selected, no worth optimizing for it
            result = bitmap.copy(bitmap.config, false)
        }
        // rotating by 0, 90, 180 or 270 degrees doesn't require extra cropping
        if (degreesRotated % 90 != 0) {
            // extra crop because non rectangular crop cannot be done directly on the image without
            // rotating first
            result = cropForRotatedImage(
                result, points, rect, degreesRotated, fixAspectRatio, aspectRatioX, aspectRatioY
            )
        }
        return result
    }

    /**
     * Resize the given bitmap to the given width/height by the given option.<br></br>
     */
    fun resizeBitmap(
        bitmap: Bitmap?,
        reqWidth: Int,
        reqHeight: Int,
        options: RequestSizeOptions
    ): Bitmap {
        try {
            if (reqWidth > 0 &&
                reqHeight > 0 &&
                (options === RequestSizeOptions.RESIZE_FIT ||
                        options === RequestSizeOptions.RESIZE_INSIDE ||
                        options === RequestSizeOptions.RESIZE_EXACT)
            ) {
                var resized: Bitmap? = null
                if (options === RequestSizeOptions.RESIZE_EXACT) {
                    resized = Bitmap.createScaledBitmap(bitmap!!, reqWidth, reqHeight, false)
                } else {
                    val width = bitmap!!.width
                    val height = bitmap.height
                    val scale = max(width / reqWidth.toFloat(), height / reqHeight.toFloat())
                    if (scale > 1 || options === RequestSizeOptions.RESIZE_FIT) {
                        resized = Bitmap.createScaledBitmap(
                            bitmap, (width / scale).toInt(), (height / scale).toInt(), false
                        )
                    }
                }
                if (resized != null) {
                    if (resized != bitmap) {
                        bitmap.recycle()
                    }
                    return resized
                }
            }
        } catch (e: Exception) {
        }
        return bitmap!!
    }
}
