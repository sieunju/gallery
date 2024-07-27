package com.gallery.ui.internal.core

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import com.gallery.ui.R
import com.gallery.ui.model.GalleryFilterData
import com.gallery.ui.model.PhotoPicker

/**
 * Description : Gallery Provider 구현체 클래스
 *
 * Created by juhongmin on 2024. 7. 27.
 */
internal class GalleryProvider(
    private val context: Context
) {
    private val contentResolver: ContentResolver by lazy { context.contentResolver }

    companion object {
        const val ID = MediaStore.MediaColumns._ID
    }

    @Throws(IllegalStateException::class, NullPointerException::class)
    fun retrieveCursor(
        params: GalleryParams
    ): Cursor {
        return contentResolver.query(
            params.uri,
            params.getColumns(),
            if (params.isAll) null else "${MediaStore.MediaColumns.BUCKET_ID} ==?",
            params.selectionArgs,
            params.order
        ) ?: throw NullPointerException("Cursor NullPointerException")
    }

    /**
     * Retrieve Gallery List
     */
    fun retrieveList(
        cursor: Cursor,
        params: GalleryParams
    ): List<PhotoPicker> {
        val list = mutableListOf<PhotoPicker>()
        for (idx in 0 until params.pageSize) {
            if (cursor.moveToNext()) {
                if (params.uri == MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {
                    list.add(PhotoPicker.Photo(cursor))
                } else if (params.uri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI) {
                    list.add(PhotoPicker.Video(cursor))
                }
            }
        }
        return list
    }

    /**
     * Retrieve Directories
     */
    fun retrieveDirectories(): List<GalleryFilterData> {
        val dataList = mutableListOf<GalleryFilterData>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        )
        val selection = StringBuilder()
        val selectionArgs = mutableListOf<String>()
        val sort = "$ID DESC "
        var prevPhotoUri = ""
        var prevBucketId = ""
        var prevBucketName = ""
        var prevCount: Int = -1
        while (true) {
            val cursor = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                if (selection.isEmpty()) null else selection.toString(),
                if (selectionArgs.isEmpty()) null else selectionArgs.toTypedArray(),
                sort
            ) ?: break
            if (cursor.moveToFirst()) {
                val contentId = cursor.getString(
                    cursor.getColumnIndexOrThrow(ID)
                )
                val photoUri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentId
                ).toString()
                val bucketId = cursor.getString(
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                )
                val bucketName = cursor.getString(
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                )
                val count = cursor.count

                if (!cursor.isClosed) {
                    cursor.close()
                }

                if (selection.isNotEmpty()) {
                    selection.append(" AND ")
                }

                selection.append(MediaStore.Images.Media.BUCKET_ID)
                selection.append(" !=?")
                selectionArgs.add(bucketId)

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

                if (prevCount == -1) {
                    prevCount = count
                    dataList.add(
                        GalleryFilterData(
                            bucketId = "ALL",
                            bucketName = context.getString(R.string.txt_album_all),
                            photoUri = photoUri,
                            count = count
                        )
                    )
                }

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
                break
            }
        }
        return dataList
    }

    fun getPhotoThumbnail(imageId: Long, size: Int): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver.loadThumbnail(
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId),
                Size(size, size),
                null
            )
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Thumbnails.getThumbnail(
                contentResolver,
                imageId,
                MediaStore.Images.Thumbnails.MINI_KIND,
                BitmapFactory.Options()
            )
        }
    }

    fun getVideoThumbnail(imageId: Long, size: Int): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver.loadThumbnail(
                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, imageId),
                Size(size, size),
                null
            )
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Video.Thumbnails.getThumbnail(
                contentResolver,
                imageId,
                MediaStore.Video.Thumbnails.MINI_KIND,
                BitmapFactory.Options()
            )
        }
    }
}