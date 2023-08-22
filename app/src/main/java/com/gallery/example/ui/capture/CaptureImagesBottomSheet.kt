package com.gallery.example.ui.capture

import android.app.Dialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView.ScaleType
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.gallery.example.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * Description : 이미지들을 선택하고 확인할 바텀 시트
 *
 * Created by juhongmin on 2023/08/20
 */
internal class CaptureImagesBottomSheet : BottomSheetDialogFragment() {

    private lateinit var llContainer: LinearLayoutCompat

    private val images: MutableList<Any> by lazy { mutableListOf() }
    private val requestManager: RequestManager by lazy { Glide.with(this) }

    fun setData(images: List<Any>): CaptureImagesBottomSheet {
        this.images.addAll(images)
        return this
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog
                .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as View
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.d_capture_images, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        llContainer = view.findViewById(R.id.llContainer)
        handleImage(view)
    }

    private fun handleImage(view: View) {
        images.forEach {
            val iv = AppCompatImageView(view.context).apply {
                layoutParams = LinearLayoutCompat.LayoutParams(
                    LinearLayoutCompat.LayoutParams.MATCH_PARENT,
                    LinearLayoutCompat.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 50
                    bottomMargin = 50
                }
                scaleType = ScaleType.FIT_CENTER
                adjustViewBounds = true
            }
            requestManager.load(it)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(iv)
            llContainer.addView(iv)
        }
    }

    fun simpleShow(fm: FragmentManager) {
        super.show(fm, "CaptureImagesBottomSheet")
    }
}