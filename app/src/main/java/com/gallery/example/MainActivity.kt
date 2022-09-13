package com.gallery.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gallery.core.GalleryProvider
import com.gallery.core.model.GalleryFilterData
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
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