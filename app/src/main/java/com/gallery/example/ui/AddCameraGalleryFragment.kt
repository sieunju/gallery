package com.gallery.example.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.gallery.core.GalleryProvider
import com.gallery.example.R
import com.gallery.model.GalleryItem
import com.gallery.ui.GalleryListener
import com.gallery.ui.GalleryRecyclerView
import com.google.android.material.snackbar.Snackbar
import com.hmju.permissions.extension.dp
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class AddCameraGalleryFragment : Fragment(R.layout.f_add_camera_gallery), GalleryListener {

    private val disposable: CompositeDisposable by lazy { CompositeDisposable() }

    lateinit var galleryRecyclerView: GalleryRecyclerView

    @Inject
    lateinit var galleryProvider: GalleryProvider

    private var takePictureUrl: Uri? = null

    private val cameraCallback =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                Timber.d("TakePicture $takePictureUrl")
                savePicture(takePictureUrl.toString())
            } else {
                Timber.d("CameraCallback Fail")
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(view) {
            galleryRecyclerView = view.findViewById(R.id.rvGallery)
            galleryRecyclerView.setRequestManager(Glide.with(this@AddCameraGalleryFragment))
            galleryRecyclerView.addItemDecoration(GridDividerItemDecoration(2.dp))
            galleryRecyclerView.setListener(this@AddCameraGalleryFragment)
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

    override fun onCameraOpen() {
        takePictureUrl = galleryProvider.createGalleryPhotoUri("com.gallery.example.provider")
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, takePictureUrl)
        cameraCallback.launch(intent)
    }

    override fun onPhotoPicker(item: GalleryItem, isAdd: Boolean) {
        Timber.d("onPhotoPicker $isAdd $item")
    }

    override fun onMaxPickerCount() {
        Snackbar.make(this.view!!, "Max Picker Count", Snackbar.LENGTH_SHORT).show()
    }

    private fun fetchCursor() {
        Single.just(galleryProvider.fetchGallery())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                galleryRecyclerView.setCursor(it)
            }, {
            }).addTo(disposable)
    }

    private fun savePicture(uri: String) {
        Single.create<Pair<Boolean, String>> {
            try {
                val result =
                    galleryProvider.saveGalleryPicture(uri, "gallery_${System.currentTimeMillis()}")
                it.onSuccess(result)
            } catch (ex: Exception) {
                it.onError(ex)
            }
        }
            .subscribeOn(Schedulers.io())
            .subscribe({
                Timber.d("SUCC $it")
                galleryProvider.deleteCacheDirectory()
            }, {
                Timber.d("ERROR $it")
                galleryProvider.deleteCacheDirectory()
            }).addTo(disposable)
    }
}
