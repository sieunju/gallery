package com.gallery.example.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.gallery.core.GalleryProvider
import com.gallery.example.R
import com.gallery.ui.GalleryRecyclerView
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class AddCameraGalleryFragment : Fragment(R.layout.f_add_camera_gallery) {

    private val disposable: CompositeDisposable by lazy { CompositeDisposable() }

    lateinit var galleryRecyclerView: GalleryRecyclerView

    @Inject
    lateinit var galleryProvider: GalleryProvider

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(view) {
            galleryRecyclerView = view.findViewById(R.id.rvGallery)
        }

        fetchCursor()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposable.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!disposable.isDisposed) {
            disposable.dispose()
        }
    }

    private fun fetchCursor() {
        Single.just(galleryProvider.fetchGallery())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                galleryRecyclerView.setCursor(it)
            }, {
            }).addTo(disposable)
    }
}
