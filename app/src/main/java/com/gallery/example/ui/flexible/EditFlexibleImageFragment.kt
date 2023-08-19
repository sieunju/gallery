package com.gallery.example.ui.flexible

import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.gallery.core.Factory
import com.gallery.core.GalleryProvider
import com.gallery.edit.FlexibleImageEditView
import com.gallery.example.R
import com.gallery.example.ui.capture.CaptureImagesBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Description : [FlexibleImageEditView] 의 사용방법을 안내하는 Fragment
 *
 * Created by juhongmin on 2023/08/20
 */
internal class EditFlexibleImageFragment : Fragment(
    R.layout.f_edit_flexible_image
) {
    private lateinit var ivEdit: FlexibleImageEditView
    private lateinit var rvContents: RecyclerView
    private lateinit var clProgress: ConstraintLayout

    private val requestManager: RequestManager by lazy { Glide.with(this) }
    private val adapter: Adapter by lazy { Adapter(requestManager, viewHolderListener) }

    private val galleryProvider: GalleryProvider by lazy { Factory.create(requireContext().applicationContext) }
    private lateinit var cursor: Cursor
    private val dataList: MutableList<EditFlexibleImageItem> by lazy { mutableListOf() }
    private val selectedPhotoMap: MutableMap<String, EditFlexibleImageItem> by lazy { mutableMapOf() }

    private var isLast: Boolean = false
    private val viewHolderListener: EditFlexibleImageViewHolder.Listener =
        object : EditFlexibleImageViewHolder.Listener {
            override fun onSelectPhoto(data: EditFlexibleImageItem): List<EditFlexibleImageItem> {
                handleEditFlexibleImage(data)
                val updateList = selectedPhotoMap.map { it.value }.toMutableList()
                updateList.add(data)
                adapter.sortedPickerMap(data.selectedNum == null, data, selectedPhotoMap)
                return updateList
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
        initData()
    }

    private fun initView(view: View) {
        rvContents = view.findViewById(R.id.rvContents)
        ivEdit = view.findViewById(R.id.ivEdit)
        clProgress = view.findViewById(R.id.clProgress)
        view.findViewById<AppCompatTextView>(R.id.tvConfirm).setOnClickListener {
            CaptureImagesBottomSheet()
                .setData(getSelectedImages())
                .simpleShow(childFragmentManager)
        }
        rvContents.layoutManager = GridLayoutManager(view.context, 3)
        rvContents.adapter = adapter
        rvContents.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (isLast) return
                if (!rv.canScrollVertically(1)) {
                    // 맨 마지막 스크롤
                    onLoadNextPage()
                }
            }
        })
    }

    private fun initData() {
        flow {
            cursor = galleryProvider.fetchGallery()
            emit(reqPagingList(cursor))
        }
            .flowOn(Dispatchers.IO)
            .onEach {
                dataList.clear()
                dataList.addAll(it)
                adapter.submitList(dataList)
            }
            .launchIn(lifecycleScope)
    }

    private fun reqPagingList(cursor: Cursor): List<EditFlexibleImageItem> {
        isLast = cursor.isLast
        if (isLast) return listOf()

        val list = mutableListOf<EditFlexibleImageItem>()
        for (idx in 0 until 100) {
            if (cursor.moveToNext()) {
                runCatching {
                    val imageUri = cursor.getImageUri()
                    list.add(EditFlexibleImageItem(imageUri.toString()))
                }
            } else {
                break
            }
        }
        return list
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

    private fun getSelectedImages(): List<String> {
        return selectedPhotoMap.map { it.value.imageUrl }
    }

    private fun handleEditFlexibleImage(item: EditFlexibleImageItem) {
        clProgress.visibility = View.VISIBLE
        flow {
            val bitmap = galleryProvider.pathToBitmap(item.imageUrl)
            emit(item.imageUrl to bitmap)
        }.flowOn(Dispatchers.IO)
            .catch { Timber.d("ERROR $it") }
            .onEach {
                Timber.d("SUCC $it")
                ivEdit.loadBitmap(it.second)
                clProgress.visibility = View.GONE
            }
            .launchIn(lifecycleScope)
    }

    private class Adapter(
        private val requestManager: RequestManager,
        private val viewHolderListener: EditFlexibleImageViewHolder.Listener
    ) : ListAdapter<EditFlexibleImageItem, EditFlexibleImageViewHolder>(SimpleDiffUtil()) {
        private class SimpleDiffUtil : DiffUtil.ItemCallback<EditFlexibleImageItem>() {
            override fun areItemsTheSame(
                oldItem: EditFlexibleImageItem,
                newItem: EditFlexibleImageItem
            ): Boolean {
                return oldItem.imageUrl == newItem.imageUrl
            }

            override fun areContentsTheSame(
                oldItem: EditFlexibleImageItem,
                newItem: EditFlexibleImageItem
            ): Boolean {
                return oldItem == newItem
            }
        }

        override fun submitList(list: MutableList<EditFlexibleImageItem>?) {
            super.submitList(list?.toMutableList())
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): EditFlexibleImageViewHolder {
            return EditFlexibleImageViewHolder(requestManager, parent, viewHolderListener)
        }

        override fun onBindViewHolder(holder: EditFlexibleImageViewHolder, position: Int) {
            holder.onBindView(getItem(position))
        }

        override fun onBindViewHolder(
            holder: EditFlexibleImageViewHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            if (payloads.size == 0) {
                this.onBindViewHolder(holder, position)
            } else {
                if (payloads[0] is List<*>) {
                    holder.onPayloadBindView(payloads[0] as List<*>)
                }
            }
        }

        /**
         * 사진을 선택 / 해제 후
         * 선택한 숫자들을 다시 재 정렬 처리하는 함수
         * @param isAdd 추가 or 제거
         * @param item 갤러리 선택 아이템
         * @param map 현재 선택한 사진들
         */
        fun sortedPickerMap(
            isAdd: Boolean,
            item: EditFlexibleImageItem,
            map: MutableMap<String, EditFlexibleImageItem>
        ) {
            if (isAdd) {
                item.selectedNum = map.size.plus(1)
                map[item.imageUrl] = item
            } else {
                item.selectedNum = null
                map.remove(item.imageUrl)
                var idx = 1
                map.forEach {
                    it.value.selectedNum = idx
                    idx++
                }
            }
        }
    }
}