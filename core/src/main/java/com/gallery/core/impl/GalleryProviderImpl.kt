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
import com.gallery.core.model.GalleryQueryParameter
import timber.log.Timber

/**
 * Description : Gallery Provider 구현체 클래스
 *
 * Created by juhongmin on 2022/09/13
 */
internal class GalleryProviderImpl constructor(
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
    override fun fetchDirectories(): List<GalleryFilterData> {
        // Permissions Check
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

                Timber.d("Directories ${Thread.currentThread()}")
                if (cursor.moveToLast()) {
                    val contentId = getContentsId(cursor)
                    val photoUri = getPhotoUri(contentId)
                    val bucketId = getBucketId(cursor)
                    val bucketName = getBucketName(cursor)
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

    override fun fetchGallery(params: GalleryQueryParameter): Cursor {
        val projection = arrayOf(ID)
        val order = "$ID ${params.order}"
        val selection = "$BUCKET_ID ==?"

        return contentResolver.query(
            CONTENT_URI,
            projection,
            if (params.isAll) null else selection,
            params.selectionArgs,
            order
        ) ?: throw NullPointerException("Cursor NullPointerException")
    }

    override fun fetchGallery(): Cursor {
        return fetchGallery(GalleryQueryParameter())
    }

    private fun getContentsId(cursor: Cursor): String {
        return try {
            cursor.getString(cursor.getColumnIndexOrThrow(ID))
        } catch (ex: Exception) {
            ""
        }
    }

    private fun getPhotoUri(id: String): String {
        return try {
            Uri.withAppendedPath(CONTENT_URI, id).toString()
        } catch (ex: NullPointerException) {
            ""
        }
    }

    private fun getBucketId(cursor: Cursor): String {
        return try {
            cursor.getString(cursor.getColumnIndexOrThrow(BUCKET_ID))
        } catch (ex: Exception) {
            ""
        }
    }

    private fun getBucketName(cursor: Cursor): String {
        return try {
            cursor.getString(cursor.getColumnIndexOrThrow(BUCKET_NAME))
        } catch (ex: Exception) {
            ""
        }
    }
}
