package com.gallery.ui.internal.view

import android.app.Dialog
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.net.toUri
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.gallery.ui.R
import com.gallery.ui.internal.core.GalleryProvider
import com.gallery.ui.internal.getDeviceWidth
import com.gallery.ui.model.PhotoPicker
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Description : Photo Detail Screen
 *
 * Created by juhongmin on 2024. 8. 15.
 */
internal class DetailPickerBottomSheet : BottomSheetDialogFragment() {

    fun interface Listener {
        fun onSelected(
            item: PhotoPicker
        )
    }

    private lateinit var provider: GalleryProvider
    private var data: PhotoPicker? = null
    private val deviceWidth: Int by lazy { requireContext().getDeviceWidth() }
    private var listener: Listener? = null

    // [s] View
    private var ivEdit: FlexibleImageEditView? = null
    // [e] View

    fun setProvider(
        provider: GalleryProvider
    ): DetailPickerBottomSheet {
        this.provider = provider
        return this
    }

    fun setPhoto(
        item: PhotoPicker
    ): DetailPickerBottomSheet {
        data = item
        return this
    }

    fun setListener(l: Listener): DetailPickerBottomSheet {
        listener = l
        return this
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog
                .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as View
            val behavior = BottomSheetBehavior.from(bottomSheet)
            bottomSheet.updateLayoutParams<ViewGroup.LayoutParams> {
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
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
        ivEdit = view.findViewById(R.id.ivEdit)
        view.findViewById<AppCompatImageView>(R.id.ivClose).setOnClickListener { dismiss() }
        view.findViewById<AppCompatTextView>(R.id.tvSelected).setOnClickListener { _ ->
            data?.let {
                listener?.onSelected(it)
                dismiss()
            }
        }
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                getBitmap()?.let { resizeBitmap(it, deviceWidth) }
            }
            Timber.d("Bitmap ${bitmap?.width}")
            if (bitmap == null) {
                dismiss()
                return@launch
            }
            ivEdit?.loadBitmap(bitmap)
        }
    }

    private fun getBitmap(): Bitmap? {
        return try {
            val data = data as? PhotoPicker.Photo ?: return null
            val contentResolver = requireContext().contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, data.contentUri.toUri())
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, data.contentUri.toUri())
            }
        } catch (ex: Exception) {
            null
        }
    }

    fun resizeBitmap(
        image: Bitmap,
        maxWidth: Int
    ): Bitmap {
        val width = image.width
        val height = image.height
        var newWidth = width
        var newHeight = height
        var rate = 0.0F
        if (width > height) {
            if (maxWidth < width) {
                rate = maxWidth / width.toFloat()
                newHeight = (height * rate).toInt()
                newWidth = maxWidth
            }
        } else if (maxWidth < height) {
            rate = maxWidth / height.toFloat()
            newWidth = (width * rate).toInt()
            newHeight = maxWidth
        }

        return Bitmap.createScaledBitmap(image, newWidth, newHeight, true)
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
