package com.gallery.core.model

import com.gallery.core.impl.GalleryProviderImpl

/**
 * Description : Gallery Query Data Model
 *
 * Created by juhongmin on 2022/09/13
 */
class GalleryQueryParameter {
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
}
