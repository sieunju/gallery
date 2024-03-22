package com.gallery.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Description : PhotoPicker BottomSheet
 *
 * Created by juhongmin on 3/21/24
 */
class PhotoPickerBottomSheet : BottomSheetDialogFragment() {

    private var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.d_photo_picker, container)
        return rootView!!
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rootView = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.setOnShowListener { onShow(it) }
    }

    private fun onShow(dialogInterface: DialogInterface) {
        if (dialogInterface !is BottomSheetDialog) return

    }

    /**
     * BottomSheet FullHeight
     */
    private fun setFullHeightBottomSheet(
        bottomSheet: BottomSheetDialog
    ): BottomSheetBehavior<View>? {
        return try {
            val view = bottomSheet.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            ) as View
            view.updateLayoutParams {
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
            BottomSheetBehavior.from(view)
        } catch (ex: Exception) {
            null
        }
    }

    private fun getBehavior(
        bottomSheet: BottomSheetDialog
    ): BottomSheetBehavior<View>? {
        return try {
            val view = bottomSheet.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            ) as View
            BottomSheetBehavior.from(view)
        } catch (ex: Exception) {
            null
        }
    }
}
