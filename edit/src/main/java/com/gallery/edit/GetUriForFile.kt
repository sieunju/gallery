package com.gallery.edit

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

/**
 * This class exist because of two issues. One is related to the new Scope Storage for OS 10+
 * Where we should not access external storage anymore. Because of this we cannot get a external uri
 *
 * Using FileProvider to retrieve the path can return a value that is not the real one for some devices
 * This happen in specific devices and OSs. Because of this is needed to do a lot of if/else and
 * try/catch to just use the latest cases when need.
 *
 * This code is not good, but work. I don't suggest anyone to reproduce it.
 *
 * Most of the devices will work fine, but if you worry about memory usage, please remember to clean
 * the cache from time to time,
 */

const val authority = ".cropper.fileprovider"
const val CROP_LIB_CACHE = "CROP_LIB_CACHE"

internal fun getFilePathFromUri(context: Context, uri: Uri, uniqueName: Boolean): String =
    if (uri.path?.contains("file://") == true) uri.path!!
    else getFileFromContentUri(context, uri, uniqueName).path

private fun getFileFromContentUri(context: Context, contentUri: Uri, uniqueName: Boolean): File {
    // Preparing Temp file name
    val fileExtension = getFileExtension(context, contentUri) ?: ""
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = ("temp_file_" + if (uniqueName) timeStamp else "") + ".$fileExtension"
    // Creating Temp file
    val tempFile = File(context.cacheDir, fileName)
    tempFile.createNewFile()
    // Initialize streams
    var oStream: FileOutputStream? = null
    var inputStream: InputStream? = null

    try {
        oStream = FileOutputStream(tempFile)
        inputStream = context.contentResolver.openInputStream(contentUri)

        inputStream?.let { copy(inputStream, oStream) }
        oStream.flush()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        // Close streams
        inputStream?.close()
        oStream?.close()
    }

    return tempFile
}

private fun getFileExtension(context: Context, uri: Uri): String? =
    if (uri.scheme == ContentResolver.SCHEME_CONTENT)
        MimeTypeMap.getSingleton().getExtensionFromMimeType(context.contentResolver.getType(uri))
    else uri.path?.let { MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(File(it)).toString()) }

@Throws(IOException::class)
private fun copy(source: InputStream, target: OutputStream) {
    val buf = ByteArray(8192)
    var length: Int
    while (source.read(buf).also { length = it } > 0) {
        target.write(buf, 0, length)
    }
}

internal fun getUriForFile(context: Context, file: File): Uri {
    val authority = context.packageName + authority
    try {
        return FileProvider.getUriForFile(context, authority, file)
    } catch (e: Exception) {
        try {
            // Note: Periodically clear this cache
            val cacheFolder = File(context.cacheDir, CROP_LIB_CACHE)
            val cacheLocation = File(cacheFolder, file.name)
            var input: InputStream? = null
            var output: OutputStream? = null
            try {
                input = FileInputStream(file)
                output = FileOutputStream(cacheLocation) // appending output stream
                input.copyTo(output)
                return FileProvider.getUriForFile(context, authority, cacheLocation)
            } catch (e: Exception) {
                val path = "content://$authority/files/my_images/"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Files.createDirectories(Paths.get(path))
                } else {
                    val directory = File(path)
                    if (!directory.exists()) directory.mkdirs()
                }

                return Uri.parse("$path${file.name}")
            } finally {
                input?.close()
                output?.close()
            }
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val cacheDir = context.externalCacheDir
                cacheDir?.let {
                    try {
                        return Uri.fromFile(File(cacheDir.path, file.absolutePath))
                    } catch (e: Exception) {
                    }
                }
            }
            return Uri.fromFile(file)
        }
    }
}
