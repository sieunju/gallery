package com.gallery.example.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.gallery.example.BR
import com.gallery.example.R
import com.gallery.example.databinding.FCaptureBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * Description :
 *
 * Created by juhongmin on 2023/01/05
 */
@AndroidEntryPoint
internal class CaptureBottomSheetFragment : BottomSheetDialogFragment() {

    private val viewModel: CaptureBottomSheetFragmentViewModel by viewModels()

    private lateinit var binding: FCaptureBottomSheetBinding

    private val windowManager: WindowManager by lazy { requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    private val adapter: Adapter by lazy { Adapter() }

    private val dataList: MutableList<Bitmap> by lazy { mutableListOf() }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            setupRatio(bottomSheetDialog)
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return DataBindingUtil.inflate<FCaptureBottomSheetBinding>(
            inflater,
            R.layout.f_capture_bottom_sheet,
            container, false
        ).run {
            binding = this
            lifecycleOwner = this@CaptureBottomSheetFragment
            setVariable(BR.vm, viewModel)
            this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvContents.adapter = adapter
        val pagerSnap = PagerSnapHelper()
        pagerSnap.attachToRecyclerView(binding.rvContents)
        adapter.setDataList(dataList)
    }

    fun setBitmapList(list: List<Bitmap>): CaptureBottomSheetFragment {
        dataList.addAll(list)
        return this
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
        val contentsHeight = getDeviceHeight()
            .minus(getNavigationBarHeight())
            .minus(getStatusBarHeight())
        layoutParams.height = (contentsHeight * 0.7F).toInt()
        bottomSheet.layoutParams = layoutParams
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
        behavior.isDraggable = true
    }

    private fun getDeviceHeight(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.height()
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.heightPixels
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

    inner class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {

        private val dataList: MutableList<Bitmap> by lazy { mutableListOf() }

        fun setDataList(newList: List<Bitmap>?) {
            if (newList == null) return
            dataList.clear()
            dataList.addAll(newList)
            notifyItemRangeInserted(0, itemCount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(parent)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            runCatching {
                if (dataList.size > position) {
                    holder.onBindView(dataList[position])
                }
            }
        }

        override fun getItemCount(): Int {
            return dataList.size
        }

        inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.vh_capture, parent, false)
        ) {
            private val ivThumb: AppCompatImageView by lazy { itemView.findViewById(R.id.ivThumb) }

            fun onBindView(bitmap: Bitmap) {
                ivThumb.setImageBitmap(bitmap)
            }
        }
    }
}
