package com.gallery.example.core

import android.Manifest
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import com.gallery.core.GalleryProvider
import com.gallery.core.model.GalleryQueryParameter
import com.gallery.core_rx.*
import com.gallery.example.CoreApiService
import com.gallery.example.R
import com.google.android.material.snackbar.Snackbar
import com.hmju.permissions.SimplePermissions
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import timber.log.Timber
import java.net.URL
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class CoreFragment : Fragment(R.layout.fragment_core) {

    private val disposable: CompositeDisposable by lazy { CompositeDisposable() }

    @Inject
    lateinit var galleryProvider: GalleryProvider

    @Inject
    lateinit var apiService: CoreApiService

    private lateinit var ivThumb1: AppCompatImageView

    private lateinit var ivThumb2: AppCompatImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        with(view) {

            ivThumb1 = findViewById(R.id.ivThumb1)
            ivThumb2 = findViewById(R.id.ivThumb2)

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

            findViewById<Button>(R.id.bUpload1).setOnClickListener {
                performSingleUpload()
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
        galleryProvider.fetchGalleryRx()
            .flatMap {
                val ranPos = Random.nextInt(0, it.count)
                it.moveToPosition(ranPos)
                galleryProvider.cursorToPhotoUriRx(it)
            }.flatMap { galleryProvider.pathToBitmapRx(it, ivThumb1.width) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                ivThumb1.setImageBitmap(it)
            }, {
                Timber.d("ERROR $it")
            }).addTo(disposable)
    }

    /**
     * Cache File 생성후 거기에 첫번째 갤러리 Bitmap 을 입히는 처리
     */
    private fun performCreateCacheFile() {
        galleryProvider.fetchDirectoriesRx()
            .map {
                val ranPos = Random.nextInt(0, it.size.minus(1).coerceAtLeast(0))
                val query = GalleryQueryParameter()
                query.filterId = it[ranPos].bucketId
                return@map query
            }
            .flatMap { galleryProvider.fetchGalleryRx(it) }
            .flatMap {
                it.moveToLast()
                galleryProvider.cursorToPhotoUriRx(it)
            }
            .flatMap { galleryProvider.pathToBitmapRx(it) }
            .flatMap { galleryProvider.saveBitmapToFileRx(it) }
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
        galleryProvider.fetchDirectoriesRx()
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
        galleryProvider.fetchGalleryRx()
            .flatMap {
                val ranPos = Random.nextInt(0, it.count)
                it.moveToPosition(ranPos)
                galleryProvider.cursorToPhotoUriRx(it)
            }
            .flatMap { galleryProvider.pathToBitmapRx(it, ivThumb1.width) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                ivThumb1.setImageBitmap(it)
            }, {
                Timber.d("ERROR $it")
            }).addTo(disposable)

    }

    private fun performSingleUpload() {
        galleryProvider.fetchGalleryRx()
            .flatMap {
                val ranPos = Random.nextInt(0, it.count)
                it.moveToPosition(ranPos)
                galleryProvider.cursorToPhotoUriRx(it)
            }
            .flatMap { galleryProvider.pathToMultipartRx(it, "files", 500) }
            .flatMap { apiService.uploads(listOf(it)) }
            .map {
                it.asJsonObject
                    .get("pathList")
                    .asJsonArray[0]
                    .asJsonObject
                    .get("path")
                    .asString
            }
            .map { URL("https://cdn.qtzz.synology.me/$it").readBytes() }
            .map { BitmapFactory.decodeByteArray(it, 0, it.size) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                ivThumb2.setImageBitmap(it)
            }, {
                Timber.d("ERROR $it")
            }).addTo(disposable)
    }
}