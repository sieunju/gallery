package com.gallery.example

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import com.gallery.core.GalleryProvider
import com.gallery.core.model.GalleryFilterData
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random
import kotlin.random.nextInt

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var galleryProvider: GalleryProvider

    private val ivThumb: AppCompatImageView by lazy { findViewById(R.id.ivThumb) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.bDirectory).setOnClickListener {
            performFetchDirectories()
        }

        findViewById<Button>(R.id.bRandomGallery).setOnClickListener {
            performRandomGalleryBitmap()
        }
    }

    private fun performRandomGalleryBitmap() {
        Single.create<Bitmap> {
            try {
                val allCursor = galleryProvider.fetchGallery()
                val ranPos = Random.nextInt(0, allCursor.count)
                Timber.d("Count ${allCursor.count} RanPos $ranPos")
                allCursor.moveToPosition(ranPos)
                val photoUri = galleryProvider.cursorToPhotoUri(allCursor)
                if (photoUri != null) {
                    Timber.d("Photo Uri $photoUri")
                    it.onSuccess(galleryProvider.pathToBitmap(photoUri, ivThumb.width))
                }
            } catch (ex: Exception) {
                it.onError(ex)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Timber.d("SUCC ")
                ivThumb.setImageBitmap(it)
            },{
                Timber.d("ERROR $it")
            })
    }

    private fun performFetchDirectories() {
        fetchGalleryDirectory()
    }

    private fun fetchDirectoriesRx(): Single<List<GalleryFilterData>> {
        return Single.create<List<GalleryFilterData>> {
            try {
                val list = galleryProvider.fetchDirectories()
                it.onSuccess(list)
            } catch (ex: Exception) {
                it.onError(ex)
            }
        }.subscribeOn(Schedulers.io())
    }

    private fun fetchGalleryDirectory() {
        fetchDirectoriesRx()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Timber.d("SUCC GalleryDirectory List $it")
            }, {
                Timber.d("ERROR GalleryDirectory $it")
            })
    }
}