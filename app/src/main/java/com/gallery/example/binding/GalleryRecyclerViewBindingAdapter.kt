package com.gallery.example.binding

import android.database.Cursor
import android.graphics.Bitmap
import androidx.databinding.BindingAdapter
import com.gallery.edit.CropImageEditView
import com.gallery.model.GalleryItem
import com.gallery.ui.GalleryListener
import com.gallery.ui.GalleryRecyclerView

/**
 * Description : Gallery RecyclerView Binding Adapter
 *
 * Created by juhongmin on 2022/11/30
 */
internal object GalleryRecyclerViewBindingAdapter {
    interface GalleryCameraOpenListener {
        fun callback()
    }

    interface GalleryPhotoClickListener {
        fun callback(item: GalleryItem, isAdd: Boolean)
    }

    interface GalleryMaxClickListener {
        fun callback()
    }

    /**
     * GalleryRecyclerView setListener
     * DataBinding Example
     *
     */
    @JvmStatic
    @BindingAdapter(
        "onGalleryCameraOpen",
        "onGalleryPhotoClick",
        "onGalleryMaxPicker", requireAll = false
    )
    fun GalleryRecyclerView.setGalleryRecyclerViewListener(
        cameraOpen: GalleryCameraOpenListener? = null,
        photoClick: GalleryPhotoClickListener? = null,
        maxClick: GalleryMaxClickListener? = null
    ) {
        setListener(object : GalleryListener {
            override fun onCameraOpen() {
                cameraOpen?.callback()
            }

            override fun onPhotoPicker(item: GalleryItem, isAdd: Boolean) {
                photoClick?.callback(item, isAdd)
            }

            override fun onMaxPickerCount() {
                maxClick?.callback()
            }
        })
    }

    /**
     * GalleryRecyclerView Set Cusor
     * DataBinding Example
     */
    @JvmStatic
    @BindingAdapter("cursor")
    fun setGalleryCursor(
        view: GalleryRecyclerView,
        cursor: Cursor?
    ) {
        view.setCursor(cursor)
    }

    @JvmStatic
    @BindingAdapter("setCropImageEditBitmap")
    fun setCropImageEditBitmap(
        view: CropImageEditView,
        bitmap: Bitmap?
    ) {
        view.setImageBitmap(bitmap)
    }
}
