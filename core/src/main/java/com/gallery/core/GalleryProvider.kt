package com.gallery.core

import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.WorkerThread
import com.gallery.core.model.GalleryFilterData
import com.gallery.core.model.GalleryQueryParameter
import com.gallery.model.CropImageEditModel
import okhttp3.MultipartBody
import java.io.File
import java.io.FileOutputStream

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

    /**
     * Converter Current Cursor -> Images Local Uri content://
     * @param cursor Current Cursor
     */
    fun cursorToPhotoUri(cursor: Cursor): String?

    /**
     * Converter Local Path -> Bitmap
     * @param path Local Path content://...
     */
    fun pathToBitmap(path: String): Bitmap

    /**
     * Converter Local Path -> Bitmap
     * @param path Local Path content://...
     * @param limitWidth Image Limit Resize Width
     */
    fun pathToBitmap(path: String, limitWidth: Int): Bitmap

    /**
     * DrawableRes to Bitmap
     * @param redId Drawable Resource Id
     */
    fun pathToBitmap(@DrawableRes redId: Int): Bitmap

    /**
     * Converter Bitmap to OkHttp.MultipartBody
     * .jpg Compress Type
     * @param bitmap Source Bitmap
     * @param name MultipartBody key Name
     */
    fun bitmapToMultipart(bitmap: Bitmap, name: String): MultipartBody.Part

    /**
     * Converter Bitmap to OkHttp.MultipartBody
     * @param bitmap Source Bitmap
     * @param name MultipartBody key Name
     * @param filename MultipartBody FileName
     * @param suffix ex.) .jpg, .png..
     */
    fun bitmapToMultipart(
        bitmap: Bitmap,
        name: String,
        filename: String,
        suffix: String
    ): MultipartBody.Part

    /**
     * Copy Bitmap to File
     * @param bitmap Source Bitmap
     * @param fos FileStream
     */
    fun copyBitmapToFile(
        bitmap: Bitmap,
        fos: FileOutputStream
    )

    /**
     * Delete File
     *
     * @param path File Path
     *
     * @return true -> Delete Success, false -> Delete Fail
     */
    fun deleteFile(path: String?): Boolean

    /**
     * Delete CacheDirectory
     *
     * @return true -> Delete Success, false -> Delete Fail
     */
    fun deleteCacheDirectory(): Boolean

    /**
     * 비율에 맞게 비트맵 리사이징 하는 함수.
     * @param bitmap Source Bitmap
     * @param width 리사이징 하고 싶은 너비
     * @param height 리사이징 하고 싶은 높이
     *
     * @return Resize Bitmap..
     */
    fun ratioResizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap

    /**
     * Create Temp File
     * .jpg Type
     */
    fun createTempFile(): File?

    /**
     * Create Temp File
     */
    fun createFile(name: String, suffix: String): File?

    /**
     * FlexibleImageView Capture Bitmap
     *
     * @param originalBitmap FlexibleImageView Bitmap Resource
     * @param srcRect FlexibleImageView getStateItem
     * @param width FlexibleImageView Root Layout Width
     * @param height FlexibleImageView Root Layout Height
     */
    fun getFlexibleImageToBitmap(
        originalBitmap: Bitmap,
        srcRect: RectF,
        width: Int,
        height: Int
    ): Bitmap

    /**
     * FlexibleImageView Capture Bitmap
     *
     * @param originalBitmap FlexibleImageView Bitmap Resource
     * @param srcRect FlexibleImageView getStateItem
     * @param width FlexibleImageView Root Layout Width
     * @param height FlexibleImageView Root Layout Height
     * @param color Color Resource Int
     */
    fun getFlexibleImageToBitmap(
        originalBitmap: Bitmap,
        srcRect: RectF,
        width: Int,
        height: Int,
        @ColorInt color: Int
    ): Bitmap

    /**
     * Save Bitmap File Completed File Info Return
     * File Location Cache Directory
     */
    fun saveBitmapToFile(bitmap: Bitmap): File?

    /**
     * CropImageEditView Edit Completed -> Bitmap Return
     * CropImageEditView 에서 원하는 영역을 지정한후 해당 부분
     * Bitmap 으로 리턴하고 싶은 경우 해당 함수를 사용합니다.
     */
    @WorkerThread
    fun getCropImageEditToBitmap(editModel: CropImageEditModel): Bitmap?

    /**
     * CropImageEditView Edit Completed -> Bitmap Return
     * CropImageEditView 에서 원하는 영역을 지정한후 해당 부분
     * Bitmap 으로 리턴하고 싶은 경우 해당 함수를 사용합니다.
     */
    @WorkerThread
    fun getCropImageEditToBitmap(
        originalBitmap: Bitmap?,
        points: FloatArray,
        degreesRotated: Int,
        fixAspectRatio: Boolean,
        aspectRatioX: Int,
        aspectRatioY: Int,
        flipHorizontally: Boolean,
        flipVertically: Boolean
    ): Bitmap?
}
