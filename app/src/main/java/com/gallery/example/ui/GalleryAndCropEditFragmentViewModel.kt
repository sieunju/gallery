package com.gallery.example.ui

import android.database.Cursor
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gallery.core.GalleryProvider
import com.gallery.core.model.GalleryQueryParameter
import com.gallery.core_rx.fetchDirectoriesRx
import com.gallery.core_rx.fetchGalleryRx
import com.gallery.core_rx.pathToBitmapRx
import com.gallery.example.SingleLiveEvent
import com.gallery.model.GalleryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import java.util.concurrent.TimeUnit
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
    val startViewHolderClickEvent: SingleLiveEvent<Int> by lazy { SingleLiveEvent() }

    private val _cursor: MutableLiveData<Cursor> by lazy { MutableLiveData() }
    val cursor: LiveData<Cursor> get() = _cursor
    private val _selectPhotoBitmap: MutableLiveData<Bitmap> by lazy { MutableLiveData() }
    val selectPhotoBitmap: LiveData<Bitmap> get() = _selectPhotoBitmap

    fun start() {
        galleryProvider.fetchDirectoriesRx()
            .map {
                val ranIdx = Random.nextInt(it.size)
                val params = GalleryQueryParameter()
                params.filterId = it[ranIdx].bucketId
                params.isAscOrder = true
                return@map params
            }
            .flatMap { galleryProvider.fetchGalleryRx(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess {
                _cursor.value = it
                performClickPosition()
            }
            .subscribe().addTo(disposable)
    }

    /**
     * 초기 RecyclerView 클릭 처리하는 함수
     */
    private fun performClickPosition() {
        Single.just(0)
            .delay(400, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                startViewHolderClickEvent.value = it
            }, {

            }).addTo(disposable)
    }

    private fun getBitmap(imagePath: String) {
        galleryProvider.pathToBitmapRx(imagePath)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { _selectPhotoBitmap.value = it }
            .subscribe().addTo(disposable)
    }

    fun onPhotoClick(item: GalleryItem, isAdd: Boolean) {
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

