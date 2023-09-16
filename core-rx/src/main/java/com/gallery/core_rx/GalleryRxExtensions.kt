package com.gallery.core_rx

import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.gallery.core.GalleryProvider
import com.gallery.core.enums.ImageType
import com.gallery.core.model.GalleryData
import com.gallery.core.model.GalleryFilterData
import com.gallery.core.model.GalleryQueryParameter
import com.gallery.model.CropImageEditModel
import com.gallery.model.FlexibleStateItem
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.MultipartBody
import java.io.File
import java.io.FileOutputStream

/**
 * Description : GalleryProvider ReactiveX 확장 함수
 *
 * Created by juhongmin on 2022/12/11
 */

internal inline fun <reified R : Any> simpleRx(crossinline callback: () -> R): Single<R> {
    return Single.create<R> { emitter ->
        try {
            val result = callback()
            emitter.onSuccess(result)
        } catch (ex: Exception) {
            emitter.onError(ex)
        }
    }.subscribeOn(Schedulers.io())
}

/**
 * Rx Extensions
 * @see GalleryProvider.fetchDirectories()
 */
@Suppress("unused")
fun GalleryProvider.fetchDirectoriesRx(): Single<List<GalleryFilterData>> {
    return simpleRx { fetchDirectories() }
}

/**
 * Rx Extensions
 * @see GalleryProvider.fetchGallery()
 */
@Suppress("unused")
fun GalleryProvider.fetchCursorRx(
    params: GalleryQueryParameter = GalleryQueryParameter()
): Single<Cursor> {
    return simpleRx { fetchCursor(params) }
}

@Suppress("unused")
fun GalleryProvider.fetchListRx(
    cursor: Cursor,
    params: GalleryQueryParameter = GalleryQueryParameter()
): Single<List<GalleryData>> {
    return simpleRx { fetchList(cursor, params) }
}

/**
 * Rx Extensions
 * @see GalleryProvider.cursorToPhotoUri()
 */
@Suppress("unused")
fun GalleryProvider.cursorToPhotoUriRx(
    cursor: Cursor
): Single<String> {
    return simpleRx { cursorToPhotoUri(cursor) }
}

/**
 * Rx Extensions
 * @see GalleryProvider.pathToBitmap()
 */
@Suppress("unused")
fun GalleryProvider.pathToBitmapRx(
    path: String
): Single<Bitmap> {
    return simpleRx { pathToBitmap(path) }
}

/**
 * Rx Extensions
 * @see GalleryProvider.pathToBitmap()
 */
@Suppress("unused")
fun GalleryProvider.pathToBitmapRx(
    path: String,
    limitWidth: Int
): Single<Bitmap> {
    return simpleRx { pathToBitmap(path, limitWidth) }
}

/**
 * Rx Extensions
 * @see GalleryProvider.pathToBitmap()
 */
@Suppress("unused")
fun GalleryProvider.pathToBitmapRx(
    @DrawableRes redId: Int
): Single<Bitmap> {
    return simpleRx { pathToBitmap(redId) }
}

/**
 * Rx Extensions
 * @see GalleryProvider.pathToMultipart()
 */
@Suppress("unused")
fun GalleryProvider.pathToMultipartRx(
    path: String,
    name: String,
    resizeWidth: Int
): Single<MultipartBody.Part> {
    return simpleRx { bitmapToMultipart(pathToBitmap(path, resizeWidth), name) }
}

/**
 * Rx Extensions
 * @see GalleryProvider.bitmapToMultipart()
 */
@Suppress("unused")
fun GalleryProvider.bitmapToMultipartRx(bitmap: Bitmap, name: String): Single<MultipartBody.Part> {
    return simpleRx { bitmapToMultipart(bitmap, name) }
}

/**
 * Rx Extensions
 * @see GalleryProvider.bitmapToMultipart()
 */
@Suppress("unused")
fun GalleryProvider.bitmapToMultipartRx(
    bitmap: Bitmap,
    name: String,
    filename: String,
    suffix: String
): Single<MultipartBody.Part> {
    return simpleRx { bitmapToMultipart(bitmap, name, filename, suffix) }
}

/**
 * Rx Extensions
 * @see GalleryProvider.copyBitmapToFile()
 */
@Suppress("unused")
fun GalleryProvider.copyBitmapToFileRx(
    bitmap: Bitmap,
    fos: FileOutputStream
): Single<Boolean> {
    return simpleRx { copyBitmapToFile(bitmap, fos) }
}

/**
 * Rx Extensions
 * @see GalleryProvider.deleteFile()
 */
@Suppress("unused")
fun GalleryProvider.deleteFileRx(
    path: String?
): Single<Boolean> {
    return simpleRx { deleteFile(path) }
}

/**
 * Rx Extensions
 * @see GalleryProvider.deleteCacheDirectory()
 */
@Suppress("unused")
fun GalleryProvider.deleteCacheDirectoryRx(): Single<Boolean> {
    return simpleRx { deleteCacheDirectory() }
}

/**
 * Rx Extensions
 * @see GalleryProvider.ratioResizeBitmap()
 */
@Suppress("unused")
fun GalleryProvider.ratioResizeBitmapRx(
    bitmap: Bitmap,
    width: Int,
    height: Int
): Single<Bitmap> {
    return simpleRx { ratioResizeBitmap(bitmap, width, height) }
}

/**
 * Rx Extensions
 * @see GalleryProvider.createTempFile()
 */
@Suppress("unused")
fun GalleryProvider.createTempFileRx(): Single<File> {
    return simpleRx { createTempFile() }
}

/**
 * Rx Extensions
 * @see GalleryProvider.createFile()
 */
@Suppress("unused")
fun GalleryProvider.createFileRx(
    name: String,
    suffix: String
): Single<File> {
    return simpleRx { createFile(name, suffix) }
}

/**
 * Rx Extensions
 * @see GalleryProvider.getFlexibleImageToBitmap()
 */
@Suppress("unused")
fun GalleryProvider.getFlexibleImageToBitmapRx(
    originalImagePath: String,
    flexibleItem: FlexibleStateItem
): Single<Bitmap> {
    return simpleRx { getFlexibleImageToBitmap(originalImagePath, flexibleItem) }
}

/**
 * Rx Extensions
 * @see GalleryProvider.getFlexibleImageToBitmap()
 */
@Suppress("unused")
fun GalleryProvider.getFlexibleImageToBitmapRx(
    originalImagePath: String,
    srcRect: RectF,
    width: Int,
    height: Int
): Single<Bitmap> {
    return simpleRx { getFlexibleImageToBitmap(originalImagePath, srcRect, width, height) }
}

/**
 * Rx Extensions
 * @see GalleryProvider.getFlexibleImageToBitmap()
 */
@Suppress("unused")
fun GalleryProvider.getFlexibleImageToBitmapRx(
    originalBitmap: Bitmap,
    srcRect: RectF,
    width: Int,
    height: Int
): Single<Bitmap> {
    return simpleRx { getFlexibleImageToBitmap(originalBitmap, srcRect, width, height) }
}

/**
 * Rx Extensions
 * @see GalleryProvider.getFlexibleImageToBitmap()
 */
@Suppress("unused")
fun GalleryProvider.getFlexibleImageToBitmapRx(
    originalBitmap: Bitmap,
    srcRect: RectF,
    width: Int,
    height: Int,
    @ColorInt color: Int
): Single<Bitmap> {
    return simpleRx { getFlexibleImageToBitmap(originalBitmap, srcRect, width, height, color) }
}

/**
 * Rx Extensions
 * @see GalleryProvider.getFlexibleImageToMultipart()
 */
@Suppress("unused")
fun GalleryProvider.getFlexibleImageToMultipartRx(
    originalImagePath: String,
    flexibleItem: FlexibleStateItem,
    multipartKey: String
): Single<MultipartBody.Part> {
    return simpleRx { getFlexibleImageToMultipart(originalImagePath, flexibleItem, multipartKey) }
}

/**
 * Rx Extensions
 * @see GalleryProvider.saveBitmapToFile()
 */
@Suppress("unused")
fun GalleryProvider.saveBitmapToFileRx(
    bitmap: Bitmap
): Single<File> {
    return simpleRx { saveBitmapToFile(bitmap) }
}

/**
 * Rx Extensions
 * @see GalleryProvider.getCropImageEditToBitmap()
 */
@Suppress("unused")
fun GalleryProvider.getCropImageEditToBitmapRx(
    editModel: CropImageEditModel
): Single<Bitmap> {
    return simpleRx { getCropImageEditToBitmap(editModel) }
}

/**
 * Rx Extensions
 * @see GalleryProvider.getCropImageEditToBitmap()
 */
@Suppress("unused")
fun GalleryProvider.getCropImageEditToBitmapRx(
    originalBitmap: Bitmap?,
    points: FloatArray,
    degreesRotated: Int,
    fixAspectRatio: Boolean,
    aspectRatioX: Int,
    aspectRatioY: Int,
    flipHorizontally: Boolean,
    flipVertically: Boolean
): Single<Bitmap> {
    return simpleRx {
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
 * Rx Extensions
 * @see GalleryProvider.createGalleryPhotoUri()
 */
@Suppress("unused")
fun GalleryProvider.createGalleryPhotoUriRx(
    authority: String
): Single<Uri> {
    return simpleRx { createGalleryPhotoUri(authority) }
}

/**
 * Rx Extensions
 * @see GalleryProvider.saveGalleryPicture()
 */
@Suppress("unused")
fun GalleryProvider.saveGalleryPictureRx(
    pictureUri: String, name: String
): Single<Pair<Boolean, String>> {
    return simpleRx { saveGalleryPicture(pictureUri, name) }
}

/**
 * Rx ImageType
 * @see GalleryProvider.getImageType
 */
@Suppress("unused")
fun GalleryProvider.getImageTypeRx(
    imagePath: String
): Single<ImageType> {
    return simpleRx { getImageType(imagePath) }
}

/**
 * Rx getThumbnail
 * @see GalleryProvider.getThumbnail
 */
@Suppress("unused")
fun GalleryProvider.getThumbnailRx(
    imageId: Long
): Single<Bitmap> {
    return simpleRx { getThumbnail(imageId) }
}

/**
 * Rx getThumbnail
 * @see GalleryProvider.getThumbnail
 */
@Suppress("unused")
fun GalleryProvider.getThumbnailRx(
    imageId: Long,
    width: Int,
    height: Int
): Single<Bitmap> {
    return simpleRx { getThumbnail(imageId, width, height) }
}
