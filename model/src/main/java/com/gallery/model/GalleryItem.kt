package com.gallery.model

/**
 * Description : Gallery RecyclerView Item Data Model
 *
 * Created by juhongmin on 2022/11/23
 */
data class GalleryItem(
    val id: Long,
    val imagePath: String
) : BaseGalleryItem() {

}
