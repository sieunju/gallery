package com.gallery.ui

import com.gallery.model.GalleryItem

/**
 * Description : GalleryRecyclerView Listener
 *
 * Created by juhongmin on 2022/11/26
 */
interface GalleryListener {

    /**
     * Camera Open Click
     */
    fun onCameraOpen()

    /**
     * Photo Picker Callback
     * @param item Click GalleryItem
     * @param isAdd true Picker, false No Picker
     */
    fun onPhotoPicker(item: GalleryItem, isAdd: Boolean)

    /**
     * Picker Max Count Callback
     */
    fun onMaxPickerCount()

    /**
     * Listener that can be cleared if clicked in duplicate.
     * @param item Click GalleryItem
     * @return true SamePhoto removeGallery
     */
    fun isCurrentPhoto(item: GalleryItem): Boolean
}
