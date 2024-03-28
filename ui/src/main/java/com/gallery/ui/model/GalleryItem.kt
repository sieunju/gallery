package com.gallery.ui.model

/**
 * Description : Gallery RecyclerView Item Data Model
 *
 * Created by juhongmin on 2022/11/23
 */
@Deprecated("곧 삭제할 예정입니다.")
data class GalleryItem(
    val imagePath: String,
    var isSelected: Boolean = false,
    var selectedNum: String = "1",
    val bucketName: String,
    val rotation: Int = 0
) : BaseGalleryItem()
