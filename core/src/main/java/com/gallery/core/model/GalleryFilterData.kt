package com.gallery.core.model

/**
 * Description : 갤러리 디렉토리 데이터 모델
 *
 * Created by juhongmin on 2022/09/13
 */
data class GalleryFilterData(
    val bucketId: String,
    val bucketName: String,
    val photoUri: String,
    val count: Int
) {
    override fun equals(other: Any?): Boolean {
        return if (other is GalleryFilterData) {
            other.bucketId == bucketId
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        var result = bucketId.hashCode()
        result = 31 * result + bucketName.hashCode()
        result = 31 * result + photoUri.hashCode()
        result = 31 * result + count
        return result
    }
}