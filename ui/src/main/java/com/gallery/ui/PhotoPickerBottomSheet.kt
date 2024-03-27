package com.gallery.ui

import android.content.DialogInterface
import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
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
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.gallery.core.Factory
import com.gallery.core.GalleryProvider
import com.gallery.core.model.GalleryData
import com.gallery.core.model.GalleryFilterData
import com.gallery.core.model.GalleryQueryParameter
import com.gallery.ui.internal.GridItemDecoration
import com.gallery.ui.internal.PhotoPickerAdapter
import com.gallery.ui.internal.PhotoPickerImageLoader
import com.gallery.ui.internal.dp
import com.gallery.ui.internal.getDeviceWidth
import com.gallery.ui.model.PhotoPicker
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Description : PhotoPicker BottomSheet
 *
 * Created by juhongmin on 3/21/24
 */
class PhotoPickerBottomSheet : BottomSheetDialogFragment(), PhotoPickerAdapter.Listener {

    // [s] Core
    private val coreProvider: GalleryProvider by lazy { Factory.create(requireContext()) }
    private val directoryList: MutableList<GalleryFilterData> by lazy { mutableListOf() }
    private var photoCursor: Cursor? = null
    private val photoQueryParams: GalleryQueryParameter by lazy {
        GalleryQueryParameter().apply {
            addColumns(MediaColumns.DATE_ADDED)
        }
    }
    private var videoCursor: Cursor? = null
    private val videoQueryParams: GalleryQueryParameter by lazy {
        GalleryQueryParameter(MediaStore.Video.Media.EXTERNAL_CONTENT_URI).apply {
            addColumns(MediaColumns.DURATION)
            addColumns(MediaColumns.DATE_ADDED)
        }
    }
    private val dataList: MutableList<PhotoPicker> by lazy { mutableListOf() }
    private var isLoading: Boolean = false
    private val isAllLast: Boolean
        get() = photoQueryParams.isLast && videoQueryParams.isLast
    // [e] Core

    private val photoAdapter: PhotoPickerAdapter by lazy { PhotoPickerAdapter(this, coreProvider) }

    // [s] View
    private val overrideSize: Int by lazy { requireContext().getDeviceWidth() / 3 }
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
        photoCursor = coreProvider.fetchCursor(photoQueryParams)
        videoCursor = coreProvider.fetchCursor(videoQueryParams)
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
        PhotoPickerImageLoader.setCoreProvider(coreProvider)
        initView(view)
        initData()
        dialog?.setOnShowListener { onShow(it) }
        dialog?.setOnDismissListener { dismiss() }
    }

    override fun dismiss() {
        cancelListener?.callback()
        super.dismiss()
    }

    override fun asyncSaveCache(item: PhotoPicker) {
        if (item is PhotoPicker.Camera) return
        lifecycleScope.launch {
            if (item is PhotoPicker.Photo) {
                PhotoPickerImageLoader.savePhotoThumbnail(item.id, item.imagePath, overrideSize)
            } else if (item is PhotoPicker.Video) {
                PhotoPickerImageLoader.saveVideoThumbnail(item.id, item.imagePath, overrideSize)
            }
        }
    }

    private fun initData() {
        // TODO Permissions Check
        isLoading = true
        val directoryJob = flow { emit(reqDirectories()) }
        val galleryJob = flow { emit(reqGalleryList()) }
        directoryJob.combine(galleryJob) { directoryList, galleryList ->
            handleInitSuccess(directoryList, galleryList)
        }.launchIn(lifecycleScope)
    }

    /**
     * Handle Init Data Response Success
     *
     * @param directoryList Directory List
     * @param photoList PhotoList
     */
    private fun handleInitSuccess(
        directoryList: List<GalleryFilterData>,
        photoList: List<PhotoPicker>
    ) {
        this.directoryList.clear()
        this.directoryList.addAll(directoryList)
        this.dataList.clear()
        this.dataList.add(PhotoPicker.Camera)
        this.dataList.addAll(photoList)

        directoryList.getOrNull(0)?.let {
            tvSelectFilter?.text = it.bucketName
        }
        photoAdapter.submitList(this.dataList)
        isLoading = false
    }

    private suspend fun reqGalleryList(): List<PhotoPicker> {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                val list = mutableListOf<PhotoPicker>()
                list.addAll(reqPhotoList(photoCursor, photoQueryParams))
                list.addAll(reqVideoList(videoCursor, videoQueryParams))
                list.sortByDescending { item ->
                    when (item) {
                        is PhotoPicker.Photo -> item.dateTaken
                        is PhotoPicker.Video -> item.dateTaken
                        is PhotoPicker.Camera -> Int.MAX_VALUE
                    }
                }
                list
            } catch (ex: Exception) {
                listOf()
            }
        }
    }

    /**
     * Request PhotoList
     */
    private suspend fun reqPhotoList(
        cursor: Cursor?,
        params: GalleryQueryParameter
    ): List<PhotoPicker> {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                if (cursor == null) throw NullPointerException("Cursor is Null")
                coreProvider.fetchList(cursor, params).map { it.toUi() }
            } catch (ex: Exception) {
                listOf()
            }
        }
    }

    private suspend fun reqVideoList(
        cursor: Cursor?,
        params: GalleryQueryParameter
    ): List<PhotoPicker> {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                if (cursor == null) throw NullPointerException("Cursor is Null")
                coreProvider.fetchList(cursor, params).map { it.toUi() }
            } catch (ex: Exception) {
                listOf()
            }
        }
    }

    private fun GalleryData.toUi(): PhotoPicker {
        return if (getField<Int>(MediaColumns.DURATION) == null) {
            PhotoPicker.Photo(this)
        } else {
            PhotoPicker.Video(this)
        }
    }

    private suspend fun reqDirectories(): List<GalleryFilterData> {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                coreProvider.fetchDirectories()
            } catch (ex: Exception) {
                listOf()
            }
        }

    }

    private fun onLoadPage() {
        lifecycleScope.launch {
            isLoading = true
            dataList.addAll(reqGalleryList())
            photoAdapter.submitList(dataList)
            isLoading = false
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
            dismiss()
        }
        view.findViewById<LinearLayoutCompat>(R.id.llSubmit).setOnClickListener {
            cancelListener = null
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
                    if (isAllLast || isLoading) return
                    val itemCount = recyclerView.adapter?.itemCount ?: 0
                    var pos = 0
                    when (val lm = recyclerView.layoutManager) {
                        is LinearLayoutManager -> pos = lm.findLastVisibleItemPosition()
                    }
                    // 현재 포지션이 중간 이상 넘어간 경우 페이징 처리
                    if (itemCount.minus(1) <= pos) {
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
