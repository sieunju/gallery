package com.gallery.ui.model

import android.provider.MediaStore
import com.gallery.core.model.GalleryData
import java.util.Locale

/**
 * Description : PhotoPicker 전용 클래스
 *
 * Created by juhongmin on 3/23/24
 */
sealed interface PhotoPicker {

    object Camera : PhotoPicker

    /**
     * Photo Data Model
     * @param contentUri Photo Image Url
     * @param isSelected Selected
     * @param selectedNum Selected Number
     * @param albumName Album Name
     * @param dateTaken Content Date (Sort)
     */
    data class Photo(
        val id: Long,
        val contentUri: String,
        var isSelected: Boolean = false,
        var selectedNum: String = "1",
        val albumName: String,
        val dateTaken: Int
    ) : PhotoPicker {

        constructor(
            data: GalleryData
        ) : this(
            id = data.id,
            contentUri = data.uri.toString(),
            isSelected = false,
            selectedNum = "1",
            albumName = data.getField(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME) ?: "",
            dateTaken = data.getField(MediaStore.MediaColumns.DATE_TAKEN) ?: -1
        )
    }

    /**
     * Video Data Model
     * @param contentUri Video Content Url
     * @param isSelected Selected
     * @param selectedNum Selected Number
     * @param albumName Album Name
     * @param duration Video Duration
     * @param dateTaken Content Date (Sort)
     */
    data class Video(
        val id: Long,
        val contentUri: String,
        var isSelected: Boolean = false,
        var selectedNum: String = "1",
        val albumName: String,
        val duration: Int,
        val dateTaken: Int
    ) : PhotoPicker {

        constructor(
            data: GalleryData
        ) : this(
            id = data.id,
            contentUri = data.uri.toString(),
            isSelected = false,
            selectedNum = "1",
            albumName = data.getField(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME) ?: "",
            duration = data.getField(MediaStore.MediaColumns.DURATION) ?: -1,
            dateTaken = data.getField(MediaStore.MediaColumns.DATE_TAKEN) ?: -1
        )

        var durationText: String? = null
            get() {
                if (duration == -1) {
                    field = "00:00"
                } else {
                    val sec = duration / 1000
                    val hour = sec / 3600
                    val remainSec = sec % 3600
                    val min = remainSec / 60
                    field = if (hour > 0) {
                        StringBuilder()
                            .append(String.format(Locale.getDefault(), "%02d", hour))
                            .append(":")
                            .append(String.format(Locale.getDefault(), "%02d", min))
                            .append(":")
                            .append(String.format(Locale.getDefault(), "%02d", remainSec))
                            .toString()
                    } else {
                        StringBuilder()
                            .append(String.format(Locale.getDefault(), "%02d", min))
                            .append(":")
                            .append(String.format(Locale.getDefault(), "%02d", remainSec))
                            .toString()
                    }
                }
                return field
            }
    }

    companion object {
        fun GalleryData.toUi(): PhotoPicker {
            return if (getField<Int>(MediaStore.MediaColumns.DURATION) == null) {
                Photo(this)
            } else {
                Video(this)
            }
        }
    }
}
