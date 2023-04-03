package com.gallery.example.ui

import android.database.Cursor
import android.graphics.Bitmap
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gallery.core.GalleryProvider
import com.gallery.core.model.GalleryFilterData
import com.gallery.core.model.GalleryQueryParameter
import com.gallery.core_rx.*
import com.gallery.example.SingleLiveEvent
import com.gallery.model.FlexibleStateItem
import com.gallery.ui.model.GalleryItem
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
) : ViewModel() {

    private val disposable: CompositeDisposable by lazy { CompositeDisposable() }

    val startDismissEvent: SingleLiveEvent<Unit> by lazy { SingleLiveEvent() }
    val startSnackBarEvent: SingleLiveEvent<String> by lazy { SingleLiveEvent() }
    val startViewHolderClickEvent: SingleLiveEvent<Int> by lazy { SingleLiveEvent() }

    private val _cursor: MutableLiveData<Cursor> by lazy { MutableLiveData() }
    val cursor: LiveData<Cursor> get() = _cursor

    private val _isLoading: MutableLiveData<Boolean> by lazy { MutableLiveData(false) }
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _selectPhotoBitmap: SingleLiveEvent<Pair<Bitmap, FlexibleStateItem?>> by lazy { SingleLiveEvent() }
    val selectPhotoBitmap: LiveData<Pair<Bitmap, FlexibleStateItem?>> get() = _selectPhotoBitmap
    private val selectPhotoMap: MutableMap<String, FlexibleStateItem> by lazy { mutableMapOf() } // 선택한 사진 정보

    private val _startSendEditImageBitmap: MutableLiveData<List<Bitmap>> by lazy { MutableLiveData() }
    val startSendEditImageBitmap: LiveData<List<Bitmap>> get() = _startSendEditImageBitmap

    private var prevImagePath: String = ""

    // 현재 편집창에서 처리되는 위치값들
    private val currentFlexibleStateItem: FlexibleStateItem by lazy { FlexibleStateItem() }
    private var currentFilterItem: GalleryFilterData? = null
    val filterList: MutableList<GalleryFilterData> by lazy { mutableListOf() }
    private val queryParameter: GalleryQueryParameter by lazy { GalleryQueryParameter() }
    private val _selectedFilterTitle: MutableLiveData<String> by lazy { MutableLiveData() }
    val selectedFilterTitle: LiveData<String> get() = _selectedFilterTitle

    fun start() {
        galleryProvider.fetchDirectoriesRx()
            .doOnSubscribe { showLoading(true) }
            .map { initParameter(it) }
            .flatMap { galleryProvider.fetchGalleryRx(it) }
            .delay(500, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess {
                _cursor.value = it
                performClickPosition()
                showLoading(false)
            }
            .subscribe().addTo(disposable)
    }

    private fun initParameter(list: List<GalleryFilterData>): GalleryQueryParameter {
        filterList.clear()
        filterList.addAll(list)
        currentFilterItem = list[0]
        queryParameter.filterId = list[0].bucketId
        _selectedFilterTitle.postValue(list[0].bucketName)
        return queryParameter
    }

    fun onSelectedFilter(data: GalleryFilterData) {
        currentFilterItem = data
        queryParameter.filterId = data.bucketId
        _selectedFilterTitle.value = data.bucketName
        galleryProvider.fetchGalleryRx(queryParameter)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { _cursor.value = it }
            .subscribe().addTo(disposable)
    }

    fun onDismiss() {
        startDismissEvent.call()
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

    /**
     * FlexibleImageView 에 변경 하기 위한 함수
     */
    private fun handleChangeBitmap(newImagePath: String) {
        savePreviewStateItem()
            .doOnSubscribe { showLoading(true) }
            .flatMap { galleryProvider.pathToBitmapRx(newImagePath) }
            .flatMap { handleChangeBitmapSuccess(newImagePath, it) }
            .delay(200, TimeUnit.MILLISECONDS)
            .doOnSuccess {
                showLoading(false)
                Timber.d("이미지 타입 ${galleryProvider.getImageType(newImagePath)}")
            }
            .subscribe().addTo(disposable)
    }

    private fun handleChangeBitmapSuccess(newImagePath: String, bitmap: Bitmap): Single<Unit> {
        return Single.create { emitter ->
            val cacheStateItem = selectPhotoMap[newImagePath]
            // 이미 존재하는 경우
            if (cacheStateItem != null) {
                _selectPhotoBitmap.value = bitmap to cacheStateItem
            } else {
                selectPhotoMap[newImagePath] = FlexibleStateItem()
                _selectPhotoBitmap.value = bitmap to null
            }
            prevImagePath = newImagePath
            emitter.onSuccess(Unit)
        }.subscribeOn(AndroidSchedulers.mainThread())
    }

    private fun showLoading(isVisible: Boolean) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            _isLoading.value = isVisible
        } else {
            _isLoading.postValue(isVisible)
        }
    }

    /**
     * 다른 사진 선택할때 이전 상태값들 저장하는 함수
     */
    private fun savePreviewStateItem(): Single<Unit> {
        return Single.create { emitter ->
            if (prevImagePath.isNotEmpty()) {
                val stateItem = selectPhotoMap[prevImagePath]
                if (stateItem != null) {
                    currentFlexibleStateItem.valueCopy(stateItem)
                }
            }
            emitter.onSuccess(Unit)
        }.subscribeOn(Schedulers.io())
    }

    private fun handleRemovePhoto(item: GalleryItem) {
        selectPhotoMap.remove(item.imagePath)
        val nextImagePath = selectPhotoMap.keys.lastOrNull()
        if (nextImagePath != null) {
            handleChangeBitmap(nextImagePath)
        } else {
            // Empty Selected Photo
        }
    }

    fun onPhotoClick(item: GalleryItem, isAdd: Boolean) {
        if (isAdd) {
            Timber.d("onPhotoClick ${item}")
            handleChangeBitmap(item.imagePath)
        } else {
            handleRemovePhoto(item)
        }
    }

    fun onMaxPhotoClick() {
        startSnackBarEvent.value = "Max Picker Count"
    }

    fun isSamePhoto(clickItem: GalleryItem): Boolean {
        return prevImagePath == clickItem.imagePath
    }

    fun sendEditImageBitmap() {
        var bitmapCount = 0
        savePreviewStateItem()
            .doOnSubscribe { showLoading(true) }
            .flatMap { galleryProvider.deleteCacheDirectoryRx() }
            .map {
                val workList = mutableListOf<Single<Bitmap>>()
                selectPhotoMap.forEach { entry ->
                    workList.add(galleryProvider.getFlexibleImageToBitmapRx(entry.key, entry.value))
                }
                bitmapCount = workList.size
                return@map workList
            }
            .toFlowable()
            .flatMap { Single.mergeDelayError(it).buffer(bitmapCount) }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                showLoading(false)
                _startSendEditImageBitmap.value = it
            }
            .subscribe().addTo(disposable)
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
