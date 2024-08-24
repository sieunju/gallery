package com.gallery.ui.internal

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.collection.LruCache
import com.gallery.ui.internal.core.GalleryProvider
import com.gallery.ui.model.PhotoPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Description : Glide 처리하지 않고 직접 이미지 캐싱 처리하는 클래스
 *
 * Created by juhongmin on 3/27/24
 */
@SuppressLint("StaticFieldLeak")
internal object ImageLoader {

    private val cache: LruCache<String, Bitmap> by lazy { initCache() }

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

    /**
     * 썸네일 온메모리에 저장하는 함수
     * @param provider Core
     * @param data Picker Data
     * @param size Thumbnail Size
     */
    suspend fun saveThumbnail(
        provider: GalleryProvider,
        data: PhotoPicker,
        size: Int
    ): PhotoPicker {
        if (data is PhotoPicker.Camera) return data
        return withContext(Dispatchers.IO) {
            if (data is PhotoPicker.Photo) {
                cache.put(data.contentUri, provider.getPhotoThumbnail(data.id, size))
            } else if (data is PhotoPicker.Video) {
                cache.put(data.contentUri, provider.getVideoThumbnail(data.id, size))
            }
            data
        }
    }

    fun getCacheBitmap(key: String): Bitmap? {
        return cache.get(key)
    }
}