package com.gallery.ui.internal.listener

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

    /**
     * 이전에 선택한 사진인지 체크하여 값을 변경처리하는 함수
     * @param item 비교하고자 하는 사진
     */
    fun setSelectedGallery(item: PhotoPicker)

    fun onShowExpandPhoto(item: PhotoPicker)
}