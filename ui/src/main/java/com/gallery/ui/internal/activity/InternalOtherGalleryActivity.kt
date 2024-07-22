package com.gallery.ui.internal.activity

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.content.Intent.ACTION_PICK
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.gallery.ui.R
import timber.log.Timber

/**
 * Description : 외부앱 실행후 리턴받는 Fake Activity
 *
 * Created by juhongmin on 2024. 7. 21.
 */
internal class InternalOtherGalleryActivity : AppCompatActivity() {

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            Timber.d("ImageUri ${uri}")
            finish()
        } else {
            Timber.d("NULL")
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(0, 0)
        super.onCreate(savedInstanceState)
        val pickerIntent = Intent(ACTION_GET_CONTENT)
        pickerIntent.type = "image/*"
        Intent.createChooser(Intent(ACTION_PICK).apply {
            type = "image/*"
        }, getString(R.string.txt_other_app_gallery)).apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickerIntent))
        }.also { imagePickerLauncher.launch(it) }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}
