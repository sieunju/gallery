package com.gallery.core

import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
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
     * @param exifOrientation ExifInterface Orientation
     * @param matrix Image Matrix
     *
     * @return true 정의된 Rotate 방식입니다.
     * @return false 정의되지 않은 Rotate 방식입니다.
     */
    fun setExifRotation(
        exifOrientation: Int,
        matrix: Matrix
    ) {
        Log.d("JLOGGER", "setRotate ${exifOrientation}")
        when (exifOrientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> {}
        }
    }

    /**
     * Glide 라이브러리에서 따옴
     */
    fun getExifOrientationDegrees(exifOrientation: Int): Int {
        val degreesToRotate: Int = when (exifOrientation) {
            ExifInterface.ORIENTATION_TRANSPOSE, ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180, ExifInterface.ORIENTATION_FLIP_VERTICAL -> 180
            ExifInterface.ORIENTATION_TRANSVERSE, ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
        return degreesToRotate
    }

    /**
     * Glide 라이브러리에서 따옴
     */
    fun initializeMatrixForRotation(exifOrientation: Int, matrix: Matrix) {
        when (exifOrientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> {}
        }
    }
}
