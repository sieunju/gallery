package com.gallery.edit.detector

/**
 * Description: Flexible State Data Class
 * 각 초기값은
 * Scale 1.0F
 * Focus 0F
 * Rotate 0F 추구 회전 기능도 넣을 예정
 * Flip 1F
 * Created by juhongmin on 11/21/21
 */
data class FlexibleStateItem(
    var scale: Float = 1.0F,
    var focusX: Float = 0F,
    var focusY: Float = 0F,
    var rotationDegree: Float = 0F,
    var flipX: Float = 1F,
    var flipY: Float = 1F
) {

    var startScale: Float = -1F // 처음 Scale 값

    val scaleX: Float
        get() = scale * flipX
    val scaleY: Float
        get() = scale * flipY
    var minScale: Float = -1F
    var imgWidth: Int = -1 // 실제 이미지 너비
    var imgHeight: Int = -1 // 실제 이미지 높이

    val currentImgWidth: Float
        get() = if (imgWidth == -1) -1F else imgWidth * scale
    val currentImgHeight: Float
        get() = if (imgHeight == -1) -1F else imgHeight * scale

    /**
     * Reset Item
     */
    fun reset() {
        scale = if (minScale != -1F) {
            minScale
        } else {
            1.0F
        }
        focusX = 0F
        focusY = 0F
        rotationDegree = 0F
        flipX = 1F
        flipY = 1F
        startScale = -1F
    }

    /**
     * 값들만 copy 하고 싶을때 호출하는 함수
     * @param copyItem Copy 하고 싶은 아이템
     */
    fun valueCopy(copyItem: FlexibleStateItem) {
        copyItem.startScale = startScale
        copyItem.minScale = minScale
        copyItem.scale = scale
        copyItem.focusX = focusX
        copyItem.focusY = focusY
        copyItem.rotationDegree = rotationDegree
        copyItem.flipX = flipX
        copyItem.flipY = flipY
		copyItem.imgWidth = imgWidth
		copyItem.imgHeight = imgHeight
    }

	override fun toString(): String {
		val str = StringBuilder("StateItem(")
		str.append("scale=${scale}")
		str.append(" focusX=${focusX}")
		str.append(" focusY=${focusY}")
		str.append(" imgWidth=${imgWidth}")
		str.append(" imgHeight=${imgHeight}")
		str.append(")")
		return str.toString()
	}
}