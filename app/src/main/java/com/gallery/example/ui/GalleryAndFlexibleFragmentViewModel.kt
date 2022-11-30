package com.gallery.example.ui

import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
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
internal class GalleryAndFlexibleFragmentViewModel @Inject constructor(
    private val galleryProvider: GalleryProvider
) : ViewModel() {

    private val disposable: CompositeDisposable by lazy { CompositeDisposable() }

    val startCameraOpenEvent: SingleLiveEvent<Intent> by lazy { SingleLiveEvent() }
    val startSnackBarEvent: SingleLiveEvent<String> by lazy { SingleLiveEvent() }
    val startViewHolderClickEvent: SingleLiveEvent<Int> by lazy { SingleLiveEvent() }
    val startSaveStateItem: SingleLiveEvent<String> by lazy { SingleLiveEvent() }


    private val _cursor: MutableLiveData<Cursor> by lazy { MutableLiveData() }
    val cursor: LiveData<Cursor> get() = _cursor

    private val _selectPhotoBitmap : SingleLiveEvent<Pair<Bitmap,FlexibleStateItem?>> by lazy { SingleLiveEvent() }
    val selectPhotoBitmap: LiveData<Pair<Bitmap,FlexibleStateItem?>> get() = _selectPhotoBitmap
    private val selectPhotoMap: HashMap<String, FlexibleStateItem?> by lazy { hashMapOf() }

    private var currentImagePath : String = ""

    private var takePictureUrl: Uri? = null

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
        Single.just(1)
            .delay(400, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                startViewHolderClickEvent.value = it
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
        // 초기값 셋팅
        if (selectPhotoMap.size == 0) {
            Timber.d("초기값 셋팅입니다. ")
            selectPhotoMap[item.imagePath] = null
            requestBitmap(item.imagePath)
            currentImagePath = item.imagePath
        } else if (selectPhotoMap.containsKey(item.imagePath) && isAdd) {
            Timber.d("이전에 선택한 값이고 추가입니다. ")
            // 선택한 사진값이 이전에 있는 경우
            requestBitmap(item.imagePath,selectPhotoMap[item.imagePath])
            startSaveStateItem.value = currentImagePath
            currentImagePath = item.imagePath
        } else {
            Timber.d("삭제 압니다. ${selectPhotoMap.containsKey(item.imagePath)}")
            selectPhotoMap.remove(item.imagePath)
            currentImagePath = item.imagePath
        }
    }

    fun setCurrentStateItem(key: String, currentStateItem: FlexibleStateItem) {
        // 이전 사진값 저장
        Timber.d("이전값 셋팅합니다.")
        selectPhotoMap[key] = currentStateItem
        requestBitmap(currentImagePath)
    }

    private fun requestBitmap(imagePath: String, stateItem: FlexibleStateItem? = null) {
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
                _selectPhotoBitmap.value = it to stateItem
            }, {

            }).addTo(disposable)
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
