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
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.gallery.core.model.GalleryFilterData
import com.gallery.example.BR
import com.gallery.example.R
import com.gallery.example.databinding.DGalleryBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hmju.permissions.extension.dp
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
        with(binding) {
            rvGallery.setRequestManager(Glide.with(this@GalleryBottomSheetDialog))
            rvGallery.addItemDecoration(GridDividerItemDecoration(3.dp))
            rvGallery.setLifecycle(this@GalleryBottomSheetDialog)

            cvCrop.setOnClickListener {
                if (it.isSelected) {
                    edit.centerCrop()
                    it.isSelected = false
                } else {
                    edit.fitCenter()
                    it.isSelected = true
                }
            }

            tvSelectedAlbum.setOnClickListener {
                showSelectAlbumBottomSheet()
            }

            root.post {
                Timber.d("RootHeight ${root.height}")
            }
        }

        with(viewModel) {

            startViewHolderClickEvent.observe(viewLifecycleOwner) {
                binding.rvGallery.requestViewHolderClick(it)
            }

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
        layoutParams.height = getDeviceHeight()
            .minus(getNavigationBarHeight())
            .minus(getStatusBarHeight())
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
}
