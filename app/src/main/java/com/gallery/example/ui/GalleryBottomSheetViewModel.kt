package com.gallery.example.ui

import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gallery.core.GalleryProvider
import com.gallery.edit.detector.FlexibleStateItem
import com.gallery.example.SingleLiveEvent
import com.gallery.model.GalleryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
internal class GalleryBottomSheetViewModel @Inject constructor(
    private val galleryProvider: GalleryProvider
): ViewModel() {

    private val disposable: CompositeDisposable by lazy { CompositeDisposable() }

    val startSnackBarEvent: SingleLiveEvent<String> by lazy { SingleLiveEvent() }
    val startViewHolderClickEvent: SingleLiveEvent<Int> by lazy { SingleLiveEvent() }

    private val _cursor: MutableLiveData<Cursor> by lazy { MutableLiveData() }
    val cursor: LiveData<Cursor> get() = _cursor

    private val _selectPhotoBitmap: SingleLiveEvent<Pair<Bitmap, FlexibleStateItem?>> by lazy { SingleLiveEvent() }
    val selectPhotoBitmap: LiveData<Pair<Bitmap, FlexibleStateItem?>> get() = _selectPhotoBitmap
    private val selectPhotoMap: MutableMap<String, FlexibleStateItem> by lazy { mutableMapOf() } // 선택한 사진 정보

    private var prevImagePath: String = ""

    private var takePictureUrl: Uri? = null

    // 현재 편집창에서 처리되는 위치값들
    private val currentFlexibleStateItem: FlexibleStateItem by lazy { FlexibleStateItem() }

    fun start() {
        Single.create<Cursor> { emitter ->
            try {
                val cursor = galleryProvider.fetchGallery()
                emitter.onSuccess(cursor)
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                _cursor.value = it
                performClickPosition()
            }, {

            }).addTo(disposable)
    }

    /**
     * 초기 RecyclerView 클릭 처리하는 함수
     */
    private fun performClickPosition() {
        Single.just(0)
            .delay(200, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                startViewHolderClickEvent.value = it
            }, {

            }).addTo(disposable)
    }

    /**
     * FlexibleImageEditView onStateItem Listener
     */
    fun onStateItem(newItem: FlexibleStateItem) {
        newItem.valueCopy(currentFlexibleStateItem)
    }

    fun onPhotoClick(item: GalleryItem, isAdd: Boolean) {
        if (isAdd) {
            // 로딩바 구현
            performChangeBitmap(item.imagePath)
        } else {
            performRemovePhoto(item)
        }
    }

    /**
     * FlexibleImageView 에 변경 하기 위한 함수
     */
    private fun performChangeBitmap(newImagePath: String) {
        Single.create<Bitmap> { emitter ->
            try {
                savePreviewStateItem()
                val bitmap = galleryProvider.pathToBitmap(newImagePath)
                emitter.onSuccess(bitmap)
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                val cacheStateItem = selectPhotoMap[newImagePath]
                // 이미 존재하는 경우
                if (cacheStateItem != null) {
                    _selectPhotoBitmap.value = it to cacheStateItem
                } else {
                    selectPhotoMap[newImagePath] = FlexibleStateItem()
                    _selectPhotoBitmap.value = it to null
                }
                prevImagePath = newImagePath
            }, {

            }).addTo(disposable)
    }

    private fun performRemovePhoto(item: GalleryItem) {
        selectPhotoMap.remove(item.imagePath)
        val nextImagePath = selectPhotoMap.keys.lastOrNull()
        if (nextImagePath != null) {
            performChangeBitmap(nextImagePath)
        } else {
            // Empty Selected Photo
        }
    }

    /**
     * 다른 사진 선택할때 이전 상태값들 저장하는 함수
     */
    private fun savePreviewStateItem() {
        if (prevImagePath.isNotEmpty()) {
            val stateItem = selectPhotoMap[prevImagePath]
            if (stateItem != null) {
                currentFlexibleStateItem.valueCopy(stateItem)
            }
        }
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
