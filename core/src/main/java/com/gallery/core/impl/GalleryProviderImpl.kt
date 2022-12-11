package com.gallery.core.impl

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.gallery.core.GalleryProvider
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
import kotlin.math.*

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
    @Throws(IllegalStateException::class, NullPointerException::class)
    override fun fetchGallery(params: GalleryQueryParameter): Cursor {
        if (!isReadStoragePermissionsGranted()) throw IllegalStateException("Permissions PERMISSION_DENIED")
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
            getPhotoUri(getContentsId(cursor))
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
     * @param path Local Path content://...
     */
    @Throws(IOException::class, IllegalArgumentException::class, FileNotFoundException::class)
    override fun pathToBitmap(path: String, limitWidth: Int): Bitmap {
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
        val bitmap = getFlexibleImageToBitmap(originalImagePath,flexibleItem)
        return bitmapToMultipart(bitmap,multipartKey)
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
        var croppedBitmap = cropBitmapObjectHandleOOM(
            originalBitmap,
            points,
            degreesRotated,
            fixAspectRatio,
            aspectRatioX,
            aspectRatioY,
            flipHorizontally,
            flipVertically
        )
        croppedBitmap = resizeBitmap(
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

    /**
     * 저장소 쓰기 권한 체크
     * @return true 쓰기 권한 허용, false 쓰기 권한 거부 상태
     */
    private fun isWriteStoragePermissionsGranted(): Boolean {
        return context.packageManager.checkPermission(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
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

    /**
     * Get a rectangle for the given 4 points (x0,y0,x1,y1,x2,y2,x3,y3) by finding the min/max 2
     * points that contains the given 4 points and is a straight rectangle.
     */
    private fun getRectFromPoints(
        points: FloatArray,
        imageWidth: Int,
        imageHeight: Int,
        fixAspectRatio: Boolean,
        aspectRatioX: Int,
        aspectRatioY: Int
    ): Rect {
        val left = max(0f, getRectLeft(points)).roundToInt()
        val top = max(0f, getRectTop(points)).roundToInt()
        val right = min(imageWidth.toFloat(), getRectRight(points)).roundToInt()
        val bottom = min(imageHeight.toFloat(), getRectBottom(points)).roundToInt()
        val rect = Rect(left, top, right, bottom)
        if (fixAspectRatio) {
            fixRectForAspectRatio(rect, aspectRatioX, aspectRatioY)
        }
        return rect
    }

    /**
     * Get left value of the bounding rectangle of the given points.
     */
    private fun getRectLeft(points: FloatArray): Float {
        return min(min(min(points[0], points[2]), points[4]), points[6])
    }

    /**
     * Get top value of the bounding rectangle of the given points.
     */
    private fun getRectTop(points: FloatArray): Float {
        return min(min(min(points[1], points[3]), points[5]), points[7])
    }

    /**
     * Get right value of the bounding rectangle of the given points.
     */
    private fun getRectRight(points: FloatArray): Float {
        return max(max(max(points[0], points[2]), points[4]), points[6])
    }

    /**
     * Get bottom value of the bounding rectangle of the given points.
     */
    private fun getRectBottom(points: FloatArray): Float {
        return max(max(max(points[1], points[3]), points[5]), points[7])
    }

    /**
     * Get width of the bounding rectangle of the given points.
     */
    private fun getRectWidth(points: FloatArray): Float {
        return getRectRight(points) - getRectLeft(points)
    }

    /**
     * Get height of the bounding rectangle of the given points.
     */
    private fun getRectHeight(points: FloatArray): Float {
        return getRectBottom(points) - getRectTop(points)
    }

    /**
     * Get horizontal center value of the bounding rectangle of the given points.
     */
    private fun getRectCenterX(points: FloatArray): Float {
        return (getRectRight(points) + getRectLeft(points)) / 2f
    }

    /**
     * Get vertical center value of the bounding rectangle of the given points.
     */
    private fun getRectCenterY(points: FloatArray): Float {
        return (getRectBottom(points) + getRectTop(points)) / 2f
    }

    /**
     * Fix the given rectangle if it doesn't confirm to aspect ration rule.<br></br>
     * Make sure that width and height are equal if 1:1 fixed aspect ratio is requested.
     */
    private fun fixRectForAspectRatio(rect: Rect, aspectRatioX: Int, aspectRatioY: Int) {
        if (aspectRatioX == aspectRatioY && rect.width() != rect.height()) {
            if (rect.height() > rect.width()) {
                rect.bottom -= rect.height() - rect.width()
            } else {
                rect.right -= rect.width() - rect.height()
            }
        }
    }

    /**
     * Special crop of bitmap rotated by not stright angle, in this case the original crop bitmap
     * contains parts beyond the required crop area, this method crops the already cropped and rotated
     * bitmap to the final rectangle.<br></br>
     * Note: rotating by 0, 90, 180 or 270 degrees doesn't require extra cropping.
     */
    private fun cropForRotatedImage(
        bitmap: Bitmap?,
        points: FloatArray,
        rect: Rect,
        degreesRotated: Int,
        fixAspectRatio: Boolean,
        aspectRatioX: Int,
        aspectRatioY: Int
    ): Bitmap? {
        var tempBitmap = bitmap
        if (degreesRotated % 90 != 0) {
            var adjLeft = 0
            var adjTop = 0
            var width = 0
            var height = 0
            val rads = Math.toRadians(degreesRotated.toDouble())
            val compareTo =
                if (degreesRotated < 90 || degreesRotated in 181..269) rect.left else rect.right
            var i = 0
            while (i < points.size) {
                if (points[i] >= compareTo - 1 && points[i] <= compareTo + 1) {
                    adjLeft = abs(sin(rads) * (rect.bottom - points[i + 1]))
                        .toInt()
                    adjTop = abs(cos(rads) * (points[i + 1] - rect.top))
                        .toInt()
                    width = abs((points[i + 1] - rect.top) / sin(rads))
                        .toInt()
                    height = abs((rect.bottom - points[i + 1]) / cos(rads))
                        .toInt()
                    break
                }
                i += 2
            }
            rect[adjLeft, adjTop, adjLeft + width] = adjTop + height
            if (fixAspectRatio) {
                fixRectForAspectRatio(rect, aspectRatioX, aspectRatioY)
            }
            val bitmapTmp = tempBitmap
            tempBitmap = Bitmap.createBitmap(
                bitmap!!,
                rect.left,
                rect.top,
                rect.width(),
                rect.height()
            )
            if (bitmapTmp != tempBitmap) {
                bitmapTmp?.recycle()
            }
        }
        return tempBitmap
    }

    /**
     * Crop image bitmap from given bitmap using the given points in the original bitmap and the given
     * rotation.<br></br>
     * if the rotation is not 0,90,180 or 270 degrees then we must first crop a larger area of the
     * image that contains the requires rectangle, rotate and then crop again a sub rectangle.<br></br>
     * If crop fails due to OOM we scale the cropping image by 0.5 every time it fails until it is
     * small enough.
     */
    private fun cropBitmapObjectHandleOOM(
        bitmap: Bitmap,
        points: FloatArray,
        degreesRotated: Int,
        fixAspectRatio: Boolean,
        aspectRatioX: Int,
        aspectRatioY: Int,
        flipHorizontally: Boolean,
        flipVertically: Boolean
    ): Bitmap? {
        var scale = 1
        while (true) {
            try {
                return cropBitmapObjectWithScale(
                    bitmap,
                    points,
                    degreesRotated,
                    fixAspectRatio,
                    aspectRatioX,
                    aspectRatioY,
                    1 / scale.toFloat(),
                    flipHorizontally,
                    flipVertically
                )
            } catch (e: OutOfMemoryError) {
                scale *= 2
                if (scale > 8) {
                    throw e
                }
            }
        }
    }

    /**
     * Crop image bitmap from given bitmap using the given points in the original bitmap and the given
     * rotation.<br></br>
     * if the rotation is not 0,90,180 or 270 degrees then we must first crop a larger area of the
     * image that contains the requires rectangle, rotate and then crop again a sub rectangle.
     *
     * @param scale how much to scale the cropped image part, use 0.5 to lower the image by half (OOM
     * handling)
     */
    private fun cropBitmapObjectWithScale(
        bitmap: Bitmap,
        points: FloatArray,
        degreesRotated: Int,
        fixAspectRatio: Boolean,
        aspectRatioX: Int,
        aspectRatioY: Int,
        scale: Float,
        flipHorizontally: Boolean,
        flipVertically: Boolean
    ): Bitmap? {
        // get the rectangle in original image that contains the required cropped area (larger for non
        // rectangular crop)
        val rect = getRectFromPoints(
            points,
            bitmap.width,
            bitmap.height,
            fixAspectRatio,
            aspectRatioX,
            aspectRatioY
        )
        // crop and rotate the cropped image in one operation
        val matrix = Matrix()
        matrix.setRotate(degreesRotated.toFloat(), bitmap.width / 2.0f, bitmap.height / 2.0f)
        matrix.postScale(
            if (flipHorizontally) -scale else scale,
            if (flipVertically) -scale else scale
        )
        var result = Bitmap.createBitmap(
            bitmap,
            rect.left,
            rect.top,
            rect.width(),
            rect.height(),
            matrix,
            true
        )
        if (result == bitmap) {
            // corner case when all bitmap is selected, no worth optimizing for it
            result = bitmap.copy(bitmap.config, false)
        }
        // rotating by 0, 90, 180 or 270 degrees doesn't require extra cropping
        if (degreesRotated % 90 != 0) {
            // extra crop because non rectangular crop cannot be done directly on the image without
            // rotating first
            result = cropForRotatedImage(
                result, points, rect, degreesRotated, fixAspectRatio, aspectRatioX, aspectRatioY
            )
        }
        return result
    }

    /**
     * Resize the given bitmap to the given width/height by the given option.<br></br>
     */
    private fun resizeBitmap(
        bitmap: Bitmap?,
        reqWidth: Int,
        reqHeight: Int,
        options: RequestSizeOptions
    ): Bitmap {
        try {
            if (reqWidth > 0 &&
                reqHeight > 0 &&
                (options === RequestSizeOptions.RESIZE_FIT ||
                        options === RequestSizeOptions.RESIZE_INSIDE ||
                        options === RequestSizeOptions.RESIZE_EXACT)
            ) {
                var resized: Bitmap? = null
                if (options === RequestSizeOptions.RESIZE_EXACT) {
                    resized = Bitmap.createScaledBitmap(bitmap!!, reqWidth, reqHeight, false)
                } else {
                    val width = bitmap!!.width
                    val height = bitmap.height
                    val scale = max(width / reqWidth.toFloat(), height / reqHeight.toFloat())
                    if (scale > 1 || options === RequestSizeOptions.RESIZE_FIT) {
                        resized = Bitmap.createScaledBitmap(
                            bitmap, (width / scale).toInt(), (height / scale).toInt(), false
                        )
                    }
                }
                if (resized != null) {
                    if (resized != bitmap) {
                        bitmap.recycle()
                    }
                    return resized
                }
            }
        } catch (e: Exception) {
        }
        return bitmap!!
    }
}
