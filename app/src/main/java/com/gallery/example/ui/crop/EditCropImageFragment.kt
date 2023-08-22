package com.gallery.example.ui.crop

import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
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
import com.gallery.core.model.GalleryQueryParameter
import com.gallery.edit.CropImageEditView
import com.gallery.example.R
import com.gallery.example.ui.capture.CaptureImagesBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Description : [CropImageEditView] 의 사용방법을 안내하는 Fragment
 *
 * Created by juhongmin on 2023/08/20
 */
internal class EditCropImageFragment : Fragment(
    R.layout.f_edit_crop_image
) {

    private lateinit var ivEdit: CropImageEditView
    private lateinit var rvContents: RecyclerView
    private lateinit var clProgress: ConstraintLayout
    private lateinit var ivRotate: AppCompatImageView

    private val requestManager: RequestManager by lazy { Glide.with(this) }
    private val adapter: Adapter by lazy { Adapter(requestManager, viewHolderListener) }

    private val galleryProvider: GalleryProvider by lazy { Factory.create(requireContext().applicationContext) }
    private val dataList: MutableList<EditCropImageItem> by lazy { mutableListOf() }
    private val queryParameter: GalleryQueryParameter by lazy { GalleryQueryParameter() }
    private lateinit var cursor: Cursor

    private val viewHolderListener: EditCropImageViewHolder.Listener =
        object : EditCropImageViewHolder.Listener {
            override fun onSelectPhoto(data: EditCropImageItem) {
                lifecycleScope.launch {
                    clProgress.visibility = View.VISIBLE
                    val bitmap = withContext(Dispatchers.IO) {
                        galleryProvider.pathToBitmap(data.imageUrl)
                    }
                    ivEdit.setImageBitmap(bitmap)
                    clProgress.visibility = View.GONE
                }

            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
        initData()
    }

    private fun initView(view: View) {
        rvContents = view.findViewById(R.id.rvContents)
        clProgress = view.findViewById(R.id.clProgress)
        ivEdit = view.findViewById(R.id.ivEdit)
        ivRotate = view.findViewById(R.id.ivRotate)
        ivRotate.setOnClickListener {
            var rotate = ivEdit.rotatedDegrees.plus(90)
            if (rotate >= 360) {
                rotate = 0
            }
            ivEdit.rotatedDegrees = rotate
        }
        view.findViewById<AppCompatTextView>(R.id.tvConfirm).setOnClickListener {
            showSelectedImagesBottomSheet()
        }
        rvContents.layoutManager = GridLayoutManager(view.context, 3)
        rvContents.adapter = adapter
        rvContents.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (queryParameter.isLast) return
                if (!rv.canScrollVertically(1)) {
                    // 맨 마지막 스크롤
                    onLoadNextPage()
                }
            }
        })
    }

    private fun initData() {
        flow {
            cursor = galleryProvider.fetchCursor(queryParameter)
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

    private fun reqPagingList(cursor: Cursor): List<EditCropImageItem> {
        return galleryProvider.fetchList(cursor, queryParameter)
            .map { EditCropImageItem(it.uri.toString()) }
    }

    private fun onLoadNextPage() {
        flow { emit(reqPagingList(cursor)) }
            .map { dataList.addAll(it) }
            .flowOn(Dispatchers.IO)
            .onEach { adapter.submitList(dataList) }
            .launchIn(lifecycleScope)
    }

    private fun showSelectedImagesBottomSheet() {
        clProgress.visibility = View.VISIBLE
        flow {
            val bitmap = galleryProvider.getCropImageEditToBitmap(ivEdit.getEditInfo())
            emit(bitmap)
        }.flowOn(Dispatchers.IO)
            .catch { clProgress.visibility = View.GONE }
            .onEach {
                CaptureImagesBottomSheet()
                    .setData(listOf(it))
                    .simpleShow(childFragmentManager)
                clProgress.visibility = View.GONE
            }
            .launchIn(lifecycleScope)
    }
}

private class Adapter(
    private val requestManager: RequestManager,
    private val viewHolderListener: EditCropImageViewHolder.Listener
) : ListAdapter<EditCropImageItem, EditCropImageViewHolder>(SimpleDiffUtil()) {
    private class SimpleDiffUtil : DiffUtil.ItemCallback<EditCropImageItem>() {
        override fun areItemsTheSame(
            oldItem: EditCropImageItem,
            newItem: EditCropImageItem
        ): Boolean {
            return oldItem.imageUrl == newItem.imageUrl
        }

        override fun areContentsTheSame(
            oldItem: EditCropImageItem,
            newItem: EditCropImageItem
        ): Boolean {
            return oldItem == newItem
        }
    }

    override fun submitList(list: MutableList<EditCropImageItem>?) {
        super.submitList(list?.toMutableList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EditCropImageViewHolder {
        return EditCropImageViewHolder(requestManager, parent, viewHolderListener)
    }

    override fun onBindViewHolder(holder: EditCropImageViewHolder, position: Int) {
        holder.onBindView(getItem(position))
    }
}
