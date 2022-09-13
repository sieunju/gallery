package com.gallery.core.impl

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.gallery.core.GalleryProvider
import com.gallery.core.model.GalleryFilterData
import javax.inject.Inject

/**
 * Description : Gallery Provider 구현체 클래스
 *
 * Created by juhongmin on 2022/09/13
 */
class GalleryProviderImpl @Inject constructor(
    private val context: Context
) : GalleryProvider {

    private val contentResolver: ContentResolver by lazy { context.contentResolver }

    companion object {
        val CONTENT_URI: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        const val ID = MediaStore.Images.Media._ID
        const val DEFAULT_GALLERY_FILTER_ID = "ALL"
        const val DEFAULT_GALLERY_FILTER_NAME = "최근 항목"

        @SuppressLint("InlinedApi")
        val BUCKET_ID = MediaStore.Images.Media.BUCKET_ID

        @SuppressLint("InlinedApi")
        val BUCKET_NAME = MediaStore.Images.Media.BUCKET_DISPLAY_NAME
    }

    /**
     * 저장소 읽기 권한 체크
     * @return true 읽기 권한 허용, false 읽기 권한 거부 상태
     */
    private fun isReadStoragePermissionsGranted(): Boolean {
        return context.packageManager.checkPermission(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            context.packageName
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("Recycle")
    override fun fetchDirectory(): List<GalleryFilterData> {
        if (!isReadStoragePermissionsGranted()) throw IllegalStateException("'android.permission.READ_EXTERNAL_STORAGE' is not Granted! ")
        val dataList = mutableListOf<GalleryFilterData>()
        val projection = arrayOf(
            ID,
            BUCKET_ID,
            BUCKET_NAME
        )
        val selection = StringBuilder()
        val selectionArgs = mutableListOf<String>()
        val sort = "$ID DESC "
        var prevPhotoUri = ""
        var prevBucketId = ""
        var prevBucketName = ""
        var prevCount: Int = -1
        try {
            loop@ while (true) {
                val cursor = contentResolver.query(
                    CONTENT_URI,
                    projection,
                    if (selection.isEmpty()) null else selection.toString(),
                    if (selectionArgs.isEmpty()) null else selectionArgs.toTypedArray(),
                    sort
                ) ?: break@loop

                if (cursor.moveToLast()) {
                    val contentId = cursor.getString(cursor.getColumnIndexOrThrow(ID))
                    val photoUri = Uri.withAppendedPath(CONTENT_URI, contentId).toString()
                    val bucketId = cursor.getString(cursor.getColumnIndexOrThrow(BUCKET_ID))
                    val bucketName = cursor.getString(cursor.getColumnIndexOrThrow(BUCKET_NAME))
                    val count = cursor.count

                    if (!cursor.isClosed) {
                        cursor.close()
                    }

                    // Where Setting
                    if (selection.isNotEmpty()) {
                        selection.append(" AND ")
                    }

                    selection.append(BUCKET_ID)
                    selection.append(" !=?")
                    selectionArgs.add(bucketId)

                    // 앨범명 유효성 검사.
                    if (prevBucketId.isNotEmpty()) {
                        val diffCount = prevCount - count
                        prevCount = count
                        dataList.add(
                            GalleryFilterData(
                                bucketId = prevBucketId,
                                bucketName = prevBucketName,
                                photoUri = prevPhotoUri,
                                count = diffCount
                            )
                        )
                    }

                    // 초기값 세팅
                    if (prevCount == -1) {
                        prevCount = count
                        dataList.add(
                            GalleryFilterData(
                                bucketId = DEFAULT_GALLERY_FILTER_ID,
                                bucketName = DEFAULT_GALLERY_FILTER_NAME,
                                photoUri = photoUri,
                                count = count
                            )
                        )
                    }

                    // Set Data.
                    prevPhotoUri = photoUri
                    prevBucketId = bucketId
                    prevBucketName = bucketName

                } else {
                    // 맨 마지막 앨범 추가
                    if (prevCount != 0) {
                        dataList.add(
                            GalleryFilterData(
                                bucketId = prevBucketId,
                                bucketName = prevBucketName,
                                photoUri = prevPhotoUri,
                                count = prevCount
                            )
                        )
                    }

                    if (!cursor.isClosed) {
                        cursor.close()
                    }
                    break@loop
                }
            }
            return dataList
        } catch (ex: Exception) {
            throw ex
        }
    }

    @SuppressLint("Recycle")
    override fun fetchGallery(filterId: String): Cursor {
        val projection = arrayOf(ID)
        val sort = "$ID DESC"
        val selection = "$BUCKET_ID ==?"

        val isAll: Boolean = filterId == DEFAULT_GALLERY_FILTER_ID
        return contentResolver.query(
            CONTENT_URI, projection, if (isAll) null else selection,
            if (isAll) null else arrayOf(filterId),
            sort
        ) ?: throw NullPointerException("Cursor NullPointerException")
    }
}
