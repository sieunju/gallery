package com.gallery.ui.internal

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.gallery.core.GalleryProvider
import com.gallery.ui.model.PhotoPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Description : Glide 처리하지 않고 직접 이미지 캐싱 처리하는 클래스
 *
 * Created by juhongmin on 3/27/24
 */
internal object PhotoPickerImageLoader {

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

    suspend fun saveThumbnail(
        requestManager: RequestManager,
        data: PhotoPicker,
        size: Int
    ): PhotoPicker {
        if (data is PhotoPicker.Camera) return data
        return withContext(Dispatchers.IO) {
            try {
                if (data is PhotoPicker.Photo) {
                    cache.put(data.contentUri, provider.getPhotoThumbnail(data.id, size))
                } else if (data is PhotoPicker.Video) {
                    cache.put(data.contentUri, provider.getVideoThumbnail(data.id, size))
                }
                return@withContext data
            } catch (ex: Exception) {
                Timber.d("SaveThumbnail Error $ex")
                // Failed to create thumbnail
                val imagePath = when (data) {
                    is PhotoPicker.Photo -> data.contentUri
                    is PhotoPicker.Video -> data.contentUri
                    else -> null
                }
                requestManager
                    .asBitmap()
                    .load(imagePath)
                    .into(object : CustomTarget<Bitmap>(size, size) {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            if (imagePath != null) {
                                cache.put(imagePath, resource)
                            }
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    })

                return@withContext data
            }
        }
    }

    fun getCacheBitmap(key: String): Bitmap? {
        return cache.get(key)
    }
}