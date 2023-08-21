package com.example.myscichart.my_graphs.graph

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat

import com.example.myscichart.R
import com.example.myscichart.my_graphs.appLocale
import com.example.myscichart.my_graphs.getColorFromAttr

import com.scichart.charting.model.dataSeries.XyyDataSeries
import com.scichart.charting.visuals.annotations.AnnotationCoordinateMode
import com.scichart.charting.visuals.annotations.BoxAnnotation
import com.scichart.charting.visuals.annotations.CustomAnnotation
import com.scichart.charting.visuals.annotations.HorizontalAnchorPoint
import com.scichart.charting.visuals.annotations.VerticalAnchorPoint
import com.scichart.charting.visuals.renderableSeries.FastBandRenderableSeries
import com.scichart.drawing.common.SolidBrushStyle
import com.scichart.drawing.common.SolidPenStyle
import java.util.Date
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

fun createThresholdAnnotations(
    context: Context,
    dataRange: Pair<Double, Double>,
    thresholds: List<Double>
): List<BoxAnnotation> {
    return thresholds.map {
        BoxAnnotation(context).apply {
            val y = 1 - (it - dataRange.first) / (dataRange.second - dataRange.first)
            x1 = 0; y1 = y; x2 = 1; y2 = y + 0.001f
            setBackgroundColor(ContextCompat.getColor(context, R.color.results_graph_axis))
            coordinateMode = AnnotationCoordinateMode.Relative
        }
    }
}

fun createMinMaxBpmAnnotations(
    context: Context,
    timeseries: Timeseries,
    xAxis: XAxis,
    dataRange: Pair<Double, Double>,
    showMax: Boolean = true
): List<CustomAnnotation> {
    var maxPoint: Pair<Date, Double>? = null
    var minPoint: Pair<Date, Double>? = null
    for (i in 0 until timeseries.x.size) {
        if (timeseries.y[i].isNaN()) {
            continue
        }
        if (maxPoint == null || maxPoint.second < timeseries.y[i]) {
            maxPoint = Pair(timeseries.x[i], timeseries.y[i])
        } else if (minPoint == null || minPoint.second > timeseries.y[i]) {
            minPoint = Pair(timeseries.x[i], timeseries.y[i])
        }
    }
    val visibleRange = xAxis.seconds

    val lastPoint = with(timeseries.x) {
        if (this.isNotEmpty()) {
            this.last().time
        } else {
            return emptyList()
        }
    }
    val annotations = ArrayList<CustomAnnotation>()
    if (showMax) {
        maxPoint?.let {
            annotations.addAll(
                createHeartBpmAnnotation(
                    context,
                    it.first,
                    it.second,
                    true,
                    lastPoint,
                    visibleRange,
                    dataRange
                )
            )
        }
    }
    minPoint?.let {
        annotations.addAll(
            createHeartBpmAnnotation(
                context,
                it.first,
                it.second,
                false,
                lastPoint,
                visibleRange,
                dataRange
            )
        )
    }
    return annotations
}

private fun createHeartBpmAnnotation(
    context: Context,
    x: Date,
    y: Double,
    isMaxPoint: Boolean,
    lastPointEpochMillis: Long,
    xVisibleRangeDiff: Int,
    dataRange: Pair<Double, Double>
): List<CustomAnnotation> {
    val pointDrawable = if (isMaxPoint) {
        R.drawable.red_circle_point
    } else {
        R.drawable.green_circle_point
    }
    val yVisibleRangeDiff = dataRange.second - dataRange.first

    return listOf(CustomAnnotation(context).apply {
        setContentView(ImageView(context).apply {
            setImageResource(pointDrawable)
        })
        horizontalAnchorPoint = HorizontalAnchorPoint.Center
        verticalAnchorPoint = VerticalAnchorPoint.Center
        x1 = x.time
        y1 = y + yVisibleRangeDiff * 0.01
        coordinateMode = AnnotationCoordinateMode.Absolute
    }, CustomAnnotation(context).apply {
        setContentView(TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setTextColor(context.getColorFromAttr(R.attr.primarySurfaceText))
            text = TextUtils.concat(
                y.roundToInt().toString(), "\n",
                context.getString(R.string.session_flow_review_heart_graph_units)
                    .lowercase(appLocale())
            )
            gravity = Gravity.CENTER_HORIZONTAL
        })
        x1 = if (x.time + xVisibleRangeDiff * 1000 * 0.1 > lastPointEpochMillis) {
            (x.time - xVisibleRangeDiff * 1000 * 0.1).toLong()
        } else {
            x.time + xVisibleRangeDiff * 1000 * 0.01
        }
        y1 = if (isMaxPoint) min(y + 20, dataRange.second) else max(
            y - 3,
            dataRange.first + 20
        )
        coordinateMode = AnnotationCoordinateMode.Absolute
    })
}

fun createHeartHistoricalMinMaxSeries(
    context: Context,
    xAxis: XAxis,
    historicalMin: Double,
    historicalMax: Double
): FastBandRenderableSeries {
    val historicalHighLowDataSeries = XyyDataSeries(
        Date::class.javaObjectType,
        Double::class.javaObjectType
    ).apply {
        append(xAxis.startDate, historicalMax, historicalMin)
        append(xAxis.endDate, historicalMax, historicalMin)
    }
    return FastBandRenderableSeries().apply {
        val dashPolidPenStyle =
            SolidPenStyle(
                context.getColorFromAttr(R.attr.heartHistoricalAvg),
                true,
                2f,
                floatArrayOf(10f, 7f)
            )
        strokeStyle = dashPolidPenStyle
        strokeY1Style = dashPolidPenStyle
        fillBrushStyle = SolidBrushStyle(Color.TRANSPARENT)
        fillY1BrushStyle = SolidBrushStyle(Color.TRANSPARENT)

        dataSeries = historicalHighLowDataSeries
    }
}

fun createHistoricalAverageMinMaxSeries(
    context: Context,
    xAxis: XAxis,
    historicalMin: Double,
    historicalMax: Double
): FastBandRenderableSeries {
    val historicalColor = context.getColorFromAttr(R.attr.heartHistoricalAvg)
    val averageDataSeries = XyyDataSeries(
        Date::class.javaObjectType,
        Double::class.javaObjectType
    ).apply {
        append(xAxis.startDate, historicalMax, historicalMin)
        append(xAxis.endDate, historicalMax, historicalMin)
    }
    return FastBandRenderableSeries().apply {
        strokeStyle = SolidPenStyle(Color.TRANSPARENT, false, 0f, null)
        strokeY1Style = SolidPenStyle(Color.TRANSPARENT, false, 0f, null)
        fillBrushStyle = SolidBrushStyle(historicalColor)
        fillY1BrushStyle = SolidBrushStyle(historicalColor)

        dataSeries = averageDataSeries
    }
}

fun createBirdAnnotations(
    context: Context,
    xAxis: XAxis,
    birdTimes: List<Int>
): List<CustomAnnotation> {
    return birdTimes.map {
        xAxis.startDate.time.plus(it * DateUtils.SECOND_IN_MILLIS)
    }.map {
        createCustomAnnotation(
            context,
            context.resources.getDimension(R.dimen.results_screen_graph_annotation_icon_size)
                .toInt(), it, R.drawable.ic_bird
        ).apply {
            horizontalAnchorPoint = HorizontalAnchorPoint.Center
        }
    }
}

fun createRecoveryAnnotations(
    context: Context,
    xAxis: XAxis,
    recoveryTimes: List<Int>
): List<CustomAnnotation> {
    return recoveryTimes.map {
        xAxis.startDate.time.plus(it * DateUtils.SECOND_IN_MILLIS)
    }.map {
        createCustomAnnotation(
            context,
            context.resources.getDimension(R.dimen.results_screen_graph_annotation_icon_size)
                .toInt(), it, R.drawable.ic_recoveries
        ).apply {
            horizontalAnchorPoint = HorizontalAnchorPoint.Center
        }
    }
}

private fun createCustomAnnotation(
    context: Context,
    annotationWidth: Int,
    timestamp: Long,
    imageResource: Int,
    biasDimenId: Int = R.dimen.results_screen_graph_annotation_bias
): CustomAnnotation {
    val value = TypedValue()
    context.resources.getValue(biasDimenId, value, true)
    val annotationBias = value.float

    val imageView = ImageView(context).apply {
        layoutParams = ViewGroup.LayoutParams(annotationWidth, annotationWidth)
        scaleType = ImageView.ScaleType.FIT_CENTER
        setImageResource(imageResource)
    }

    return CustomAnnotation(context).apply {
        setContentView(imageView)
        x1 = Date(timestamp)
        y1 = annotationBias
        coordinateMode = AnnotationCoordinateMode.RelativeY
    }
}