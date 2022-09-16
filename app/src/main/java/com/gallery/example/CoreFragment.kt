package com.gallery.example

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import com.gallery.core.GalleryProvider
import com.gallery.core.model.GalleryFilterData
import com.gallery.core.model.GalleryQueryParameter
import com.gallery.core.toPhotoUri
import com.google.android.material.snackbar.Snackbar
import com.hmju.permissions.SimplePermissions
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class CoreFragment : Fragment(R.layout.fragment_core) {

    private val disposable: CompositeDisposable by lazy { CompositeDisposable() }

    @Inject
    lateinit var galleryProvider: GalleryProvider

    private lateinit var ivThumb: AppCompatImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ivThumb = view.findViewById(R.id.ivThumb)

        with(view) {

            findViewById<Button>(R.id.bDirectory).setOnClickListener {
                performFetchDirectories()
            }

            findViewById<Button>(R.id.bCacheFile).setOnClickListener {
                performCreateCacheFile()
            }

            findViewById<Button>(R.id.bRandomGallery).setOnClickListener {
                performRandomGalleryBitmap()
            }

            findViewById<Button>(R.id.bPermissions).setOnClickListener {
                SimplePermissions(requireActivity())
                    .requestPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    .build { b, strings ->

                    }
            }

            findViewById<Button>(R.id.bRandomGallery2).setOnClickListener {
                performRandomGalleryBitmap2()
            }
        }
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

    /**
     * 갤러리를 랜덤으로 가져와 Bitmap 으로 변환 하는 함수
     */
    private fun performRandomGalleryBitmap() {
        Single.create<Bitmap> {
            try {
                val allCursor = galleryProvider.fetchGallery()
                val ranPos = Random.nextInt(0, allCursor.count)
                Timber.d("Count ${allCursor.count} RanPos $ranPos")
                allCursor.moveToPosition(ranPos)
                val photoUri = allCursor.toPhotoUri()
                if (photoUri != null) {
                    Timber.d("Photo Uri $photoUri")
                    it.onSuccess(galleryProvider.pathToBitmap(photoUri, ivThumb.width))
                }
            } catch (ex: Exception) {
                it.onError(ex)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Timber.d("SUCC ")
                ivThumb.setImageBitmap(it)
            }, {
                Timber.d("ERROR $it")
            }).addTo(disposable)
    }

    /**
     * Cache File 생성후 거기에 첫번째 갤러리 Bitmap 을 입히는 처리
     */
    private fun performCreateCacheFile() {
        Single.create<String> {
            try {
                val list = galleryProvider.fetchDirectories()
                val ranPos = Random.nextInt(0, list.size.minus(1).coerceAtLeast(0))
                val cursor = galleryProvider.fetchGallery(GalleryQueryParameter().apply {
                    filterId = list[ranPos].bucketId
                })
                val file = galleryProvider.createFile("FILE", ".jpg")
                if (file != null) {
                    val fos = FileOutputStream(file)
                    cursor.moveToLast()
                    val bitmap = galleryProvider.pathToBitmap(cursor.toPhotoUri() ?: "", 1080)
                    galleryProvider.copyBitmapToFile(bitmap, fos)
                    fos.close()
                    it.onSuccess(file.path)
                }
            } catch (ex: Exception) {
                it.onError(ex)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (Random.nextBoolean()) {
                    galleryProvider.deleteCacheDirectory()
                    Snackbar.make(requireView(), "캐시파일 삭제 성공!", Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(requireView(), "성공 $it", Snackbar.LENGTH_SHORT).show()
                }
            }, {
                Snackbar.make(requireView(), it.message ?: "", Snackbar.LENGTH_SHORT).show()
            }).addTo(disposable)
    }

    /**
     * 갤러리 디렉토리 처리함수
     */
    private fun performFetchDirectories() {
        Single.create<List<GalleryFilterData>> {
            try {
                val list = galleryProvider.fetchDirectories()
                it.onSuccess(list)
            } catch (ex: Exception) {
                it.onError(ex)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Timber.d("SUCC GalleryDirectory List $it")
            }, {
                Timber.d("ERROR GalleryDirectory $it")
            }).addTo(disposable)
    }

    /**
     * 갤러리를 랜덤으로 가져와 Bitmap 으로 변환 하는 함수
     */
    private fun performRandomGalleryBitmap2() {
        Single.create<Bitmap> {
            try {
                val allCursor = galleryProvider.fetchGallery()
                val ranPos = Random.nextInt(0, allCursor.count)
                Timber.d("Count ${allCursor.count} RanPos $ranPos")
                allCursor.moveToPosition(ranPos)
                val photoUri = galleryProvider.cursorToPhotoUri(allCursor)
                if (photoUri != null) {
                    Timber.d("Photo Uri $photoUri")
                    it.onSuccess(galleryProvider.pathToBitmap(photoUri, ivThumb.width))
                }
            } catch (ex: Exception) {
                it.onError(ex)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Timber.d("SUCC ")
                ivThumb.setImageBitmap(it)
            }, {
                Timber.d("ERROR $it")
            }).addTo(disposable)
    }
}