package com.gallery.core

import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.TypedValue
import androidx.core.database.getStringOrNull
import okhttp3.MultipartBody

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

/**
 * Converter Contents Path to Multipart
 */
fun GalleryProvider.pathToMultipart(path: String, name: String): MultipartBody.Part? {
    return try {
        bitmapToMultipart(pathToBitmap(path), name)
    } catch (ex: Exception) {
        null
    }
}

/**
 * Converter Contents Path to Multipart
 * @param path ex.) content://
 * @param name Multipart.Body Upload key
 * @param resizeWidth Resize Limit Width
 */
fun GalleryProvider.pathToMultipart(
    path: String,
    name: String,
    resizeWidth: Int
): MultipartBody.Part? {
    return try {
        bitmapToMultipart(pathToBitmap(path, resizeWidth), name)
    } catch (ex: Exception) {
        null
    }
}
