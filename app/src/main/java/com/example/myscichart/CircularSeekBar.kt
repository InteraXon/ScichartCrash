package com.example.myscichart

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

class CircularSeekBar : View {


    companion object {
        // Minimum touch target size in DP. 48dp is the Android design recommendation
        private const val MIN_TOUCH_TARGET_DP = 48

        //Geometric (clockwise, relative to 3 o'clock)
        private const val DEF_START_ANGLE = 270F
        private const val DEF_END_ANGLE = 270F
        private val DEF_CIRCLE_STYLE = Paint.Cap.ROUND.ordinal
        private const val DEF_POINTER_STROKE_WIDTH = 14F
        private const val DEF_CIRCLE_STROKE_WIDTH = 14F
        private const val DEF_MAX = 100
        private const val DEF_PROGRESS = 50
        private const val DEF_CIRCLE_COLOR = Color.DKGRAY
        private const val DEF_CIRCLE_PROGRESS_COLOR = Color.GREEN
        private const val DEF_CIRCLE_FILL_COLOR = Color.TRANSPARENT
        private const val DEF_POINTER_COLOR = Color.TRANSPARENT
        private const val DEF_LOCK_ENABLED = true
        private const val DEF_ENABLE_INTERACTION = true

        // Used to avoid overflow.
        private const val SMALL_DEGREE_BIAS = .1F
    }

    var onCircularSeekBarChangeListener: OnCircularSeekBarChangeListener? = null

    // Used for enabling/disabling the lock option for easier setting 0 or max progress.
    private var lockEnabled = true

    // Current progress
    var progress = 0F

    // Max value
    private var max = 0F

    // The style of the fill arc: can be butt, round or square.
    private var arcPaintCapStyle = Paint.Cap.ROUND

    // The width of the circle (in pixels).
    private var circleStrokeWidth = 0F

    // The radius of the pointer (in pixels).
    private var pointerStrokeWidth = 0F

    private var startAngle = 0F

    private var endAngle = 0F

    // Angle of the pointer arc. The pointer is a circle if the style is round.
    private var pointerAngle = SMALL_DEGREE_BIAS

    // Draw the active circle (represents progress).
    private var circleProgressPaint = Paint()

    // Draw the inactive circle.
    private var circlePaint = Paint()

    // Draw the circle fill.
    private var circleFillPaint = Paint()

    // Draw the center of the pointer.
    private var pointerPaint = Paint()

    // Enable / disable progress adjusting
    private var enableInteraction = true

    // Represents the circle of the seekbar.
    private val circleRectF = RectF()

    // Distance in degrees that the current progress makes up in the circle.
    private var progressDegrees = 0F

    // Used to draw the circle
    private var circlePath = Path()

    // Draw the progress on the circle.
    private var circleProgressPath = Path()

    // Draw the pointer arc on the circle.
    private var circlePointerPath = Path()

    // Set ot true when the user is touching the circle on ACTION_DOWN.
    private var pointerMoving = false

    // The width of the circle based on the View width
    private var circleWidth = 0F

    // The height of the circle based on the View height
    private var circleHeight = 0F

    // Represents the calculated progress mark on the circle, in geometric degrees.
    private var pointerPosition = 0F

    // Pointer position in terms of X and Y coordinates.
    private val pointerPositionXY = FloatArray(2)

    // Max of the circle in degrees.
    private var totalCircleDegrees = 0F

    private var circleColor = DEF_CIRCLE_COLOR

    var circleProgressColor = DEF_CIRCLE_PROGRESS_COLOR
        set(value) {
            field = value
            initCircleProgressPaint()
        }

    private var pointerColor = DEF_POINTER_COLOR

    private var circleFillColor = DEF_CIRCLE_FILL_COLOR

    //Makes it easier to hit the 0 progress mark when moving counter clockwise beyond the start of the circle
    private var lockAtStart = true

    // Makes it easier to hit the 100% (max) progress mark when moving clockwise beyond the end of the circle
    private var lockAtEnd = true


    constructor(context: Context) : this(context, null) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs, defStyleAttr)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.translate((this.width / 2).toFloat(), (this.height / 2).toFloat())

        canvas.drawPath(circlePath, circleFillPaint)

        canvas.drawPath(circlePath, circlePaint)

        if (progress > 0F) {
            canvas.drawPath(circleProgressPath, circleProgressPaint)
        }

        if (enableInteraction) {
            canvas.drawPath(circlePointerPath, pointerPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var height = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        var width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        if (height == 0) height = width
        if (width == 0) width = height

        setMeasuredDimension(width, height)

        // Set the circle width and height based on the view for the moment
        val padding = (circleStrokeWidth / 2F).coerceAtLeast(pointerStrokeWidth / 2)
        circleHeight = height / 2F - padding
        circleWidth = width / 2F - padding

        recalculateAll()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!enableInteraction || !isEnabled)
            return false

        // Convert coordinates to our internal coordinate system
        val x = event.x - width / 2
        val y = event.y - height / 2

        // Get the distance from the center of the circle in terms of x and y
        val distanceX = circleRectF.centerX() - x
        val distanceY = circleRectF.centerY() - y

        // Get the distance from the center of the circle in terms of a radius
        val touchEventRadius =
            sqrt(distanceX.toDouble().pow(2.0) + distanceY.toDouble().pow(2.0))
                .toFloat()

        // Convert minimum touch target into px
        val minimumTouchTarget = UiUtils.dpToPx(resources, MIN_TOUCH_TARGET_DP)
        // Either uses the minimumTouchTarget size or larger if the ring/pointer is larger
        val additionalRadius: Float = if (circleStrokeWidth < minimumTouchTarget) {
            // If the width is less than the minimumTouchTarget, use the minimumTouchTarget
            minimumTouchTarget / 2F
        } else {
            // Otherwise use the width
            circleStrokeWidth / 2F
        }
        // Max outer radius of the circle, including the minimumTouchTarget or wheel width
        val outerRadius = circleHeight.coerceAtLeast(circleWidth) + additionalRadius
        // Min inner radius of the circle, including the minimumTouchTarget or wheel width
        val innerRadius = circleHeight.coerceAtMost(circleWidth) - additionalRadius

        var touchAngle: Float =
            (atan2(y.toDouble(), x.toDouble()) / Math.PI * 180 % 360F).toFloat() // Verified
        touchAngle = if (touchAngle < 0) 360F + touchAngle else touchAngle // Verified

        // Represents the clockwise distance from startAngle to the touch angle.
        var cwDistanceFromStart = touchAngle - startAngle
        if (cwDistanceFromStart < 0) {
            cwDistanceFromStart += 360F
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {

                //Represents the clockwise distance from pointerPosition to the touch angle.
                var cwDistanceFromPointer = touchAngle - pointerPosition
                if (cwDistanceFromPointer < 0) {
                    cwDistanceFromPointer += 360F
                }

                // Represents the counter-clockwise distance from pointerPosition to the touch angle.
                val ccwDistanceFromPointer = 360F - cwDistanceFromPointer

                // These are only used for ACTION_DOWN for handling if the pointer was the part that was touched
                val pointerRadiusDegrees =
                    (pointerStrokeWidth * 180 / (Math.PI * circleHeight.coerceAtLeast(
                        circleWidth
                    ))).toFloat()
                val pointerDegrees = pointerRadiusDegrees.coerceAtLeast(pointerAngle / 2F)
                // This is for if the first touch is on the actual pointer.
                if (touchEventRadius in innerRadius..outerRadius && (cwDistanceFromPointer <= pointerDegrees || ccwDistanceFromPointer <= pointerDegrees)) {
                    setProgressBasedOnAngle(pointerPosition)
                    recalculateAll()
                    invalidate()
                    onCircularSeekBarChangeListener?.onStartTrackingTouch(this)
                    pointerMoving = true
                    lockAtEnd = false
                    lockAtStart = false
                } else if (cwDistanceFromStart > totalCircleDegrees) {
                    // If the user is touching outside of the start AND end
                    pointerMoving = false
                    return false
                } else if (touchEventRadius in innerRadius..outerRadius) {
                    // If the user is touching near the circle
                    setProgressBasedOnAngle(touchAngle)
                    recalculateAll()
                    invalidate()
                    onCircularSeekBarChangeListener?.onStartTrackingTouch(this)
                    onCircularSeekBarChangeListener?.onProgressChanged(this, progress, true)
                    pointerMoving = true
                    lockAtEnd = false
                    lockAtStart = false
                } else {
                    // If the user is not touching near the circle
                    pointerMoving = false
                    return false
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (pointerMoving) {
                    // Represents the counter-clockwise distance from startAngle to the touch angle.
                    val ccwDistanceFromStart = 360F - cwDistanceFromStart

                    // Represents the clockwise distance from endAngle to the touch angle.
                    var cwDistanceFromEnd = touchAngle - endAngle
                    if (cwDistanceFromEnd < 0) {
                        cwDistanceFromEnd += 360F
                    }

                    val smallInCircle = totalCircleDegrees / 3F
                    var cwPointerFromStart = pointerPosition - startAngle
                    cwPointerFromStart =
                        if (cwPointerFromStart < 0) cwPointerFromStart + 360F else cwPointerFromStart

                    val touchOverStart = ccwDistanceFromStart < smallInCircle
                    val touchOverEnd = cwDistanceFromEnd < smallInCircle
                    val pointerNearStart = cwPointerFromStart < smallInCircle
                    val pointerNearEnd = cwPointerFromStart > totalCircleDegrees - smallInCircle
                    val progressNearZero = progress < max / 3F
                    val progressNearMax = progress > max / 3F * 2F

                    if (progressNearMax) {
                        if (pointerNearStart) {
                            lockAtEnd = touchOverStart
                        } else if (pointerNearEnd) {
                            lockAtEnd = touchOverEnd
                        }
                    } else if (progressNearZero && pointerNearStart) {
                        lockAtStart = touchOverStart
                    }

                    if (lockAtStart && lockEnabled) {
                        progress = 0f
                        recalculateAll()
                        invalidate()
                        onCircularSeekBarChangeListener?.onProgressChanged(this, progress, true)
                    } else if (lockAtEnd && lockEnabled) {
                        progress = max
                        recalculateAll()
                        invalidate()
                        onCircularSeekBarChangeListener?.onProgressChanged(this, progress, true)
                    } else if (touchEventRadius <= outerRadius) {
                        if (cwDistanceFromStart <= totalCircleDegrees) {
                            setProgressBasedOnAngle(touchAngle)
                        }
                        recalculateAll()
                        invalidate()
                        onCircularSeekBarChangeListener?.onProgressChanged(this, progress, true)
                    }
                } else {
                    return false
                }
            }
            MotionEvent.ACTION_UP -> {
                if (pointerMoving) {
                    pointerMoving = false
                    invalidate()
                    onCircularSeekBarChangeListener?.onStopTrackingTouch(this)
                } else {
                    return false
                }
            }
            // Used when the parent view intercepts touches for things like scrolling
            MotionEvent.ACTION_CANCEL -> {
                pointerMoving = false
                invalidate()
            }
        }

        if (event.action == MotionEvent.ACTION_MOVE && parent != null) {
            parent.requestDisallowInterceptTouchEvent(true)
        }

        return true
    }

    fun setupProgress(value: Float) {

        if (progress == value) return

        progress = value
        onCircularSeekBarChangeListener?.onProgressChanged(this, progress, false)
        recalculateAll()
        invalidate()
    }

    fun setupMax(value: Float) {

        if (value < 0) return

        max = value
        if (max <= progress) {
            progress = 0F // If the new max is less than current progress, set progress to zero
            onCircularSeekBarChangeListener?.onProgressChanged(this, progress, false)
        }
        recalculateAll()
        invalidate()
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        val attrArray =
            context.obtainStyledAttributes(attrs, R.styleable.CircularSeekBar, defStyle, 0)

        initAttributes(attrArray)
        attrArray.recycle()
        initPaints()
    }

    private fun initAttributes(attrArray: TypedArray) {
        circleStrokeWidth = attrArray.getDimension(
            R.styleable.CircularSeekBar_circle_stroke_width,
            DEF_CIRCLE_STROKE_WIDTH
        )
        circleColor = attrArray.getColor(R.styleable.CircularSeekBar_circle_color, DEF_CIRCLE_COLOR)
        circleProgressColor = attrArray.getColor(
            R.styleable.CircularSeekBar_circle_progress_color,
            DEF_CIRCLE_PROGRESS_COLOR
        )
        circleFillColor =
            attrArray.getColor(R.styleable.CircularSeekBar_circle_fill, DEF_CIRCLE_FILL_COLOR)
        val circleStyleOrdinal =
            attrArray.getInt(R.styleable.CircularSeekBar_circle_style, DEF_CIRCLE_STYLE)
        arcPaintCapStyle = Paint.Cap.values()[circleStyleOrdinal]

        pointerStrokeWidth = attrArray.getDimension(
            R.styleable.CircularSeekBar_pointer_stroke_width,
            DEF_POINTER_STROKE_WIDTH
        )
        pointerColor =
            attrArray.getColor(R.styleable.CircularSeekBar_pointer_color, DEF_POINTER_COLOR)
        enableInteraction = attrArray.getBoolean(
            R.styleable.CircularSeekBar_enable_interaction,
            DEF_ENABLE_INTERACTION
        )
        lockEnabled =
            attrArray.getBoolean(R.styleable.CircularSeekBar_lock_enabled, DEF_LOCK_ENABLED)

        // Modulo 360 to avoid constant conversion
        startAngle = (360F + attrArray.getFloat(
            R.styleable.CircularSeekBar_start_angle,
            DEF_START_ANGLE
        ) % 360F) % 360F
        endAngle = (360F + attrArray.getFloat(
            R.styleable.CircularSeekBar_end_angle,
            DEF_END_ANGLE
        ) % 360F) % 360F

        if (startAngle == endAngle) {
            endAngle -= SMALL_DEGREE_BIAS
        }

        if (!enableInteraction) {
            pointerStrokeWidth = 0F
        }

        progress = attrArray.getInt(R.styleable.CircularSeekBar_progress, DEF_PROGRESS).toFloat()
        max = attrArray.getInt(R.styleable.CircularSeekBar_max, DEF_MAX).toFloat()
    }

    private fun initPaints() {
        with(circlePaint) {
            isAntiAlias = true
            isDither = true
            color = circleColor
            strokeWidth = circleStrokeWidth
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = arcPaintCapStyle
        }

        with(circleFillPaint) {
            isAntiAlias = true
            isDither = true
            color = circleFillColor
            strokeWidth = circleStrokeWidth
            strokeJoin = Paint.Join.ROUND
            strokeCap = arcPaintCapStyle
            style = Paint.Style.FILL_AND_STROKE
        }

        initCircleProgressPaint()

        with(pointerPaint) {
            isAntiAlias = true
            isDither = true
            color = pointerColor
            style = Paint.Style.STROKE
            strokeWidth = pointerStrokeWidth
            strokeJoin = Paint.Join.ROUND
            strokeCap = arcPaintCapStyle
        }
    }

    private fun initCircleProgressPaint() {
        with(circleProgressPaint) {
            isAntiAlias = true
            isDither = true
            color = circleProgressColor
            style = Paint.Style.STROKE
            strokeWidth = circleStrokeWidth
            strokeJoin = Paint.Join.ROUND
            strokeCap = arcPaintCapStyle
        }
    }

    // Calculate total degrees between startAngle and endAngle
    private fun calculateTotalDegrees() {
        // Length of the entire circle/arc
        totalCircleDegrees = (360F - (startAngle - endAngle)) % 360F
        if (totalCircleDegrees <= 0F) {
            totalCircleDegrees = 360F
        }
    }

    // Calculate progress degrees (aka sweep angle).
    private fun calculateProgressDegrees() {
        progressDegrees = pointerPosition - startAngle
        if (progressDegrees < 0) {
            progressDegrees += 360F
        }
    }

    // Calculate the pointer position (and the end of the progress arc) in degrees.
    private fun calculatePointerPosition() {
        val progressPercent = progress / max
        val progressDegree = progressPercent * totalCircleDegrees
        pointerPosition = startAngle + progressDegree
        pointerPosition =
            (if (pointerPosition < 0) 360F + pointerPosition else pointerPosition) % 360F
    }

    private fun calculatePointerXYPosition() {
        var pm = PathMeasure(circleProgressPath, false)
        val returnValue = pm.getPosTan(pm.length, pointerPositionXY, null)
        if (!returnValue) {
            pm = PathMeasure(circlePath, false)
            pm.getPosTan(0f, pointerPositionXY, null)
        }
    }

    private fun resetPaths() {
        with(circlePath) {
            reset()
            addArc(circleRectF, startAngle, totalCircleDegrees)
        }

        // draw an extra arc to match the pointer arc.
        val extendStart = startAngle - pointerAngle / 2.0F
        var extendDegrees = (progressDegrees + pointerAngle)
        if (extendDegrees >= 360F) {
            extendDegrees = 360F - SMALL_DEGREE_BIAS
        }
        with(circleProgressPath) {
            reset()
            addArc(circleRectF, extendStart, extendDegrees)
        }

        val pointerStart = pointerPosition - pointerAngle / 2.0F
        with(circlePointerPath) {
            reset()
            addArc(circleRectF, pointerStart, pointerAngle)
        }
    }

    private fun resetRect() {
        circleRectF.set(-circleWidth, -circleHeight, circleWidth, circleHeight)
    }

    private fun setProgressBasedOnAngle(angle: Float) {
        pointerPosition = angle
        calculateProgressDegrees()
        progress = max * progressDegrees / totalCircleDegrees
    }

    private fun recalculateAll() {
        calculateTotalDegrees()
        calculatePointerPosition()
        calculateProgressDegrees()
        resetRect()
        resetPaths()
        calculatePointerXYPosition()
    }

    interface OnCircularSeekBarChangeListener {

        fun onProgressChanged(circularSeekBar: CircularSeekBar, progress: Float, fromUser: Boolean)

        fun onStopTrackingTouch(seekBar: CircularSeekBar)

        fun onStartTrackingTouch(seekBar: CircularSeekBar)
    }
}