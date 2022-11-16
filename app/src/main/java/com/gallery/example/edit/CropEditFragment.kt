package com.gallery.example.edit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.gallery.core.GalleryProvider
import com.gallery.core.toPhotoUri
import com.gallery.edit.CropImageEditView
import com.gallery.example.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
internal class CropEditFragment : Fragment(R.layout.f_crop_edit) {

    @Inject
    lateinit var galleryProvider: GalleryProvider

    private lateinit var ivCropEdit: CropImageEditView
    private lateinit var clCapture: ConstraintLayout
    private lateinit var ivCapture: AppCompatImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(view) {
            ivCropEdit = findViewById(R.id.ivEdit)
            clCapture = findViewById(R.id.clCapture)
            ivCapture = findViewById(R.id.ivCapture)

            findViewById<Button>(R.id.bRandomGallery).setOnClickListener {
                performRandomGallery()
            }

            findViewById<Button>(R.id.bRotate).setOnClickListener {
                ivCropEdit.rotatedDegrees += 90
            }

            findViewById<Button>(R.id.bFlipHorizontal).setOnClickListener {
                ivCropEdit.isFlippedHorizontally = !ivCropEdit.isFlippedHorizontally
            }

            findViewById<Button>(R.id.bFlipVertical).setOnClickListener {
                ivCropEdit.isFlippedVertically = !ivCropEdit.isFlippedVertically
            }

            findViewById<Button>(R.id.bCapture).setOnClickListener {
                performCapture(true)
            }

            findViewById<Button>(R.id.bCaptureHidden).setOnClickListener {
                performCapture(false)
            }
        }
    }

    private fun performRandomGallery() {
        lifecycleScope.launch(Dispatchers.Main) {
            val bitmap = getRandomGalleryBitmap()
//            val bitmap = getSampleBitmap()
            if (bitmap != null) {
                ivCropEdit.setImageBitmap(bitmap)
            }
        }
    }

    private suspend fun getSampleBitmap(): Bitmap? = withContext(Dispatchers.IO) {
        return@withContext try {
            val bytes =
                URL("https://image.zdnet.co.kr/2021/08/27/48a2291e7cbed1be50aa28880b58477e.jpg")
                    .readBytes()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (ex: IOException) {
            null
        }
    }

    private suspend fun getRandomGalleryBitmap(): Bitmap? = withContext(Dispatchers.IO) {
        val allCursor = galleryProvider.fetchGallery()
        val ranPos = Random.nextInt(0, allCursor.count)
        allCursor.moveToPosition(ranPos)
        val photoUri = allCursor.toPhotoUri()
        if (photoUri != null) {
            return@withContext galleryProvider.pathToBitmap(photoUri)
        } else {
            return@withContext null
        }
    }

    private fun performCapture(isShow: Boolean) {
        lifecycleScope.launch(Dispatchers.Main) {
            if (isShow) {
                ivCapture.setImageBitmap(getCaptureBitmap())
                clCapture.visibility = View.VISIBLE
                ivCapture.visibility = View.VISIBLE
            } else {
                ivCapture.setImageBitmap(null)
                clCapture.visibility = View.GONE
                ivCapture.visibility = View.GONE
            }
        }
    }

    private suspend fun getCaptureBitmap(): Bitmap? = withContext(Dispatchers.IO) {
        return@withContext galleryProvider.getCropImageEditToBitmap(ivCropEdit.getEditInfo())
    }
}
