package com.gallery.example.ui

import android.database.Cursor
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gallery.core.GalleryProvider
import com.gallery.core_rx.*
import com.gallery.example.SingleLiveEvent
import com.gallery.model.FlexibleStateItem
import com.gallery.ui.model.GalleryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Description :
 *
 * Created by juhongmin on 2022/12/12
 */
@HiltViewModel
internal class FlexibleEditFragmentViewModel @Inject constructor(
    private val galleryProvider: GalleryProvider
) : ViewModel() {

    private val disposable: CompositeDisposable by lazy { CompositeDisposable() }

    val startSnackBarEvent: SingleLiveEvent<String> by lazy { SingleLiveEvent() }
    val startViewHolderClickEvent: SingleLiveEvent<Int> by lazy { SingleLiveEvent() }

    private val _cursor: MutableLiveData<Cursor> by lazy { MutableLiveData() }
    val cursor: LiveData<Cursor> get() = _cursor

    private val _selectPhotoBitmap: SingleLiveEvent<Pair<Bitmap, FlexibleStateItem?>> by lazy { SingleLiveEvent() }
    val selectPhotoBitmap: LiveData<Pair<Bitmap, FlexibleStateItem?>> get() = _selectPhotoBitmap
    private val selectPhotoMap: MutableMap<String, FlexibleStateItem> by lazy { mutableMapOf() } // 선택한 사진 정보

    private val _startSendEditImageBitmap: MutableLiveData<List<Bitmap>> by lazy { MutableLiveData() }
    val startSendEditImageBitmap: LiveData<List<Bitmap>> get() = _startSendEditImageBitmap

    private var prevImagePath: String = ""

    // 현재 편집창에서 처리되는 위치값들
    private val currentFlexibleStateItem: FlexibleStateItem by lazy { FlexibleStateItem() }

    fun start() {
        galleryProvider.fetchGalleryRx()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { _cursor.value = it }
            .flatMap { Single.just(0) }
            .delay(200, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { startViewHolderClickEvent.value = it }
            .subscribe().addTo(disposable)
    }

    /**
     * FlexibleImageEditView onStateItem Listener
     */
    fun onStateItem(newItem: FlexibleStateItem) {
        newItem.valueCopy(currentFlexibleStateItem)
    }

    /**
     * FlexibleImageView 에 변경 하기 위한 함수
     */
    private fun performChangeBitmap(newImagePath: String) {
        savePreviewStateItem()
        galleryProvider.pathToBitmapRx(newImagePath)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess {
                val cacheStateItem = selectPhotoMap[newImagePath]
                // 이미 존재하는 경우
                if (cacheStateItem != null) {
                    _selectPhotoBitmap.value = it to cacheStateItem
                } else {
                    selectPhotoMap[newImagePath] = FlexibleStateItem()
                    _selectPhotoBitmap.value = it to null
                }
                prevImagePath = newImagePath
            }
            .subscribe().addTo(disposable)
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

    fun onPhotoClick(item: GalleryItem, isAdd: Boolean) {
        if (isAdd) {
            performChangeBitmap(item.imagePath)
        } else {
            performRemovePhoto(item)
        }
    }

    fun onMaxPhotoClick() {
        startSnackBarEvent.value = "Max Picker Count"
    }

    fun isSamePhoto(clickItem: GalleryItem): Boolean {
        return prevImagePath == clickItem.imagePath
    }

    fun sendEditImageBitmap() {
        savePreviewStateItem()
        galleryProvider.deleteCacheDirectoryRx()
            .map {
                val workList = mutableListOf<Single<Bitmap>>()
                selectPhotoMap.forEach { entry ->
                    workList.add(galleryProvider.getFlexibleImageToBitmapRx(entry.key, entry.value))
                }
                return@map workList
            }
            .toFlowable()
            .flatMap { Single.mergeDelayError(it) }
            .flatMap { galleryProvider.saveBitmapToFileRx(it).toFlowable() }
            .subscribe({
                Timber.d("SUCC $it")
            }, {
                Timber.d("ERROR $it")
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
