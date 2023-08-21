package com.example.myscichart.my_graphs.graph

import android.content.Context
import com.example.myscichart.R
import com.example.myscichart.my_graphs.getColorFromAttr
import com.scichart.charting.visuals.axes.DateAxis
import com.scichart.core.utility.DateIntervalUtil
import com.scichart.data.model.DateRange
import com.scichart.drawing.common.SolidPenStyle
import org.threeten.bp.DateTimeUtils
import org.threeten.bp.OffsetDateTime
import java.lang.Long.max
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

class XAxis(
    val xLabelling: XLabelling,
    private val sessionStart: OffsetDateTime,
    val seconds: Int
) {
    enum class XLabelling {
        ABSOLUTE_TIME,
        RELATIVE_TIME,
    }

    companion object {
        const val MIN_SECONDS = 60L
        private val ZERO_TIME = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val startDate: Date
        get() {
            return when (xLabelling) {
                XLabelling.ABSOLUTE_TIME -> DateTimeUtils.toDate(sessionStart.toInstant())
                XLabelling.RELATIVE_TIME -> Date(ZERO_TIME)
            }
        }

    val endDate: Date
        get() {
            return when (xLabelling) {
                XLabelling.ABSOLUTE_TIME -> DateTimeUtils.toDate(
                    sessionStart.toInstant().plusSeconds(max(seconds.toLong(), MIN_SECONDS))
                )

                XLabelling.RELATIVE_TIME -> Date(
                    ZERO_TIME + max(
                        seconds.toLong(),
                        MIN_SECONDS
                    ) * 1000
                )
            }
        }
}

fun createXAxis(context: Context, xAxis: XAxis): DateAxis {
    return DateAxis(context).apply {
        drawLabels = true
        drawMajorBands = false
        drawMajorTicks = true
        drawMinorTicks = false
        drawMajorGridLines = false
        minorDelta = Date(DateIntervalUtil.fromHours(1.0))
        autoTicks = true
        drawMinorGridLines = false
        isLabelCullingEnabled = false
        autoFitMarginalLabels = true
        minimalZoomConstrain = DateIntervalUtil.fromSeconds(XAxis.MIN_SECONDS.toDouble())
        /*tickLabelStyle = FontStyle(
            TypefaceManager.obtainTypeface(context, CustomTypeface.PROXIMA_NOVA_REG),
            TypefaceManager.obtainTypeface(context, CustomTypeface.MUSEO_SANS_ROUNDED_100),
            context.resources.getDimension(R.dimen.results_screen_graph_labels_text_size),
            context.getColorFromAttr(R.attr.graphXAxis),
            true
        )*/
        majorTickLineStyle = SolidPenStyle(
            context.getColorFromAttr(R.attr.graphXAxis),
            true,
            2f,
            null
        )
        textFormatting = getLabelTextFormatting(xAxis)
        subDayTextFormatting = getLabelTextFormatting(xAxis)

        val thresholdBarWidthMillis =
            (xAxis.endDate.time - xAxis.startDate.time) * THRESHOLD_BAR_WIDTH
        val start = (xAxis.startDate.time - thresholdBarWidthMillis).toLong()
        visibleRange = DateRange(Date(start), xAxis.endDate)
    }
}

fun getLabelTextFormatting(xAxis: XAxis): String {
    return when (xAxis.xLabelling) {
        XAxis.XLabelling.ABSOLUTE_TIME -> {
            when {
                xAxis.seconds > TimeUnit.SECONDS.convert(4, TimeUnit.HOURS) -> {
                    "h a"
                }

                xAxis.seconds <= TimeUnit.SECONDS.convert(4, TimeUnit.MINUTES) -> {
                    "h:mm:ss a"
                }

                else -> {
                    "h:mm a"
                }
            }
        }

        XAxis.XLabelling.RELATIVE_TIME -> {
            if (xAxis.seconds >= TimeUnit.SECONDS.convert(1, TimeUnit.HOURS)) {
                "H:mm"
            } else {
                "m:ss"
            }
        }
    }
}