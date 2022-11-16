package com.gallery.edit

import android.content.res.Resources
import android.graphics.Color
import android.os.Parcel
import android.os.Parcelable
import android.util.TypedValue
import androidx.annotation.ColorInt
import com.gallery.edit.enums.CropCornerShape
import com.gallery.edit.enums.CropShape
import com.gallery.edit.enums.Guidelines
import com.gallery.edit.enums.ScaleType
import com.gallery.edit.internal.dp


/**
 * All the possible options that can be set to customize crop image.<br></br>
 * Initialized with default values.
 */
open class CropImageOptions : Parcelable {

    /** The shape of the cropping window.  */
    @JvmField
    var cropShape: CropShape

    /**
     * The shape of cropper corners
     */
    @JvmField
    var cornerShape: CropCornerShape

    /**
     * The radius of the circular crop corner
     */
    @JvmField
    var cropCornerRadius: Float

    /**
     * An edge of the crop window will snap to the corresponding edge of a specified bounding box when
     * the crop window edge is less than or equal to this distance (in pixels) away from the bounding
     * box edge. (in pixels)
     */
    @JvmField
    var snapRadius: Float

    /**
     * The radius of the touchable area around the handle. (in pixels)<br></br>
     * We are basing this value off of the recommended 48dp Rhythm.<br></br>
     * See: http://developer.android.com/design/style/metrics-grids.html#48dp-rhythm
     */
    @JvmField
    var touchRadius: Float

    /** whether the guidelines should be on, off, or only showing when resizing.  */
    @JvmField
    var guidelines: Guidelines

    /** The initial scale type of the image in the crop image view  */
    @JvmField
    var scaleType: ScaleType

    /**
     * if to show crop overlay UI what contains the crop window UI surrounded by background over the
     * cropping image.<br></br>
     * default: true, may disable for animation or frame transition.
     */
    @JvmField
    var showCropOverlay: Boolean

    /**
     * if auto-zoom functionality is enabled.<br></br>
     * default: true.
     */
    @JvmField
    var autoZoomEnabled: Boolean

    /** if multi-touch should be enabled on the crop box default: false  */
    @JvmField
    var multiTouchEnabled: Boolean

    /** if the crop window can be moved by dragging the center; default: true  */
    @JvmField
    var centerMoveEnabled: Boolean

    /** The max zoom allowed during cropping.  */
    @JvmField
    var maxZoom: Int

    /**
     * The initial crop window padding from image borders in percentage of the cropping image
     * dimensions.
     */
    @JvmField
    var initialCropWindowPaddingRatio: Float

    /** whether the width to height aspect ratio should be maintained or free to change.  */
    @JvmField
    var fixAspectRatio: Boolean

    /** the X value of the aspect ratio.  */
    @JvmField
    var aspectRatioX: Int

    /** the Y value of the aspect ratio.  */
    @JvmField
    var aspectRatioY: Int

    /** the thickness of the guidelines lines in pixels. (in pixels)  */
    @JvmField
    var borderLineThickness: Float

    /** the color of the guidelines lines  */
    @JvmField
    @ColorInt
    var borderLineColor: Int

    /** thickness of the corner line. (in pixels)  */
    @JvmField
    var borderCornerThickness: Float

    /** the offset of corner line from crop window border. (in pixels)  */
    @JvmField
    var borderCornerOffset: Float

    /** the length of the corner line away from the corner. (in pixels)  */
    @JvmField
    var borderCornerLength: Float

    /** the color of the corner line  */
    @JvmField
    @ColorInt
    var borderCornerColor: Int

    /**
     * The fill color of circle corner
     */
    @JvmField
    var circleCornerFillColorHexValue: Int

    /** the thickness of the guidelines lines. (in pixels)  */
    @JvmField
    var guidelinesThickness: Float

    /** the color of the guidelines lines  */
    @JvmField
    @ColorInt
    var guidelinesColor: Int

    /**
     * the color of the overlay background around the crop window cover the image parts not in the
     * crop window.
     */
    @JvmField
    @ColorInt
    var backgroundColor: Int

    /** the min width the crop window is allowed to be. (in pixels)  */
    @JvmField
    var minCropWindowWidth: Int

    /** the min height the crop window is allowed to be. (in pixels)  */
    @JvmField
    var minCropWindowHeight: Int

    /**
     * the min width the resulting cropping image is allowed to be, affects the cropping window
     * limits. (in pixels)
     */
    @JvmField
    var minCropResultWidth: Int

    /**
     * the min height the resulting cropping image is allowed to be, affects the cropping window
     * limits. (in pixels)
     */
    @JvmField
    var minCropResultHeight: Int

    /**
     * the max width the resulting cropping image is allowed to be, affects the cropping window
     * limits. (in pixels)
     */
    @JvmField
    var maxCropResultWidth: Int

    /**
     * the max height the resulting cropping image is allowed to be, affects the cropping window
     * limits. (in pixels)
     */
    @JvmField
    var maxCropResultHeight: Int

    /** Init options with defaults.  */
    constructor() {
        val dm = Resources.getSystem().displayMetrics
        cropShape = CropShape.RECTANGLE
        cornerShape = CropCornerShape.RECTANGLE
        circleCornerFillColorHexValue = Color.WHITE
        cropCornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, dm)
        snapRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, dm)
        touchRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, dm)
        guidelines = Guidelines.ON_TOUCH
        scaleType = ScaleType.FIT_CENTER
        showCropOverlay = true
        autoZoomEnabled = true
        multiTouchEnabled = false
        centerMoveEnabled = true
        maxZoom = 4
        initialCropWindowPaddingRatio = 0.1f
        fixAspectRatio = false
        aspectRatioX = 1
        aspectRatioY = 1
        borderLineThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, dm)
        borderLineColor = Color.argb(170, 255, 255, 255)
        borderCornerThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, dm)
        borderCornerOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, dm)
        borderCornerLength = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14f, dm)
        borderCornerColor = Color.WHITE
        guidelinesThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, dm)
        guidelinesColor = Color.argb(170, 255, 255, 255)
        backgroundColor = Color.argb(119, 0, 0, 0)
        minCropWindowWidth = 42.dp
        minCropWindowHeight = 42.dp
        minCropResultWidth = 40
        minCropResultHeight = 40
        maxCropResultWidth = 99999
        maxCropResultHeight = 99999
    }

    /** Create object from parcel.  */
    protected constructor(parcel: Parcel) {
        cropShape = CropShape.values()[parcel.readInt()]
        cornerShape = CropCornerShape.values()[parcel.readInt()]
        cropCornerRadius = parcel.readFloat()
        snapRadius = parcel.readFloat()
        touchRadius = parcel.readFloat()
        guidelines = Guidelines.values()[parcel.readInt()]
        scaleType = ScaleType.values()[parcel.readInt()]
        showCropOverlay = parcel.readByte().toInt() != 0
        autoZoomEnabled = parcel.readByte().toInt() != 0
        multiTouchEnabled = parcel.readByte().toInt() != 0
        centerMoveEnabled = parcel.readByte().toInt() != 0
        maxZoom = parcel.readInt()
        initialCropWindowPaddingRatio = parcel.readFloat()
        fixAspectRatio = parcel.readByte().toInt() != 0
        aspectRatioX = parcel.readInt()
        aspectRatioY = parcel.readInt()
        borderLineThickness = parcel.readFloat()
        borderLineColor = parcel.readInt()
        borderCornerThickness = parcel.readFloat()
        borderCornerOffset = parcel.readFloat()
        borderCornerLength = parcel.readFloat()
        borderCornerColor = parcel.readInt()
        circleCornerFillColorHexValue = parcel.readInt()
        guidelinesThickness = parcel.readFloat()
        guidelinesColor = parcel.readInt()
        backgroundColor = parcel.readInt()
        minCropWindowWidth = parcel.readInt()
        minCropWindowHeight = parcel.readInt()
        minCropResultWidth = parcel.readInt()
        minCropResultHeight = parcel.readInt()
        maxCropResultWidth = parcel.readInt()
        maxCropResultHeight = parcel.readInt()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(cropShape.ordinal)
        dest.writeInt(cornerShape.ordinal)
        dest.writeFloat(cropCornerRadius)
        dest.writeFloat(snapRadius)
        dest.writeFloat(touchRadius)
        dest.writeInt(guidelines.ordinal)
        dest.writeInt(scaleType.ordinal)
        dest.writeByte((if (showCropOverlay) 1 else 0).toByte())
        dest.writeByte((if (autoZoomEnabled) 1 else 0).toByte())
        dest.writeByte((if (multiTouchEnabled) 1 else 0).toByte())
        dest.writeByte((if (centerMoveEnabled) 1 else 0).toByte())
        dest.writeInt(maxZoom)
        dest.writeFloat(initialCropWindowPaddingRatio)
        dest.writeByte((if (fixAspectRatio) 1 else 0).toByte())
        dest.writeInt(aspectRatioX)
        dest.writeInt(aspectRatioY)
        dest.writeFloat(borderLineThickness)
        dest.writeInt(borderLineColor)
        dest.writeFloat(borderCornerThickness)
        dest.writeFloat(borderCornerOffset)
        dest.writeFloat(borderCornerLength)
        dest.writeInt(borderCornerColor)
        dest.writeInt(circleCornerFillColorHexValue)
        dest.writeFloat(guidelinesThickness)
        dest.writeInt(guidelinesColor)
        dest.writeInt(backgroundColor)
        dest.writeInt(minCropWindowWidth)
        dest.writeInt(minCropWindowHeight)
        dest.writeInt(minCropResultWidth)
        dest.writeInt(minCropResultHeight)
        dest.writeInt(maxCropResultWidth)
        dest.writeInt(maxCropResultHeight)
    }

    override fun describeContents(): Int {
        return 0
    }

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

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<CropImageOptions?> =
            object : Parcelable.Creator<CropImageOptions?> {
                override fun createFromParcel(parcel: Parcel): CropImageOptions? {
                    return CropImageOptions(parcel)
                }

                override fun newArray(size: Int): Array<CropImageOptions?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
