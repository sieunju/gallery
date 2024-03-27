package com.gallery.core.model

import android.net.Uri
import android.provider.MediaStore
import com.gallery.core.impl.GalleryProviderImpl

/**
 * Description : Gallery Query Data Model
 *
 * Created by juhongmin on 2022/09/13
 */
/**
 * @param uri ContentUri [MediaStore.Images.Media.EXTERNAL_CONTENT_URI], [MediaStore.Video.Media.EXTERNAL_CONTENT_URI]
 * @param filterId Bucket ID
 * @param pageNo Page Number
 * @param pageSize PageSize
 * @param order Query Order
 * @param isLast Paging is Last
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
data class GalleryQueryParameter(
    val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
    var filterId: String = "", // bucket id
    var pageNo: Int = 1,
    val pageSize: Int = 100,
    val order: String = "${MediaStore.MediaColumns.DATE_TAKEN} DESC, ${MediaStore.MediaColumns.DATE_ADDED} DESC",
    var isLast: Boolean = false
) {

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
        columns.add(MediaStore.MediaColumns._ID)
        columns.add(MediaStore.MediaColumns.BUCKET_ID)
        columns.add(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
    }

    fun initParams() {
        pageNo = 1
        isLast = false
    }
}
