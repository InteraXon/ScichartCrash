package com.example.myscichart.my_graphs.graph

import android.content.Context
import android.util.TypedValue
import com.example.myscichart.R
//import com.interaxon.muse.session.data_tracking.TfliteDataTracker
import com.scichart.charting.model.dataSeries.XyDataSeries
import com.scichart.charting.visuals.renderableSeries.FastLineRenderableSeries
import com.scichart.drawing.common.SolidPenStyle
import org.threeten.bp.DateTimeUtils
import java.util.*

class Timeseries(
    val x: ArrayList<Date>,
    val y: ArrayList<Double>,
    val digitalLine: Boolean
) {
    companion object {
        fun create(
            data: ArrayList<Double>,
            digitalLine: Boolean,
            xAxis: XAxis
        ): Timeseries {
            if (data.isEmpty()) {
                return Timeseries(ArrayList(), ArrayList(), digitalLine)
            }

            val startInstant = DateTimeUtils.toInstant(xAxis.startDate)
            val values = if (digitalLine) {
                ArrayList<Double>(data.size * 2 + 1).apply {
                    data.forEach {
                        add(it)
                        add(it)
                    }
                    add(data.last())
                }
            } else {
                data.apply {
                    add(data.last())
                }
            }
            val period = xAxis.seconds.toDouble() / (values.size.toDouble() - 1)

            val xData = ArrayList<Date>(values.size)
                .apply {
                    for (x in 0 until values.size) {
                        add(
                            DateTimeUtils.toDate(
                                startInstant.plusNanos((period * 1000000000 * x).toLong())
                            )
                        )
                    }
                }
            val yData = ArrayList<Double>(values.size)
            for (i in 0 until values.size) {
                if (values[i] >= 0) {
                    yData.add(values[i])
                } else {
                    yData.add(Double.NaN)
                }
            }
            return Timeseries(xData, yData, digitalLine)
        }
    }
}

fun createRenderableSeries(context: Context, timeseries: Timeseries): FastLineRenderableSeries {
    val dataSeries = XyDataSeries(Date::class.javaObjectType, Double::class.javaObjectType)
    val renderableSeries = FastLineRenderableSeries().apply {
        val thickness = if (timeseries.digitalLine) {
            context.resources.getDimension(R.dimen._2sdp)
        } else {
            2f
        }
        strokeStyle = SolidPenStyle(
            TypedValue().also {
                context.theme.resolveAttribute(R.attr.timeseries, it, true)
            }.data, true, thickness, null
        )
        this.dataSeries = dataSeries
        setIsDigitalLine(timeseries.digitalLine)
    }

    dataSeries.append(timeseries.x, timeseries.y)
    return renderableSeries
}

fun createRenderableSeriesForGaps(
    context: Context,
    timeseries: Timeseries,
    fallbackInitialValue: Double
): FastLineRenderableSeries {
    val dataSeries = XyDataSeries(Date::class.javaObjectType, Double::class.javaObjectType)
    val renderableSeries = FastLineRenderableSeries().apply {
        val thickness = if (timeseries.digitalLine) {
            context.resources.getDimension(R.dimen._1sdp)
        } else {
            1f
        }
        strokeStyle = SolidPenStyle(
            TypedValue().also {
                context.theme.resolveAttribute(R.attr.timeseriesGap, it, true)
            }.data, true, thickness, floatArrayOf(5.0f, 5.0f)
        )
        this.dataSeries = dataSeries
        setIsDigitalLine(timeseries.digitalLine)
    }

    dataSeries.append(
        timeseries.x,
        createGaplessTimeseries(fallbackInitialValue, timeseries.y)
    )
    return renderableSeries
}

fun createGaplessTimeseries(
    fallbackInitialValue: Double,
    timeseries: ArrayList<Double>
): ArrayList<Double> {
    val gapless = ArrayList<Double>(timeseries.size)
    var prevNonNan = fallbackInitialValue
    for (value in timeseries) {
        gapless.add(
            if (value.isNaN() || value < 0) {
                prevNonNan
            } else {
                prevNonNan = value
                value
            }
        )
    }
    return gapless
}