package com.gallery.ui

import com.gallery.model.GalleryItem

/**
 * Description : GalleryRecyclerView Listener
 *
 * Created by juhongmin on 2022/11/26
 */
interface GalleryListener {

    fun onCameraOpen()

    /**
     * Photo Picker Callback
     * @param item Current GalleryItem
     * @param isAdd true Picker, false No Picker
     */
    fun onPhotoPicker(item: GalleryItem, isAdd: Boolean)

    /**
     * Picker Max Count Callback
     */
    fun onMaxPickerCount()

}
