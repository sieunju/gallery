package com.gallery.core

import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface

/**
 * Description : GalleryProvider 에 필요한 Utils Class
 *
 * Created by juhongmin on 2023/04/03
 */
internal object Extensions {

    /**
     * 파일 접미사 기준으로 Bitmap 압축형식 변환 처리함수
     * @param suffix ex.) .png, .jpg..
     *
     * @return Bitmap.CompressFormat
     * @see Bitmap.CompressFormat.JPEG
     * @see Bitmap.CompressFormat.PNG
     */
    fun toCompressFormat(suffix: String): Bitmap.CompressFormat {
        return if (suffix == ".png") {
            Bitmap.CompressFormat.PNG
        } else {
            Bitmap.CompressFormat.JPEG
        }
    }

    /**
     * Media ID 값 리턴하는 함수
     * 에러 발생시 빈값으로 리턴
     *
     * @return Media ID
     */
    fun Cursor.getContentsId(): String {
        return try {
            getString(getColumnIndexOrThrow(MediaStore.Images.Media._ID))
        } catch (ex: Exception) {
            ""
        }
    }

    /**
     * 앨범 아이디값 리턴하는 함수
     * 에러 발생시 빈값으로 리턴
     *
     * @return Album ID
     */
    fun Cursor.getBucketId(): String {
        return try {
            getString(getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID))
        } catch (ex: Exception) {
            ""
        }
    }

    /**
     * 앨범명 리턴하는 함수
     * 에러 발생시 빈값으로 리턴
     *
     * @return Album Name
     */
    fun Cursor.getBucketName(): String {
        return try {
            getString(getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME))
        } catch (ex: Exception) {
            ""
        }
    }

    /**
     * 이미지명 리턴하는 함수
     * 에러 발생시 빈값으로 리턴
     *
     * @return Image Name
     */
    fun Cursor.getDisplayName(): String {
        return try {
            getString(getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
        } catch (ex: Exception) {
            ""
        }
    }

    /**
     * Media ID 값을 content://.. 으로 변환해서 리턴하는 함수
     * @param id Media ID
     *
     * @return content://media/external/images/media/{id}
     */
    fun getPhotoUri(id: String): String {
        return try {
            Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString()
        } catch (ex: NullPointerException) {
            ""
        }
    }

    /**
     * set Image Rotate Func.
     * @param orientation ExifInterface Orientation
     * @param matrix Image Matrix
     *
     * @return true 정의된 Rotate 방식입니다.
     * @return false 정의되지 않은 Rotate 방식입니다.
     */
    fun setRotate(orientation: Int, matrix: Matrix): Boolean {
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                matrix.postRotate(0F)
                true
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                matrix.postRotate(180f)
                true
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                matrix.postRotate(270f)
                true
            }
            else -> false
        }
    }
}
