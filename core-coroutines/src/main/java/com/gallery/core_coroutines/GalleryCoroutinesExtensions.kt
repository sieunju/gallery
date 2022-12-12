package com.gallery.core_coroutines

import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.gallery.core.GalleryProvider
import com.gallery.core.model.GalleryFilterData
import com.gallery.core.model.GalleryQueryParameter
import com.gallery.model.CropImageEditModel
import com.gallery.model.FlexibleStateItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import java.io.File
import java.io.FileOutputStream

/**
 * Description : GalleryProvider Coroutines 확장 함수
 *
 * Created by juhongmin on 2022/12/12
 */

internal suspend inline fun <reified R : Any> simpleCoroutines(crossinline callback: () -> R): Result<R> {
    return withContext(Dispatchers.IO) {
        try {
            val result = callback()
            Result.success(result)
        } catch (ex: Exception) {
            Result.failure<R>(ex)
        }
    }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.fetchDirectories()
 */
@Suppress("unused")
suspend fun GalleryProvider.fetchDirectoriesCo(): Result<List<GalleryFilterData>> {
    return simpleCoroutines { fetchDirectories() }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.fetchGallery()
 */
@Suppress("unused")
suspend fun GalleryProvider.fetchGalleryCo(
    params: GalleryQueryParameter = GalleryQueryParameter()
): Result<Cursor> {
    return simpleCoroutines { fetchGallery(params) }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.cursorToPhotoUri()
 */
@Suppress("unused")
suspend fun GalleryProvider.cursorToPhotoUriCo(
    cursor: Cursor
): Result<String> {
    return simpleCoroutines { cursorToPhotoUri(cursor) }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.pathToBitmap()
 */
@Suppress("unused")
suspend fun GalleryProvider.pathToBitmapCo(
    path: String
): Result<Bitmap> {
    return simpleCoroutines { pathToBitmap(path) }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.pathToBitmap()
 */
@Suppress("unused")
suspend fun GalleryProvider.pathToBitmapCo(
    path: String,
    limitWidth: Int
): Result<Bitmap> {
    return simpleCoroutines { pathToBitmap(path, limitWidth) }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.pathToBitmap()
 */
@Suppress("unused")
suspend fun GalleryProvider.pathToBitmapCo(
    @DrawableRes redId: Int
): Result<Bitmap> {
    return simpleCoroutines { pathToBitmap(redId) }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.pathToMultipart()
 */
@Suppress("unused")
suspend fun GalleryProvider.pathToMultipartCo(
    path: String,
    name: String,
    resizeWidth: Int
): Result<MultipartBody.Part> {
    return simpleCoroutines { bitmapToMultipart(pathToBitmap(path, resizeWidth), name) }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.bitmapToMultipart()
 */
@Suppress("unused")
suspend fun GalleryProvider.bitmapToMultipartCo(
    bitmap: Bitmap,
    name: String
): Result<MultipartBody.Part> {
    return simpleCoroutines { bitmapToMultipart(bitmap, name) }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.bitmapToMultipart()
 */
@Suppress("unused")
suspend fun GalleryProvider.bitmapToMultipartCo(
    bitmap: Bitmap,
    name: String,
    filename: String,
    suffix: String
): Result<MultipartBody.Part> {
    return simpleCoroutines { bitmapToMultipart(bitmap, name, filename, suffix) }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.copyBitmapToFile()
 */
@Suppress("unused")
suspend fun GalleryProvider.copyBitmapToFileCo(
    bitmap: Bitmap,
    fos: FileOutputStream
): Result<Boolean> {
    return simpleCoroutines { copyBitmapToFile(bitmap, fos) }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.deleteFile()
 */
@Suppress("unused")
suspend fun GalleryProvider.deleteFileCo(
    path: String?
): Result<Boolean> {
    return simpleCoroutines { deleteFile(path) }
}

/**
 * Rx Extensions
 * @see GalleryProvider.deleteCacheDirectory()
 */
@Suppress("unused")
suspend fun GalleryProvider.deleteCacheDirectoryCo(): Result<Boolean> {
    return simpleCoroutines { deleteCacheDirectory() }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.ratioResizeBitmap()
 */
@Suppress("unused")
suspend fun GalleryProvider.ratioResizeBitmapCo(
    bitmap: Bitmap,
    width: Int,
    height: Int
): Result<Bitmap> {
    return simpleCoroutines { ratioResizeBitmap(bitmap, width, height) }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.createTempFile()
 */
@Suppress("unused")
suspend fun GalleryProvider.createTempFileCo(): Result<File> {
    return simpleCoroutines { createTempFile() }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.createFile()
 */
@Suppress("unused")
suspend fun GalleryProvider.createFileCo(
    name: String,
    suffix: String
): Result<File> {
    return simpleCoroutines { createFile(name, suffix) }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.getFlexibleImageToBitmap()
 */
@Suppress("unused")
suspend fun GalleryProvider.getFlexibleImageToBitmapCo(
    originalImagePath: String,
    flexibleItem: FlexibleStateItem
): Result<Bitmap> {
    return simpleCoroutines { getFlexibleImageToBitmap(originalImagePath, flexibleItem) }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.getFlexibleImageToBitmap()
 */
@Suppress("unused")
suspend fun GalleryProvider.getFlexibleImageToBitmapCo(
    originalImagePath: String,
    srcRect: RectF,
    width: Int,
    height: Int
): Result<Bitmap> {
    return simpleCoroutines { getFlexibleImageToBitmap(originalImagePath, srcRect, width, height) }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.getFlexibleImageToBitmap()
 */
@Suppress("unused")
suspend fun GalleryProvider.getFlexibleImageToBitmapCo(
    originalBitmap: Bitmap,
    srcRect: RectF,
    width: Int,
    height: Int
): Result<Bitmap> {
    return simpleCoroutines { getFlexibleImageToBitmap(originalBitmap, srcRect, width, height) }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.getFlexibleImageToBitmap()
 */
@Suppress("unused")
suspend fun GalleryProvider.getFlexibleImageToBitmapCo(
    originalBitmap: Bitmap,
    srcRect: RectF,
    width: Int,
    height: Int,
    @ColorInt color: Int
): Result<Bitmap> {
    return simpleCoroutines {
        getFlexibleImageToBitmap(
            originalBitmap,
            srcRect,
            width,
            height,
            color
        )
    }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.getFlexibleImageToMultipart()
 */
@Suppress("unused")
suspend fun GalleryProvider.getFlexibleImageToMultipartCo(
    originalImagePath: String,
    flexibleItem: FlexibleStateItem,
    multipartKey: String
): Result<MultipartBody.Part> {
    return simpleCoroutines {
        getFlexibleImageToMultipart(
            originalImagePath,
            flexibleItem,
            multipartKey
        )
    }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.saveBitmapToFile()
 */
@Suppress("unused")
suspend fun GalleryProvider.saveBitmapToFileCo(
    bitmap: Bitmap
): Result<File> {
    return simpleCoroutines { saveBitmapToFile(bitmap) }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.getCropImageEditToBitmap()
 */
@Suppress("unused")
suspend fun GalleryProvider.getCropImageEditToBitmapCo(
    editModel: CropImageEditModel
): Result<Bitmap> {
    return simpleCoroutines { getCropImageEditToBitmap(editModel) }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.getCropImageEditToBitmap()
 */
@Suppress("unused")
suspend fun GalleryProvider.getCropImageEditToBitmapCo(
    originalBitmap: Bitmap?,
    points: FloatArray,
    degreesRotated: Int,
    fixAspectRatio: Boolean,
    aspectRatioX: Int,
    aspectRatioY: Int,
    flipHorizontally: Boolean,
    flipVertically: Boolean
): Result<Bitmap> {
    return simpleCoroutines {
        getCropImageEditToBitmap(
            originalBitmap,
            points,
            degreesRotated,
            fixAspectRatio,
            aspectRatioX,
            aspectRatioY,
            flipHorizontally,
            flipVertically
        )
    }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.createGalleryPhotoUri()
 */
@Suppress("unused")
suspend fun GalleryProvider.createGalleryPhotoUriCo(
    authority: String
): Result<Uri> {
    return simpleCoroutines { createGalleryPhotoUri(authority) }
}

/**
 * Coroutines Extensions
 * @see GalleryProvider.saveGalleryPicture()
 */
@Suppress("unused")
suspend fun GalleryProvider.saveGalleryPictureCo(
    pictureUri: String, name: String
): Result<Pair<Boolean, String>> {
    return simpleCoroutines { saveGalleryPicture(pictureUri, name) }
}
