package com.example.myscichart.my_graphs.graph

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.example.myscichart.R
import com.example.myscichart.my_graphs.graph.XAxis.Companion.MIN_SECONDS
import com.scichart.charting.ClipMode
import com.scichart.charting.ClipModeTarget
import com.scichart.charting.Direction2D
import com.scichart.charting.modifiers.AxisDragModifierBase
import com.scichart.charting.modifiers.PinchZoomModifier
import com.scichart.charting.modifiers.XAxisDragModifier
import com.scichart.charting.visuals.SciChartSurface
import com.scichart.charting.visuals.annotations.AnnotationCoordinateMode
import com.scichart.charting.visuals.annotations.BoxAnnotation
import com.scichart.charting.visuals.annotations.LineAnnotation
import com.scichart.charting.visuals.axes.DateAxis
import com.scichart.charting.visuals.axes.IAxis
import com.scichart.data.model.DateRange
import com.scichart.drawing.common.SolidPenStyle
import com.scichart.extensions.builders.SciChartBuilder
import java.lang.ref.WeakReference
import java.util.Date
import java.util.concurrent.TimeUnit


class TimeseriesView(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface GestureCallback {
        fun onPerformingZoom(point: PointF, xValue: Double, yValue: Double)
        fun onFinishedZoom()
        fun onPerformingPan(xDelta: Float, yDelta: Float, isSecondHalf: Boolean)
        fun onFinishedPan()
    }

    fun interface ZoomInstructionCallback {
        fun onZoomInstructionUpdated(showZoomIn: Boolean)
    }

    private class CustomPinchZoomModifier(
        val gestureCallback: WeakReference<GestureCallback>,
        val zoomInstructionCallback: ZoomInstructionCallback
    ) : PinchZoomModifier() {

        private var isZoomInVisible = true

        override fun performZoom(point: PointF, xValue: Double, yValue: Double) {
            gestureCallback.get()?.onPerformingZoom(point, xValue, yValue)
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            super.onScaleEnd(detector)
            gestureCallback.get()?.onFinishedZoom()
        }

        fun zoom(point: PointF, xValue: Double, yValue: Double) {
            when (val axis = xAxis) {
                is DateAxis -> {
                    if (isCompletelyZoomedOut(axis)) {
                        if (xValue < 0) {
                            super.performZoom(point, xValue, yValue)
                        }
                        if (!isZoomInVisible) {
                            isZoomInVisible = true
                            zoomInstructionCallback.onZoomInstructionUpdated(isZoomInVisible)
                        }
                    } else if (isCompletelyZoomedIn(axis)) {
                        super.performZoom(point, xValue, yValue)
                        if (isZoomInVisible) {
                            isZoomInVisible = false
                            zoomInstructionCallback.onZoomInstructionUpdated(isZoomInVisible)
                        }
                    } else {
                        super.performZoom(point, xValue, yValue)
                        if (!isZoomInVisible) {
                            isZoomInVisible = true
                            zoomInstructionCallback.onZoomInstructionUpdated(isZoomInVisible)
                        }
                    }
                }
            }
        }

        private fun isCompletelyZoomedOut(axis: DateAxis): Boolean {
            return axis.visibleRange.min <= axis.dataRange.min
                    && axis.visibleRange.max >= axis.dataRange.max
        }

        private fun isCompletelyZoomedIn(axis: DateAxis): Boolean {
            val visibleWindowMs = axis.visibleRange.max.time - axis.visibleRange.min.time
            val minWindowMs = ((MIN_SECONDS * 1000) + 1000)
            return visibleWindowMs <= minWindowMs
        }
    }

    private class CustomXAxisDragModifier(
        val gestureCallback: WeakReference<GestureCallback>
    ) : XAxisDragModifier() {

        override fun performPan(xDelta: Float, yDelta: Float, isSecondHalf: Boolean, axis: IAxis) {
            gestureCallback.get()?.onPerformingPan(xDelta, yDelta, isSecondHalf)
        }

        override fun onUp(e: MotionEvent?) {
            super.onUp(e)
            gestureCallback.get()?.onFinishedPan()
        }

        fun pan(xDelta: Float, yDelta: Float, isSecondHalf: Boolean) {
            super.performPan(xDelta, yDelta, isSecondHalf, xAxis)
        }
    }

    enum class MinMaxAnnotations {
        MIN_MAX,
        MIN_ONLY,
        NONE
    }

    lateinit var xAxis: XAxis
    lateinit var dataRange: Pair<Double, Double>
    lateinit var coloredRanges: List<ColoredRange>
    lateinit var thresholds: List<Double>
    lateinit var timeseries: Timeseries
    var enableCalibrationGapAnnotation = false
    var birdTimes: List<Int> = emptyList()
    var recoveryTimes: List<Int> = emptyList()
    var minMaxAnnotations = MinMaxAnnotations.NONE
    var gapFallbackValue: Double? = null
    private var zoomModifier: CustomPinchZoomModifier? = null
    private var panModifier: CustomXAxisDragModifier? = null

    val zoomingEnabled: Boolean
        get() {
            return xAxis.seconds > MIN_SECONDS
        }

    private val surface by lazy {
        SciChartSurface(context).apply {
            setBackgroundColor(
                TypedValue().also {
                    context.theme.resolveAttribute(R.attr.frontSurface, it, true)
                }.data
            )
            renderableSeriesAreaBorderStyle = null
        }
    }

    fun performZoom(point: PointF, xValue: Double, yValue: Double) {
        zoomModifier?.zoom(point, xValue, yValue)
    }

    fun performPan(xDelta: Float, yDelta: Float, isSecondHalf: Boolean) {
        panModifier?.pan(xDelta, yDelta, isSecondHalf)
    }

    fun build() {
        surface.xAxes.add(createXAxis(context, xAxis))
        surface.yAxes.add(createYAxis(context, dataRange))
        surface.annotations.addAll(createThresholdAnnotations(context, dataRange, thresholds))
        surface.annotations.addAll(createColoredRangeAnnotations(context, coloredRanges))
        gapFallbackValue?.let {
            surface.renderableSeries.add(createRenderableSeriesForGaps(context, timeseries, it))
        }
        val series = createRenderableSeries(context, timeseries)
        surface.renderableSeries.add(series)
        when (minMaxAnnotations) {
            MinMaxAnnotations.MIN_MAX -> surface.annotations.addAll(
                createMinMaxBpmAnnotations(
                    context,
                    timeseries,
                    xAxis,
                    dataRange,
                    showMax = true
                )
            )

            MinMaxAnnotations.MIN_ONLY -> surface.annotations.addAll(
                createMinMaxBpmAnnotations(
                    context,
                    timeseries,
                    xAxis,
                    dataRange,
                    showMax = false
                )
            )

            MinMaxAnnotations.NONE -> {
            }
        }
        surface.annotations.add(LineAnnotation(context).apply {
            x1 = 0; y1 = 1; x2 = 1; y2 = 1
            val axisColor = ContextCompat.getColor(context, R.color.results_graph_axis)
            stroke = SolidPenStyle(axisColor, false, 2f, null)
            coordinateMode = AnnotationCoordinateMode.Relative
        })

        if (enableCalibrationGapAnnotation) {
            when (val date = findMiddleOfCalibrationGap()) {
                null -> {}
                else -> {
                    addHiddenCalibrationAnnotation(date)
                }
            }
        }
        surface.annotations.addAll(createBirdAnnotations(context, xAxis, birdTimes))
        surface.annotations.addAll(createRecoveryAnnotations(context, xAxis, recoveryTimes))

        addView(surface, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        SciChartBuilder.instance()
            .newAnimator(series)
            .withSweepTransformation()
            .withInterpolator(AccelerateDecelerateInterpolator())
            .withDuration(1000L)
            .withStartDelay(50L).start()
    }

    fun setupGestures(
        gestureCallback: GestureCallback,
        zoomInstructionCallback: ZoomInstructionCallback,
    ) {
        if (zoomingEnabled) {
            setupGestures(gestureCallback, zoomInstructionCallback, surface)
        }
    }

    private fun setupGestures(
        gestureCallback: GestureCallback,
        zoomInstructionCallback: ZoomInstructionCallback,
        surface: SciChartSurface
    ) {
        zoomModifier = CustomPinchZoomModifier(
            WeakReference(gestureCallback),
            zoomInstructionCallback
        ).apply {
            direction = Direction2D.XDirection
        }
        panModifier = CustomXAxisDragModifier(WeakReference(gestureCallback)).apply {
            dragMode = AxisDragModifierBase.AxisDragMode.Pan
            minTouchArea = 1000.0f
            clipModeX = ClipMode.ClipAtExtents
            clipModeTargetX = ClipModeTarget.VisibleRangeLimit
        }
        surface.chartModifiers.add(panModifier)
        surface.chartModifiers.add(zoomModifier)
    }

    private fun findMiddleOfCalibrationGap(): Date? {
        if (timeseries.x.isEmpty() || timeseries.x.size != timeseries.y.size) {
            return null
        }
        if (!timeseries.y[0].isNaN() && timeseries.y[0] >= 0) {
            return null
        }

        var index = 0
        while (timeseries.y[index].isNaN() || timeseries.y[index] < 0) {
            index++
            if (index >= timeseries.y.size - 1) {
                break
            }
        }
        return timeseries.x[index / 2]
    }

    fun addHistoricalAnnotations(
        xAxis: XAxis,
        heartHistoricalAbsMinRate: Double?,
        heartHistoricalAbsMaxRate: Double?,
        heartHistoricalAvgMinRate: Double?,
        heartHistoricalAvgMaxRate: Double?
    ) {
        if (heartHistoricalAbsMinRate != null && heartHistoricalAbsMaxRate != null) {
            surface.renderableSeries.add(
                context?.let {
                    createHeartHistoricalMinMaxSeries(
                        it,
                        xAxis,
                        heartHistoricalAbsMinRate,
                        heartHistoricalAbsMaxRate
                    )
                }
            )
        }
        if (heartHistoricalAvgMinRate != null && heartHistoricalAvgMaxRate != null) {
            surface.renderableSeries.add(
                context?.let {
                    createHistoricalAverageMinMaxSeries(
                        it,
                        xAxis,
                        heartHistoricalAvgMinRate,
                        heartHistoricalAvgMaxRate
                    )
                }
            )
        }
    }

    private fun addHiddenCalibrationAnnotation(xBase: Date) {
        val visibleRangeSeconds = (surface.xAxes[0].visibleRange.diff as Date).time / 1000
        val iconVisibilityThreshold = TimeUnit.MINUTES.toSeconds(10)
        if (iconVisibilityThreshold < visibleRangeSeconds) return

        val halfIconHeight = (GraphCardView.MIND_ACTIVE - GraphCardView.MIND_CALM) / 2
        val halfIconWidth = 30000

        val yBase = GraphCardView.MIND_CALM + halfIconHeight

        val hiddenCalibrationAnnotation = BoxAnnotation(context).apply {
            x1 = Date(xBase.time - halfIconWidth)
            x2 = Date(xBase.time + halfIconWidth)
            y1 = yBase - halfIconHeight
            y2 = yBase + halfIconHeight
            coordinateMode = AnnotationCoordinateMode.Absolute
            setContentView(ImageView(context).apply {
                setImageResource(R.drawable.ic_gap)
                scaleType = ImageView.ScaleType.FIT_CENTER
            })
        }
        surface.annotations.add(hiddenCalibrationAnnotation)
    }

    fun setVisibleRange(start: Date) {
        surface.xAxes.forEach {
            when (it) {
                is DateAxis -> it.visibleRange = DateRange(start, xAxis.endDate)
            }
        }
    }
}


