package com.gallery.ui.internal

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.gallery.core.GalleryProvider
import com.gallery.ui.R
import com.gallery.ui.model.PhotoPicker

/**
 * Description : Selected Photo Picker Adapter
 *
 * Created by juhongmin on 3/27/24
 */
internal class SelectedPhotoPickerAdapter(
    private val listener: PhotoPickerAdapter.Listener,
    private val provider: GalleryProvider
) : RecyclerView.Adapter<BasePickerViewHolder>() {

    private val placeHolder: ColorDrawable by lazy { ColorDrawable(Color.parseColor("#eeeeee")) }
    private val requestManager: RequestManager by lazy { listener.getRequestManager() }
    private val dataList: MutableList<PhotoPicker> by lazy { mutableListOf() }

    /**
     * 데이터가 변경되었을때 이전 데이터들 비교하여 갱신 처리 함수
     * @param newList oldList + 새로운 데이터 리스트
     */
    fun submitList(newList: List<PhotoPicker>) {
        val diffResult = DiffUtil.calculateDiff(SimpleDiffUtil(dataList, newList))
        dataList.clear()
        dataList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BasePickerViewHolder {
        return when (viewType) {
            R.layout.vh_child_selected_photo -> PhotoViewHolder(parent)
            R.layout.vh_child_selected_video -> VideoViewHolder(parent)
            else -> throw IllegalArgumentException("Invalid ViewType")
        }
    }

    override fun onBindViewHolder(holder: BasePickerViewHolder, position: Int) {
        val item = dataList.getOrNull(position) ?: return
        holder.onBindView(item)
    }

    override fun getItemViewType(position: Int): Int {
        val item = dataList.getOrNull(position) ?: return super.getItemViewType(position)
        return when (item) {
            is PhotoPicker.Photo -> R.layout.vh_child_selected_photo
            is PhotoPicker.Video -> R.layout.vh_child_selected_video
            else -> super.getItemViewType(position)
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    inner class SimpleDiffUtil(
        private val oldList: List<PhotoPicker>,
        private val newList: List<PhotoPicker>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            val oldItem = oldList[oldPosition]
            val newItem = newList[newPosition]
            return if (oldItem is PhotoPicker.Photo && newItem is PhotoPicker.Photo) {
                oldItem.imagePath == newItem.imagePath
            } else if (oldItem is PhotoPicker.Video && newItem is PhotoPicker.Video) {
                oldItem.imagePath == newItem.imagePath
            } else false
        }

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            val oldItem = oldList[oldPosition]
            val newItem = newList[newPosition]
            return if (oldItem is PhotoPicker.Photo && newItem is PhotoPicker.Photo) {
                oldItem == newItem
            } else if (oldItem is PhotoPicker.Video && newItem is PhotoPicker.Video) {
                oldItem == newItem
            } else false
        }
    }

    inner class PhotoViewHolder(
        parent: ViewGroup
    ) : BasePickerViewHolder(parent, R.layout.vh_child_selected_photo) {

        private val ivThumb: AppCompatImageView by lazy { itemView.findViewById(R.id.ivThumb) }
        private val clRemove: ConstraintLayout by lazy { itemView.findViewById(R.id.clRemove) }
        private val overrideSize: Int by lazy { itemView.context.getDeviceWidth() / 3 }
        private var data: PhotoPicker.Photo? = null

        init {
            clRemove.background = GradientDrawable(
                GradientDrawable.Orientation.BL_TR,
                intArrayOf(Color.parseColor("#4D222222"), Color.parseColor("#4D222222"))
            ).apply {
                cornerRadius = 8F.dp
            }
            ivThumb.setOnClickListener { data?.let { listener.removePicker(-1, it) } }
            clRemove.setOnClickListener { data?.let { listener.removePicker(-1, it) } }
        }

        override fun onBindView(item: PhotoPicker) {
            if (item !is PhotoPicker.Photo) return
            data = item
            PhotoPickerImageLoader.getCacheBitmap(item.imagePath)?.let {
                requestManager.load(it)
                    .override(overrideSize)
                    .placeholder(placeHolder)
                    .into(ivThumb)
            }
        }
    }

    inner class VideoViewHolder(
        parent: ViewGroup
    ) : BasePickerViewHolder(parent, R.layout.vh_child_selected_video) {

        private val ivThumb: AppCompatImageView by lazy { itemView.findViewById(R.id.ivThumb) }
        private val clRemove: ConstraintLayout by lazy { itemView.findViewById(R.id.clRemove) }
        private val overrideSize: Int by lazy { itemView.context.getDeviceWidth() / 3 }
        private var data: PhotoPicker.Video? = null

        init {
            clRemove.background = GradientDrawable(
                GradientDrawable.Orientation.BL_TR,
                intArrayOf(Color.parseColor("#4D222222"), Color.parseColor("#4D222222"))
            ).apply {
                cornerRadius = 8F.dp
            }
            ivThumb.setOnClickListener { data?.let { listener.removePicker(-1, it) } }
            clRemove.setOnClickListener { data?.let { listener.removePicker(-1, it) } }
        }

        override fun onBindView(item: PhotoPicker) {
            if (item !is PhotoPicker.Video) return
            data = item
            PhotoPickerImageLoader.getCacheBitmap(item.imagePath)?.let {
                requestManager.load(it)
                    .override(overrideSize)
                    .placeholder(placeHolder)
                    .into(ivThumb)
            }
        }
    }
}
