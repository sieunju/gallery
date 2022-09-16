package com.gallery.core.impl

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.gallery.core.GalleryProvider
import com.gallery.core.model.GalleryFilterData
import com.gallery.core.model.GalleryQueryParameter
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

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
     * Fetch Gallery Directory
     * 갤러리 조회 함수
     */
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

    /**
     * Fetch Selected FilterId Gallery
     * @param params QueryParameter
     */
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

    /**
     * Fetch All Gallery
     */
    override fun fetchGallery(): Cursor {
        return fetchGallery(GalleryQueryParameter())
    }

    /**
     * Converter Current Cursor -> Images Local Uri content://
     * @param cursor Current Cursor
     */
    override fun cursorToPhotoUri(cursor: Cursor): String? {
        return try {
            getPhotoUri(getContentsId(cursor))
        } catch (ex: Exception) {
            null
        }
    }

    /**
     * Converter Local Path -> Bitmap
     * @param path Local Path content://...
     */
    override fun pathToBitmap(path: String, limitWidth: Int): Bitmap {
        val uri = Uri.parse(path)
        var bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
        } else {
            BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
        }

        val matrix = Matrix()
        // 이미지 회전 이슈 처리.
        contentResolver.openInputStream(uri)?.let {
            val exif = ExifInterface(it)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            setRotate(
                orientation = orientation,
                matrix = matrix
            )

            it.close()
        }

        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        // 이미지 리사이징 처리 필요에 따라 사용할지 말지 정의.
        if (limitWidth < bitmap.width) {
            // 비율에 맞게 높이값 계산
            val height = limitWidth * bitmap.height / bitmap.width
            bitmap = Bitmap.createScaledBitmap(bitmap, limitWidth, height, true)
        }

        return bitmap
    }

    /**
     * DrawableRes to Bitmap
     * @param redId Drawable Resource Id
     */
    override fun pathToBitmap(redId: Int): Bitmap {
        return BitmapFactory.decodeResource(context.resources, redId)
    }

    /**
     * Converter Bitmap to OkHttp.MultipartBody
     * .jpg Compress Type
     * @param bitmap Source Bitmap
     * @param name MultipartBody key Name
     */
    override fun bitmapToMultipart(bitmap: Bitmap, name: String): MultipartBody.Part {
        return bitmapToMultipart(bitmap, name, "${System.currentTimeMillis()}.jpg", ".jpg")
    }

    /**
     * Converter Bitmap to OkHttp.MultipartBody
     * @param bitmap Source Bitmap
     * @param name MultipartBody key Name
     * @param filename MultipartBody FileName
     * @param suffix ex.) .jpg, .png..
     */
    override fun bitmapToMultipart(
        bitmap: Bitmap,
        name: String,
        filename: String,
        suffix: String
    ): MultipartBody.Part {
        val stream = ByteArrayOutputStream()
        bitmap.compress(toCompressFormat(suffix), 100, stream)
        return MultipartBody.Part.createFormData(
            name = name,
            filename = "${filename}$suffix",
            body = stream.toByteArray().toRequestBody(
                contentType = suffix.toMediaTypeOrNull()
            )
        )
    }

    /**
     * Copy Bitmap to File
     * @param bitmap Source Bitmap
     * @param fos FileStream
     */
    override fun copyBitmapToFile(bitmap: Bitmap, fos: FileOutputStream) {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
    }

    /**
     * Delete File
     *
     * @param path File Path
     * @return true -> Delete Success, false -> Delete Fail
     */
    override fun deleteFile(path: String?): Boolean {
        return if (path != null) {
            try {
                contentResolver.delete(Uri.parse(path), null, null)
                true
            } catch (ex: Exception) {
                false
            }
        } else {
            false
        }
    }

    /**
     * Delete Cache Directory
     */
    override fun deleteCacheDirectory(): Boolean {
        return try {
            val dir = context.getExternalFilesDir(
                Environment.DIRECTORY_PICTURES
            ) ?: return false
            val iterator = dir.listFiles()?.iterator() ?: return false
            while (iterator.hasNext()) {
                try {
                    iterator.next().delete()
                } catch (_: Exception) {
                }
            }
            true
        } catch (ex: Exception) {
            false
        }
    }

    /**
     * 비율에 맞게 비트맵 리사이징 하는 함수.
     * @param bitmap Source Bitmap
     * @param width 리사이징 하고 싶은 너비
     * @param height 리사이징 하고 싶은 높이
     *
     * @return Resize Bitmap..
     */
    override fun ratioResizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        val xScale: Float = width.toFloat() / bitmap.width.toFloat()
        val yScale: Float = height.toFloat() / bitmap.height.toFloat()
        // 가장 큰 비율 가져옴.
        val maxScale = Math.max(xScale, yScale)

        val scaledWidth: Float = maxScale * bitmap.width.toFloat()
        val scaledHeight: Float = maxScale * bitmap.height.toFloat()

        val left: Float = (width - scaledWidth) / 2
        val right: Float = left + scaledWidth
        val top: Float = (height - scaledHeight) / 2
        val bottom: Float = top + scaledHeight

        val rect = RectF(left, top, right, bottom)

        val dest = Bitmap.createBitmap(width, height, bitmap.config)
        val canvas = Canvas(dest)
        canvas.drawBitmap(bitmap, null, rect, null)
        return dest
    }

    @SuppressLint("SimpleDateFormat")
    override fun createTempFile(): File? {
        return createFile("${SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())}_", ".jpg")
    }

    override fun createFile(name: String, suffix: String): File? {
        return try {
            File.createTempFile(
                name,
                suffix,
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            )
        } catch (ex: IOException) {
            null
        }
    }

    private fun toCompressFormat(suffix: String): Bitmap.CompressFormat {
        return if (suffix == ".png") {
            Bitmap.CompressFormat.PNG
        } else {
            Bitmap.CompressFormat.JPEG
        }
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

    /**
     * set Image Rotate Func.
     * @param orientation ExifInterface Orientation
     * @param matrix Image Matrix
     *
     */
    private fun setRotate(orientation: Int, matrix: Matrix): Boolean {
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
