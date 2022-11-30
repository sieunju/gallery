package com.gallery.example.ui

import android.database.Cursor
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gallery.core.GalleryProvider
import com.gallery.core.model.GalleryQueryParameter
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
import kotlin.random.Random

/**
 * Description :
 *
 * Created by juhongmin on 2022/11/30
 */
@HiltViewModel
internal class GalleryAndCropEditFragmentViewModel @Inject constructor(
    private val galleryProvider: GalleryProvider
) : ViewModel() {

    private val disposable: CompositeDisposable by lazy { CompositeDisposable() }

    val startSnackBarEvent: SingleLiveEvent<String> by lazy { SingleLiveEvent() }

    private val _cursor: MutableLiveData<Cursor> by lazy { MutableLiveData() }
    val cursor: LiveData<Cursor> get() = _cursor
    private val _selectPhotoBitmap: MutableLiveData<Bitmap> by lazy { MutableLiveData() }
    val selectPhotoBitmap: LiveData<Bitmap> get() = _selectPhotoBitmap

    fun start() {
        Single.create<Cursor> { emitter ->
            try {
                val directories = galleryProvider.fetchDirectories()
                val ranIdx = Random.nextInt(directories.size)
                val filter = directories[ranIdx]
                val params = GalleryQueryParameter()
                params.filterId = filter.bucketId
                params.isAscOrder = true
                val cursor = galleryProvider.fetchGallery(params)
                emitter.onSuccess(cursor)
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Timber.d("SUCC $it")
                _cursor.value = it
            }, {
                Timber.d("ERROR $it")
            }).addTo(disposable)
    }

    private fun getBitmap(imagePath: String) {
        Single.create<Bitmap> { emitter ->
            try {
                val bitmap = galleryProvider.pathToBitmap(imagePath)
                emitter.onSuccess(bitmap)
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                _selectPhotoBitmap.value = it
            }, {

            }).addTo(disposable)
    }

    fun onPhotoClick(item: GalleryItem, isAdd: Boolean) {
        Timber.d("onPhotoClick $item $isAdd")
        if (isAdd) {
            getBitmap(item.imagePath)
        }
    }

    fun onMaxPhotoClick() {
        startSnackBarEvent.value = "Max Picker Count"
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

