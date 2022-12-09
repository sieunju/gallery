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
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.gallery.example.BR
import com.gallery.example.R
import com.gallery.example.databinding.DGalleryBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.hmju.permissions.extension.dp
import dagger.hilt.android.AndroidEntryPoint

/**
 * Description :
 *
 * Created by juhongmin on 2022/12/09
 */
@AndroidEntryPoint
internal class GalleryBottomSheetDialog : BottomSheetDialogFragment() {

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

//    override fun onStart() {
//        super.onStart()
//        if (dialog is BottomSheetDialog) {
//            (dialog as BottomSheetDialog).runCatching {
//                behavior.state = BottomSheetBehavior.STATE_EXPANDED
//                behavior.skipCollapsed = true
//            }
//        }
//    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            rvGallery.setRequestManager(Glide.with(this@GalleryBottomSheetDialog))
            rvGallery.addItemDecoration(GridDividerItemDecoration(2.dp))
            rvGallery.setLifecycle(this@GalleryBottomSheetDialog)
        }

        with(viewModel) {

            startViewHolderClickEvent.observe(viewLifecycleOwner) {
                binding.rvGallery.requestViewHolderClick(it)
            }

            startSnackBarEvent.observe(viewLifecycleOwner) {
                Snackbar.make(view, it, Snackbar.LENGTH_SHORT).show()
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
}
