package com.gallery.core

import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.core.database.getStringOrNull

/**
 * Cursor Local Photo Uri ex.) content://
 */
fun Cursor.toPhotoUri(): String? {
    val id = getStringOrNull(getColumnIndex(MediaStore.Images.Media._ID))
    return if (id != null) {
        Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString()
    } else {
        null
    }
}
