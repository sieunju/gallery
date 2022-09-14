package com.gallery.core

import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.DrawableRes
import com.gallery.core.model.GalleryFilterData
import com.gallery.core.model.GalleryQueryParameter
import okhttp3.MultipartBody
import java.io.File

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
    fun cursorToPhotoUri(cursor: Cursor) : String?

    /**
     * Converter Local Path -> Bitmap
     * @param path Local Path content://...
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
     * Delete File
     *
     * @param path File Path
     * @return true -> Delete Success, false -> Delete Fail
     */
    fun deleteFile(path: String?): Boolean

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
}
