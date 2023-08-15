package com.gallery.example.ui

import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.gallery.core.model.GalleryFilterData
import com.gallery.example.BR
import com.gallery.example.R
import com.gallery.example.databinding.DGalleryBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Description :
 *
 * Created by juhongmin on 2022/12/09
 */
@AndroidEntryPoint
internal class GalleryBottomSheetDialog : BottomSheetDialogFragment(),
    SelectAlbumBottomSheetDialog.Listener {

    private val viewModel: GalleryBottomSheetViewModel by viewModels()

    private lateinit var binding: DGalleryBottomSheetBinding

    private val windowManager: WindowManager by lazy { requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    private val adapter: Adapter by lazy { Adapter() }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            setupRatio(bottomSheetDialog)
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return DataBindingUtil.inflate<DGalleryBottomSheetBinding>(
            inflater,
            R.layout.d_gallery_bottom_sheet,
            container,
            false
        ).run {
            binding = this
            lifecycleOwner = this@GalleryBottomSheetDialog
            setVariable(BR.vm, viewModel)
            this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        with(binding) {
//            rvGallery.setRequestManager(Glide.with(this@GalleryBottomSheetDialog))
//            rvGallery.addItemDecoration(GridDividerItemDecoration(3.dp))
//            rvGallery.setLifecycle(this@GalleryBottomSheetDialog)


//            cvCrop.setOnClickListener {
//                if (it.isSelected) {
//                    edit.centerCrop()
//                    it.isSelected = false
//                } else {
//                    edit.fitCenter()
//                    it.isSelected = true
//                }
//            }

            tvSelectedAlbum.setOnClickListener {
                showSelectAlbumBottomSheet()
            }
        }

        with(viewModel) {

//            startViewHolderClickEvent.observe(viewLifecycleOwner) {
//                binding.rvGallery.requestViewHolderClick(it)
//            }

            startSnackBarEvent.observe(viewLifecycleOwner) {
                androidx.appcompat.app.AlertDialog.Builder(view.context)
                    .setMessage(it)
                    .show()
            }

            startDismissEvent.observe(viewLifecycleOwner) {
                dismiss()
            }

            startSendEditImageBitmap.observe(viewLifecycleOwner) {
                showCaptureDialog(it)
            }

            dataList.observe(viewLifecycleOwner) {
                adapter.submitList(it)
            }

            start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearDisposable()
    }

    fun simpleShow(fm: FragmentManager) {
        super.show(fm, "GalleryBottomSheetDialog")
    }

    /**
     * BottomSheet Device 비율에 맞게 높이값 조정 하는 함수
     */
    private fun setupRatio(bottomSheetDialog: BottomSheetDialog) {
        val bottomSheet =
            bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as View
        val behavior = BottomSheetBehavior.from(bottomSheet)
        val layoutParams = bottomSheet.layoutParams
        var realHeight = getDeviceHeight()
        if (isShowNavigationBar()) {
            realHeight -= getNavigationBarHeight()
        }
        realHeight -= getStatusBarHeight()

        layoutParams.height = realHeight
        bottomSheet.layoutParams = layoutParams
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
        behavior.isDraggable = false
    }

    private fun getDeviceHeight(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.height()
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            displayMetrics.heightPixels
        }
    }

    private fun isShowNavigationBar(): Boolean {
        val id = resources.getIdentifier("config_showNavigationBar", "bool", "android")
        return try {
            resources.getBoolean(id)
        } catch (ex: Resources.NotFoundException) {
            true
        }
    }

    private fun getNavigationBarHeight(): Int {
        val id: Int = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (id > 0) {
            resources.getDimensionPixelSize(id)
        } else 0
    }

    private fun getStatusBarHeight(): Int {
        val id: Int = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) {
            resources.getDimensionPixelSize(id)
        } else 0
    }

    private fun showSelectAlbumBottomSheet() {
        SelectAlbumBottomSheetDialog()
            .setFilterList(viewModel.filterList)
            .setListener(this)
            .simpleShow(childFragmentManager)
    }

    override fun onSelectedFilter(data: GalleryFilterData) {
        Timber.d("onSelectedFilter $data")
        viewModel.onSelectedFilter(data)
    }

    private fun showCaptureDialog(list: List<Bitmap>) {
        CaptureBottomSheetFragment()
            .setBitmapList(list)
            .simpleShow(childFragmentManager)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearDisposable()
    }

    private fun initRecyclerView() {
        binding.rvContents.adapter = adapter
        binding.rvContents.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (viewModel.pageModel.isLast || viewModel.pageModel.isLoading || binding.rvContents.adapter == null) {
                    return
                } else {
                    val itemCount = recyclerView.adapter?.itemCount ?: 0
                    var pos = 0
                    // GridLayoutManager 은 LinearLayoutManager 로직과 동일하게 처리 함
                    when (val lm = recyclerView.layoutManager) {
                        is LinearLayoutManager -> pos = lm.findLastVisibleItemPosition()
                    }
                    // 현재 포지션이 중간 이상 넘어간 경우 페이징 처리
                    val updatePosition = itemCount - pos / 2
                    if (pos >= updatePosition) {
                        viewModel.onLoadPage()
                    }
                }
            }
        })
    }

    data class GalleryExamplePagingModel(
        var isLoading: Boolean = true,
        var isLast: Boolean = false
    )

    data class GalleryExampleItem(
        val imagePath: String,
        var isSelected: Boolean = false,
        var selectedNum: String = "1"
    ) {
        var thumbnailBitmap: Bitmap? = null
    }

    inner class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {

        private val dataList: MutableList<GalleryExampleItem> by lazy { mutableListOf() }

        inner class GalleryDiffUtil(
            private val newList: List<GalleryExampleItem>,
            private val oldList: List<GalleryExampleItem>
        ) : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return oldList.size
            }

            override fun getNewListSize(): Int {
                return newList.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = oldList[oldItemPosition]
                val newItem = newList[newItemPosition]
                return oldItem.imagePath == newItem.imagePath
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = oldList[oldItemPosition]
                val newItem = newList[newItemPosition]
                return oldItem.imagePath == newItem.imagePath
            }
        }

        fun submitList(newList: List<GalleryExampleItem>?) {
            if (newList == null) return

            val diffResult = DiffUtil.calculateDiff(GalleryDiffUtil(newList, dataList))
            dataList.clear()
            dataList.addAll(newList)
            diffResult.dispatchUpdatesTo(this)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(parent)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.onBindView(dataList[position])
        }

        override fun getItemCount(): Int {
            return dataList.size
        }

        inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.vh_example_gallery, parent, false
            )
        ) {
            private val ivThumb: AppCompatImageView by lazy { itemView.findViewById(com.gallery.ui.R.id.ivThumb) }
            private val vSelected: View by lazy { itemView.findViewById(com.gallery.ui.R.id.vSelected) }
            private val clSelectedNumber: ConstraintLayout by lazy { itemView.findViewById(com.gallery.ui.R.id.clSelectedNumber) }
            private val vSelectedNum: View by lazy { itemView.findViewById(com.gallery.ui.R.id.vSelectedNum) }
            private val tvSelectNum: AppCompatTextView by lazy { itemView.findViewById(com.gallery.ui.R.id.tvSelectNum) }
            private val placeHolder: ColorDrawable by lazy {
                ColorDrawable(
                    Color.argb(
                        50,
                        60,
                        63,
                        65
                    )
                )
            }

            private var resizeWidth = 300
            private var model: GalleryExampleItem? = null
            private val requestManager: RequestManager by lazy { Glide.with(this@GalleryBottomSheetDialog) }

            init {

            }

            fun onBindView(item: GalleryExampleItem) {
                model = item
                Timber.d("썸네일 ${item.thumbnailBitmap?.byteCount}")
                if (item.thumbnailBitmap != null) {
                    ivThumb.setImageBitmap(item.thumbnailBitmap)
                } else {
                    requestManager.load(item.imagePath)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .override(resizeWidth, resizeWidth)
                        .placeholder(placeHolder)
                        .into(ivThumb)
                }
            }
        }
    }
}
