package com.gallery.example.binding

import android.graphics.Bitmap
import androidx.databinding.BindingAdapter
import com.gallery.edit.FlexibleImageEditListener
import com.gallery.edit.FlexibleImageEditView
import com.gallery.edit.detector.FlexibleStateItem

/**
 * Description : FlexibleImageEidtView Binding Adapter
 *
 * Created by juhongmin on 2022/12/03
 */
internal object FlexibleImageEditBindingAdapter {

    interface FlexibleStateUpdateListener {
        fun callback(newItem: FlexibleStateItem)
    }

    @JvmStatic
    @BindingAdapter("imageBitmap", "stateItem", requireAll = false)
    fun setFlexibleEditImageBitmap(
        view: FlexibleImageEditView,
        bitmap: Bitmap?,
        stateItem: FlexibleStateItem?
    ) {
        if (stateItem == null) {
            view.loadBitmap(bitmap)
        } else {
            view.loadBitmap(bitmap, stateItem)
        }
    }

    @JvmStatic
    @BindingAdapter("onStateUpdated", requireAll = false)
    fun setFlexibleEditImageListener(
        view: FlexibleImageEditView,
        updateStateListener: FlexibleStateUpdateListener?
    ) {
        view.listener = object : FlexibleImageEditListener {
            override fun onUpdateStateItem(newItem: FlexibleStateItem) {
                try {
                    updateStateListener?.callback(newItem)
                } catch (ex: Exception) {

                }
            }
        }
    }
}
