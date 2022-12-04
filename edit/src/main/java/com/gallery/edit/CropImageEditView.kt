package com.gallery.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.util.AttributeSet
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.gallery.edit.enums.CropCornerShape
import com.gallery.edit.enums.CropShape
import com.gallery.edit.enums.Guidelines
import com.gallery.edit.enums.ScaleType
import com.gallery.edit.internal.*
import com.gallery.model.CropImageEditModel
import kotlin.math.*

/** Custom view that provides cropping capabilities to an image.  */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class CropImageEditView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs),
    CropOverlayView.CropWindowChangeListener {

    /** Image view widget used to show the image for cropping.  */
    private val imageView: ImageView

    /** Overlay over the image view to show cropping UI.  */
    private val cropOverlayView: CropOverlayView

    /** The matrix used to transform the cropping image in the image view  */
    private val mImageMatrix: Matrix by lazy { Matrix() }

    /** Reusing matrix instance for reverse matrix calculations.  */
    private val mImageInverseMatrix: Matrix by lazy { Matrix() }

    /** Rectangle used in image matrix transformation calculation (reusing rect instance)  */
    private val mImagePoints: FloatArray by lazy { FloatArray(8) }

    /** Rectangle used in image matrix transformation for scale calculation (reusing rect instance)  */
    private val mScaleImagePoints: FloatArray by lazy { FloatArray(8) }

    /** Animation class to smooth animate zoom-in/out  */
    private var mAnimation: CropImageAnimation? = null
    private var originalBitmap: Bitmap? = null

    /** The image rotation value used during loading of the image so we can reset to it  */
    private var mInitialDegreesRotated = 0

    /** How much the image is rotated from original clockwise  */
    private var mDegreesRotated = 0

    /** if the image flipped horizontally  */
    private var mFlipHorizontally: Boolean = false

    /** if the image flipped vertically  */
    private var mFlipVertically: Boolean = false
    private var mLayoutWidth = 0
    private var mLayoutHeight = 0

    /** The initial scale type of the image in the crop image view  */
    private var mScaleType: ScaleType

    /**
     * if to show crop overlay UI what contains the crop window UI surrounded by background over the
     * cropping image.<br></br>
     * default: true, may disable for animation or frame transition.
     */
    private var mShowCropOverlay = true

    /**
     * if auto-zoom functionality is enabled.<br></br>
     * default: true.
     */
    private var mAutoZoomEnabled = true

    /** The max zoom allowed during cropping  */
    private var mMaxZoom: Int

    /** callback to be invoked when crop overlay is released.  */
    private var mOnCropOverlayReleasedListener: OnSetCropOverlayReleasedListener? = null

    /** callback to be invoked when crop overlay is moved.  */
    private var mOnSetCropOverlayMovedListener: OnSetCropOverlayMovedListener? = null

    /** callback to be invoked when crop window is changed.  */
    private var mOnSetCropWindowChangeListener: OnSetCropWindowChangeListener? = null

    /** The sample size the image was loaded by if was loaded by URI  */
    private var loadedSampleSize = 1

    /** The current zoom level to to scale the cropping image  */
    private var mZoom = 1f

    /** The X offset that the cropping image was translated after zooming  */
    private var mZoomOffsetX = 0f

    /** The Y offset that the cropping image was translated after zooming  */
    private var mZoomOffsetY = 0f

    /** Used to restore the cropping windows rectangle after state restore  */
    private var mRestoreCropWindowRect: RectF? = null

    /** Used to restore image rotation after state restore  */
    private var mRestoreDegreesRotated = 0

    /**
     * Used to detect size change to handle auto-zoom using [.handleCropWindowChanged] in [.layout].
     */
    private var mSizeChanged = false

    /** Get / set the scale type of the image in the crop view.  */
    var scaleType: ScaleType
        get() = mScaleType
        set(scaleType) {
            if (scaleType != mScaleType) {
                mScaleType = scaleType
                mZoom = 1f
                mZoomOffsetY = 0f
                mZoomOffsetX = mZoomOffsetY
                cropOverlayView.resetCropOverlayView()
                requestLayout()
            }
        }

    /**
     * The shape of the cropping area - rectangle/circular.<br></br>
     * To set square/circle crop shape set aspect ratio to 1:1.
     *
     * When setting RECTANGLE_VERTICAL_ONLY or RECTANGLE_HORIZONTAL_ONLY you may also want to
     * use a free aspect ratio (to allow the crop window to change in the desired dimension
     * whilst staying the same in the other dimension) and have the crop window start covering the
     * entirety of the image (so that the crop window has no space to move in the other dimension).
     * These can be done with
     * [CropImageEditView.setFixedAspectRatio] } (with argument `false`) and
     * [CropImageEditView.wholeImageRect] } (with argument `cropImageView.getWholeImageRect()`).
     */
    var cropShape: CropShape
        get() = cropOverlayView.cropShape
        set(cropShape) {
            cropOverlayView.setCropShape(cropShape)
        }

    /**
     * The shape of the crop corner in the crop overlay (Rectangular / Circular)
     */
    var cornerShape: CropCornerShape
        get() = cropOverlayView.cornerShape
        set(cornerShape) {
            cropOverlayView.setCropCornerShape(cornerShape)
        }

    /** Set auto-zoom functionality to enabled/disabled.  */
    var isAutoZoomEnabled: Boolean
        get() = mAutoZoomEnabled
        set(autoZoomEnabled) {
            if (mAutoZoomEnabled != autoZoomEnabled) {
                mAutoZoomEnabled = autoZoomEnabled
                handleCropWindowChanged(inProgress = false, animate = false)
                cropOverlayView.invalidate()
            }
        }

    /** The max zoom allowed during cropping.  */
    var maxZoom: Int
        get() = mMaxZoom
        set(maxZoom) {
            if (mMaxZoom != maxZoom && maxZoom > 0) {
                mMaxZoom = maxZoom
                handleCropWindowChanged(inProgress = false, animate = false)
                cropOverlayView.invalidate()
            }
        }

    /**
     * Set / Get the amount of degrees (between 0 and 360) the cropping image is rotated clockwise.<br></br>
     * 0, 90, 180, 270, 360
     */
    var rotatedDegrees: Int
        get() = mDegreesRotated
        set(value) {
            if (value != 0 && value != 90 && value != 180 && value != 270 && value != 360) {
                throw IllegalArgumentException("RotateDegrees 0, 90, 180, 270, 360")
            }
            if (mDegreesRotated != value) {
                rotateImage(value - mDegreesRotated)
            }
        }

    /**
     * whether the aspect ratio is fixed or not; true fixes the aspect ratio, while false allows it to
     * be changed.
     */
    val isFixAspectRatio: Boolean
        get() = cropOverlayView.isFixAspectRatio

    /** Sets whether the image should be flipped horizontally  */
    var isFlippedHorizontally: Boolean
        get() = mFlipHorizontally
        set(flipHorizontally) {
            if (mFlipHorizontally != flipHorizontally) {
                mFlipHorizontally = flipHorizontally
                applyImageMatrix(
                    width = width.toFloat(),
                    height = height.toFloat(),
                    center = true,
                    animate = false
                )
            }
        }

    /** the Android Uri to save the cropped image to  */
    var customOutputUri: Uri? = null

    /** Sets whether the image should be flipped vertically  */
    var isFlippedVertically: Boolean
        get() = mFlipVertically
        set(flipVertically) {
            if (mFlipVertically != flipVertically) {
                mFlipVertically = flipVertically
                applyImageMatrix(
                    width = width.toFloat(),
                    height = height.toFloat(),
                    center = true,
                    animate = false
                )
            }
        }
    /** Get the current guidelines option set.  */
    /**
     * Sets the guidelines for the CropOverlayView to be either on, off, or to show when resizing the
     * application.
     */
    var guidelines: Guidelines?
        get() = cropOverlayView.guidelines
        set(guidelines) {
            cropOverlayView.setGuidelines(guidelines!!)
        }

    /** both the X and Y values of the aspectRatio.  */
    val aspectRatio: Pair<Int, Int>
        get() = Pair(cropOverlayView.aspectRatioX, cropOverlayView.aspectRatioY)

    /**
     * if to show crop overlay UI what contains the crop window UI surrounded by background over the
     * cropping image.<br></br>
     * default: true, may disable for animation or frame transition.
     */
    var isShowCropOverlay: Boolean
        get() = mShowCropOverlay
        set(showCropOverlay) {
            if (mShowCropOverlay != showCropOverlay) {
                mShowCropOverlay = showCropOverlay
                setCropOverlayVisibility()
            }
        }

    /**
     * Gets the source Bitmap's dimensions. This represents the largest possible crop rectangle.
     *
     * @return a Rect instance dimensions of the source Bitmap
     */
    val wholeImageRect: Rect?
        get() {
            val loadedSampleSize = loadedSampleSize
            val bitmap = originalBitmap ?: return null
            val orgWidth = bitmap.width * loadedSampleSize
            val orgHeight = bitmap.height * loadedSampleSize
            return Rect(0, 0, orgWidth, orgHeight)
        } // get the points of the crop rectangle adjusted to source bitmap

    /**
     * Gets the crop window's position relative to the source Bitmap (not the image displayed in the
     * CropImageView) using the original image rotation.
     *
     * @return a Rect instance containing cropped area boundaries of the source Bitmap
     *
     * Set the crop window position and size to the given rectangle.<br></br>
     * Image to crop must be first set before invoking this, for async - after complete callback.
     *
     * rect window rectangle (position and size) relative to source bitmap
     */
    var cropRect: Rect?
        get() {
            val loadedSampleSize = loadedSampleSize
            val bitmap = originalBitmap ?: return null
            // get the points of the crop rectangle adjusted to source bitmap
            val points = cropPoints
            val orgWidth = bitmap.width * loadedSampleSize
            val orgHeight = bitmap.height * loadedSampleSize
            // get the rectangle for the points (it may be larger than original if rotation is not stright)
            return getRectFromPoints(
                points,
                orgWidth,
                orgHeight,
                cropOverlayView.isFixAspectRatio,
                cropOverlayView.aspectRatioX,
                cropOverlayView.aspectRatioY
            )
        }
        set(rect) {
            cropOverlayView.initialCropWindowRect = rect
        }

    /**
     * Gets the crop window's position relative to the parent's view at screen.
     *
     * @return a Rect instance containing cropped area boundaries of the source Bitmap
     */
    val cropWindowRect: RectF
        get() = cropOverlayView.cropWindowRect // Get crop window position relative to the displayed image.

    /**
     * Gets the 4 points of crop window's position relative to the source Bitmap (not the image
     * displayed in the CropImageView) using the original image rotation.<br></br>
     * Note: the 4 points may not be a rectangle if the image was rotates to NOT straight angle (!=
     * 90/180/270).
     *
     * @return 4 points (x0,y0,x1,y1,x2,y2,x3,y3) of cropped area boundaries
     */
    val cropPoints: FloatArray
        get() {
            // Get crop window position relative to the displayed image.
            val cropWindowRect = cropOverlayView.cropWindowRect
            val points = floatArrayOf(
                cropWindowRect.left,
                cropWindowRect.top,
                cropWindowRect.right,
                cropWindowRect.top,
                cropWindowRect.right,
                cropWindowRect.bottom,
                cropWindowRect.left,
                cropWindowRect.bottom
            )
            mImageMatrix.invert(mImageInverseMatrix)
            mImageInverseMatrix.mapPoints(points)
            val resultPoints = FloatArray(points.size)
            for (i in points.indices) {
                resultPoints[i] = points[i] * loadedSampleSize
            }
            return resultPoints
        }

    /**
     * the min size the resulting cropping image is allowed to be, affects the cropping window limits
     * (in pixels).<br></br>
     */
    fun setMinCropResultSize(minCropResultWidth: Int, minCropResultHeight: Int) {
        cropOverlayView.setMinCropResultSize(minCropResultWidth, minCropResultHeight)
    }

    /**
     * the max size the resulting cropping image is allowed to be, affects the cropping window limits
     * (in pixels).<br></br>
     */
    fun setMaxCropResultSize(maxCropResultWidth: Int, maxCropResultHeight: Int) {
        cropOverlayView.setMaxCropResultSize(maxCropResultWidth, maxCropResultHeight)
    }

    /** Set multi-touch functionality to enabled/disabled.  */
    fun setMultiTouchEnabled(multiTouchEnabled: Boolean) {
        if (cropOverlayView.setMultiTouchEnabled(multiTouchEnabled)) {
            handleCropWindowChanged(inProgress = false, animate = false)
            cropOverlayView.invalidate()
        }
    }

    /** Set moving of the crop window by dragging the center to enabled/disabled.  */
    fun setCenterMoveEnabled(centerMoveEnabled: Boolean) {
        if (cropOverlayView.setCenterMoveEnabled(centerMoveEnabled)) {
            handleCropWindowChanged(inProgress = false, animate = false)
            cropOverlayView.invalidate()
        }
    }

    /**
     * Sets whether the aspect ratio is fixed or not; true fixes the aspect ratio, while false allows
     * it to be changed.
     */
    fun setFixedAspectRatio(fixAspectRatio: Boolean) {
        cropOverlayView.setFixedAspectRatio(fixAspectRatio)
    }

    /**
     * Sets the both the X and Y values of the aspectRatio.<br></br>
     * Sets fixed aspect ratio to TRUE.
     *
     * @param aspectRatioX int that specifies the new X value of the aspect ratio
     * @param aspectRatioY int that specifies the new Y value of the aspect ratio
     */
    fun setAspectRatio(aspectRatioX: Int, aspectRatioY: Int) {
        cropOverlayView.aspectRatioX = aspectRatioX
        cropOverlayView.aspectRatioY = aspectRatioY
        setFixedAspectRatio(true)
    }

    /** Clears set aspect ratio values and sets fixed aspect ratio to FALSE.  */
    fun clearAspectRatio() {
        cropOverlayView.aspectRatioX = 1
        cropOverlayView.aspectRatioY = 1
        setFixedAspectRatio(false)
    }

    /**
     * An edge of the crop window will snap to the corresponding edge of a specified bounding box when
     * the crop window edge is less than or equal to this distance (in pixels) away from the bounding
     * box edge. (default: 3dp)
     */
    fun setSnapRadius(snapRadius: Float) {
        if (snapRadius >= 0) cropOverlayView.setSnapRadius(snapRadius)
    }

    /** Reset crop window to initial rectangle.  */
    fun resetCropRect() {
        mZoom = 1f
        mZoomOffsetX = 0f
        mZoomOffsetY = 0f
        mDegreesRotated = mInitialDegreesRotated
        mFlipHorizontally = false
        mFlipVertically = false
        applyImageMatrix(
            width = width.toFloat(),
            height = height.toFloat(),
            center = false,
            animate = false
        )
        cropOverlayView.resetCropWindowRect()
    }

    /** Set the callback t  */
    fun setOnSetCropOverlayReleasedListener(listener: OnSetCropOverlayReleasedListener?) {
        mOnCropOverlayReleasedListener = listener
    }

    /** Set the callback when the cropping is moved  */
    fun setOnSetCropOverlayMovedListener(listener: OnSetCropOverlayMovedListener?) {
        mOnSetCropOverlayMovedListener = listener
    }

    /** Set the callback when the crop window is changed  */
    fun setOnCropWindowChangedListener(listener: OnSetCropWindowChangeListener?) {
        mOnSetCropWindowChangeListener = listener
    }

    /**
     * Sets a Bitmap as the content of the CropImageView.
     *
     * @param bitmap the Bitmap to set
     */
    fun setImageBitmap(bitmap: Bitmap?) {
        cropOverlayView.initialCropWindowRect = null
        if (originalBitmap == null || originalBitmap != bitmap) {
            clearImageResource()
            originalBitmap = bitmap
            imageView.setImageBitmap(originalBitmap)
            loadedSampleSize = 1
            mDegreesRotated = 0
            applyImageMatrix(
                width = width.toFloat(),
                height = height.toFloat(),
                center = true,
                animate = false
            )
            cropOverlayView.resetCropOverlayView()
            setCropOverlayVisibility()
        }
    }

    /** Clear the current image set for cropping.  */
    fun clearImage() {
        clearImageResource()
        cropOverlayView.initialCropWindowRect = null
    }

    /**
     * Rotates image by the specified number of degrees clockwise.<br></br>
     * Negative values represent counter-clockwise rotations.
     *
     * @param degrees Integer specifying the number of degrees to rotate.
     */
    private fun rotateImage(degrees: Int) {
        if (originalBitmap != null) {
            // Force degrees to be a non-zero value between 0 and 360 (inclusive)
            val newDegrees =
                if (degrees < 0) degrees % 360 + 360
                else degrees % 360
            val flipAxes = (
                    !cropOverlayView.isFixAspectRatio &&
                            (newDegrees in 46..134 || newDegrees in 216..304)
                    )

            RECT.set(cropOverlayView.cropWindowRect)
            var halfWidth =
                (if (flipAxes) RECT.height() else RECT.width()) / 2f
            var halfHeight =
                (if (flipAxes) RECT.width() else RECT.height()) / 2f

            if (flipAxes) {
                val isFlippedHorizontally = mFlipHorizontally
                mFlipHorizontally = mFlipVertically
                mFlipVertically = isFlippedHorizontally
            }
            mImageMatrix.invert(mImageInverseMatrix)
            POINTS[0] = RECT.centerX()
            POINTS[1] = RECT.centerY()
            POINTS[2] = 0f
            POINTS[3] = 0f
            POINTS[4] = 1f
            POINTS[5] = 0f
            mImageInverseMatrix.mapPoints(POINTS)
            // This is valid because degrees is not negative.
            mDegreesRotated = (mDegreesRotated + newDegrees) % 360
            applyImageMatrix(
                width = width.toFloat(),
                height = height.toFloat(),
                center = true,
                animate = false
            )
            // adjust the zoom so the crop window size remains the same even after image scale change
            mImageMatrix.mapPoints(POINTS2, POINTS)
            mZoom /= sqrt(
                (POINTS2[4] - POINTS2[2]).toDouble().pow(2.0) +
                        (POINTS2[5] - POINTS2[3]).toDouble().pow(2.0)
            ).toFloat()
            mZoom = max(mZoom, 1f)
            applyImageMatrix(
                width = width.toFloat(),
                height = height.toFloat(),
                center = true,
                animate = false
            )
            mImageMatrix.mapPoints(POINTS2, POINTS)
            // adjust the width/height by the changes in scaling to the image
            val change = sqrt(
                (POINTS2[4] - POINTS2[2]).toDouble().pow(2.0) +
                        (POINTS2[5] - POINTS2[3]).toDouble().pow(2.0)
            )
            halfWidth *= change.toFloat()
            halfHeight *= change.toFloat()
            // calculate the new crop window rectangle to center in the same location and have proper
            // width/height
            RECT[POINTS2[0] - halfWidth, POINTS2[1] - halfHeight, POINTS2[0] + halfWidth] =
                POINTS2[1] + halfHeight
            cropOverlayView.resetCropOverlayView()
            cropOverlayView.cropWindowRect = RECT
            applyImageMatrix(
                width = width.toFloat(),
                height = height.toFloat(),
                center = true,
                animate = false
            )
            handleCropWindowChanged(inProgress = false, animate = false)
            // make sure the crop window rectangle is within the cropping image bounds after all the
            // changes
            cropOverlayView.fixCurrentCropWindowRect()
        }
    }

    /** Flips the image horizontally.  */
    fun flipImageHorizontally() {
        mFlipHorizontally = !mFlipHorizontally
        applyImageMatrix(
            width = width.toFloat(),
            height = height.toFloat(),
            center = true,
            animate = false
        )
    }

    /** Flips the image vertically.  */
    fun flipImageVertically() {
        mFlipVertically = !mFlipVertically
        applyImageMatrix(
            width = width.toFloat(),
            height = height.toFloat(),
            center = true,
            animate = false
        )
    }

    /**
     * Getter 편집중인 것들을 객체로 리턴하는 함수
     */
    fun getEditInfo(): CropImageEditModel {
        return CropImageEditModel(
            bitmap = originalBitmap,
            points = cropPoints,
            degreesRotated = mDegreesRotated,
            fixAspectRatio = isFixAspectRatio,
            aspectRatioX = cropOverlayView.aspectRatioX,
            aspectRatioY = cropOverlayView.aspectRatioY,
            flipHorizontally = mFlipHorizontally,
            flipVertically = mFlipVertically
        )
    }

    /**
     * Clear the current image set for cropping.<br></br>
     * Full clear will also clear the data of the set image like Uri or Resource id while partial
     * clear will only clear the bitmap and recycle if required.
     */
    private fun clearImageResource() {
        // if we allocated the bitmap, release it as fast as possible
        originalBitmap?.recycle()
        originalBitmap = null

        loadedSampleSize = 1
        mDegreesRotated = 0
        mZoom = 1f
        mZoomOffsetX = 0f
        mZoomOffsetY = 0f
        mImageMatrix.reset()
        mRestoreCropWindowRect = null
        mRestoreDegreesRotated = 0
        imageView.setImageBitmap(null)
        setCropOverlayVisibility()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val bitmap = originalBitmap
        if (bitmap != null) {
            // Bypasses a baffling bug when used within a ScrollView, where heightSize is set to 0.
            if (heightSize == 0) heightSize = bitmap.height
            val desiredWidth: Int
            val desiredHeight: Int
            var viewToBitmapWidthRatio = Double.POSITIVE_INFINITY
            var viewToBitmapHeightRatio = Double.POSITIVE_INFINITY
            // Checks if either width or height needs to be fixed
            if (widthSize < bitmap.width) {
                viewToBitmapWidthRatio = widthSize.toDouble() / bitmap.width.toDouble()
            }
            if (heightSize < bitmap.height) {
                viewToBitmapHeightRatio = heightSize.toDouble() / bitmap.height.toDouble()
            }
            // If either needs to be fixed, choose smallest ratio and calculate from there
            if (viewToBitmapWidthRatio != Double.POSITIVE_INFINITY ||
                viewToBitmapHeightRatio != Double.POSITIVE_INFINITY
            ) {
                if (viewToBitmapWidthRatio <= viewToBitmapHeightRatio) {
                    desiredWidth = widthSize
                    desiredHeight = (bitmap.height * viewToBitmapWidthRatio).toInt()
                } else {
                    desiredHeight = heightSize
                    desiredWidth = (bitmap.width * viewToBitmapHeightRatio).toInt()
                }
            } else {
                // Otherwise, the picture is within frame layout bounds. Desired width is simply picture
                // size
                desiredWidth = bitmap.width
                desiredHeight = bitmap.height
            }
            val width = getOnMeasureSpec(widthMode, widthSize, desiredWidth)
            val height = getOnMeasureSpec(heightMode, heightSize, desiredHeight)
            mLayoutWidth = width
            mLayoutHeight = height
            setMeasuredDimension(mLayoutWidth, mLayoutHeight)
        } else setMeasuredDimension(widthSize, heightSize)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (mLayoutWidth > 0 && mLayoutHeight > 0) {
            // Gets original parameters, and creates the new parameters
            val origParams = this.layoutParams
            origParams.width = mLayoutWidth
            origParams.height = mLayoutHeight
            layoutParams = origParams
            if (originalBitmap != null) {
                applyImageMatrix(
                    (r - l).toFloat(), (b - t).toFloat(),
                    center = true,
                    animate = false
                )
                // after state restore we want to restore the window crop, possible only after widget size
                // is known
                val restoreCropWindowRect = mRestoreCropWindowRect
                if (restoreCropWindowRect != null) {
                    if (mRestoreDegreesRotated != mInitialDegreesRotated) {
                        mDegreesRotated = mRestoreDegreesRotated
                        applyImageMatrix(
                            width = (r - l).toFloat(),
                            height = (b - t).toFloat(),
                            center = true,
                            animate = false
                        )
                        mRestoreDegreesRotated = 0
                    }
                    mImageMatrix.mapRect(mRestoreCropWindowRect)
                    cropOverlayView.cropWindowRect = restoreCropWindowRect
                    handleCropWindowChanged(inProgress = false, animate = false)
                    cropOverlayView.fixCurrentCropWindowRect()
                    mRestoreCropWindowRect = null
                } else if (mSizeChanged) {
                    mSizeChanged = false
                    handleCropWindowChanged(inProgress = false, animate = false)
                }
            } else updateImageBounds(true)
        } else updateImageBounds(true)
    }

    /**
     * Detect size change to handle auto-zoom using [.handleCropWindowChanged]
     * in [.layout].
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mSizeChanged = oldw > 0 && oldh > 0
    }

    /**
     * Handle crop window change to:<br></br>
     * 1. Execute auto-zoom-in/out depending on the area covered of cropping window relative to the
     * available view area.<br></br>
     * 2. Slide the zoomed sub-area if the cropping window is outside of the visible view sub-area.
     * <br></br>
     *
     * @param inProgress is the crop window change is still in progress by the user
     * @param animate if to animate the change to the image matrix, or set it directly
     */
    private fun handleCropWindowChanged(inProgress: Boolean, animate: Boolean) {
        val width = width
        val height = height
        if (originalBitmap != null && width > 0 && height > 0) {
            val cropRect = cropOverlayView.cropWindowRect
            if (inProgress) {
                if (cropRect.left < 0 || cropRect.top < 0 || cropRect.right > width || cropRect.bottom > height) {
                    applyImageMatrix(
                        width = width.toFloat(),
                        height = height.toFloat(),
                        center = false,
                        animate = false
                    )
                }
            } else if (mAutoZoomEnabled || mZoom > 1) {
                var newZoom = 0f
                // keep the cropping window covered area to 50%-65% of zoomed sub-area
                if (mZoom < mMaxZoom && cropRect.width() < width * 0.5f && cropRect.height() < height * 0.5f) {
                    newZoom = min(
                        mMaxZoom.toFloat(),
                        min(
                            width / (cropRect.width() / mZoom / 0.64f),
                            height / (cropRect.height() / mZoom / 0.64f)
                        )
                    )
                }
                if (mZoom > 1 && (cropRect.width() > width * 0.65f || cropRect.height() > height * 0.65f)) {
                    newZoom = max(
                        1f,
                        min(
                            width / (cropRect.width() / mZoom / 0.51f),
                            height / (cropRect.height() / mZoom / 0.51f)
                        )
                    )
                }
                if (!mAutoZoomEnabled) newZoom = 1f

                if (newZoom > 0 && newZoom != mZoom) {
                    if (animate) {
                        if (mAnimation == null) {
                            // lazy create animation single instance
                            mAnimation = CropImageAnimation(imageView, cropOverlayView)
                        }
                        // set the state for animation to start from
                        mAnimation?.setStartState(mImagePoints, mImageMatrix)
                    }
                    mZoom = newZoom
                    applyImageMatrix(width.toFloat(), height.toFloat(), true, animate)
                }
            }
            if (mOnSetCropWindowChangeListener != null && !inProgress) {
                mOnSetCropWindowChangeListener!!.onCropWindowChanged()
            }
        }
    }

    /**
     * Apply matrix to handle the image inside the image view.
     *
     * @param width the width of the image view
     * @param height the height of the image view
     */
    private fun applyImageMatrix(width: Float, height: Float, center: Boolean, animate: Boolean) {
        val bitmap = originalBitmap
        if (bitmap != null && width > 0 && height > 0) {
            mImageMatrix.invert(mImageInverseMatrix)
            val cropRect = cropOverlayView.cropWindowRect
            mImageInverseMatrix.mapRect(cropRect)
            mImageMatrix.reset()
            // move the image to the center of the image view first so we can manipulate it from there
            mImageMatrix.postTranslate(
                (width - bitmap.width) / 2, (height - bitmap.height) / 2
            )
            mapImagePointsByImageMatrix()
            // rotate the image the required degrees from center of image
            if (mDegreesRotated > 0) {
                mImageMatrix.postRotate(
                    mDegreesRotated.toFloat(),
                    getRectCenterX(mImagePoints),
                    getRectCenterY(mImagePoints)
                )
                mapImagePointsByImageMatrix()
            }
            // scale the image to the image view, image rect transformed to know new width/height
            val scale = min(
                width / getRectWidth(mImagePoints),
                height / getRectHeight(mImagePoints)
            )
            if (mScaleType == ScaleType.FIT_CENTER || mScaleType == ScaleType.CENTER_INSIDE && scale < 1 ||
                scale > 1 && mAutoZoomEnabled
            ) {
                mImageMatrix.postScale(
                    scale,
                    scale,
                    getRectCenterX(mImagePoints),
                    getRectCenterY(mImagePoints)
                )
                mapImagePointsByImageMatrix()
            } else if (mScaleType == ScaleType.CENTER_CROP) {
                mZoom = max(
                    getWidth() / getRectWidth(mImagePoints),
                    getHeight() / getRectHeight(mImagePoints)
                )
            }
            // scale by the current zoom level
            val scaleX = if (mFlipHorizontally) -mZoom else mZoom
            val scaleY = if (mFlipVertically) -mZoom else mZoom
            mImageMatrix.postScale(
                scaleX,
                scaleY,
                getRectCenterX(mImagePoints),
                getRectCenterY(mImagePoints)
            )
            mapImagePointsByImageMatrix()
            mImageMatrix.mapRect(cropRect)

            if (mScaleType == ScaleType.CENTER_CROP && center && !animate) {
                mZoomOffsetX = 0f
                mZoomOffsetY = 0f
            } else if (center) {
                // set the zoomed area to be as to the center of cropping window as possible
                mZoomOffsetX =
                    if (width > getRectWidth(mImagePoints)) 0f
                    else max(
                        min(
                            width / 2 - cropRect.centerX(),
                            -getRectLeft(mImagePoints)
                        ),
                        getWidth() - getRectRight(mImagePoints)
                    ) / scaleX

                mZoomOffsetY =
                    if (height > getRectHeight(mImagePoints)) 0f
                    else max(
                        min(
                            height / 2 - cropRect.centerY(),
                            -getRectTop(mImagePoints)
                        ),
                        getHeight() - getRectBottom(mImagePoints)
                    ) / scaleY
            } else {
                // adjust the zoomed area so the crop window rectangle will be inside the area in case it
                // was moved outside
                mZoomOffsetX = (
                        min(
                            max(mZoomOffsetX * scaleX, -cropRect.left),
                            -cropRect.right + width
                        ) / scaleX
                        )

                mZoomOffsetY = (
                        min(
                            max(mZoomOffsetY * scaleY, -cropRect.top),
                            -cropRect.bottom + height
                        ) / scaleY
                        )
            }
            // apply to zoom offset translate and update the crop rectangle to offset correctly
            mImageMatrix.postTranslate(mZoomOffsetX * scaleX, mZoomOffsetY * scaleY)
            cropRect.offset(mZoomOffsetX * scaleX, mZoomOffsetY * scaleY)
            cropOverlayView.cropWindowRect = cropRect
            mapImagePointsByImageMatrix()
            cropOverlayView.invalidate()
            // set matrix to apply
            if (animate) {
                // set the state for animation to end in, start animation now
                mAnimation!!.setEndState(mImagePoints, mImageMatrix)
                imageView.startAnimation(mAnimation)
            } else imageView.imageMatrix = mImageMatrix
            // update the image rectangle in the crop overlay
            updateImageBounds(false)
        }
    }

    /**
     * Adjust the given image rectangle by image transformation matrix to know the final rectangle of
     * the image.<br></br>
     * To get the proper rectangle it must be first reset to original image rectangle.
     */
    private fun mapImagePointsByImageMatrix() {
        mImagePoints[0] = 0f
        mImagePoints[1] = 0f
        mImagePoints[2] = originalBitmap!!.width.toFloat()
        mImagePoints[3] = 0f
        mImagePoints[4] = originalBitmap!!.width.toFloat()
        mImagePoints[5] = originalBitmap!!.height.toFloat()
        mImagePoints[6] = 0f
        mImagePoints[7] = originalBitmap!!.height.toFloat()
        mImageMatrix.mapPoints(mImagePoints)
        mScaleImagePoints[0] = 0f
        mScaleImagePoints[1] = 0f
        mScaleImagePoints[2] = 100f
        mScaleImagePoints[3] = 0f
        mScaleImagePoints[4] = 100f
        mScaleImagePoints[5] = 100f
        mScaleImagePoints[6] = 0f
        mScaleImagePoints[7] = 100f
        mImageMatrix.mapPoints(mScaleImagePoints)
    }

    /**
     * Set visibility of crop overlay to hide it when there is no image or specificly set by client.
     */
    private fun setCropOverlayVisibility() {
        cropOverlayView.visibility =
            if (mShowCropOverlay && originalBitmap != null) VISIBLE else INVISIBLE
    }

    /**
     * Set visibility of progress bar when async loading/cropping is in process and show is enabled.
     */
    @Deprecated("삭제할 예정")
    private fun setProgressBarVisibility() {

    }

    /** Update the scale factor between the actual image bitmap and the shown image.<br></br>  */
    private fun updateImageBounds(clear: Boolean) {
        if (originalBitmap != null && !clear) {
            // Get the scale factor between the actual Bitmap dimensions and the displayed dimensions for
            // width/height.
            val scaleFactorWidth =
                100f * loadedSampleSize / getRectWidth(mScaleImagePoints)
            val scaleFactorHeight =
                100f * loadedSampleSize / getRectHeight(mScaleImagePoints)
            cropOverlayView.setCropWindowLimits(
                width.toFloat(), height.toFloat(), scaleFactorWidth, scaleFactorHeight
            )
        }
        // set the bitmap rectangle and update the crop window after scale factor is set
        cropOverlayView.setBounds(if (clear) null else mImagePoints, width, height)
    }

    /** Interface definition for a callback to be invoked when the crop overlay is released.  */
    fun interface OnSetCropOverlayReleasedListener {

        /**
         * Called when the crop overlay changed listener is called and inProgress is false.
         *
         * @param rect The rect coordinates of the cropped overlay
         */
        fun onCropOverlayReleased(rect: Rect?)
    }

    /** Interface definition for a callback to be invoked when the crop overlay is released.  */
    fun interface OnSetCropOverlayMovedListener {

        /**
         * Called when the crop overlay is moved
         *
         * @param rect The rect coordinates of the cropped overlay
         */
        fun onCropOverlayMoved(rect: Rect?)
    }

    /** Interface definition for a callback to be invoked when the crop overlay is released.  */
    fun interface OnSetCropWindowChangeListener {

        /** Called when the crop window is changed  */
        fun onCropWindowChanged()
    }

    /** Interface definition for a callback to be invoked when image async loading is complete.  */
    fun interface OnSetImageUriCompleteListener {

        /**
         * Called when a crop image view has completed loading image for cropping.<br></br>
         * If loading failed error parameter will contain the error.
         *
         * @param view The crop image view that loading of image was complete.
         * @param uri the URI of the image that was loading
         * @param error if error occurred during loading will contain the error, otherwise null.
         */
        fun onSetImageUriComplete(view: CropImageEditView, uri: Uri, error: Exception?)
    }

    /**
     * Determines the specs for the onMeasure function. Calculates the width or height depending on
     * the mode.
     *
     * @param measureSpecMode The mode of the measured width or height.
     * @param measureSpecSize The size of the measured width or height.
     * @param desiredSize The desired size of the measured width or height.
     * @return The final size of the width or height.
     */
    private fun getOnMeasureSpec(
        measureSpecMode: Int,
        measureSpecSize: Int,
        desiredSize: Int
    ): Int {
        // Measure Width
        return when (measureSpecMode) {
            MeasureSpec.EXACTLY -> measureSpecSize // Must be this size
            MeasureSpec.AT_MOST -> min(
                desiredSize,
                measureSpecSize
            ) // Can't be bigger than...; match_parent value
            else -> desiredSize // Be whatever you want; wrap_content
        }
    }

    /**
     * Get a rectangle for the given 4 points (x0,y0,x1,y1,x2,y2,x3,y3) by finding the min/max 2
     * points that contains the given 4 points and is a straight rectangle.
     */
    fun getRectFromPoints(
        points: FloatArray,
        imageWidth: Int,
        imageHeight: Int,
        fixAspectRatio: Boolean,
        aspectRatioX: Int,
        aspectRatioY: Int
    ): Rect {
        val left = max(0f, getRectLeft(points)).roundToInt()
        val top = max(0f, getRectTop(points)).roundToInt()
        val right = min(imageWidth.toFloat(), getRectRight(points)).roundToInt()
        val bottom = min(imageHeight.toFloat(), getRectBottom(points)).roundToInt()
        val rect = Rect(left, top, right, bottom)
        if (fixAspectRatio) {
            fixRectForAspectRatio(rect, aspectRatioX, aspectRatioY)
        }
        return rect
    }

    /**
     * Fix the given rectangle if it doesn't confirm to aspect ration rule.<br></br>
     * Make sure that width and height are equal if 1:1 fixed aspect ratio is requested.
     */
    private fun fixRectForAspectRatio(rect: Rect, aspectRatioX: Int, aspectRatioY: Int) {
        if (aspectRatioX == aspectRatioY && rect.width() != rect.height()) {
            if (rect.height() > rect.width()) {
                rect.bottom -= rect.height() - rect.width()
            } else {
                rect.right -= rect.width() - rect.height()
            }
        }
    }

    init {
        val options = CropImageOptions()
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.CropImageEditView, 0, 0)
            try {
                options.fixAspectRatio = ta.getBoolean(
                    R.styleable.CropImageEditView_cropFixAspectRatio,
                    options.fixAspectRatio
                )
                options.aspectRatioX = ta.getInteger(
                    R.styleable.CropImageEditView_cropAspectRatioX,
                    options.aspectRatioX
                )
                options.aspectRatioY = ta.getInteger(
                    R.styleable.CropImageEditView_cropAspectRatioY,
                    options.aspectRatioY
                )
                options.scaleType = ScaleType.values()[
                        ta.getInt(
                            R.styleable.CropImageEditView_cropScaleType,
                            options.scaleType.ordinal
                        )
                ]
                options.autoZoomEnabled = ta.getBoolean(
                    R.styleable.CropImageEditView_cropAutoZoomEnabled,
                    options.autoZoomEnabled
                )
                options.multiTouchEnabled = ta.getBoolean(
                    R.styleable.CropImageEditView_cropMultiTouchEnabled, options.multiTouchEnabled
                )
                options.centerMoveEnabled = ta.getBoolean(
                    R.styleable.CropImageEditView_cropCenterMoveEnabled, options.centerMoveEnabled
                )
                options.maxZoom =
                    ta.getInteger(R.styleable.CropImageEditView_cropMaxZoom, options.maxZoom)
                options.cropShape = CropShape.values()[
                        ta.getInt(
                            R.styleable.CropImageEditView_cropShape,
                            options.cropShape.ordinal
                        )
                ]
                options.cornerShape = CropCornerShape.values()[
                        ta.getInt(
                            R.styleable.CropImageEditView_cornerShape,
                            options.cornerShape.ordinal
                        )
                ]
                options.cropCornerRadius = ta.getDimension(
                    R.styleable.CropImageEditView_cropCornerRadius,
                    options.cropCornerRadius
                )
                options.guidelines = Guidelines.values()[
                        ta.getInt(
                            R.styleable.CropImageEditView_cropGuidelines, options.guidelines.ordinal
                        )
                ]
                options.snapRadius = ta.getDimension(
                    R.styleable.CropImageEditView_cropSnapRadius,
                    options.snapRadius
                )
                options.touchRadius = ta.getDimension(
                    R.styleable.CropImageEditView_cropTouchRadius,
                    options.touchRadius
                )
                options.initialCropWindowPaddingRatio = ta.getFloat(
                    R.styleable.CropImageEditView_cropInitialCropWindowPaddingRatio,
                    options.initialCropWindowPaddingRatio
                )
                options.circleCornerFillColorHexValue = ta.getInteger(
                    R.styleable.CropImageEditView_cropCornerCircleFillColor,
                    options.circleCornerFillColorHexValue
                )
                options.borderLineThickness = ta.getDimension(
                    R.styleable.CropImageEditView_cropBorderLineThickness,
                    options.borderLineThickness
                )
                options.borderLineColor = ta.getInteger(
                    R.styleable.CropImageEditView_cropBorderLineColor,
                    options.borderLineColor
                )
                options.borderCornerThickness = ta.getDimension(
                    R.styleable.CropImageEditView_cropBorderCornerThickness,
                    options.borderCornerThickness
                )
                options.borderCornerOffset = ta.getDimension(
                    R.styleable.CropImageEditView_cropBorderCornerOffset, options.borderCornerOffset
                )
                options.borderCornerLength = ta.getDimension(
                    R.styleable.CropImageEditView_cropBorderCornerLength, options.borderCornerLength
                )
                options.borderCornerColor = ta.getInteger(
                    R.styleable.CropImageEditView_cropBorderCornerColor, options.borderCornerColor
                )
                options.guidelinesThickness = ta.getDimension(
                    R.styleable.CropImageEditView_cropGuidelinesThickness,
                    options.guidelinesThickness
                )
                options.guidelinesColor = ta.getInteger(
                    R.styleable.CropImageEditView_cropGuidelinesColor,
                    options.guidelinesColor
                )
                options.overlayBackgroundColor = ta.getInteger(
                    R.styleable.CropImageEditView_cropOverlayBackgroundColor,
                    options.overlayBackgroundColor
                )
                options.backgroundColor = ta.getInteger(
                    R.styleable.CropImageEditView_cropBackgroundColor,
                    options.backgroundColor
                )
                options.showCropOverlay = ta.getBoolean(
                    R.styleable.CropImageEditView_cropShowCropOverlay,
                    mShowCropOverlay
                )
                options.borderCornerThickness = ta.getDimension(
                    R.styleable.CropImageEditView_cropBorderCornerThickness,
                    options.borderCornerThickness
                )
                options.minCropWindowWidth = ta.getDimension(
                    R.styleable.CropImageEditView_cropMinCropWindowWidth,
                    options.minCropWindowWidth.toFloat()
                ).toInt()
                options.minCropWindowHeight = ta.getDimension(
                    R.styleable.CropImageEditView_cropMinCropWindowHeight,
                    options.minCropWindowHeight.toFloat()
                ).toInt()
                options.minCropResultWidth = ta.getFloat(
                    R.styleable.CropImageEditView_cropMinCropResultWidthPX,
                    options.minCropResultWidth.toFloat()
                ).toInt()
                options.minCropResultHeight = ta.getFloat(
                    R.styleable.CropImageEditView_cropMinCropResultHeightPX,
                    options.minCropResultHeight.toFloat()
                ).toInt()
                options.maxCropResultWidth = ta.getFloat(
                    R.styleable.CropImageEditView_cropMaxCropResultWidthPX,
                    options.maxCropResultWidth.toFloat()
                ).toInt()
                options.maxCropResultHeight = ta.getFloat(
                    R.styleable.CropImageEditView_cropMaxCropResultHeightPX,
                    options.maxCropResultHeight.toFloat()
                ).toInt()
                // if aspect ratio is set then set fixed to true
                if (ta.hasValue(R.styleable.CropImageEditView_cropAspectRatioX) &&
                    ta.hasValue(R.styleable.CropImageEditView_cropAspectRatioX) &&
                    !ta.hasValue(R.styleable.CropImageEditView_cropFixAspectRatio)
                ) {
                    options.fixAspectRatio = true
                }
            } finally {
                ta.recycle()
            }
        }
        options.validate()
        mScaleType = options.scaleType
        mAutoZoomEnabled = options.autoZoomEnabled
        mMaxZoom = options.maxZoom
        mShowCropOverlay = options.showCropOverlay

        val inflater = LayoutInflater.from(context)
        val v = inflater.inflate(R.layout.v_crop_image, this, true)
        v.findViewById<View>(R.id.vBg).setBackgroundColor(options.backgroundColor)
        imageView = v.findViewById(R.id.iv)
        imageView.scaleType = ImageView.ScaleType.MATRIX
        cropOverlayView = v.findViewById(R.id.overlay)
        cropOverlayView.setCropWindowChangeListener(this)
        cropOverlayView.setInitialAttributeValues(options)
    }

    override fun onCropWindowChanged(inProgress: Boolean) {
        handleCropWindowChanged(inProgress, true)
        val listener = mOnCropOverlayReleasedListener
        if (listener != null && !inProgress) listener.onCropOverlayReleased(cropRect)
        val movedListener = mOnSetCropOverlayMovedListener
        if (movedListener != null && inProgress) movedListener.onCropOverlayMoved(cropRect)
    }
}
