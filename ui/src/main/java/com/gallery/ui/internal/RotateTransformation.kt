package com.gallery.ui.internal

import android.graphics.Bitmap
import android.graphics.Matrix
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest


/**
 * Description : Glide Image Loader 에서 회전 처리 해주는 transformation
 *
 * Created by juhongmin on 2023/05/11
 */
internal class RotateTransformation(
    private val rotateRotationAngle: Float
) : BitmapTransformation() {

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("rotate$rotateRotationAngle".toByteArray())
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotateRotationAngle)
        return Bitmap.createBitmap(
            toTransform,
            0,
            0,
            toTransform.width,
            toTransform.height,
            matrix,
            true
        )
    }
}