package com.gallery.core

import android.database.Cursor
import com.gallery.core.model.GalleryFilterData

/**
 * Description : 갤러리 사진 데이터들 가져오는 Interface
 *
 * Created by juhongmin on 2022/09/13
 */
interface GalleryProvider {
    fun fetchDirectory(): List<GalleryFilterData>
    fun fetchGallery(filterId: String): Cursor
}
