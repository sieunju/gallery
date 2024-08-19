package com.gallery.ui.internal.core

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.content.Intent.ACTION_PICK
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Size
import androidx.activity.result.ActivityResultLauncher
import com.gallery.ui.R
import com.gallery.ui.internal.ImageLoader
import com.gallery.ui.model.PhotoPicker
import com.gallery.ui.model.PickerAlbum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException

/**
 * Description : Gallery 에 필요한 비즈니스 로직 처리 클래스
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

    /**
     * Get Cursor
     */
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
    private fun retrieveList(
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
    private fun retrieveDirectories(): List<PickerAlbum.Normal> {
        val list = mutableListOf<PickerAlbum.Normal>()
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
                    PickerAlbum.Normal(
                        id = prevBucketId,
                        name = prevBucketName,
                        imagePath = prevPhotoUri,
                        count = diffCount
                    ).run { list.add(this) }
                }

                if (prevCount == -1) {
                    prevCount = count
                    PickerAlbum.Normal(
                        id = "ALL",
                        name = context.getString(R.string.txt_album_all),
                        imagePath = photoUri,
                        count = count
                    ).run { list.add(this) }
                }

                prevPhotoUri = photoUri
                prevBucketId = bucketId
                prevBucketName = bucketName
            } else {
                // 맨 마지막 앨범 추가
                if (prevCount != 0) {
                    PickerAlbum.Normal(
                        id = prevBucketId,
                        name = prevBucketName,
                        imagePath = prevPhotoUri,
                        count = prevCount
                    ).run { list.add(this) }
                }

                if (!cursor.isClosed) {
                    cursor.close()
                }
                break
            }
        }
        return list
    }

    /**
     * Request Album List UI
     */
    suspend fun reqAlbumList(): List<PickerAlbum> {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                retrieveDirectories()
            } catch (ex: Exception) {
                listOf()
            }
        }
    }

    /**
     * Request GalleryList UI
     *
     * @param scope Coroutine Scope
     * @param overrideSize Thumbnail Size
     * @param photoCursor Photo Cursor
     * @param photoParams Photo Params
     * @param videoCursor Video Cursor
     * @param videoParams Video Params
     */
    suspend fun reqGalleryList(
        scope: CoroutineScope,
        overrideSize: Int,
        photoCursor: Cursor,
        photoParams: GalleryParams,
        videoCursor: Cursor,
        videoParams: GalleryParams
    ): List<PhotoPicker> {
        return try {
            val photo = scope.async(Dispatchers.IO) {
                return@async retrieveList(photoCursor, photoParams)
                    .onEach {
                        ImageLoader.saveThumbnail(this@GalleryProvider, it, overrideSize)
                    }
            }
            val video = scope.async(Dispatchers.IO) {
                return@async retrieveList(videoCursor, videoParams)
                    .onEach {
                        ImageLoader.saveThumbnail(this@GalleryProvider, it, overrideSize)
                    }
            }
            photo.await().plus(video.await()).sortedByDescending { item ->
                when (item) {
                    is PhotoPicker.Photo -> item.id
                    is PhotoPicker.Video -> item.id
                    is PhotoPicker.Camera -> Long.MAX_VALUE
                }
            }
        } catch (ex: Exception) {
            listOf()
        }
    }

    /**
     * Uri에 따라서 썸네일 가져오는 함수
     * @param uri MediaStore.Images.Media.EXTERNAL_CONTENT_URI or MediaStore.Video.Media.EXTERNAL_CONTENT_URI
     * @param size 썸네일 사이즈
     */
    fun getThumbnail(
        uri: Uri,
        id: Long,
        size: Int
    ): Bitmap? {
        var bitmap: Bitmap?
        try {
            bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.loadThumbnail(
                    ContentUris.withAppendedId(uri, id),
                    Size(size, size),
                    null
                )
            } else {
                when (uri) {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI -> {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Thumbnails.getThumbnail(
                            contentResolver,
                            id,
                            MediaStore.Images.Thumbnails.MINI_KIND,
                            null
                        )
                    }

                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI -> {
                        @Suppress("DEPRECATION")
                        MediaStore.Video.Thumbnails.getThumbnail(
                            contentResolver,
                            id,
                            MediaStore.Video.Thumbnails.MINI_KIND,
                            null
                        )
                    }

                    else -> {
                        null
                    }
                }

            }
        } catch (ex: IOException) {
            Timber.d("Error $ex $id")
            bitmap = getOriginThumbnail(uri, id)
        }
        return bitmap
    }

    private fun getOriginThumbnail(
        uri: Uri,
        id: Long
    ): Bitmap? {
        return contentResolver.openInputStream(ContentUris.withAppendedId(uri, id))?.use {
            val options = BitmapFactory.Options()
            options.inSampleSize = 8
            BitmapFactory.decodeStream(
                it,
                null,
                options
            )
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, size: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > size || width > size) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= size && halfWidth / inSampleSize >= size) {
                inSampleSize *= 2
            }
        }
        Timber.d("SampleSize ${inSampleSize}")

        return inSampleSize
    }

    /**
     * Getter Photo Thumbnail
     * @param imageId Content ID
     * @param size Thumbnail Size
     */
    fun getPhotoThumbnail(
        imageId: Long,
        size: Int
    ): Bitmap {
        return getThumbnail(
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            id = imageId,
            size = size
        )!!
    }

    /**
     * Getter Video Thumbnail
     * @param imageId Content Id
     * @param size Thumbnail Size
     */
    fun getVideoThumbnail(
        imageId: Long,
        size: Int
    ): Bitmap {
        return getThumbnail(
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            id = imageId,
            size = size
        )!!
    }

    fun moveToOtherApp(launcher: ActivityResultLauncher<Intent>) {
        val pickerIntent = Intent(ACTION_GET_CONTENT)
        pickerIntent.type = "image/*"
        Intent.createChooser(Intent(ACTION_PICK).apply {
            type = "image/*"
        }, context.getString(R.string.txt_other_app_gallery)).apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickerIntent))
        }.also { launcher.launch(it) }
    }

    fun getPermissions(): Array<String> {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            list.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.READ_MEDIA_IMAGES)
            list.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        return list.toTypedArray()
    }

    /**
     * Move To Settings Screen
     */
    fun moveToSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            context.startActivity(this)
        }
    }

    /**
     * Getter Gallery Count
     */
    fun getContentCount(): Int {
        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns._ID),
            null,
            null,
            null
        )
        val count = cursor?.count ?: 0
        cursor?.close()
        return count
    }
}