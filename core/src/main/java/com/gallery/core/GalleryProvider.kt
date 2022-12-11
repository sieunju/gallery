package com.gallery.core

import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.WorkerThread
import com.gallery.core.model.GalleryFilterData
import com.gallery.core.model.GalleryQueryParameter
import com.gallery.model.CropImageEditModel
import com.gallery.model.FlexibleStateItem
import okhttp3.MultipartBody
import okio.IOException
import java.io.File
import java.io.FileNotFoundException
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
    @Throws(IllegalStateException::class, Exception::class)
    fun fetchDirectories(): List<GalleryFilterData>

    /**
     * Fetch Selected FilterId Gallery
     * @param params QueryParameter
     */
    @Throws(IllegalStateException::class, NullPointerException::class)
    fun fetchGallery(params: GalleryQueryParameter): Cursor

    /**
     * Fetch All Gallery
     */
    @Throws(IllegalStateException::class, NullPointerException::class)
    fun fetchGallery(): Cursor

    /**
     * Converter Current Cursor -> Images Local Uri content://
     * @param cursor Current Cursor
     */
    @Throws(NullPointerException::class, IllegalArgumentException::class)
    fun cursorToPhotoUri(cursor: Cursor): String

    /**
     * Converter Local Path -> Bitmap
     * @param path Local Path content://...
     */
    @Throws(IOException::class, IllegalArgumentException::class, FileNotFoundException::class)
    fun pathToBitmap(path: String): Bitmap

    /**
     * Converter Local Path -> Bitmap
     * @param path Local Path content://...
     * @param limitWidth Image Limit Resize Width
     */
    @Throws(IOException::class, IllegalArgumentException::class, FileNotFoundException::class)
    fun pathToBitmap(path: String, limitWidth: Int): Bitmap

    /**
     * DrawableRes to Bitmap
     * @param redId Drawable Resource Id
     */
    @Throws(IllegalArgumentException::class)
    fun pathToBitmap(@DrawableRes redId: Int): Bitmap

    /**
     * Converter Bitmap to OkHttp.MultipartBody
     * .jpg Compress Type
     * @param bitmap Source Bitmap
     * @param name MultipartBody key Name
     */
    @Throws(NullPointerException::class, IllegalArgumentException::class)
    fun bitmapToMultipart(bitmap: Bitmap, name: String): MultipartBody.Part

    /**
     * Converter Bitmap to OkHttp.MultipartBody
     * @param bitmap Source Bitmap
     * @param name MultipartBody key Name
     * @param filename MultipartBody FileName
     * @param suffix ex.) .jpg, .png..
     */
    @Throws(NullPointerException::class, IllegalArgumentException::class)
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
    @Throws(NullPointerException::class, IllegalArgumentException::class)
    fun copyBitmapToFile(
        bitmap: Bitmap,
        fos: FileOutputStream
    ): Boolean

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
    @Throws(IllegalArgumentException::class)
    fun ratioResizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap

    /**
     * Create Temp File
     * .jpg Type
     * @throws IOException
     */
    @Throws(IOException::class)
    fun createTempFile(): File

    /**
     * Create Temp File
     * @throws IOException
     */
    @Throws(IOException::class)
    fun createFile(name: String, suffix: String): File

    /**
     * FlexibleImageView Capture Bitmap
     *
     * @param originalImagePath Original ImagePath
     * @param flexibleItem FlexibleStateItem
     * @throws NullPointerException
     */
    @Throws(NullPointerException::class, IllegalArgumentException::class)
    fun getFlexibleImageToBitmap(
        originalImagePath: String,
        flexibleItem: FlexibleStateItem
    ): Bitmap

    /**
     * FlexibleImageView Capture Bitmap
     *
     * @param originalImagePath Original ImagePath
     * @param srcRect FlexibleImageView getStateItem
     * @param width FlexibleImageView Root Layout Width
     * @param height FlexibleImageView Root Layout Height
     */
    @Throws(NullPointerException::class, IllegalArgumentException::class)
    fun getFlexibleImageToBitmap(
        originalImagePath: String,
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
     */
    @Throws(NullPointerException::class, IllegalArgumentException::class)
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
    @Throws(NullPointerException::class, IllegalArgumentException::class)
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
    @Throws(
        FileNotFoundException::class,
        SecurityException::class,
        NullPointerException::class,
        IllegalArgumentException::class
    )
    fun saveBitmapToFile(bitmap: Bitmap): File

    /**
     * CropImageEditView Edit Completed -> Bitmap Return
     * CropImageEditView 에서 원하는 영역을 지정한후 해당 부분
     * Bitmap 으로 리턴하고 싶은 경우 해당 함수를 사용합니다.
     */
    @WorkerThread
    @Throws(IllegalArgumentException::class)
    fun getCropImageEditToBitmap(editModel: CropImageEditModel): Bitmap

    /**
     * CropImageEditView Edit Completed -> Bitmap Return
     * CropImageEditView 에서 원하는 영역을 지정한후 해당 부분
     * Bitmap 으로 리턴하고 싶은 경우 해당 함수를 사용합니다.
     */
    @WorkerThread
    @Throws(IllegalArgumentException::class)
    fun getCropImageEditToBitmap(
        originalBitmap: Bitmap?,
        points: FloatArray,
        degreesRotated: Int,
        fixAspectRatio: Boolean,
        aspectRatioX: Int,
        aspectRatioY: Int,
        flipHorizontally: Boolean,
        flipVertically: Boolean
    ): Bitmap

    /**
     * 카메라 열어서 사진을 캐시 디렉토리에 저장할 URI 을 생성하는 함수 입니다.
     */
    @Throws(NullPointerException::class, IllegalArgumentException::class)
    fun createGalleryPhotoUri(authority: String): Uri

    /**
     * 카메라에서 사진을 찍은후 갤러리에 저장하는 함수입니다.
     */
    @Throws(Exception::class)
    fun saveGalleryPicture(pictureUri: String, name: String): Pair<Boolean, String>
}
