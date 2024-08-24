package com.gallery.core.model

import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

/**
 * Description : Cursor 를 페이징 처리할때 사용하는 데이터 모델
 *
 * Created by juhongmin on 2023/08/21
 */
class GalleryData(
    cursor: Cursor,
    params: GalleryQueryParameter
) {
    val uri: Uri
    val fields: HashMap<String, Any> = hashMapOf()
    val id: Long

    init {
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        uri = Uri.withAppendedPath(params.uri, cursor.getString(idIndex))
        id = cursor.getLong(idIndex)
        params.getColumns().forEach { columnName ->
            try {
                val columnIdx = cursor.getColumnIndexOrThrow(columnName)
                when (cursor.getType(columnIdx)) {
                    Cursor.FIELD_TYPE_INTEGER -> fields[columnName] = cursor.getInt(columnIdx)
                    Cursor.FIELD_TYPE_FLOAT -> fields[columnName] = cursor.getFloat(columnIdx)
                    Cursor.FIELD_TYPE_STRING -> fields[columnName] = cursor.getString(columnIdx)
                    Cursor.FIELD_TYPE_BLOB -> fields[columnName] = cursor.getBlob(columnIdx)
                    else -> {}
                }
            } catch (ex: IllegalArgumentException) {
                // ignore
            }
        }
    }

    inline fun <reified T> getField(columnName: String): T? {
        val value = fields[columnName] ?: return null
        return value as? T
    }

    override fun equals(other: Any?): Boolean {
        return if (other is GalleryData) {
            uri == other.uri
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + fields.hashCode()
        return result
    }
}
