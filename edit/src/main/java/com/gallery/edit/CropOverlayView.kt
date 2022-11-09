package com.gallery.edit

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import com.gallery.edit.CropImageView.CropShape
import com.gallery.edit.CropImageView.Guidelines
import com.gallery.edit.handler.CropWindowHandler
import com.gallery.edit.handler.CropWindowMoveHandler
import java.util.*
import kotlin.math.*

/**
 * References : Get https://github.com/ArthurHub/Android-Image-Cropper
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class CropOverlayView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    /** Gesture detector used for multi touch box scaling  */
    var scaleDetector: ScaleGestureDetector? = null

    /** Boolean to see if multi touch is enabled for the crop rectangle  */
    var multiTouchEnabled: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (field && scaleDetector == null) {
                    scaleDetector = ScaleGestureDetector(context, ScaleListener())
                }
            }
        }

    /**
     * Boolean to see if movement via dragging center is enabled for the crop rectangle
     */
    var centerMoveEnabled: Boolean = true
        set(value) {
            if (field != value) {
                field = value
            }
        }

    /**
     * Handler from crop window stuff, moving and knowing position.
     */
    private val cropWindowHandler: CropWindowHandler by lazy { CropWindowHandler() }

    /** Listener to public crop window changes  */
    var cropWindowChangeListener: CropWindowChangeListener? = null

    /** Rectangle used for drawing  */
    private val drawRect: RectF by lazy { RectF() }

    /** The Paint used to draw the white rectangle around the crop area.  */
    private var borderPaint: Paint? = null

    /** The Paint used to draw the corners of the Border  */
    private var borderCornerPaint: Paint? = null

    /** The Paint used to draw the guidelines within the crop area when pressed.  */
    private var guidelinePaint: Paint? = null

    /** The Paint used to darken the surrounding areas outside the crop area.  */
    private var backgroundPaint: Paint? = null

    /** Used for oval crop window shape or non-straight rotation drawing.  */
    private val path: Path by lazy { Path() }

    /** The bounding box around the Bitmap that we are cropping.  */
    private val boundsPoints: FloatArray by lazy { FloatArray(8) }

    /** The bounding box around the Bitmap that we are cropping.  */
    private val calcBounds: RectF by lazy { RectF() }

    /** The bounding image view width used to know the crop overlay is at view edges.  */
    private var viewWidth = 0

    /** The bounding image view height used to know the crop overlay is at view edges.  */
    private var viewHeight = 0

    /** The offset to draw the border corener from the border  */
    private var borderCornerOffset = 0f

    /** the length of the border corner to draw  */
    private var borderCornerLength = 0f

    /** The initial crop window padding from image borders  */
    private var initialCropWindowPaddingRatio = 0f

    /** The radius of the touch zone (in pixels) around a given Handle.  */
    private var touchRadius = 0f

    /**
     * An edge of the crop window will snap to the corresponding edge of a specified bounding box when
     * the crop window edge is less than or equal to this distance (in pixels) away from the bounding
     * box edge. (default: 3)
     */
    var snapRadius = 0f

    /** The Handle that is currently pressed; null if no Handle is pressed.  */
    private var moveHandler: CropWindowMoveHandler? = null

    /**
     * Flag indicating if the crop area should always be a certain aspect ratio (indicated by
     * fixAspectRatio).
     */
    /**
     * Sets whether the aspect ratio is fixed or not; true fixes the aspect ratio, while false allows
     * it to be changed.
     */
    var isFixAspectRatio = false
        set(value) {
            if (field != value) {
                field = value
                if (initializedCropWindow) {
                    initCropWindow()
                    invalidate()
                }
            }
        }

    /** save the current aspect ratio of the image  */
    /** Sets the X value of the aspect ratio; is defaulted to 1.  */
    var aspectRatioX = 0
        set(value) {
            require(value > 0) { "Cannot set aspect ratio value to a number less than or equal to 0." }
            if (field != value) {
                field = value
                targetAspectRatio = field.toFloat() / aspectRatioY
                if (initializedCropWindow) {
                    initCropWindow()
                    invalidate()
                }
            }
        }

    /** save the current aspect ratio of the image  */
    /** Sets the Y value of the aspect ratio; is defaulted to 1. */
    var aspectRatioY = 0
        set(value) {
            require(value > 0) { "Cannot set aspect ratio value to a number less than or equal to 0." }
            if (field != value) {
                field = value
                targetAspectRatio = aspectRatioX.toFloat() / field
                if (initializedCropWindow) {
                    initCropWindow()
                    invalidate()
                }
            }
        }

    /**
     * The aspect ratio that the crop area should maintain; this variable is only used when
     * mMaintainAspectRatio is true.
     */
    private var targetAspectRatio = aspectRatioX.toFloat() / aspectRatioY

    /** Instance variables for customizable attributes  */
    /**
     * Sets the guidelines for the CropOverlayView to be either on, off, or to show when resizing the
     * application.
     */
    var guidelines: Guidelines? = null
        set(value) {
            if (field != value) {
                field = value
                if (initializedCropWindow) {
                    invalidate()
                }
            }
        }

    /** The shape of the cropping area - rectangle/circular.  */
    /** The shape of the cropping area - rectangle/circular.  */
    var cropShape: CropShape = CropShape.RECTANGLE
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    /**
     * The shape of the crop corner
     */
    var cornerShape: CropImageView.CropCornerShape? = null
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private val initCropWindowRect: Rect by lazy { Rect() }

    /** Whether the Crop View has been initialized for the first time  */
    private var initializedCropWindow = false

    /** Get crop window initial rectangle.  */
    /** Set crop window initial rectangle to be used instead of default.  */
    var initialCropWindowRect: Rect?
        get() = initCropWindowRect
        set(value) {
            initCropWindowRect.set(value ?: BitmapUtils.EMPTY_RECT)
            if (initializedCropWindow) {
                initCropWindow()
                invalidate()
                callOnCropWindowChanged(false)
            }
        }

    /** Get the left/top/right/bottom coordinates of the crop window.  */
    /** Set the left/top/right/bottom coordinates of the crop window.  */
    var cropWindowRect: RectF
        get() = cropWindowHandler.getRect()
        set(value) {
            cropWindowHandler.setRect(value)
        }

    /** Fix the current crop window rectangle if it is outside of cropping image or view bounds.  */
    fun fixCurrentCropWindowRect() {
        val rect = cropWindowRect
        fixCropWindowRectByRules(rect)
        cropWindowHandler.setRect(rect)
    }

    /**
     * Informs the CropOverlayView of the image's position relative to the ImageView. This is
     * necessary to call in order to draw the crop window.
     *
     * @param boundsPoints the image's bounding points
     * @param viewWidth The bounding image view width.
     * @param viewHeight The bounding image view height.
     */
    fun setBounds(boundsPoints: FloatArray?, viewWidth: Int, viewHeight: Int) {
        if (boundsPoints == null || !Arrays.equals(this.boundsPoints, boundsPoints)) {
            if (boundsPoints == null) {
                Arrays.fill(this.boundsPoints, 0F)
            } else {
                System.arraycopy(boundsPoints, 0, this.boundsPoints, 0, boundsPoints.size)
            }
            this.viewWidth = viewWidth
            this.viewHeight = viewHeight
            val cropRect = cropWindowHandler.getRect()
            if (cropRect.width() == 0f || cropRect.height() == 0f) {
                initCropWindow()
            }
        }
    }

    /** Resets the crop overlay view.  */
    fun resetCropOverlayView() {
        if (initializedCropWindow) {
            cropWindowRect = RectF()
            initCropWindow()
            invalidate()
        }
    }

    /**
     * the min size the resulting cropping image is allowed to be, affects the cropping window limits
     * (in pixels).<br></br>
     */
    fun setMinCropResultSize(minCropResultWidth: Int, minCropResultHeight: Int) {
        cropWindowHandler.setMinCropResultSize(minCropResultWidth, minCropResultHeight)
    }

    /**
     * the max size the resulting cropping image is allowed to be, affects the cropping window limits
     * (in pixels).<br></br>
     */
    fun setMaxCropResultSize(maxCropResultWidth: Int, maxCropResultHeight: Int) {
        cropWindowHandler.setMaxCropResultSize(maxCropResultWidth, maxCropResultHeight)
    }

    /**
     * set the max width/height and scale factor of the shown image to original image to scale the
     * limits appropriately.
     */
    fun setCropWindowLimits(
        maxWidth: Float, maxHeight: Float, scaleFactorWidth: Float, scaleFactorHeight: Float
    ) {
        cropWindowHandler.setCropWindowLimits(
            maxWidth, maxHeight, scaleFactorWidth, scaleFactorHeight
        )
    }

    /** Reset crop window to initial rectangle.  */
    fun resetCropWindowRect() {
        if (initializedCropWindow) {
            initCropWindow()
            invalidate()
            callOnCropWindowChanged(false)
        }
    }

    /**
     * Sets all initial values, but does not call initCropWindow to reset the views.<br></br>
     * Used once at the very start to initialize the attributes.
     */
    fun setInitialAttributeValues(options: CropImageOptions) {
        cropWindowHandler.setInitialAttributeValues(options)
        this.cropShape = options.cropShape
        this.snapRadius = options.snapRadius
        this.guidelines = options.guidelines
        this.isFixAspectRatio = options.fixAspectRatio
        this.aspectRatioX = options.aspectRatioX
        this.aspectRatioY = options.aspectRatioY
        multiTouchEnabled = options.multiTouchEnabled
        touchRadius = options.touchRadius
        initialCropWindowPaddingRatio = options.initialCropWindowPaddingRatio
        borderPaint = getNewPaintOrNull(options.borderLineThickness, options.borderLineColor)
        borderCornerOffset = options.borderCornerOffset
        borderCornerLength = options.borderCornerLength
        borderCornerPaint =
            getNewPaintOrNull(options.borderCornerThickness, options.borderCornerColor)
        guidelinePaint = getNewPaintOrNull(options.guidelinesThickness, options.guidelinesColor)
        backgroundPaint = getNewPaint(options.backgroundColor)
    }

    /**
     * Get left value of the bounding rectangle of the given points.
     */
    private fun getRectLeft(points: FloatArray): Float {
        return min(min(min(points[0], points[2]), points[4]), points[6])
    }

    /**
     * Get top value of the bounding rectangle of the given points.
     */
    private fun getRectTop(points: FloatArray): Float {
        return min(min(min(points[1], points[3]), points[5]), points[7])
    }

    /**
     * Get right value of the bounding rectangle of the given points.
     */
    private fun getRectRight(points: FloatArray): Float {
        return max(max(max(points[0], points[2]), points[4]), points[6])
    }

    /**
     * Get bottom value of the bounding rectangle of the given points.
     */
    private fun getRectBottom(points: FloatArray): Float {
        return max(max(max(points[1], points[3]), points[5]), points[7])
    }

    /**
     * Set the initial crop window size and position. This is dependent on the size and position of
     * the image being cropped.
     */
    private fun initCropWindow() {
        val leftLimit = getRectLeft(boundsPoints).coerceAtLeast(0F)
        val topLimit = getRectTop(boundsPoints).coerceAtLeast(0F)
        val rightLimit =
            getRectRight(boundsPoints).coerceAtMost(width.toFloat())
        val bottomLimit =
            getRectBottom(boundsPoints).coerceAtMost(height.toFloat())
        if (rightLimit <= leftLimit || bottomLimit <= topLimit) {
            return
        }
        val rect = RectF()

        // Tells the attribute functions the crop window has already been initialized
        initializedCropWindow = true
        val horizontalPadding = initialCropWindowPaddingRatio * (rightLimit - leftLimit)
        val verticalPadding = initialCropWindowPaddingRatio * (bottomLimit - topLimit)
        if (initCropWindowRect.width() > 0 && initCropWindowRect.height() > 0) {
            // Get crop window position relative to the displayed image.
            rect.left =
                leftLimit + initCropWindowRect.left / cropWindowHandler.getScaleFactorWidth()
            rect.top =
                topLimit + initCropWindowRect.top / cropWindowHandler.getScaleFactorHeight()
            rect.right =
                rect.left + initCropWindowRect.width() / cropWindowHandler.getScaleFactorWidth()
            rect.bottom =
                rect.top + initCropWindowRect.height() / cropWindowHandler.getScaleFactorHeight()

            rect.left = leftLimit.coerceAtLeast(rect.left)
            rect.top = topLimit.coerceAtLeast(rect.top)
            rect.right = rightLimit.coerceAtMost(rect.right)
            rect.bottom = bottomLimit.coerceAtMost(rect.bottom)
        } else if (isFixAspectRatio && rightLimit > leftLimit && bottomLimit > topLimit) {

            // If the image aspect ratio is wider than the crop aspect ratio,
            // then the image height is the determining initial length. Else, vice-versa.
            val bitmapAspectRatio = (rightLimit - leftLimit) / (bottomLimit - topLimit)
            if (bitmapAspectRatio > targetAspectRatio) {
                rect.top = topLimit + verticalPadding
                rect.bottom = bottomLimit - verticalPadding
                val centerX = width / 2f

                // dirty fix for wrong crop overlay aspect ratio when using fixed aspect ratio
                targetAspectRatio = this.aspectRatioX.toFloat() / this.aspectRatioY

                // Limits the aspect ratio to no less than 40 wide or 40 tall
                val cropWidth = cropWindowHandler.getMinCropWidth()
                    .coerceAtLeast(rect.height() * targetAspectRatio)
                val halfCropWidth = cropWidth / 2f
                rect.left = centerX - halfCropWidth
                rect.right = centerX + halfCropWidth
            } else {
                rect.left = leftLimit + horizontalPadding
                rect.right = rightLimit - horizontalPadding
                val centerY = height / 2f

                // Limits the aspect ratio to no less than 40 wide or 40 tall
                val cropHeight = cropWindowHandler.getMinCropHeight()
                    .coerceAtLeast(rect.width() / targetAspectRatio)
                val halfCropHeight = cropHeight / 2f
                rect.top = centerY - halfCropHeight
                rect.bottom = centerY + halfCropHeight
            }
        } else {
            // Initialize crop window to have 10% padding w/ respect to image.
            rect.left = leftLimit + horizontalPadding
            rect.top = topLimit + verticalPadding
            rect.right = rightLimit - horizontalPadding
            rect.bottom = bottomLimit - verticalPadding
        }
        fixCropWindowRectByRules(rect)
        cropWindowHandler.setRect(rect)
    }

    /** Fix the given rect to fit into bitmap rect and follow min, max and aspect ratio rules.  */
    private fun fixCropWindowRectByRules(rect: RectF) {
        if (rect.width() < cropWindowHandler.getMinCropWidth()) {
            val adj = (cropWindowHandler.getMinCropWidth() - rect.width()) / 2
            rect.left -= adj
            rect.right += adj
        }
        if (rect.height() < cropWindowHandler.getMinCropHeight()) {
            val adj = (cropWindowHandler.getMinCropHeight() - rect.height()) / 2
            rect.top -= adj
            rect.bottom += adj
        }
        if (rect.width() > cropWindowHandler.getMaxCropWidth()) {
            val adj = (rect.width() - cropWindowHandler.getMaxCropWidth()) / 2
            rect.left += adj
            rect.right -= adj
        }
        if (rect.height() > cropWindowHandler.getMaxCropHeight()) {
            val adj = (rect.height() - cropWindowHandler.getMaxCropHeight()) / 2
            rect.top += adj
            rect.bottom -= adj
        }
        calculateBounds(rect)
        if (calcBounds.width() > 0 && calcBounds.height() > 0) {
            val leftLimit = calcBounds.left.coerceAtLeast(0f)
            val topLimit = calcBounds.top.coerceAtLeast(0f)
            val rightLimit = calcBounds.right.coerceAtMost(width.toFloat())
            val bottomLimit = calcBounds.bottom.coerceAtMost(height.toFloat())
            if (rect.left < leftLimit) {
                rect.left = leftLimit
            }
            if (rect.top < topLimit) {
                rect.top = topLimit
            }
            if (rect.right > rightLimit) {
                rect.right = rightLimit
            }
            if (rect.bottom > bottomLimit) {
                rect.bottom = bottomLimit
            }
        }
        if (isFixAspectRatio && abs(rect.width() - rect.height() * targetAspectRatio) > 0.1) {
            if (rect.width() > rect.height() * targetAspectRatio) {
                val adj = abs(rect.height() * targetAspectRatio - rect.width()) / 2
                rect.left += adj
                rect.right -= adj
            } else {
                val adj = abs(rect.width() / targetAspectRatio - rect.height()) / 2
                rect.top += adj
                rect.bottom -= adj
            }
        }
    }

    /**
     * Draw crop overview by drawing background over image not in the cripping area, then borders and
     * guidelines.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw translucent background for the cropped area.
        drawBackground(canvas)
        if (cropWindowHandler.showGuidelines()) {
            // Determines whether guidelines should be drawn or not
            if (guidelines === Guidelines.ON) {
                drawGuidelines(canvas)
            } else if (guidelines === Guidelines.ON_TOUCH && moveHandler != null) {
                // Draw only when resizing
                drawGuidelines(canvas)
            }
        }
        drawBorders(canvas)
        drawCorners(canvas)
    }

    /**
     * Draw shadow background over the image not including the crop area.
     */
    @Suppress("DEPRECATION")
    private fun drawBackground(canvas: Canvas) {
        val bgPaint =
            backgroundPaint ?: Paint().apply { setBackgroundColor(Color.argb(119, 0, 0, 0)) }
        val rect = cropWindowHandler.getRect()
        val left = getRectLeft(boundsPoints).coerceAtLeast(0F)
        val top = getRectTop(boundsPoints).coerceAtLeast(0F)
        val right = getRectRight(boundsPoints).coerceAtMost(width.toFloat())
        val bottom = getRectBottom(boundsPoints).coerceAtMost(height.toFloat())
        if (cropShape === CropShape.RECTANGLE) {
            if (!isNonStraightAngleRotated()) {
                canvas.drawRect(left, top, right, rect.top, bgPaint)
                canvas.drawRect(left, rect.bottom, right, bottom, bgPaint)
                canvas.drawRect(left, rect.top, rect.left, rect.bottom, bgPaint)
                canvas.drawRect(rect.right, rect.top, right, rect.bottom, bgPaint)
            } else {
                path.reset()
                path.moveTo(boundsPoints[0], boundsPoints[1])
                path.lineTo(boundsPoints[2], boundsPoints[3])
                path.lineTo(boundsPoints[4], boundsPoints[5])
                path.lineTo(boundsPoints[6], boundsPoints[7])
                path.close()
                canvas.save()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    canvas.clipOutPath(path)
                } else {
                    canvas.clipPath(path, Region.Op.INTERSECT)
                }
                canvas.clipRect(rect, Region.Op.XOR)
                canvas.drawRect(left, top, right, bottom, bgPaint)
                canvas.restore()
            }
        } else {
            path.reset()
            drawRect[rect.left, rect.top, rect.right] = rect.bottom
            path.addOval(drawRect, Path.Direction.CW)
            canvas.save()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutPath(path)
            } else {
                canvas.clipPath(path, Region.Op.XOR)
            }
            canvas.drawRect(left, top, right, bottom, bgPaint)
            canvas.restore()
        }
    }

    /**
     * Draw 2 veritcal and 2 horizontal guidelines inside the cropping area to split it into 9 equal
     * parts.
     */
    private fun drawGuidelines(canvas: Canvas) {
        val paint = guidelinePaint
        if (paint != null) {
            val sw: Float = borderPaint?.strokeWidth ?: 0F
            val rect = cropWindowHandler.getRect()
            rect.inset(sw, sw)
            val oneThirdCropWidth = rect.width() / 3
            val oneThirdCropHeight = rect.height() / 3
            if (cropShape === CropShape.OVAL) {
                val w = rect.width() / 2 - sw
                val h = rect.height() / 2 - sw

                // Draw vertical guidelines.
                val x1 = rect.left + oneThirdCropWidth
                val x2 = rect.right - oneThirdCropWidth
                val yv =
                    (h * sin(acos(((w - oneThirdCropWidth) / w).toDouble()))).toFloat()
                canvas.drawLine(x1, rect.top + h - yv, x1, rect.bottom - h + yv, paint)
                canvas.drawLine(x2, rect.top + h - yv, x2, rect.bottom - h + yv, paint)

                // Draw horizontal guidelines.
                val y1 = rect.top + oneThirdCropHeight
                val y2 = rect.bottom - oneThirdCropHeight
                val xv =
                    (w * cos(asin(((h - oneThirdCropHeight) / h).toDouble()))).toFloat()
                canvas.drawLine(rect.left + w - xv, y1, rect.right - w + xv, y1, paint)
                canvas.drawLine(rect.left + w - xv, y2, rect.right - w + xv, y2, paint)
            } else {

                // Draw vertical guidelines.
                val x1 = rect.left + oneThirdCropWidth
                val x2 = rect.right - oneThirdCropWidth
                canvas.drawLine(x1, rect.top, x1, rect.bottom, paint)
                canvas.drawLine(x2, rect.top, x2, rect.bottom, paint)

                // Draw horizontal guidelines.
                val y1 = rect.top + oneThirdCropHeight
                val y2 = rect.bottom - oneThirdCropHeight
                canvas.drawLine(rect.left, y1, rect.right, y1, paint)
                canvas.drawLine(rect.left, y2, rect.right, y2, paint)
            }
        }
    }

    /** Draw borders of the crop area.  */
    private fun drawBorders(canvas: Canvas) {
        val paint = borderPaint
        if (paint != null) {
            val w = paint.strokeWidth
            val rect = cropWindowHandler.getRect()
            rect.inset(w / 2, w / 2)
            if (cropShape === CropShape.RECTANGLE) {
                // Draw rectangle crop window border.
                canvas.drawRect(rect, paint)
            } else {
                // Draw circular crop window border
                canvas.drawOval(rect, paint)
            }
        }
    }

    /** Draw the corner of crop overlay.  */
    private fun drawCorners(canvas: Canvas) {
        val cornerPaint = borderCornerPaint
        if (cornerPaint != null) {
            val lineWidth: Float = borderPaint?.strokeWidth ?: 0F
            val cornerWidth = cornerPaint.strokeWidth

            // for rectangle crop shape we allow the corners to be offset from the borders
            val w: Float = when (cropShape) {
                // for rectangle crop shape we allow the corners to be offset from the borders
                CropShape.RECTANGLE_VERTICAL_ONLY,
                CropShape.RECTANGLE_HORIZONTAL_ONLY,
                CropShape.RECTANGLE -> cornerWidth / 2 + borderCornerOffset
                CropShape.OVAL -> cornerWidth / 2
            }
            val rect = cropWindowHandler.getRect()
            rect.inset(w, w)
            val cornerOffset = (cornerWidth - lineWidth) / 2
            val cornerExtension = cornerWidth / 2 + cornerOffset

            // Top left
            canvas.drawLine(
                rect.left.minus(cornerOffset),
                rect.top.minus(cornerExtension),
                rect.left.minus(cornerOffset),
                rect.top.plus(borderCornerLength),
                cornerPaint
            )
            canvas.drawLine(
                rect.left.minus(cornerExtension),
                rect.top.minus(cornerOffset),
                rect.left.plus(borderCornerLength),
                rect.top.minus(cornerOffset),
                cornerPaint
            )

            // Top right
            canvas.drawLine(
                rect.right.plus(cornerOffset),
                rect.top.minus(cornerExtension),
                rect.right.plus(cornerOffset),
                rect.top.plus(borderCornerLength),
                cornerPaint
            )
            canvas.drawLine(
                rect.right.plus(cornerExtension),
                rect.top.minus(cornerOffset),
                rect.right.minus(borderCornerLength),
                rect.top.minus(cornerOffset),
                cornerPaint
            )

            // Bottom left
            canvas.drawLine(
                rect.left.minus(cornerOffset),
                rect.bottom.plus(cornerExtension),
                rect.left.minus(cornerOffset),
                rect.bottom.minus(borderCornerLength),
                cornerPaint
            )
            canvas.drawLine(
                rect.left.minus(cornerExtension),
                rect.bottom.plus(cornerOffset),
                rect.left.plus(borderCornerLength),
                rect.bottom.plus(cornerOffset),
                cornerPaint
            )

            // Bottom left
            canvas.drawLine(
                rect.right.plus(cornerOffset),
                rect.bottom.plus(cornerExtension),
                rect.right.plus(cornerOffset),
                rect.bottom.minus(borderCornerLength),
                cornerPaint
            )
            canvas.drawLine(
                rect.right.plus(cornerExtension),
                rect.bottom.plus(cornerOffset),
                rect.right.minus(borderCornerLength),
                rect.bottom.plus(cornerOffset),
                cornerPaint
            )
        }
    }

    /** Creates the Paint object for drawing.  */
    private fun getNewPaint(color: Int): Paint {
        val paint = Paint()
        paint.color = color
        return paint
    }

    /** Creates the Paint object for given thickness and color, if thickness < 0 return null.  */
    private fun getNewPaintOrNull(thickness: Float, color: Int): Paint? {
        return if (thickness > 0) {
            val borderPaint = Paint()
            borderPaint.color = color
            borderPaint.strokeWidth = thickness
            borderPaint.style = Paint.Style.STROKE
            borderPaint.isAntiAlias = true
            borderPaint
        } else {
            null
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // If this View is not enabled, don't allow for touch interactions.
        return if (isEnabled) {
            if (multiTouchEnabled) {
                scaleDetector?.onTouchEvent(event)
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    onActionDown(event.x, event.y)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    parent.requestDisallowInterceptTouchEvent(false)
                    onActionUp()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    onActionMove(event.x, event.y)
                    parent.requestDisallowInterceptTouchEvent(true)
                    true
                }
                else -> false
            }
        } else {
            false
        }
    }

    /**
     * On press down start crop window movment depending on the location of the press.<br></br>
     * if press is far from crop window then no move handler is returned (null).
     */
    private fun onActionDown(x: Float, y: Float) {
        moveHandler =
            cropWindowHandler.getMoveHandler(x, y, touchRadius, cropShape, centerMoveEnabled)
        if (moveHandler != null) {
            invalidate()
        }
    }

    /** Clear move handler starting in [.onActionDown] if exists.  */
    private fun onActionUp() {
        if (moveHandler != null) {
            moveHandler = null
            callOnCropWindowChanged(false)
            invalidate()
        }
    }

    /**
     * Handle move of crop window using the move handler created in [.onActionDown].<br></br>
     * The move handler will do the proper move/resize of the crop window.
     */
    private fun onActionMove(x: Float, y: Float) {
        val handler = moveHandler
        if (handler != null) {
            var snapRadius = this.snapRadius
            val rect = cropWindowHandler.getRect()
            if (calculateBounds(rect)) {
                snapRadius = 0f
            }
            handler.move(
                rect,
                x,
                y,
                calcBounds,
                viewWidth,
                viewHeight,
                snapRadius,
                isFixAspectRatio,
                targetAspectRatio
            )
            cropWindowHandler.setRect(rect)
            callOnCropWindowChanged(true)
            invalidate()
        }
    }

    /**
     * Calculate the bounding rectangle for current crop window, handle non-straight rotation angles.
     * <br></br>
     * If the rotation angle is straight then the bounds rectangle is the bitmap rectangle, otherwsie
     * we find the max rectangle that is within the image bounds starting from the crop window
     * rectangle.
     *
     * @param rect the crop window rectangle to start finsing bounded rectangle from
     * @return true - non straight rotation in place, false - otherwise.
     */
    private fun calculateBounds(rect: RectF): Boolean {
        var left: Float = getRectLeft(boundsPoints)
        var top: Float = getRectTop(boundsPoints)
        var right: Float = getRectRight(boundsPoints)
        var bottom: Float = getRectBottom(boundsPoints)
        return if (!isNonStraightAngleRotated()) {
            calcBounds[left, top, right] = bottom
            false
        } else {
            var x0 = boundsPoints[0]
            var y0 = boundsPoints[1]
            var x2 = boundsPoints[4]
            var y2 = boundsPoints[5]
            var x3 = boundsPoints[6]
            var y3 = boundsPoints[7]
            if (boundsPoints[7] < boundsPoints[1]) {
                if (boundsPoints[1] < boundsPoints[3]) {
                    x0 = boundsPoints[6]
                    y0 = boundsPoints[7]
                    x2 = boundsPoints[2]
                    y2 = boundsPoints[3]
                    x3 = boundsPoints[4]
                    y3 = boundsPoints[5]
                } else {
                    x0 = boundsPoints[4]
                    y0 = boundsPoints[5]
                    x2 = boundsPoints[0]
                    y2 = boundsPoints[1]
                    x3 = boundsPoints[2]
                    y3 = boundsPoints[3]
                }
            } else if (boundsPoints[1] > boundsPoints[3]) {
                x0 = boundsPoints[2]
                y0 = boundsPoints[3]
                x2 = boundsPoints[6]
                y2 = boundsPoints[7]
                x3 = boundsPoints[0]
                y3 = boundsPoints[1]
            }
            val a0 = (y3 - y0) / (x3 - x0)
            val a1 = -1f / a0
            val b0 = y0 - a0 * x0
            val b1 = y0 - a1 * x0
            val b2 = y2 - a0 * x2
            val b3 = y2 - a1 * x2
            val c0 = (rect.centerY() - rect.top) / (rect.centerX() - rect.left)
            val c1 = -c0
            val d0 = rect.top - c0 * rect.left
            val d1 = rect.top - c1 * rect.right
            left =
                left.coerceAtLeast(if ((d0 - b0) / (a0 - c0) < rect.right) (d0 - b0) / (a0 - c0) else left)
            left =
                left.coerceAtLeast(if ((d0 - b1) / (a1 - c0) < rect.right) (d0 - b1) / (a1 - c0) else left)
            left =
                left.coerceAtLeast(if ((d1 - b3) / (a1 - c1) < rect.right) (d1 - b3) / (a1 - c1) else left)
            right =
                right.coerceAtMost(if ((d1 - b1) / (a1 - c1) > rect.left) (d1 - b1) / (a1 - c1) else right)
            right =
                right.coerceAtMost(if ((d1 - b2) / (a0 - c1) > rect.left) (d1 - b2) / (a0 - c1) else right)
            right =
                right.coerceAtMost(if ((d0 - b2) / (a0 - c0) > rect.left) (d0 - b2) / (a0 - c0) else right)
            top = top.coerceAtLeast((a0 * left + b0).coerceAtLeast(a1 * right + b1))
            bottom = bottom.coerceAtMost((a1 * left + b3).coerceAtMost(a0 * right + b2))
            calcBounds.left = left
            calcBounds.top = top
            calcBounds.right = right
            calcBounds.bottom = bottom
            true
        }
    }

    /** Is the cropping image has been rotated by NOT 0,90,180 or 270 degrees.  */
    private fun isNonStraightAngleRotated(): Boolean {
        return boundsPoints[0] != boundsPoints[6] && boundsPoints[1] != boundsPoints[7]
    }

    /** Invoke on crop change listener safe, don't let the app crash on exception.  */
    private fun callOnCropWindowChanged(inProgress: Boolean) {
        try {
            cropWindowChangeListener?.onCropWindowChanged(inProgress)
        } catch (e: Exception) {
        }
    }

    /**
     * Interface definition for a callback to be invoked
     * when crop window rectangle is changing.
     */
    interface CropWindowChangeListener {
        /**
         * Called after a change in crop window rectangle.
         *
         * @param inProgress is the crop window change operation is still in progress by user touch
         */
        fun onCropWindowChanged(inProgress: Boolean)
    }

    /**
     * Handle scaling the rectangle based on two finger input
     */
    inner class ScaleListener : SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val rect: RectF = cropWindowHandler.getRect()
            val x = detector.focusX
            val y = detector.focusY
            val dY = detector.currentSpanY / 2
            val dX = detector.currentSpanX / 2
            val newTop = y - dY
            val newLeft = x - dX
            val newRight = x + dX
            val newBottom = y + dY
            if (newLeft < newRight &&
                newTop <= newBottom &&
                newLeft >= 0 &&
                newRight <= cropWindowHandler.getMaxCropWidth() &&
                newTop >= 0 &&
                newBottom <= cropWindowHandler.getMaxCropHeight()
            ) {
                rect[newLeft, newTop, newRight] = newBottom
                cropWindowHandler.setRect(rect)
                invalidate()
            }
            return true
        }
    }
}
