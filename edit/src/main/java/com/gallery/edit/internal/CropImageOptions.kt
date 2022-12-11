package com.gallery.edit.internal

import android.graphics.Color
import android.os.Parcelable
import androidx.annotation.ColorInt
import com.gallery.edit.enums.CropCornerShape
import com.gallery.edit.enums.CropShape
import com.gallery.edit.enums.Guidelines
import com.gallery.edit.enums.ScaleType
import com.gallery.edit.internal.dp
import kotlinx.parcelize.Parcelize


/**
 * All the possible options that can be set to customize crop image.<br></br>
 * Initialized with default values.
 */
@Parcelize
data class CropImageOptions(
    /** The shape of the cropping window.  */
    @JvmField
    var cropShape: CropShape = CropShape.RECTANGLE,
    /**
     * The shape of cropper corners
     */
    @JvmField
    var cornerShape: CropCornerShape = CropCornerShape.RECTANGLE,
    /**
     * The radius of the circular crop corner
     */
    @JvmField
    var cropCornerRadius: Float = 10F.dp,
    /**
     * An edge of the crop window will snap to the corresponding edge of a specified bounding box when
     * the crop window edge is less than or equal to this distance (in pixels) away from the bounding
     * box edge. (in pixels)
     */
    @JvmField
    var snapRadius: Float = 3F.dp,
    /**
     * The radius of the touchable area around the handle. (in pixels)<br></br>
     * We are basing this value off of the recommended 48dp Rhythm.<br></br>
     * See: http://developer.android.com/design/style/metrics-grids.html#48dp-rhythm
     */
    @JvmField
    var touchRadius: Float = 24F.dp,
    /** whether the guidelines should be on, off, or only showing when resizing.  */
    @JvmField
    var guidelines: Guidelines = Guidelines.ON_TOUCH,
    /** The initial scale type of the image in the crop image view  */
    @JvmField
    var scaleType: ScaleType = ScaleType.FIT_CENTER,
    /**
     * if to show crop overlay UI what contains the crop window UI surrounded by background over the
     * cropping image.<br></br>
     * default: true, may disable for animation or frame transition.
     */
    @JvmField
    var showCropOverlay: Boolean = true,
    /**
     * if auto-zoom functionality is enabled.<br></br>
     * default: true.
     */
    @JvmField
    var autoZoomEnabled: Boolean = true,
    /** if multi-touch should be enabled on the crop box default: false  */
    @JvmField
    var multiTouchEnabled: Boolean = false,
    /** if the crop window can be moved by dragging the center; default: true  */
    @JvmField
    var centerMoveEnabled: Boolean = true,
    /** The max zoom allowed during cropping.  */
    @JvmField
    var maxZoom: Int = 4,
    /**
     * The initial crop window padding from image borders in percentage of the cropping image
     * dimensions.
     */
    @JvmField
    var initialCropWindowPaddingRatio: Float = 0.1F,
    /** whether the width to height aspect ratio should be maintained or free to change.  */
    @JvmField
    var fixAspectRatio: Boolean = false,
    /** the X value of the aspect ratio.  */
    @JvmField
    var aspectRatioX: Int = 1,
    /** the Y value of the aspect ratio.  */
    @JvmField
    var aspectRatioY: Int = 1,
    /** the thickness of the guidelines lines in pixels. (in pixels)  */
    @JvmField
    var borderLineThickness: Float = 3F.dp,
    /** the color of the guidelines lines  */
    @JvmField
    @ColorInt
    var borderLineColor: Int = Color.argb(170, 255, 255, 255),
    /** thickness of the corner line. (in pixels)  */
    @JvmField
    var borderCornerThickness: Float = 2F.dp,
    /** the offset of corner line from crop window border. (in pixels)  */
    @JvmField
    var borderCornerOffset: Float = 5F.dp,
    /** the length of the corner line away from the corner. (in pixels)  */
    @JvmField
    var borderCornerLength: Float = 14F.dp,
    /** the color of the corner line  */
    @JvmField
    @ColorInt
    var borderCornerColor: Int = Color.WHITE,
    /**
     * The fill color of circle corner
     */
    @JvmField
    var circleCornerFillColorHexValue: Int = Color.WHITE,
    /** the thickness of the guidelines lines. (in pixels)  */
    @JvmField
    var guidelinesThickness: Float = 1F.dp,
    /** the color of the guidelines lines  */
    @JvmField
    @ColorInt
    var guidelinesColor: Int = Color.argb(170, 255, 255, 255),
    /**
     * the color of the overlay background around the crop window cover the image parts not in the
     * crop window.
     */
    @JvmField
    @ColorInt
    var overlayBackgroundColor: Int = Color.argb(119, 0, 0, 0),
    /** the min width the crop window is allowed to be. (in pixels)  */
    @JvmField
    var minCropWindowWidth: Int = 42.dp,
    /** the min height the crop window is allowed to be. (in pixels)  */
    @JvmField
    var minCropWindowHeight: Int = 42.dp,
    /**
     * the min width the resulting cropping image is allowed to be, affects the cropping window
     * limits. (in pixels)
     */
    @JvmField
    var minCropResultWidth: Int = 40,
    /**
     * the min height the resulting cropping image is allowed to be, affects the cropping window
     * limits. (in pixels)
     */
    @JvmField
    var minCropResultHeight: Int = 40,
    /**
     * the max width the resulting cropping image is allowed to be, affects the cropping window
     * limits. (in pixels)
     */
    @JvmField
    var maxCropResultWidth: Int = 99999,
    /**
     * the max height the resulting cropping image is allowed to be, affects the cropping window
     * limits. (in pixels)
     */
    @JvmField
    var maxCropResultHeight: Int = 99999,
    /**
     * CropImageEditView Background Color
     */
    @JvmField
    @ColorInt
    var backgroundColor: Int = Color.WHITE
) : Parcelable {

    /**
     * Validate all the options are withing valid range.
     *
     * @throws IllegalArgumentException if any of the options is not valid
     */
    fun validate() {
        require(maxZoom >= 0) { "Cannot set max zoom to a number < 1" }
        require(touchRadius >= 0) { "Cannot set touch radius value to a number <= 0 " }
        require(!(initialCropWindowPaddingRatio < 0 || initialCropWindowPaddingRatio >= 0.5)) { "Cannot set initial crop window padding value to a number < 0 or >= 0.5" }
        require(aspectRatioX > 0) { "Cannot set aspect ratio value to a number less than or equal to 0." }
        require(aspectRatioY > 0) { "Cannot set aspect ratio value to a number less than or equal to 0." }
        require(borderLineThickness >= 0) { "Cannot set line thickness value to a number less than 0." }
        require(borderCornerThickness >= 0) { "Cannot set corner thickness value to a number less than 0." }
        require(guidelinesThickness >= 0) { "Cannot set guidelines thickness value to a number less than 0." }
        require(minCropWindowHeight >= 0) { "Cannot set min crop window height value to a number < 0 " }
        require(minCropResultWidth >= 0) { "Cannot set min crop result width value to a number < 0 " }
        require(minCropResultHeight >= 0) { "Cannot set min crop result height value to a number < 0 " }
        require(maxCropResultWidth >= minCropResultWidth) { "Cannot set max crop result width to smaller value than min crop result width" }
        require(maxCropResultHeight >= minCropResultHeight) { "Cannot set max crop result height to smaller value than min crop result height" }
    }
}
