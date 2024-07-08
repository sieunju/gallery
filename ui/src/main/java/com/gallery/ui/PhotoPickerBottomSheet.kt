package com.gallery.ui

import android.animation.ObjectAnimator
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
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.gallery.core.Factory
import com.gallery.core.GalleryProvider
import com.gallery.core.model.GalleryQueryParameter
import com.gallery.ui.internal.GridItemDecoration
import com.gallery.ui.internal.PhotoPickerAdapter
import com.gallery.ui.internal.PhotoPickerBridgeListener
import com.gallery.ui.internal.PhotoPickerImageLoader
import com.gallery.ui.internal.SelectedPhotoPickerAdapter
import com.gallery.ui.internal.changeVisible
import com.gallery.ui.internal.dp
import com.gallery.ui.internal.getDeviceWidth
import com.gallery.ui.model.PhotoPicker
import com.gallery.ui.model.PhotoPicker.Companion.toUi
import com.gallery.ui.model.PickerAlbum
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
class PhotoPickerBottomSheet : BottomSheetDialogFragment(),
    PhotoPickerBridgeListener,
    SelectionAlbumBottomSheet.Listener {

    // [s] Core
    // CoreModule 의존성을 끊기 위해 ui 모듈 내에서 필요한 부분만 처리하도록 변경 예정
    private val coreProvider: GalleryProvider by lazy { Factory.create(requireContext()) }
    private val _requestManager: RequestManager by lazy { Glide.with(this) }
    private var selectedAlbum: PickerAlbum? = null
    private val albumList: MutableList<PickerAlbum> by lazy { mutableListOf() }
    private var photoCursor: Cursor? = null
    private val photoQueryParams: GalleryQueryParameter by lazy {
        GalleryQueryParameter(
            pageSize = 30
        ).apply {
            addColumns(MediaColumns.DATE_ADDED)
        }
    }
    private var videoCursor: Cursor? = null
    private val videoQueryParams: GalleryQueryParameter by lazy {
        GalleryQueryParameter(
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            pageSize = 30
        ).apply {
            addColumns(MediaColumns.DURATION)
            addColumns(MediaColumns.DATE_ADDED)
        }
    }
    private val dataList: MutableList<PhotoPicker> by lazy { mutableListOf() }
    private val selectedList: MutableList<PhotoPicker> by lazy { mutableListOf() }
    private var isLoading: Boolean = false
    private val isAllLast: Boolean
        get() = photoQueryParams.isLast && videoQueryParams.isLast
    private val photoAdapter: PhotoPickerAdapter by lazy { PhotoPickerAdapter(this) }
    private val selectedAdapter: SelectedPhotoPickerAdapter by lazy {
        SelectedPhotoPickerAdapter(this)
    }
    // [e] Core

    // [s] View
    private val overrideSize: Int by lazy { requireContext().getDeviceWidth() / 3 }
    private var rvContents: RecyclerView? = null
    private var rvSelected: RecyclerView? = null
    private var llSelectedAlbum: LinearLayoutCompat? = null
    private var tvSelectedAlbum: AppCompatTextView? = null
    private var llSubmit: LinearLayoutCompat? = null
    private var tvSelectCount: AppCompatTextView? = null
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

    override fun getRequestManager(): RequestManager {
        return _requestManager
    }

    override fun getCoroutineScope(): CoroutineScope {
        return lifecycleScope
    }

    override fun addPicker(pos: Int, item: PhotoPicker) {
        if (item is PhotoPicker.Photo) {
            item.isSelected = true
        } else if (item is PhotoPicker.Video) {
            item.isSelected = true
        }
        selectedList.add(item)
        selectedAdapter.submitList(selectedList)
        rvSelected?.scrollToPosition(selectedAdapter.itemCount.minus(1))
        reSortNumber()
        bindSelectCount()
        notifySelectionItem(selectedList)
        if (selectedList.size == 1) {
            lifecycleScope.launch {
                delay(200)
                handleSelectedPickerAni(pos)
            }
        }
    }

    override fun removePicker(pos: Int, item: PhotoPicker) {
        if (item is PhotoPicker.Photo) {
            item.isSelected = false
        } else if (item is PhotoPicker.Video) {
            item.isSelected = false
        }
        selectedList.remove(item)
        selectedAdapter.submitList(selectedList)
        reSortNumber()
        bindSelectCount()
        notifySelectionItem(selectedList + listOf(item))
        if (selectedList.isEmpty()) {
            handleSelectedPickerAni(pos)
        }
    }

    override fun onSelectedAlbum(album: PickerAlbum) {
        Timber.d("onSelectedAlbum $album")
        when(album) {
            is PickerAlbum.Normal -> {
                photoQueryParams.initParams()
                videoQueryParams.initParams()
            }
            is PickerAlbum.OtherApp -> {
                // 다른 앱 연결
            }
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
                    val updatePosition = itemCount - pos / 2
                    if (pos >= updatePosition) {
                        onLoadPage()
                    }
//                    if (itemCount.minus(1) <= pos) {
//                        onLoadPage()
//                    }
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
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = selectedAdapter
        }
    }

    /**
     * 선택한 Picker 순서 정렬 하는 함수
     */
    private fun reSortNumber() {
        selectedList.forEachIndexed { index, picker ->
            if (picker is PhotoPicker.Photo) {
                picker.selectedNum = "${index.plus(1)}"
            } else if (picker is PhotoPicker.Video) {
                picker.selectedNum = "${index.plus(1)}"
            }
        }
    }

    private fun bindSelectCount() {
        if (selectedList.isNotEmpty()) {
            llSubmit?.changeVisible(true)
            tvSelectCount?.text = "${selectedList.size}"
        } else {
            llSubmit?.changeVisible(false)
        }
    }

    /**
     * 선택한 아이템 자동 갱신 처리하는 함수
     */
    private fun notifySelectionItem(
        notifyList: List<PhotoPicker>
    ) {
        val lm = rvContents?.layoutManager as? GridLayoutManager ?: return
        var firstPos = lm.findFirstVisibleItemPosition()
        firstPos = 0.coerceAtLeast(firstPos.minus(10))
        var lastPos = lm.findLastVisibleItemPosition()
        lastPos = photoAdapter.itemCount.coerceAtMost(lastPos)
        photoAdapter.notifyItemRangeChanged(firstPos, lastPos, notifyList)
    }

    /**
     * 선택한 UI 노출 및 애니메이션 처리
     */
    private fun handleSelectedPickerAni(clickPos: Int) {
        val rvSelected = rvSelected ?: return
        val rvContents = rvContents ?: return
        // 선택한 Picker 노출
        if (selectedList.isNotEmpty()) {
            val targetPadding = 70.dp
            if (rvSelected.translationY != 0F) {
                ObjectAnimator.ofFloat(
                    rvSelected,
                    View.TRANSLATION_Y,
                    rvSelected.translationY,
                    0F
                ).apply {
                    interpolator = FastOutSlowInInterpolator()
                    addUpdateListener {
                        // -70 ~ 0
                        val value = it.animatedValue as Float
                        val paddingTop = targetPadding.plus(value)
                        rvContents.updatePadding(top = paddingTop.toInt())
                        if (clickPos != -1 && clickPos < 3) {
                            rvContents.scrollToPosition(clickPos)
                        }
                    }
                    duration = 200
                    start()
                }
            }
        } else {
            if (rvSelected.translationY != (-70F).dp) {
                rvSelected.translationY = (-70F).dp
                rvContents.updatePadding(top = 0.dp)
            }
        }
    }

    /**
     * init Data Start
     */
    private fun initData() {
        // TODO Permissions Check
        isLoading = true
        val directoryJob = flow { emit(reqAlbumList()) }
        val galleryJob = flow { emit(reqGalleryList()) }
        directoryJob.combine(galleryJob) { directoryList, galleryList ->
            handleInitSuccess(directoryList, galleryList)
        }.launchIn(lifecycleScope)
    }

    /**
     * Handle Init Data Response Success
     *
     * @param albumList Album List
     * @param photoList PhotoList
     */
    private fun handleInitSuccess(
        albumList: List<PickerAlbum>,
        photoList: List<PhotoPicker>
    ) {
        this.albumList.clear()
        this.albumList.addAll(albumList)
        this.albumList.add(PickerAlbum.OtherApp)
        this.dataList.clear()
        this.dataList.add(PhotoPicker.Camera)
        this.dataList.addAll(photoList)
        photoAdapter.submitList(this.dataList)
        isLoading = false

        albumList.getOrNull(0)?.let { bindCurrentAlbum(it) }
    }

    /**
     * Binding CurrentAlbum
     * @param selectedAlbum Selected Album Data
     */
    private fun bindCurrentAlbum(
        selectedAlbum: PickerAlbum
    ) {
        val llSelectedAlbum = this.llSelectedAlbum ?: return
        val tvSelectedAlbum = this.tvSelectedAlbum ?: return
        this.selectedAlbum = selectedAlbum
        llSelectedAlbum.changeVisible(true)
        tvSelectedAlbum.text = selectedAlbum.getTitle()
    }

    /**
     * 갤러리 리스트 가져오는 함수
     * @return PhotoPicker List (Photo or Video)
     */
    private suspend fun reqGalleryList(): List<PhotoPicker> {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                val list = mutableListOf<PhotoPicker>()
                list.addAll(reqPhotoList(photoCursor, photoQueryParams))
                list.addAll(reqVideoList(videoCursor, videoQueryParams))
                list.sortByDescending { item ->
                    when (item) {
                        is PhotoPicker.Photo -> item.id
                        is PhotoPicker.Video -> item.id
                        is PhotoPicker.Camera -> Long.MAX_VALUE
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
     * @param cursor Photo Cursor
     * @param params Photo QueryParams
     */
    private suspend fun reqPhotoList(
        cursor: Cursor?,
        params: GalleryQueryParameter
    ): List<PhotoPicker> {
        // TODO Cursor 에서 가져온다음에 섬네일 가져오는 로직 최적화로 처리할 방안 생각해볼것
        return withContext(Dispatchers.IO) {
            return@withContext try {
                if (cursor == null) throw NullPointerException("Cursor is Null")
                coreProvider.fetchList(cursor, params).map {
                    PhotoPickerImageLoader.saveThumbnail(
                        getRequestManager(),
                        it.toUi(),
                        overrideSize
                    )
                }
            } catch (ex: Exception) {
                listOf()
            }
        }
    }

    /**
     * Request VideoList
     * @param cursor Video Cursor
     * @param params Video QueryParams
     */
    private suspend fun reqVideoList(
        cursor: Cursor?,
        params: GalleryQueryParameter
    ): List<PhotoPicker> {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                if (cursor == null) throw NullPointerException("Cursor is Null")
                coreProvider.fetchList(cursor, params).map {
                    PhotoPickerImageLoader.saveThumbnail(
                        getRequestManager(),
                        it.toUi(),
                        overrideSize
                    )
                }
            } catch (ex: Exception) {
                listOf()
            }
        }
    }

    /**
     * Request Directory
     */
    private suspend fun reqAlbumList(): List<PickerAlbum> {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                coreProvider.fetchDirectories().map { PickerAlbum.Normal(it) }
            } catch (ex: Exception) {
                listOf()
            }
        }
    }

    /**
     * Load Next Page
     */
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
        llSelectedAlbum = view.findViewById(R.id.llSelectedAlbum)
        tvSelectedAlbum = view.findViewById(R.id.tvSelectedAlbum)
        llSubmit = view.findViewById(R.id.llSubmit)
        tvSelectCount = view.findViewById(R.id.tvSelectCount)
        view.findViewById<AppCompatImageView>(R.id.ivClose).setOnClickListener {
            dismiss()
        }
        view.findViewById<LinearLayoutCompat>(R.id.llSubmit).setOnClickListener {
            cancelListener = null
            submitListener?.callback()
            dismiss()
        }
        view.findViewById<LinearLayoutCompat>(R.id.llSelectedAlbum).setOnClickListener {
            showSelectionAlbum()
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

    private fun showSelectionAlbum() {
        SelectionAlbumBottomSheet()
            .setData(albumList)
            .setSelectedItem(selectedAlbum)
            .setListener(this)
            .simpleShow(childFragmentManager)
    }
}
