package com.gallery.ui.model

import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
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
            cursor: Cursor
        ) : this(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)),
            contentUri = Uri.withAppendedPath(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
            ).toString(),
            albumName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)),
            dateTaken = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
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
            cursor: Cursor
        ) : this(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)),
            contentUri = Uri.withAppendedPath(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
            ).toString(),
            albumName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)),
            duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)),
            dateTaken = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED))
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
}
