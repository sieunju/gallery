package com.gallery.core.model

import android.net.Uri
import android.provider.MediaStore
import com.gallery.core.impl.GalleryProviderImpl

/**
 * Description : Gallery Query Data Model
 *
 * Created by juhongmin on 2022/09/13
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class GalleryQueryParameter {
    // uri 에는 MediaStore.Images.Media.EXTERNAL_CONTENT_URI or
    // MediaStore.Video.Media.EXTERNAL_CONTENT_URI 로만 들어와야 한다
    var uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    var pageNo = 1
    var pageSize = 100
    var isLast = false
    var filterId: String = "" // bucket id
    var isAscOrder: Boolean = false // is Ascending order

    val order: String
        get() = if (isAscOrder) {
            "ASC"
        } else {
            "DESC"
        }

    val selectionArgs: Array<String>?
        get() = if (isAll) null else arrayOf(filterId)

    val isAll: Boolean
        get() = filterId == GalleryProviderImpl.DEFAULT_GALLERY_FILTER_ID || filterId.isEmpty()

    private val columns: MutableSet<String> = mutableSetOf()

    /**
     * Cursor 에 조회 하고 싶은 Column 값들을 추가 하는 함수
     * @param column 기본적인 컬럼 값 말고 더 조회 하고 싶은 값
     * @see MediaStore.Images.Media._ID
     * @see MediaStore.Images.ImageColumns.ORIENTATION
     * @see MediaStore.Images.Media.BUCKET_ID
     */
    fun addColumns(column: String) {
        columns.add(column)
    }

    fun getColumns(): Array<String> = columns.toTypedArray()

    init {
        columns.add(MediaStore.Images.Media._ID)
        columns.add(MediaStore.Images.Media.BUCKET_ID)
        columns.add(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
    }

    fun initParams() {
        pageNo = 1
        isLast = false
    }
}
