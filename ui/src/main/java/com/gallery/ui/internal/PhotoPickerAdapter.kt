package com.gallery.ui.internal

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.gallery.core.GalleryProvider
import com.gallery.ui.R
import com.gallery.ui.model.PhotoPicker


/**
 * Description : Photo Picker Adapter
 *
 * Created by juhongmin on 3/23/24
 */
internal class PhotoPickerAdapter(
    private val listener: Listener,
    private val provider: GalleryProvider
) : RecyclerView.Adapter<BasePickerViewHolder>() {

    interface Listener {

        fun getRequestManager(): RequestManager

        /**
         * 비동기 캐싱 처리함수
         * @param item 캐싱할 아이템
         */
        fun asyncSaveCache(item: PhotoPicker)

        /**
         * 선택 사진
         * @param pos 선택한 위치값
         * @param item
         */
        fun addPicker(pos: Int, item: PhotoPicker)

        /**
         * 선택 해제 사진
         * @param pos 선택한 위치값
         * @param item
         */
        fun removePicker(pos: Int, item: PhotoPicker)
    }

    private val crossFadeFactory: DrawableCrossFadeFactory by lazy {
        DrawableCrossFadeFactory
            .Builder()
            .setCrossFadeEnabled(true)
            .build()
    }

    private val crossFadeTransition: DrawableTransitionOptions by lazy {
        DrawableTransitionOptions
            .withCrossFade(crossFadeFactory)
    }

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
                oldItem.imagePath == newItem.imagePath
            } else if (oldItem is PhotoPicker.Video && newItem is PhotoPicker.Video) {
                oldItem.imagePath == newItem.imagePath
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
        private val clSelectedNum: ConstraintLayout by lazy { itemView.findViewById(R.id.clSelectedNum) }
        private val vBgNotSelected: View by lazy { itemView.findViewById(R.id.vBgNotSelected) }
        private val vBgSelected: View by lazy { itemView.findViewById(R.id.vBgSelected) }
        private val tvSelectNum: AppCompatTextView by lazy { itemView.findViewById(R.id.tvSelectNum) }
        private val overrideSize: Int by lazy { itemView.context.getDeviceWidth() / 3 }
        private var data: PhotoPicker.Photo? = null

        init {
//            clSelectedNum.background = GradientDrawable(
//                GradientDrawable.Orientation.BL_TR,
//                intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT)
//            ).apply {
//                cornerRadius = 10F.dp
//            }
//            clSelectedNum.clipToOutline = true

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
            // TODO 여기서 좀더 디테일 하게 캐싱되어 있지 않는 경우
            // TODO UI Thread 로 가져오는게 아닌 다른 방법으로 처리 하면 좋을거 같음
            val bitmap = PhotoPickerImageLoader.getCacheBitmap(item.imagePath)
            if (bitmap != null) {
                requestManager.load(bitmap)
                    .placeholder(placeHolder)
                    .into(ivThumb)
            } else {
                requestManager.load(provider.getPhotoThumbnail(item.id, overrideSize))
                    .placeholder(placeHolder)
                    .into(ivThumb)
                listener.asyncSaveCache(item)
            }
        }

        /**
         * Binding Handle Selection Num
         */
        private fun bindSelectionNum(
            item: PhotoPicker.Photo
        ) {
            if (item.isSelected) {
                tvSelectNum.changeVisible(true)
                vBgSelected.changeVisible(true)
                vBgNotSelected.changeVisible(false)
                tvSelectNum.text = item.selectedNum
            } else {
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
        private val clSelectedNum: ConstraintLayout by lazy { itemView.findViewById(R.id.clSelectedNum) }
        private val vBgNotSelected: View by lazy { itemView.findViewById(R.id.vBgNotSelected) }
        private val vBgSelected: View by lazy { itemView.findViewById(R.id.vBgSelected) }
        private val tvSelectNum: AppCompatTextView by lazy { itemView.findViewById(R.id.tvSelectNum) }
        private val tvDuration: AppCompatTextView by lazy { itemView.findViewById(R.id.tvDuration) }
        private val overrideSize: Int by lazy { itemView.context.getDeviceWidth() / 3 }
        private var data: PhotoPicker.Video? = null

        init {
            clSelectedNum.background = GradientDrawable(
                GradientDrawable.Orientation.BL_TR,
                intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT)
            ).apply {
                cornerRadius = 10F.dp
            }
            clSelectedNum.clipToOutline = true
            tvDuration.background = GradientDrawable(
                GradientDrawable.Orientation.BL_TR,
                intArrayOf(
                    Color.parseColor("#4D000000"),
                    Color.parseColor("#4D000000")
                )
            ).apply { cornerRadius = 5F.dp }

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
            // TODO 여기서 좀더 디테일 하게 캐싱되어 있지 않는 경우
            // TODO UI Thread 로 가져오는게 아닌 다른 방법으로 처리 하면 좋을거 같음
            val bitmap = PhotoPickerImageLoader.getCacheBitmap(item.imagePath)
            if (bitmap != null) {
                requestManager.load(bitmap)
                    .placeholder(placeHolder)
                    .into(ivThumb)
            } else {
                requestManager.load(provider.getVideoThumbnail(item.id, overrideSize))
                    .placeholder(placeHolder)
                    .into(ivThumb)
                listener.asyncSaveCache(item)
            }
        }

        /**
         * Binding Handle Selection Num
         */
        private fun bindSelectionNum(
            item: PhotoPicker.Video
        ) {
            if (item.isSelected) {
                tvSelectNum.changeVisible(true)
                vBgSelected.changeVisible(true)
                vBgNotSelected.changeVisible(false)
                tvSelectNum.text = item.selectedNum
            } else {
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