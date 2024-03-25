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
     */
    data class Photo(
        val id: Long,
        val imagePath: String,
        var isSelected: Boolean = false,
        var selectedNum: String = "1",
        val albumName: String
    ) : PhotoPicker {
        constructor(
            data: GalleryData
        ) : this(
            id = data.id,
            imagePath = data.uri.toString(),
            isSelected = false,
            selectedNum = "1",
            albumName = data.getField(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME) ?: ""
        )
    }

    /**
     * Video Data Model
     * @param imagePath Photo Image Url
     * @param isSelected Selected
     * @param selectedNum Selected Number
     * @param albumName Album Name
     * @param duration Video Duration
     */
    data class Video(
        val id: Long,
        val imagePath: String,
        var isSelected: Boolean = false,
        var selectedNum: String = "1",
        val albumName: String,
        val duration: Long,
    ) : PhotoPicker {
        constructor(
            data: GalleryData
        ) : this(
            id = data.id,
            imagePath = data.uri.toString(),
            isSelected = false,
            selectedNum = "1",
            albumName = data.getField(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME) ?: "",
            duration = data.getField(MediaStore.MediaColumns.DURATION) ?: -1
        )

        var durationText: String? = null
            get() {
                if (duration == -1L) {
                    field = "00:00"
                } else {
                    val sec = duration / 1000
                    val hour = sec / 3600
                    val remainSec = sec % 3600
                    val min = remainSec / 60
                    field = if (hour > 0) {
                        StringBuilder()
                            .append(String.format("%02d", hour)).append(":")
                            .append(String.format("%02d", min)).append(":")
                            .append(String.format("%02d", remainSec))
                            .toString()
                    } else {
                        StringBuilder()
                            .append(String.format("%02d", min)).append(":")
                            .append(String.format("%02d", remainSec))
                            .toString()
                    }
                }
                return field
            }
    }
}
