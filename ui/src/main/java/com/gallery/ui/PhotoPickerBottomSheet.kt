package com.gallery.ui

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.content.Intent.ACTION_PICK
import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import com.gallery.ui.internal.GridItemDecoration
import com.gallery.ui.internal.PhotoPickerImageLoader
import com.gallery.ui.internal.adapter.PhotoPickerAdapter
import com.gallery.ui.internal.adapter.SelectedPhotoPickerAdapter
import com.gallery.ui.internal.changeVisible
import com.gallery.ui.internal.core.GalleryParams
import com.gallery.ui.internal.core.GalleryProvider
import com.gallery.ui.internal.dp
import com.gallery.ui.internal.getDeviceWidth
import com.gallery.ui.internal.listener.PhotoPickerBridgeListener
import com.gallery.ui.model.PhotoPicker
import com.gallery.ui.model.PickerAlbum
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    private val coreProvider: GalleryProvider by lazy { GalleryProvider(requireContext()) }
    private val _requestManager: RequestManager by lazy { Glide.with(this) }
    private var selectedAlbum: PickerAlbum? = null
    private val albumList: MutableList<PickerAlbum> by lazy { mutableListOf() }
    private var photoCursor: Cursor? = null
    private val photoQueryParams: GalleryParams by lazy { GalleryParams() }
    private var videoCursor: Cursor? = null
    private val videoQueryParams: GalleryParams by lazy {
        GalleryParams(
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).apply {
            addColumns(MediaColumns.DURATION)
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
    private lateinit var otherGalleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionListener: ActivityResultLauncher<Array<String>>
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
        fun callback(selectedList: List<String>)
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
        photoCursor = coreProvider.retrieveCursor(photoQueryParams)
        videoCursor = coreProvider.retrieveCursor(videoQueryParams)
        otherGalleryLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val uri = result.data?.data
            if (result.resultCode == Activity.RESULT_OK && uri != null) {
                submitListener?.callback(listOf(uri.toString()))
                dismiss()
            }
        }
        permissionListener = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            Timber.d("Result ${result}")
        }
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

    override fun setSelectedGallery(item: PhotoPicker) {
        val findItem = selectedList.find {
            if (it is PhotoPicker.Photo && item is PhotoPicker.Photo) {
                it.id == item.id
            } else if (it is PhotoPicker.Video && item is PhotoPicker.Video) {
                it.id == item.id
            } else {
                false
            }
        } ?: return
        if (findItem is PhotoPicker.Photo && item is PhotoPicker.Photo) {
            item.isSelected = findItem.isSelected
            item.selectedNum = findItem.selectedNum
        } else if (findItem is PhotoPicker.Video && item is PhotoPicker.Video) {
            item.isSelected = findItem.isSelected
            item.selectedNum = findItem.selectedNum
        }
    }

    override fun onSelectedAlbum(album: PickerAlbum) {
        Timber.d("onSelectedAlbum $album")
        when (album) {
            is PickerAlbum.Normal -> {
                photoQueryParams.initParams()
                photoQueryParams.filterId = album.id
                photoCursor = coreProvider.retrieveCursor(photoQueryParams)
                videoQueryParams.initParams()
                videoQueryParams.filterId = album.id
                videoCursor = coreProvider.retrieveCursor(videoQueryParams)
                bindCurrentAlbum(album)
                fetchAlbumPhotos()
            }

            is PickerAlbum.OtherApp -> {
                moveToOtherApp()
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
            layoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
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
            tvSelectCount?.text = selectedList.size.toString()
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

    private fun fetchAllAlbums() {
        lifecycleScope.launch {
            albumList.clear()
            albumList.addAll(reqAlbumList())
            albumList.getOrNull(0)?.let { bindCurrentAlbum(it) }
        }
    }

    private fun fetchAlbumPhotos() {
        lifecycleScope.launch {
            isLoading = true
            dataList.clear()
            dataList.addAll(reqGalleryList())
            photoAdapter.submitList(dataList)
            isLoading = false
        }
    }

    /**
     * init Data Start
     */
    private fun initData() {
        // TODO Permissions Check
        fetchAllAlbums()
        fetchAlbumPhotos()
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
        params: GalleryParams
    ): List<PhotoPicker> {
        // TODO Cursor 에서 가져온다음에 섬네일 가져오는 로직 최적화로 처리할 방안 생각해볼것
        return withContext(Dispatchers.IO) {
            return@withContext try {
                if (cursor == null) throw NullPointerException("Cursor is Null")
                coreProvider.retrieveList(cursor, params).onEach {
                    PhotoPickerImageLoader.saveThumbnail(
                        getRequestManager(),
                        it,
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
        params: GalleryParams
    ): List<PhotoPicker> {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                if (cursor == null) throw NullPointerException("Cursor is Null")
                coreProvider.retrieveList(cursor, params).onEach {
                    PhotoPickerImageLoader.saveThumbnail(
                        getRequestManager(),
                        it,
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
                coreProvider.retrieveDirectories()
                    .map { PickerAlbum.Normal(it) }
                    .plus(PickerAlbum.OtherApp)
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
            val contentUris = selectedList.mapNotNull {
                when (it) {
                    is PhotoPicker.Photo -> it.contentUri
                    is PhotoPicker.Video -> it.contentUri
                    else -> null
                }
            }
            submitListener?.callback(contentUris)
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

    private fun moveToOtherApp() {
        val pickerIntent = Intent(ACTION_GET_CONTENT)
        pickerIntent.type = "image/*"
        Intent.createChooser(Intent(ACTION_PICK).apply {
            type = "image/*"
        }, getString(R.string.txt_other_app_gallery)).apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickerIntent))
        }.also { otherGalleryLauncher.launch(it) }
    }
}
