package com.gallery.example.ui.gallery

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gallery.core.Factory
import com.gallery.core.GalleryProvider
import com.gallery.core.model.GalleryFilterData
import com.gallery.core.model.GalleryQueryParameter
import com.gallery.example.R
import com.gallery.example.ui.select_album.SelectAlbumBottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import java.text.DecimalFormat

/**
 * Description : 일반적인 갤러리 예제 화면
 *
 * Created by juhongmin on 2023/08/15
 */
internal class GeneralGalleryFragment : Fragment(
    R.layout.f_general_gallery
), SelectAlbumBottomSheetDialog.Listener {

    private lateinit var rvContents: RecyclerView
    private lateinit var tvSelectAlbum: AppCompatTextView
    private val adapter: Adapter by lazy { Adapter(this) }

    private val galleryProvider: GalleryProvider by lazy { Factory.create(requireContext().applicationContext) }
    private lateinit var cursor: Cursor
    private val albumList: MutableList<GalleryFilterData> by lazy { mutableListOf() }
    private val queryParameter: GalleryQueryParameter by lazy { GalleryQueryParameter() }
    private val dataList: MutableList<GeneralGalleryItem> by lazy { mutableListOf() }
    private var currentAlbum: GalleryFilterData? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
        initData()
    }

    override fun onSelectedFilter(data: GalleryFilterData) {
        bindingSelectAlbum(data)
        queryParameter.filterId = data.bucketId
        flow {
            cursor = galleryProvider.fetchGallery(queryParameter)
            emit(reqPagingList(cursor))
        }.flowOn(Dispatchers.IO)
            .onEach {
                dataList.clear()
                dataList.addAll(it)
                adapter.submitList(dataList)
            }.launchIn(lifecycleScope)
    }

    private fun initData() {
        flow {
            cursor = galleryProvider.fetchGallery(queryParameter)
            emit(reqPagingList(cursor))
        }
            .flowOn(Dispatchers.IO)
            .onEach {
                dataList.clear()
                dataList.addAll(it)
                adapter.submitList(dataList)
            }
            .launchIn(lifecycleScope)

        flow { emit(galleryProvider.fetchDirectories()) }
            .flowOn(Dispatchers.IO)
            .onEach { list ->
                albumList.clear()
                albumList.addAll(list)
                list.getOrNull(0)?.let {
                    bindingSelectAlbum(it)
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun reqPagingList(cursor: Cursor): List<GeneralGalleryItem> {
        if (cursor.isLast) return listOf()
        val list = mutableListOf<GeneralGalleryItem>()
        for (idx in 0 until 100) {
            if (cursor.moveToNext()) {
                runCatching {
                    val imageUri = cursor.getImageUri()
                    list.add(GeneralGalleryItem(imageUri.toString()))
                }
            } else {
                break
            }
        }
        return list
    }

    private fun bindingSelectAlbum(data: GalleryFilterData) {
        currentAlbum = data
        tvSelectAlbum.text = "${data.bucketName} (${DecimalFormat("#,###").format(data.count)})"
    }

    private fun onLoadNextPage() {
        flow { emit(reqPagingList(cursor)) }
            .map { dataList.addAll(it) }
            .flowOn(Dispatchers.IO)
            .onEach { adapter.submitList(dataList) }
            .launchIn(lifecycleScope)
    }

    private fun Cursor.getImageUri(): Uri {
        val columnId = try {
            cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        } catch (ex: IllegalArgumentException) {
            0
        }
        return Uri.withAppendedPath(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            getLong(columnId).toString()
        )
    }

    private fun initView(view: View) {
        rvContents = view.findViewById(R.id.rvContents)
        tvSelectAlbum = view.findViewById(R.id.tvSelectedAlbum)
        view.findViewById<LinearLayoutCompat>(R.id.llAlbum).setOnClickListener {
            SelectAlbumBottomSheetDialog()
                .setCurrentAlbum(currentAlbum)
                .setData(albumList)
                .setListener(this)
                .simpleShow(childFragmentManager)
        }
        rvContents.layoutManager = GridLayoutManager(view.context, 3)
        rvContents.adapter = adapter
        rvContents.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (!rv.canScrollVertically(1)) {
                    // 맨 마지막 스크롤
                    onLoadNextPage()
                }
            }
        })
    }

    private class Adapter(
        private val fragment: Fragment
    ) : ListAdapter<GeneralGalleryItem, GeneralGalleryViewHolder>(SimpleDiffUtil()) {

        private class SimpleDiffUtil : DiffUtil.ItemCallback<GeneralGalleryItem>() {
            override fun areItemsTheSame(
                oldItem: GeneralGalleryItem,
                newItem: GeneralGalleryItem
            ): Boolean {
                return oldItem.imageUrl == newItem.imageUrl
            }

            override fun areContentsTheSame(
                oldItem: GeneralGalleryItem,
                newItem: GeneralGalleryItem
            ): Boolean {
                return oldItem == newItem
            }
        }

        private val viewHolderListener: GeneralGalleryViewHolder.Listener =
            object : GeneralGalleryViewHolder.Listener {

                override fun onSelectedImage(data: GeneralGalleryItem) {
                    Timber.d("onSelectedImage $data")
                }
            }

        override fun submitList(list: MutableList<GeneralGalleryItem>?) {
            super.submitList(list?.toMutableList())
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): GeneralGalleryViewHolder {
            return GeneralGalleryViewHolder(Glide.with(fragment), parent, viewHolderListener)
        }

        override fun onBindViewHolder(holder: GeneralGalleryViewHolder, position: Int) {
            holder.onBindView(getItem(position))
        }
    }
}