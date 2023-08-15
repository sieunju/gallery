package com.gallery.core.impl

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import androidx.annotation.WorkerThread
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.gallery.core.CropImageEditExtensions
import com.gallery.core.Extensions
import com.gallery.core.Extensions.getBucketId
import com.gallery.core.Extensions.getBucketName
import com.gallery.core.Extensions.getContentsId
import com.gallery.core.Extensions.getDisplayName
import com.gallery.core.GalleryProvider
import com.gallery.core.enums.ImageType
import com.gallery.core.model.GalleryFilterData
import com.gallery.core.model.GalleryQueryParameter
import com.gallery.model.CropImageEditModel
import com.gallery.model.FlexibleStateItem
import com.gallery.model.RequestSizeOptions
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Description : Gallery Provider 구현체 클래스
 * CropImageEditView Reference  https://github.com/CanHub/Android-Image-Cropper
 *
 * Created by juhongmin on 2022/09/13
 */
@Suppress("unused")
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

        val DISPLAY_NAME = MediaStore.Images.Media.DISPLAY_NAME
    }

    /**
     * Fetch Gallery Directory
     * 갤러리 조회 함수
     */
    @Throws(IllegalStateException::class, Exception::class)
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

                if (cursor.moveToFirst()) {
                    val contentId = cursor.getContentsId()
                    val photoUri = Extensions.getPhotoUri(contentId)
                    val bucketId = cursor.getBucketId()
                    val bucketName = cursor.getBucketName()
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
    @Throws(IllegalStateException::class, NullPointerException::class)
    override fun fetchGallery(params: GalleryQueryParameter): Cursor {
        if (!isReadStoragePermissionsGranted()) throw IllegalStateException("Permissions PERMISSION_DENIED")
        val order = "$ID ${params.order}"
        return contentResolver.query(
            CONTENT_URI,
            params.getColumns(),
            if (params.isAll) null else "$BUCKET_ID ==?",
            params.selectionArgs,
            order
        ) ?: throw NullPointerException("Cursor NullPointerException")
    }

    /**
     * Fetch All Gallery
     */
    @Throws(IllegalStateException::class, NullPointerException::class)
    override fun fetchGallery(): Cursor {
        return fetchGallery(GalleryQueryParameter())
    }

    /**
     * Converter Current Cursor -> Images Local Uri content://
     * @param cursor Current Cursor
     */
    @Throws(NullPointerException::class, IllegalArgumentException::class)
    override fun cursorToPhotoUri(cursor: Cursor): String {
        return try {
            Extensions.getPhotoUri(cursor.getContentsId())
        } catch (ex: Exception) {
            throw ex
        }
    }

    /**
     * Converter Local Path -> Bitmap
     * @param path Local Path content://...
     */
    @Throws(IOException::class, IllegalArgumentException::class, FileNotFoundException::class)
    override fun pathToBitmap(path: String): Bitmap {
        return pathToBitmap(path, -1)
    }

    /**
     * Converter Local Path -> Bitmap
     *
     * 이미지 회전을 따로 처리 안해도 제대로 노출 되어 주석 처리
     *
     * 만약 해당 함수를 사용 할때 이미지 회전 이슈가 발생 한다면 Glide 라이브러리 사용 권장
     *
     * Ex.) Glide.with(context).asBitmap().load(path).submit().get()
     * @param path Local Path content://...
     * @param limitWidth 리사이징할 너비값
     * @exception IOException
     * @exception IllegalArgumentException
     * @exception FileNotFoundException
     * @return Bitmap
     */
    @Throws(IOException::class, IllegalArgumentException::class, FileNotFoundException::class)
    override fun pathToBitmap(path: String, limitWidth: Int): Bitmap {
        // return Glide.with(context).asBitmap().load(path).submit().get()
        val uri = Uri.parse(path)
        var bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(
                    contentResolver,
                    uri
                )
            ) { decoder: ImageDecoder, _: ImageDecoder.ImageInfo?, _: ImageDecoder.Source? ->
                decoder.isMutableRequired = true
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
        }

        val matrix = Matrix()
        // 이미지 회전 이슈 처리.
//        contentResolver.openInputStream(uri)?.let {
//            val exif = ExifInterface(it)
//            val orientation = exif.getAttributeInt(
//                ExifInterface.TAG_ORIENTATION,
//                ExifInterface.ORIENTATION_NORMAL
//            )
//            Extensions.setRotate(orientation, matrix)
//
//            it.close()
//        }

        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        // 이미지 리사이징 처리 필요에 따라 사용할지 말지 정의.
        if (limitWidth != -1 && limitWidth < bitmap.width) {
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
    @Throws(IllegalArgumentException::class)
    override fun pathToBitmap(redId: Int): Bitmap {
        return BitmapFactory.decodeResource(context.resources, redId)
    }

    /**
     * Converter Contents Path to Multipart
     * @param path ex.) content://
     * @param name Multipart.Body Upload key
     * @param resizeWidth Resize Limit Width
     */
    @Throws(
        IOException::class,
        NullPointerException::class,
        IllegalArgumentException::class,
        FileNotFoundException::class
    )
    override fun pathToMultipart(path: String, name: String, resizeWidth: Int): MultipartBody.Part {
        return bitmapToMultipart(pathToBitmap(path, resizeWidth), name)
    }

    /**
     * Converter Bitmap to OkHttp.MultipartBody
     * .jpg Compress Type
     * @param bitmap Source Bitmap
     * @param name MultipartBody key Name
     */
    @Throws(NullPointerException::class, IllegalArgumentException::class)
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
    @Throws(NullPointerException::class, IllegalArgumentException::class)
    override fun bitmapToMultipart(
        bitmap: Bitmap,
        name: String,
        filename: String,
        suffix: String
    ): MultipartBody.Part {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Extensions.toCompressFormat(suffix), 100, stream)
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
    @Throws(NullPointerException::class, IllegalArgumentException::class)
    override fun copyBitmapToFile(bitmap: Bitmap, fos: FileOutputStream): Boolean {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        return true
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
    @Throws(IllegalArgumentException::class)
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
    @Throws(IOException::class)
    override fun createTempFile(): File {
        return createFile("${SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())}_", ".jpg")
    }

    @Throws(IOException::class)
    override fun createFile(name: String, suffix: String): File {
        return try {
            File.createTempFile(
                name,
                suffix,
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            )
        } catch (ex: IOException) {
            throw ex
        }
    }

    @Throws(NullPointerException::class, IllegalArgumentException::class)
    override fun getFlexibleImageToBitmap(
        originalImagePath: String,
        flexibleItem: FlexibleStateItem
    ): Bitmap {
        val editLocation =
            flexibleItem.getImageLocation() ?: throw NullPointerException("EditLocation is Null")
        return getFlexibleImageToBitmap(
            originalImagePath, editLocation, flexibleItem.viewWidth, flexibleItem.viewHeight
        )
    }

    @Throws(NullPointerException::class, IllegalArgumentException::class)
    override fun getFlexibleImageToBitmap(
        originalImagePath: String,
        srcRect: RectF,
        width: Int,
        height: Int
    ): Bitmap {
        return getFlexibleImageToBitmap(pathToBitmap(originalImagePath), srcRect, width, height)
    }

    @Throws(NullPointerException::class, IllegalArgumentException::class)
    override fun getFlexibleImageToBitmap(
        originalBitmap: Bitmap,
        srcRect: RectF,
        width: Int,
        height: Int
    ): Bitmap {
        return getFlexibleImageToBitmap(
            originalBitmap,
            srcRect,
            width,
            height,
            Color.WHITE
        )
    }

    @Throws(NullPointerException::class, IllegalArgumentException::class)
    override fun getFlexibleImageToBitmap(
        originalBitmap: Bitmap,
        srcRect: RectF,
        width: Int,
        height: Int,
        color: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val tempBitmap = Bitmap.createScaledBitmap(
            originalBitmap,
            srcRect.width().toInt(),
            srcRect.height().toInt(),
            true
        )
        Canvas(bitmap).apply {
            drawColor(color)
            drawBitmap(tempBitmap, null, srcRect, null)
        }
        return bitmap
    }

    override fun getFlexibleImageToMultipart(
        originalImagePath: String,
        flexibleItem: FlexibleStateItem,
        multipartKey: String
    ): MultipartBody.Part {
        val bitmap = getFlexibleImageToBitmap(originalImagePath, flexibleItem)
        return bitmapToMultipart(bitmap, multipartKey)
    }

    @Throws(
        FileNotFoundException::class,
        SecurityException::class,
        NullPointerException::class,
        IllegalArgumentException::class
    )
    override fun saveBitmapToFile(bitmap: Bitmap): File {
        val file = createTempFile()
        val fos = FileOutputStream(file)
        copyBitmapToFile(bitmap, fos)
        fos.close()
        return file
    }

    @WorkerThread
    @Throws(NullPointerException::class)
    override fun getCropImageEditToBitmap(editModel: CropImageEditModel): Bitmap {
        return getCropImageEditToBitmap(
            editModel.bitmap,
            editModel.points,
            editModel.degreesRotated,
            editModel.fixAspectRatio,
            editModel.aspectRatioX,
            editModel.aspectRatioY,
            editModel.flipHorizontally,
            editModel.flipVertically
        )
    }

    @WorkerThread
    @Throws(NullPointerException::class)
    override fun getCropImageEditToBitmap(
        originalBitmap: Bitmap?,
        points: FloatArray,
        degreesRotated: Int,
        fixAspectRatio: Boolean,
        aspectRatioX: Int,
        aspectRatioY: Int,
        flipHorizontally: Boolean,
        flipVertically: Boolean
    ): Bitmap {
        if (originalBitmap == null) {
            throw NullPointerException("originalBitmap is Null")
        }
        var croppedBitmap = CropImageEditExtensions.cropBitmapObjectHandleOOM(
            originalBitmap,
            points,
            degreesRotated,
            fixAspectRatio,
            aspectRatioX,
            aspectRatioY,
            flipHorizontally,
            flipVertically
        )
        croppedBitmap = CropImageEditExtensions.resizeBitmap(
            croppedBitmap,
            0,
            0,
            RequestSizeOptions.RESIZE_FIT
        )
        return croppedBitmap
    }

    @Throws(NullPointerException::class, IllegalArgumentException::class)
    override fun createGalleryPhotoUri(authority: String): Uri {
        val file = createTempFile()
        return FileProvider.getUriForFile(
            context,
            authority,
            file
        )
    }

    @Throws(Exception::class)
    override fun saveGalleryPicture(pictureUri: String, name: String): Pair<Boolean, String> {
        // Scoped Storage
        try {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "${name}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                    put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
                    put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                }

                val collection =
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val item = contentResolver.insert(collection, values)
                    ?: return false to "Insert Resolver Error"
                val parcelFile = contentResolver.openFileDescriptor(item, "w", null)
                    ?: return false to "openFileDescriptor Error"
                val fos = FileOutputStream(parcelFile.fileDescriptor)
                val bitmap = pathToBitmap(pictureUri)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                fos.close()
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(item, values, null, null)
                parcelFile.close()
                true to pictureUri
            } else {
                // Legacy Version
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "${name}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
                    put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
                    put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                }
                val url =
                    contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: return false to "Insert Resolver Error"
                val bitmap = pathToBitmap(pictureUri)
                val imageOut = contentResolver.openOutputStream(url)
                try {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, imageOut)
                } finally {
                    imageOut?.close()
                }
                true to pictureUri
            }
        } catch (ex: Exception) {
            return false to ex.toString()
        }
    }

    override fun getImageType(imagePath: String): ImageType {
        return try {
            // content://media/external/images/media/1
            val contentUrl = Uri.parse(imagePath)
            val input = contentResolver.openInputStream(contentUrl) ?: return ImageType.UN_KNOWN
            val exifInterface = ExifInterface(input)
            input.close()

            // TAG_MODEL -> 휴대폰 단말기 모델, TAG_F_NUMBER -> 빛의 양, TAG_PHOTOGRAPHIC_SENSITIVITY -> Camera ISO,
            // TAG_EXPOSURE_TIME -> 노출 시간, TAG_APERTURE_VALUE -> 조리개 값
            // 기타 카메라 정보들이 있는 경우 캡처된 이미지 X
            if (exifInterface.getAttribute(ExifInterface.TAG_MODEL) != null && (
                        exifInterface.getAttribute(ExifInterface.TAG_F_NUMBER) != null ||
                                exifInterface.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY) != null ||
                                exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) != null ||
                                exifInterface.getAttribute(ExifInterface.TAG_APERTURE_VALUE) != null
                        )
            ) {
                return ImageType.CAMERA
            }

            val projection = arrayOf(
                ID,
                BUCKET_ID,
                BUCKET_NAME,
                DISPLAY_NAME
            )

            val contentId = contentUrl.lastPathSegment ?: return ImageType.ETC

            val cursor = contentResolver.query(
                CONTENT_URI,
                projection,
                "$ID ==?",
                arrayOf(contentId),
                null
            ) ?: return ImageType.UN_KNOWN
            cursor.moveToNext()
            val displayName = cursor.getDisplayName().lowercase()
            val bucketName = cursor.getBucketName().lowercase()
            cursor.close()

            // ScreenShot
            if (bucketName.contains("screenshots") ||
                displayName.startsWith("screenshot_")
            ) {
                return ImageType.SCREENSHOT
            }

            return ImageType.ETC
        } catch (ex: Exception) {
            ImageType.UN_KNOWN
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

    override fun getThumbnail(imageId: Long): Bitmap {
        return getThumbnail(imageId, 300, 300)
    }

    override fun getThumbnail(imageId: Long, width: Int, height: Int): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver.loadThumbnail(
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId),
                Size(width, height),
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
}
