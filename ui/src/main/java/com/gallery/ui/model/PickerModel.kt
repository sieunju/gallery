package com.gallery.ui.model

import android.net.Uri

/**
 * Description : 선택한 아이템
 *
 * Created by juhongmin on 2024. 9. 11.
 */
sealed interface PickerModel {
    data class Camera(
        val uri: Uri
    ) : PickerModel

    data class Video(
        val uri: Uri
    ) : PickerModel

    data class Photo(
        val uri: Uri
    ) : PickerModel

    companion object {
        fun PhotoPicker.toModel(): PickerModel? {
            return when (this) {
                is PhotoPicker.Photo -> Photo(contentUri)
                is PhotoPicker.Video -> Video(contentUri)
                else -> null
            }
        }
    }
}