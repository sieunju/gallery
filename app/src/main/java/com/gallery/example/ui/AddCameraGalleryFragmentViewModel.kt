package com.gallery.example.ui

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gallery.core.GalleryProvider
import com.gallery.example.SingleLiveEvent
import com.gallery.model.GalleryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

/**
 * Description :
 *
 * Created by juhongmin on 2022/11/30
 */
@HiltViewModel
internal class AddCameraGalleryFragmentViewModel @Inject constructor(
    private val galleryProvider: GalleryProvider
) : ViewModel() {

    private val disposable: CompositeDisposable by lazy { CompositeDisposable() }

    val startCameraOpenEvent: SingleLiveEvent<Intent> by lazy { SingleLiveEvent() }
    val startSnackBarEvent: SingleLiveEvent<String> by lazy { SingleLiveEvent() }

    private val _cursor: MutableLiveData<Cursor> by lazy { MutableLiveData() }
    val cursor: LiveData<Cursor> get() = _cursor
    private var takePictureUrl: Uri? = null

    fun fetchCursor() {
        Single.just(galleryProvider.fetchGallery())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                _cursor.value = it
            }, {
            }).addTo(disposable)
    }

    fun onCameraOpen() {
        takePictureUrl = galleryProvider.createGalleryPhotoUri("com.gallery.example.provider")
        startCameraOpenEvent.value = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, takePictureUrl)
        }
    }

    fun onPhotoClick(item: GalleryItem, isAdd: Boolean) {

    }

    fun onMaxPhotoClick() {
        startSnackBarEvent.value = "Max Picker Count"
    }

    fun onSuccessSavePicture() {
        val url = takePictureUrl
        if (url != null) {
            savePicture(url.toString())
        }
    }

    private fun savePicture(uri: String) {
        Single.create<Pair<Boolean, String>> {
            try {
                val result = galleryProvider.saveGalleryPicture(
                    uri,
                    "gallery_${System.currentTimeMillis()}"
                )
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

    fun clearDisposable() {
        disposable.clear()
    }

    override fun onCleared() {
        super.onCleared()
        if (!disposable.isDisposed) {
            disposable.dispose()
        }
    }
}
