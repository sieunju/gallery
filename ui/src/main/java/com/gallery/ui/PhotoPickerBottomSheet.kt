package com.gallery.ui

import android.content.DialogInterface
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gallery.core.Factory
import com.gallery.core.GalleryProvider
import com.gallery.core.model.GalleryQueryParameter
import com.gallery.ui.internal.GridItemDecoration
import com.gallery.ui.internal.PhotoPickerAdapter
import com.gallery.ui.internal.dp
import com.gallery.ui.model.PhotoPicker
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Description : PhotoPicker BottomSheet
 *
 * Created by juhongmin on 3/21/24
 */
class PhotoPickerBottomSheet : BottomSheetDialogFragment() {

    // [s] Core
    private val coreProvider: GalleryProvider by lazy { Factory.create(requireContext()) }
    private var cursor: Cursor? = null
    private val queryParams: GalleryQueryParameter by lazy { GalleryQueryParameter() }
    private val dataList: MutableList<PhotoPicker> by lazy { mutableListOf() }
    private var isLoading: Boolean = false
    // [e] Core

    private val photoAdapter: PhotoPickerAdapter by lazy { PhotoPickerAdapter(this, coreProvider) }

    // [s] View
    private var rvContents: RecyclerView? = null
    private var rvSelected: RecyclerView? = null
    private var tvSelectFilter: AppCompatTextView? = null
    // [e] View

    // [s] Config
    private var maxCount: Int = 3
    private var submitListener: OnSubmitListener? = null
    private var cancelListener: OnCancelListener? = null
    // [e] Config

    fun interface OnSubmitListener {
        fun callback()
    }

    fun interface OnCancelListener {
        fun callback()
    }

    /**
     * Set Submit Listener
     * @param l Listener
     */
    fun setSubmitListener(
        l: OnSubmitListener
    ): PhotoPickerBottomSheet {
        submitListener = l
        return this
    }

    /**
     * Set Cancel Listener
     * @param l Listener
     */
    fun setCancelListener(
        l: OnCancelListener
    ): PhotoPickerBottomSheet {
        cancelListener = l
        return this
    }

    /**
     * Set MaxCount
     * @param count
     */
    fun setMaxCount(count: Int): PhotoPickerBottomSheet {
        maxCount = count
        return this
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.PhotoPickerBottomSheet)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.d_photo_picker, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
        initData()
        dialog?.setOnShowListener { onShow(it) }
        dialog?.setOnDismissListener { cancelListener?.callback() }
    }

    private fun initData() {
        lifecycleScope.launch {
            isLoading = true
            cursor = coreProvider.fetchCursor()
            dataList.add(PhotoPicker.Camera)
            dataList.addAll(getPhotoList(cursor!!))
            photoAdapter.submitList(dataList)
            isLoading = false
        }
    }

    private fun onLoadPage() {
        lifecycleScope.launch {
            isLoading = true
            dataList.addAll(getPhotoList(cursor!!))
            photoAdapter.submitList(dataList)
            isLoading = false
            printMemory()
        }
    }

    private fun printMemory() {
        val maxMem = Runtime.getRuntime().maxMemory() / 1024 / 1024
        val totalMem = Runtime.getRuntime().totalMemory() / 1024 / 1024
        val freeMem = Runtime.getRuntime().freeMemory() / 1024 / 1024
        Timber.d("Used Mem ${totalMem.minus(freeMem)}")
    }

    private suspend fun getPhotoList(
        cursor: Cursor
    ): List<PhotoPicker> {
        return withContext(Dispatchers.IO) {
            return@withContext coreProvider.fetchList(cursor, queryParams)
                .map { PhotoPicker.Photo(it) }
        }
    }

    /**
     * initView
     * @param view Parent View
     */
    private fun initView(
        view: View
    ) {
        initContents(view)
        initSelectedContents(view)
        tvSelectFilter = view.findViewById(R.id.tvSelectFilter)
        view.findViewById<AppCompatImageView>(R.id.ivClose).setOnClickListener {
            cancelListener?.callback()
            dismiss()
        }
        view.findViewById<LinearLayoutCompat>(R.id.llSubmit).setOnClickListener {
            submitListener?.callback()
            dismiss()
        }
    }

    /**
     * init Main Contents
     * @param parentView ParentView
     */
    private fun initContents(
        parentView: View
    ) {
        rvContents = parentView.findViewById<RecyclerView>(R.id.rvContents).apply {
            layoutManager = GridLayoutManager(context, 3)
            addItemDecoration(GridItemDecoration(1.dp))
            adapter = photoAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (queryParams.isLast || isLoading) return
                    val itemCount = recyclerView.adapter?.itemCount ?: 0
                    var pos = 0
                    when (val lm = recyclerView.layoutManager) {
                        is LinearLayoutManager -> pos = lm.findLastVisibleItemPosition()
                    }
                    // 현재 포지션이 중간 이상 넘어간 경우 페이징 처리
                    val updatePosition = itemCount - pos / 2
                    if (pos >= updatePosition) {
                        onLoadPage()
                    }
                }
            })
        }
    }

    /**
     * init Selected Contents
     * @param parentView ParentView
     */
    private fun initSelectedContents(
        parentView: View
    ) {
        rvSelected = parentView.findViewById<RecyclerView>(R.id.rvSelected).apply {

        }
    }

    private fun onShow(dialogInterface: DialogInterface) {
        if (dialogInterface !is BottomSheetDialog) return
        setFullHeightBottomSheet(dialogInterface)
        val behavior = getBehavior(dialogInterface)
        behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        behavior?.skipCollapsed = true
        behavior?.isDraggable = false
    }

    /**
     * BottomSheet Full Height
     * @param bottomSheet BottomSheet
     */
    private fun setFullHeightBottomSheet(
        bottomSheet: BottomSheetDialog
    ) {
        val view = bottomSheet.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        ) as View
        view.updateLayoutParams {
            height = ViewGroup.LayoutParams.MATCH_PARENT
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
