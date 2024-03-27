package com.gallery.ui.internal

import android.graphics.Bitmap
import androidx.collection.LruCache
import com.gallery.core.GalleryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Description : Glide 처리하지 않고 직접 이미지 캐싱 처리하는 클래스
 *
 * Created by juhongmin on 3/27/24
 */
object PhotoPickerImageLoader {

    private val cache: LruCache<String, Bitmap> by lazy { initCache() }
    private lateinit var provider: GalleryProvider

    /**
     * init Cache 10mib
     */
    private fun initCache(): LruCache<String, Bitmap> {
        return object : LruCache<String, Bitmap>(10 * 1024 * 1024) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return value.byteCount / 1024
            }
        }
    }

    fun setCoreProvider(provider: GalleryProvider) {
        this.provider = provider
    }

    suspend fun savePhotoThumbnail(
        id: Long,
        key: String,
        size: Int
    ) {
        withContext(Dispatchers.IO) {
            cache.put(key, provider.getPhotoThumbnail(id, size))
        }
    }

    suspend fun saveVideoThumbnail(
        id: Long,
        key: String,
        size: Int
    ) {
        withContext(Dispatchers.IO) {
            cache.put(key, provider.getVideoThumbnail(id, size))
        }
    }

    fun getCacheBitmap(key: String): Bitmap? {
        return cache.get(key)
    }
}