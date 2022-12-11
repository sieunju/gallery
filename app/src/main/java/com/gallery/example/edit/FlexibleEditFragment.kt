package com.gallery.example.edit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.gallery.core.GalleryProvider
import com.gallery.edit.FlexibleImageEditView
import com.gallery.example.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import javax.inject.Inject
import kotlin.random.Random

/**
 * Description :
 *
 * Created by juhongmin on 2022/11/14
 */
@AndroidEntryPoint
internal class FlexibleEditFragment : Fragment(R.layout.f_flexible_edit) {

    @Inject
    lateinit var galleryProvider: GalleryProvider

    private lateinit var ivFlexible: FlexibleImageEditView
    private lateinit var clCapture: ConstraintLayout
    private lateinit var ivCapture: AppCompatImageView
    private lateinit var fRoot: FrameLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(view) {
            ivFlexible = findViewById(R.id.ivFlexible)
            clCapture = findViewById(R.id.clCapture)
            ivCapture = findViewById(R.id.ivCapture)
            fRoot = findViewById(R.id.fRoot)

            findViewById<Button>(R.id.bRandomGallery).setOnClickListener {
                performRandomGallery()
            }

            findViewById<Button>(R.id.bCenterCrop).setOnClickListener {
                ivFlexible.centerCrop()
            }

            findViewById<Button>(R.id.bFitCenter).setOnClickListener {
                ivFlexible.fitCenter()
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
            getRandomGalleryBitmap()
                .onSuccess {
                    ivFlexible.loadBitmap(it)
                }
            // val bitmap = getSampleBitmap()

        }
    }

    private suspend fun getSampleBitmap(): Bitmap? = withContext(Dispatchers.IO) {
        return@withContext try {
            // "https://image.zdnet.co.kr/2021/08/27/48a2291e7cbed1be50aa28880b58477e.jpg"
            val url =
                "https://wplyuoicjiwl13857209.cdn.ntruss.com/data2/content/image/2022/01/12/.cache/512/20220112500244.png"
            val bytes = URL(url)
                .readBytes()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (ex: IOException) {
            null
        }
    }

    private suspend fun getRandomGalleryBitmap(): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val allCursor = galleryProvider.fetchGallery()
            val ranPos = Random.nextInt(0, allCursor.count)
            allCursor.moveToPosition(ranPos)
            val photoUri = galleryProvider.cursorToPhotoUri(allCursor)
            return@withContext Result.success(galleryProvider.pathToBitmap(photoUri))
        } catch (ex: Exception) {
            return@withContext Result.failure(ex)
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
        val stateItem = ivFlexible.getStateItem() ?: return@withContext null
        val originBitmap = ivFlexible.getImageBitmap()
        return@withContext galleryProvider.getFlexibleImageToBitmap(
            originBitmap,
            stateItem,
            fRoot.width,
            fRoot.height
        )
    }
}
