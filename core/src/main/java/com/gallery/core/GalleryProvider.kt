package com.gallery.core

import android.database.Cursor
import com.gallery.core.model.GalleryFilterData
import com.gallery.core.model.GalleryQueryParameter

/**
 * Description : 갤러리 사진 데이터들 가져오는 Interface
 *
 * Created by juhongmin on 2022/09/13
 */
interface GalleryProvider {
    /**
     * Fetch Gallery Directory
     * 갤러리 조회 함수
     */
    fun fetchDirectories(): List<GalleryFilterData>

    /**
     * Fetch Selected FilterId Gallery
     * @param params QueryParameter
     */
    fun fetchGallery(params: GalleryQueryParameter): Cursor

    /**
     * Fetch All Gallery
     */
    fun fetchGallery(): Cursor
}
