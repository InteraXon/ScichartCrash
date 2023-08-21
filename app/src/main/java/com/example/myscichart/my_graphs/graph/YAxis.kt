package com.example.myscichart.my_graphs.graph

import android.content.Context
import androidx.core.content.ContextCompat
import com.scichart.charting.visuals.annotations.AnnotationCoordinateMode
import com.scichart.charting.visuals.annotations.BoxAnnotation
import com.scichart.charting.visuals.axes.AutoRange
import com.scichart.charting.visuals.axes.NumericAxis
import com.scichart.data.model.DoubleRange

const val THRESHOLD_BAR_WIDTH = 0.015

data class ColoredRange(
    val color: Int,
    val start: Double,
    val end: Double
)

fun createYAxis(context: Context, dataRange: Pair<Double, Double>): NumericAxis {
    return NumericAxis(context).apply {
        drawLabels = false
        drawMajorBands = false
        drawMajorTicks = false
        drawMinorTicks = false
        drawMajorGridLines = false
        drawMinorGridLines = false
        isLabelCullingEnabled = false
        visibleRange = DoubleRange(
            dataRange.first,
            dataRange.second
        )
        autoRange = AutoRange.Never
    }
}

fun createColoredRangeAnnotations(
    context: Context,
    coloredRanges: List<ColoredRange>
): List<BoxAnnotation> {
    return coloredRanges.map {
        BoxAnnotation(context).apply {
            x1 = 0; y1 = it.start; x2 = THRESHOLD_BAR_WIDTH; y2 = it.end
            setBackgroundColor(ContextCompat.getColor(context, it.color))
            coordinateMode = AnnotationCoordinateMode.RelativeX
        }
    }
}