package com.gallery.ui.internal

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.provider.MediaStore
import android.util.Size
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import timber.log.Timber

/**
 * Description : Thumbnail Model Loader
 *
 * Created by juhongmin on 2024. 8. 25.
 */
class ThumbnailModelLoader(
    private val context: Context
) : ModelLoader<Uri, Bitmap> {
    override fun buildLoadData(
        model: Uri,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<Bitmap> {
        return ModelLoader.LoadData(
            ObjectKey(model),
            ThumbnailFetcher(context.contentResolver, model, width, height)
        )
    }

    override fun handles(model: Uri): Boolean {
        return model.scheme == ContentResolver.SCHEME_CONTENT
    }

    class Factory(
        private val context: Context
    ) : ModelLoaderFactory<Uri, Bitmap> {
        override fun build(
            multiFactory: MultiModelLoaderFactory
        ): ModelLoader<Uri, Bitmap> {
            return ThumbnailModelLoader(context)
        }

        override fun teardown() {
            // do nothing
        }
    }

    class ThumbnailFetcher(
        private val contentResolver: ContentResolver,
        private val uri: Uri,
        private val width: Int,
        private val height: Int
    ) : DataFetcher<Bitmap> {

        override fun loadData(
            priority: Priority,
            callback: DataFetcher.DataCallback<in Bitmap>
        ) {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.loadThumbnail(
                    uri,
                    Size(width, height),
                    CancellationSignal()
                )
            } else {
                val legacyType = contentResolver.getType(uri) ?: return
                val legacyContentId = uri.lastPathSegment?.toLongOrNull() ?: return
                if (legacyType.startsWith("image")) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Thumbnails.getThumbnail(
                        contentResolver,
                        legacyContentId,
                        MediaStore.Images.Thumbnails.MINI_KIND,
                        null
                    )
                } else if (legacyType.startsWith("video")) {
                    @Suppress("DEPRECATION")
                    MediaStore.Video.Thumbnails.getThumbnail(
                        contentResolver,
                        legacyContentId,
                        MediaStore.Video.Thumbnails.MINI_KIND,
                        null
                    )
                } else {
                    null
                }
            }
            if (bitmap == null) return
            callback.onDataReady(bitmap)
        }

        override fun cleanup() {
            Timber.d("Cleanup")
        }

        override fun cancel() {
            Timber.d("Cancel")
        }

        override fun getDataClass(): Class<Bitmap> = Bitmap::class.java

        override fun getDataSource(): DataSource = DataSource.LOCAL
    }
}