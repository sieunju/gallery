package com.gallery.model

import android.graphics.Bitmap

/**
 * Description : @see [CropImageEditView] 에 관련된 데이터 모델
 *
 * Created by juhongmin on 2022/11/13
 */
data class CropImageEditModel(
    val bitmap: Bitmap?,
    val points: FloatArray,
    val degreesRotated: Int,
    val fixAspectRatio: Boolean,
    val aspectRatioX: Int,
    val aspectRatioY: Int,
    val flipHorizontally: Boolean,
    val flipVertically: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CropImageEditModel

        if (bitmap != other.bitmap) return false
        if (!points.contentEquals(other.points)) return false
        if (degreesRotated != other.degreesRotated) return false
        if (fixAspectRatio != other.fixAspectRatio) return false
        if (aspectRatioX != other.aspectRatioX) return false
        if (aspectRatioY != other.aspectRatioY) return false
        if (flipHorizontally != other.flipHorizontally) return false
        if (flipVertically != other.flipVertically) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bitmap?.hashCode() ?: 0
        result = 31 * result + points.contentHashCode()
        result = 31 * result + degreesRotated
        result = 31 * result + fixAspectRatio.hashCode()
        result = 31 * result + aspectRatioX
        result = 31 * result + aspectRatioY
        result = 31 * result + flipHorizontally.hashCode()
        result = 31 * result + flipVertically.hashCode()
        return result
    }
}
