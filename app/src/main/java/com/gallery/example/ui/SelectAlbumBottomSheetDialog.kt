package com.gallery.example.ui

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.gallery.core.model.GalleryFilterData
import com.gallery.example.R
import com.gallery.example.databinding.DSelectAlbumBottomSheetBinding
import com.gallery.example.databinding.VhSelectionAlbumBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat

/**
 * Description : 앨범 선택 Dialog
 *
 * Created by juhongmin on 2022/12/18
 */
@AndroidEntryPoint
internal class SelectAlbumBottomSheetDialog : BottomSheetDialogFragment() {

    interface Listener {
        fun onSelectedFilter(data: GalleryFilterData)
    }

    private lateinit var binding: DSelectAlbumBottomSheetBinding

    private val windowManager: WindowManager by lazy { requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    private val dataList: MutableList<GalleryFilterData> by lazy { mutableListOf() }
    private val adapter: Adapter by lazy { Adapter() }
    private var listener: Listener? = null

    fun setFilterList(list: List<GalleryFilterData>): SelectAlbumBottomSheetDialog {
        dataList.addAll(list)
        return this
    }

    fun setListener(listener: Listener): SelectAlbumBottomSheetDialog {
        this.listener = listener
        return this
    }

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
        return DataBindingUtil.inflate<DSelectAlbumBottomSheetBinding>(
            inflater,
            R.layout.d_select_album_bottom_sheet,
            container,
            false
        ).run {
            binding = this
            lifecycleOwner = this@SelectAlbumBottomSheetDialog
            this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            rvContents.adapter = adapter
        }
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
        layoutParams.height = (getDeviceHeight() * 0.8F).toInt()
        bottomSheet.layoutParams = layoutParams
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
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

    inner class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(parent)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (dataList.size > position) {
                holder.onBindView(dataList[position])
            }
        }

        override fun getItemCount(): Int {
            return dataList.size
        }

        inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(
                    R.layout.vh_selection_album,
                    parent,
                    false
                )
        ) {

            val binding: VhSelectionAlbumBinding by lazy { DataBindingUtil.bind(itemView)!! }
            private val requestManager: RequestManager by lazy { Glide.with(this@SelectAlbumBottomSheetDialog) }
            private val fmt: DecimalFormat by lazy { DecimalFormat("#,###") }
            private var data: GalleryFilterData? = null

            init {
                itemView.setOnClickListener {
                    data?.runCatching {
                        listener?.onSelectedFilter(this)
                        this@SelectAlbumBottomSheetDialog.dismiss()
                    }
                }
            }

            fun onBindView(data: GalleryFilterData) {
                this.data = data
                binding.runCatching {
                    requestManager.load(data.photoUri)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .into(ivThumb)

                    tvTitle.text = data.bucketName
                    tvCount.text = fmt.format(data.count)
                }
            }
        }
    }
}
