package com.gallery.example

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.gallery.core.GalleryProvider
import com.gallery.core.model.GalleryFilterData
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var galleryProvider: GalleryProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fetchGalleryDirectory()
    }

    private fun fetchGalleryDirectory() {
        Single.create<List<GalleryFilterData>> { emitter ->
            try {
                emitter.onSuccess(galleryProvider.fetchDirectory())
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }.subscribeOn(Schedulers.io())
            .subscribe({
                Timber.d("SUCC GalleryDirectory List $it")
            }, {
                Timber.d("ERROR GalleryDirectory $it")
            })
    }
}