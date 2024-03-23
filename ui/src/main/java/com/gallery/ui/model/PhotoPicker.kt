package com.gallery.ui.model

import android.provider.MediaStore
import com.gallery.core.model.GalleryData

/**
 * Description : PhotoPicker 전용 클래스
 *
 * Created by juhongmin on 3/23/24
 */
internal sealed interface PhotoPicker {

    object Camera : PhotoPicker

    /**
     * Photo Data Model
     * @param imagePath Photo Image Url
     * @param isSelected Selected
     * @param selectedNum Selected Number
     * @param albumName Album Name
     * @param rotation Photo Rotation 0, 90, 180, 270
     */
    data class Photo(
        val id: Long,
        val imagePath: String,
        var isSelected: Boolean = false,
        var selectedNum: String = "1",
        val albumName: String,
        val rotation: Int = 0
    ) : PhotoPicker {
        constructor(
            data: GalleryData
        ) : this(
            id = data.id,
            imagePath = data.uri.toString(),
            isSelected = false,
            selectedNum = "1",
            albumName = data.getField(MediaStore.Images.Media.BUCKET_DISPLAY_NAME) ?: ""
        )
    }
}
