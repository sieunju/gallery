package com.gallery.example.binding

import android.graphics.Bitmap
import androidx.databinding.BindingAdapter
import com.gallery.edit.CropImageEditView

/**
 * Description :
 *
 * Created by juhongmin on 2022/12/12
 */
internal object CropImageEditBindingAdapter {
    @JvmStatic
    @BindingAdapter("imageBitmap")
    fun setCropImageEditBitmap(
        view: CropImageEditView,
        bitmap: Bitmap?
    ) {
        view.setImageBitmap(bitmap)
    }

    @JvmStatic
    @BindingAdapter("rotate")
    fun setCropImageEditRotate(
        view: CropImageEditView,
        rotate: Int?
    ) {
        if (rotate == null) return
        view.rotatedDegrees = rotate
    }
}
