package com.gallery.ui.internal.adapter

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.gallery.ui.R
import com.gallery.ui.internal.ImageLoader
import com.gallery.ui.internal.changeVisible
import com.gallery.ui.internal.crossFadeTransition
import com.gallery.ui.internal.dp
import com.gallery.ui.internal.getDeviceWidth
import com.gallery.ui.internal.listener.PhotoPickerBridgeListener
import com.gallery.ui.internal.placeHolder
import com.gallery.ui.internal.setCornerAndBgColor
import com.gallery.ui.internal.viewholder.BasePickerViewHolder
import com.gallery.ui.model.PhotoPicker
import timber.log.Timber


/**
 * Description : Photo Picker Adapter
 *
 * Created by juhongmin on 3/23/24
 */
internal class PhotoPickerAdapter(
    private val listener: PhotoPickerBridgeListener
) : RecyclerView.Adapter<BasePickerViewHolder>() {

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
            R.layout.vh_child_camera -> CameraViewHolder(parent)
            R.layout.vh_child_photo -> PhotoViewHolder(parent)
            R.layout.vh_child_video -> VideoViewHolder(parent)
            else -> throw IllegalArgumentException("Invalid ViewType")
        }
    }

    override fun onBindViewHolder(holder: BasePickerViewHolder, position: Int) {
        val item = dataList.getOrNull(position) ?: return
        holder.onBindView(item)
    }

    override fun onBindViewHolder(
        holder: BasePickerViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            this.onBindViewHolder(holder, position)
        } else if (payloads[0] is List<*>) {
            @Suppress("UNCHECKED_CAST")
            holder.onPayloadBindView(payloads[0] as List<Any>)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = dataList.getOrNull(position) ?: return super.getItemViewType(position)
        return when (item) {
            is PhotoPicker.Camera -> R.layout.vh_child_camera
            is PhotoPicker.Photo -> R.layout.vh_child_photo
            is PhotoPicker.Video -> R.layout.vh_child_video
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
                oldItem.contentUri == newItem.contentUri
            } else if (oldItem is PhotoPicker.Video && newItem is PhotoPicker.Video) {
                oldItem.contentUri == newItem.contentUri
            } else oldItem is PhotoPicker.Camera && newItem is PhotoPicker.Camera
        }

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            val oldItem = oldList[oldPosition]
            val newItem = newList[newPosition]
            return if (oldItem is PhotoPicker.Photo && newItem is PhotoPicker.Photo) {
                oldItem == newItem
            } else if (oldItem is PhotoPicker.Video && newItem is PhotoPicker.Video) {
                oldItem == newItem
            } else oldItem is PhotoPicker.Camera && newItem is PhotoPicker.Camera
        }
    }

    inner class CameraViewHolder(
        parent: ViewGroup
    ) : BasePickerViewHolder(parent, R.layout.vh_child_camera) {

        override fun onBindView(item: PhotoPicker) {}
    }

    inner class PhotoViewHolder(
        parent: ViewGroup
    ) : BasePickerViewHolder(parent, R.layout.vh_child_photo) {

        private val ivThumb: AppCompatImageView by lazy { itemView.findViewById(R.id.ivThumb) }
        private val vSelected: View by lazy { itemView.findViewById(R.id.vSelected) }
        private val vBgNotSelected: View by lazy { itemView.findViewById(R.id.vBgNotSelected) }
        private val vBgSelected: View by lazy { itemView.findViewById(R.id.vBgSelected) }
        private val tvSelectNum: AppCompatTextView by lazy { itemView.findViewById(R.id.tvSelectNum) }
        private val ivExpandContent: AppCompatImageView by lazy { itemView.findViewById(R.id.ivExpandContent) }
        private var data: PhotoPicker.Photo? = null

        init {
            vBgNotSelected.setCornerAndBgColor("#80FFFFFF", 10F.dp) {
                setStroke(1.dp, Color.parseColor("#4D5F5C56"))
            }
            vBgSelected.setCornerAndBgColor("#0091EA", 10F.dp)
            ivExpandContent.setCornerAndBgColor("#33222222", 2F.dp) {
                setStroke(1.dp, Color.parseColor("#4D9C9C9C"))
            }

            ivThumb.setOnClickListener {
                val data = this.data ?: return@setOnClickListener
                if (data.isSelected) {
                    listener.removePicker(bindingAdapterPosition, data)
                } else {
                    listener.addPicker(bindingAdapterPosition, data)
                }
            }
            itemView.findViewById<ConstraintLayout>(R.id.clExpandContent).setOnClickListener {
                val data = data ?: return@setOnClickListener
                listener.onShowExpandPhoto(data)
            }
        }

        override fun onBindView(item: PhotoPicker) {
            if (item !is PhotoPicker.Photo) return
            data = item
            bindThumbnail(item)
            bindSelectionNum(item)
        }

        override fun onPayloadBindView(payloads: List<Any>) {
            for (newItem in payloads) {
                if (newItem is PhotoPicker.Photo && newItem.id == data?.id) {
                    data?.let { bindSelectionNum(it) }
                    break
                }
            }
        }

        /**
         * Binding Thumbnail
         */
        private fun bindThumbnail(
            item: PhotoPicker.Photo
        ) {
            val bitmap = ImageLoader.getCacheBitmap(item.contentUri)
            if (bitmap == null) {
                Timber.d("캐싱 안된 이미지 입니다. ${item.contentUri}")
                ivThumb.scaleType = ImageView.ScaleType.CENTER_INSIDE
                ivThumb.setImageResource(R.drawable.ic_broken_image)
//                requestManager.load(item.contentUri)
//                    .placeholder(placeHolder)
//                    .transition(crossFadeTransition)
//                    .override(overrideSize)
//                    .into(ivThumb)
            } else {
                ivThumb.scaleType = ImageView.ScaleType.CENTER_CROP
                ivThumb.setImageBitmap(bitmap)
//                requestManager.load(bitmap)
//                    .placeholder(placeHolder)
//                    .transition(crossFadeTransition)
//                    .into(ivThumb)
            }
        }

        /**
         * Binding Handle Selection Num
         */
        private fun bindSelectionNum(
            item: PhotoPicker.Photo
        ) {
            listener.setSelectedGallery(item)
            if (item.isSelected) {
                vSelected.changeVisible(true)
                tvSelectNum.changeVisible(true)
                vBgSelected.changeVisible(true)
                vBgNotSelected.changeVisible(false)
                tvSelectNum.text = item.selectedNum
            } else {
                vSelected.changeVisible(false)
                tvSelectNum.changeVisible(false)
                vBgSelected.changeVisible(false)
                vBgNotSelected.changeVisible(true)
            }
        }
    }

    inner class VideoViewHolder(
        parent: ViewGroup
    ) : BasePickerViewHolder(parent, R.layout.vh_child_video) {

        private val ivThumb: AppCompatImageView by lazy { itemView.findViewById(R.id.ivThumb) }
        private val vSelected: View by lazy { itemView.findViewById(R.id.vSelected) }
        private val vBgNotSelected: View by lazy { itemView.findViewById(R.id.vBgNotSelected) }
        private val vBgSelected: View by lazy { itemView.findViewById(R.id.vBgSelected) }
        private val tvSelectNum: AppCompatTextView by lazy { itemView.findViewById(R.id.tvSelectNum) }
        // private val ivExpandContent: AppCompatImageView by lazy { itemView.findViewById(R.id.ivExpandContent) }
        private val tvDuration: AppCompatTextView by lazy { itemView.findViewById(R.id.tvDuration) }
        private val overrideSize: Int by lazy { itemView.context.getDeviceWidth() / 3 }
        private var data: PhotoPicker.Video? = null

        init {
            vBgNotSelected.setCornerAndBgColor("#80FFFFFF", 10F.dp) {
                setStroke(1.dp, Color.parseColor("#4D5F5C56"))
            }
            vBgSelected.setCornerAndBgColor("#0091EA", 10F.dp)
//            ivExpandContent.setCornerAndBgColor("#33222222", 2F.dp) {
//                setStroke(1.dp, Color.parseColor("#4D9C9C9C"))
//            }
            tvDuration.setCornerAndBgColor("#33222222", 5F.dp)

            ivThumb.setOnClickListener {
                val data = this.data ?: return@setOnClickListener
                if (data.isSelected) {
                    listener.removePicker(bindingAdapterPosition, data)
                } else {
                    listener.addPicker(bindingAdapterPosition, data)
                }
            }
        }

        override fun onBindView(item: PhotoPicker) {
            if (item !is PhotoPicker.Video) return
            data = item
            bindThumbnail(item)
            bindSelectionNum(item)
            bindDuration(item)
        }

        override fun onPayloadBindView(payloads: List<Any>) {
            for (newItem in payloads) {
                if (newItem is PhotoPicker.Video && newItem.id == data?.id) {
                    data?.let { bindSelectionNum(it) }
                    break
                }
            }
        }

        /**
         * Binding Thumbnail
         */
        private fun bindThumbnail(
            item: PhotoPicker.Video
        ) {
            val bitmap = ImageLoader.getCacheBitmap(item.contentUri)
            if (bitmap == null) {
                Timber.d("캐싱 안된 이미지 입니다. ${item.contentUri}")
                requestManager.load(item.contentUri)
                    .placeholder(placeHolder)
                    .transition(crossFadeTransition)
                    .override(overrideSize)
                    .into(ivThumb)
            } else {
                requestManager.load(bitmap)
                    .placeholder(placeHolder)
                    .transition(crossFadeTransition)
                    .into(ivThumb)
            }
        }

        /**
         * Binding Handle Selection Num
         */
        private fun bindSelectionNum(
            item: PhotoPicker.Video
        ) {
            listener.setSelectedGallery(item)
            if (item.isSelected) {
                vSelected.changeVisible(true)
                tvSelectNum.changeVisible(true)
                vBgSelected.changeVisible(true)
                vBgNotSelected.changeVisible(false)
                tvSelectNum.text = item.selectedNum
            } else {
                vSelected.changeVisible(false)
                tvSelectNum.changeVisible(false)
                vBgSelected.changeVisible(false)
                vBgNotSelected.changeVisible(true)
            }
        }

        private fun bindDuration(
            item: PhotoPicker.Video
        ) {
            tvDuration.text = item.durationText
        }
    }
}