package com.gallery.ui.internal

import com.bumptech.glide.RequestManager
import com.gallery.ui.model.PhotoPicker
import kotlinx.coroutines.CoroutineScope

/**
 * Description : PhotoPicker 전용 Adapter 에 필요한 리스너
 *
 * Created by juhongmin on 3/29/24
 */
interface PhotoPickerBridgeListener {

    /**
     * Glide ImageManager
     */
    fun getRequestManager(): RequestManager

    fun getCoroutineScope(): CoroutineScope

    /**
     * 선택 사진
     * @param pos 선택한 위치값
     * @param item
     */
    fun addPicker(pos: Int, item: PhotoPicker)

    /**
     * 선택 해제 사진
     * @param pos 선택한 위치값
     * @param item
     */
    fun removePicker(pos: Int, item: PhotoPicker)
}