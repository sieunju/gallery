package com.gallery.ui.internal.view

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.gallery.ui.R
import com.gallery.ui.internal.core.GalleryProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

/**
 * Description : Photo Detail Screen
 *
 * Created by juhongmin on 2024. 8. 15.
 */
internal class DetailPickerBottomSheet : BottomSheetDialogFragment() {

    private lateinit var provider: GalleryProvider
    private var imageUrl: String = ""

    // [s] View
    private var ivEdit: FlexibleImageEditView? = null
    // [e] View

    fun setImage(uri: Uri, id: Long) : DetailPickerBottomSheet {

        return this
    }

    fun setImageUrl(url: String): DetailPickerBottomSheet {
        imageUrl = url
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
            behavior.isDraggable = false
            behavior.skipCollapsed = true
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.d_detail_picker, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    private fun initView(
        view: View
    ) {
        ivEdit = view.findViewById<FlexibleImageEditView>(R.id.ivEdit).also {

        }
        lifecycleScope.launch {

        }
    }

    /**
     * BottomSheet Show
     * @param fm FragmentManager
     */
    fun simpleShow(fm: FragmentManager) {
        runCatching {
            // 이미 보여지고 있는 Dialog 인경우 스킵
            if (!isAdded) {
                super.show(fm, "PhotoPickerBottomSheet")
            }
        }
    }
}
