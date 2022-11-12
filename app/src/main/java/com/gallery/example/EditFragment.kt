package com.gallery.example

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.gallery.core.GalleryProvider
import com.gallery.core.toPhotoUri
import com.gallery.edit.CropImageView
import com.gallery.edit.FlexibleImageView
import com.gallery.edit.GalleryEditView
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random


@AndroidEntryPoint
class EditFragment : Fragment(R.layout.fragment_edit) {

    private val disposable: CompositeDisposable by lazy { CompositeDisposable() }

    @Inject
    lateinit var galleryProvider: GalleryProvider

    @Inject
    lateinit var apiService: CoreApiService

    private lateinit var ivCrop: CropImageView

    private lateinit var ivFlexible: FlexibleImageView

    private lateinit var ivCapture: AppCompatImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(view) {
            ivCrop = findViewById(R.id.ivCrop)
            ivFlexible = findViewById(R.id.fiv)
            ivCapture = findViewById(R.id.ivCapture)
            val clFlexible = findViewById<ConstraintLayout>(R.id.clFlexible)

            findViewById<Button>(R.id.bChange).setOnClickListener {
                if (ivCrop.visibility == View.VISIBLE) {
                    ivCrop.visibility = View.GONE
                    clFlexible.visibility = View.VISIBLE
                    ivFlexible.visibility = View.VISIBLE
                } else {
                    ivCrop.visibility = View.VISIBLE
                    clFlexible.visibility = View.GONE
                    ivFlexible.visibility = View.GONE
                }
            }

            findViewById<Button>(R.id.bRandomGallery).setOnClickListener {
                performRandomGalleryBitmap()
            }

            findViewById<Button>(R.id.bCapture).setOnClickListener {
                if (ivCrop.visibility == View.VISIBLE) {
                    Single.create<Bitmap> { emitter ->
                        try {
                            val bitmap = ivCrop.captureImageBitmap()
                            if (bitmap != null) {
                                emitter.onSuccess(bitmap)
                            }
                        } catch (ex: Exception) {
                            emitter.onError(ex)
                        }
                    }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            ivCapture.setImageBitmap(it)
                        }, {
                            Timber.d("ERROR $it")
                        })
                } else {

                }
            }

            findViewById<Button>(R.id.bCenterCrop).setOnClickListener {
                ivFlexible.centerCrop()
            }

            findViewById<Button>(R.id.bFitCenter).setOnClickListener {
                ivFlexible.fitCenter()
            }
        }
    }

    /**
     * 갤러리를 랜덤으로 가져와 Bitmap 으로 변환 하는 함수
     */
    private fun performRandomGalleryBitmap() {
        Single.create<Bitmap> {
            try {
                val allCursor = galleryProvider.fetchGallery()
                val ranPos = Random.nextInt(0, allCursor.count)
                Timber.d("Count ${allCursor.count} RanPos $ranPos")
                allCursor.moveToPosition(ranPos)
                val photoUri = allCursor.toPhotoUri()
                if (photoUri != null) {
                    Timber.d("Photo Uri $photoUri")
                    it.onSuccess(galleryProvider.pathToBitmap(photoUri, 1080))
                }
            } catch (ex: Exception) {
                it.onError(ex)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (ivCrop.visibility == View.VISIBLE) {
                    ivCrop.setImageBitmap(it)
                } else if (ivFlexible.visibility == View.VISIBLE) {
                    ivFlexible.loadBitmap(it)
                }

            }, {
                Timber.d("ERROR $it")
            }).addTo(disposable)
    }
}
