package com.example.myscichart.my_graphs.graph

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.cardview.widget.CardView
import androidx.core.view.allViews
import androidx.databinding.DataBindingUtil
import com.example.myscichart.R
import com.example.myscichart.databinding.ViewGraphCardBinding
import com.example.myscichart.my_graphs.SessionLength.secondsToHoursMinutesOrMinutesSeconds
import org.threeten.bp.OffsetDateTime
import kotlin.math.max
import kotlin.math.min


class GraphCardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    enum class MetricType {
        PERCENTAGE,
        BPM,
        TIME,
        POINTS,
    }

    data class Data(
        val type: MetricType,
        val metric: Int,
        val title: String,
        val description: String,
        val percentageColor: Int,
        val unit: String? = "%"
    ) {
        val metricValue: String
            get() {
                return when (type) {
                    MetricType.BPM,
                    MetricType.PERCENTAGE -> metric.toString() + unit

                    MetricType.TIME -> secondsToHoursMinutesOrMinutesSeconds(metric)
                    MetricType.POINTS -> metric.toString()
                }
            }
    }

    companion object SessionConsts {
        private const val ANNOTATION_PADDING_FACTOR = 0.15


        private const val MIND_MIN = 0.0
        private const val MIND_MAX = 1.0
        const val MIND_CALM = 0.33
        const val MIND_ACTIVE = 0.67
        private const val BODY_MIN = 0.0
        private const val BODY_MAX = 1.0
        private const val BODY_STILL = 0.35

        fun createMindGraphCardView(
            context: Context,
            timeseries: List<Double>,
            birdTimes: List<Int>,
            recoveryTimes: List<Int>,
            calmPercentage: Int,
            dataTrackingStart: OffsetDateTime,
            completedSeconds: Int,
            xLabelling: XAxis.XLabelling
        ): GraphCardView {
            val c = GraphCardView(context).apply {
                setData(
                    Data(
                        MetricType.PERCENTAGE,
                        calmPercentage,
                        context.getString(R.string.sleep_session_results_mind_graph_title),
                        context.getString(R.string.sleep_session_results_mind_deep_focus),
                        R.color.mind_results_graph_calm_color
                    )
                )
                addTimeseriesView(
                    TimeseriesView(context).apply {
                        xAxis = XAxis(xLabelling, dataTrackingStart, completedSeconds)
                        this.timeseries =
                            Timeseries.create(ArrayList(timeseries), false, xAxis)
                        this.birdTimes = birdTimes
                        this.recoveryTimes = recoveryTimes
                        dataRange = Pair(
                            if (birdTimes.isEmpty() && recoveryTimes.isEmpty()) {
                                MIND_MIN
                            } else {
                                MIND_MIN - (MIND_MAX - MIND_MIN) * ANNOTATION_PADDING_FACTOR
                            },
                            MIND_MAX
                        )
                        coloredRanges = listOf(
                            ColoredRange(
                                R.color.mind_results_graph_calm_color,
                                MIND_MIN,
                                MIND_CALM
                            ),
                            ColoredRange(
                                R.color.mind_results_graph_neutral_color,
                                MIND_CALM,
                                MIND_ACTIVE
                            ),
                            ColoredRange(
                                R.color.mind_results_graph_active_color,
                                MIND_ACTIVE,
                                MIND_MAX
                            )
                        )
                        thresholds = listOf(
                            MIND_CALM,
                            MIND_ACTIVE
                        )
                        enableCalibrationGapAnnotation = completedSeconds < 5 * 60
                        build()
                    },
                    resources.getDimension(R.dimen.graph_at_a_glance_height)
                        .toInt()
                )
            }

            return c
        }

        fun createBodyGraphCardView(
            context: Context,
            timeseries: List<Double>,
            birdTimes: List<Int>,
            recoveryTimes: List<Int>,
            bodyRelaxedPercentage: Int,
            dataTrackingStart: OffsetDateTime,
            completedSeconds: Int,
            xLabelling: XAxis.XLabelling
        ): GraphCardView {
            val b = GraphCardView(context).apply {
                setData(
                    Data(
                        MetricType.PERCENTAGE,
                        bodyRelaxedPercentage,
                        context.getString(R.string.sleep_session_results_graph_detail_body_title),
                        context.getString(R.string.sleep_session_results_graph_detail_body_still),
                        R.color.body_orange
                    )
                )
                addTimeseriesView(
                    TimeseriesView(context).apply {
                        xAxis = XAxis(xLabelling, dataTrackingStart, completedSeconds)
                        this.timeseries =
                            Timeseries.create(java.util.ArrayList(timeseries), false, xAxis)
                        this.birdTimes = birdTimes
                        this.recoveryTimes = recoveryTimes
                        dataRange = Pair(
                            if (birdTimes.isEmpty() && recoveryTimes.isEmpty()) {
                                SessionConsts.BODY_MIN
                            } else {
                                BODY_MIN - (BODY_MAX - BODY_MIN) * ANNOTATION_PADDING_FACTOR
                            },
                            BODY_MAX
                        )
                        coloredRanges = listOf(
                            ColoredRange(
                                R.color.body_orange,
                                BODY_MIN,
                                BODY_STILL
                            ),
                            ColoredRange(
                                R.color.body_result_active,
                                BODY_STILL,
                                BODY_MAX
                            )
                        )
                        thresholds = listOf(BODY_STILL)
                        build()
                    },
                    resources.getDimension(R.dimen.graph_at_a_glance_height)
                        .toInt()
                )
            }

            return b
        }

        fun createHeartGraphCardView(
            context: Context,
            timeseries: List<Double>,
            birdTimes: List<Int>,
            recoveryTimes: List<Int>,
            averageHeartRate: Int,
            minMaxAnnotations: TimeseriesView.MinMaxAnnotations,
            historicalDataVisible: Boolean,
            dataTrackingStart: OffsetDateTime,
            completedSeconds: Int,
            xLabelling: XAxis.XLabelling,
            heartHistoricalAbsMinRate: Double?,
            heartHistoricalAbsMaxRate: Double?,
            heartHistoricalAvgMinRate: Double?,
            heartHistoricalAvgMaxRate: Double?
        ): GraphCardView {


            val a = GraphCardView(context).apply {
                setData(
                    Data(
                        MetricType.BPM,
                        averageHeartRate,
                        context.getString(R.string.sleep_session_results_heart_graph_title),
                        context.getString(R.string.sleep_session_results_avg_heart_rate),
                        R.color.heart_red,
                        " " + context.getString(R.string.sleep_session_results_heart_graph_units)
                    )
                )
                addTimeseriesView(
                    TimeseriesView(context).apply {
                        xAxis = XAxis(xLabelling, dataTrackingStart, completedSeconds)
                        this.timeseries =
                            Timeseries.create(java.util.ArrayList(timeseries), false, xAxis)
                        this.birdTimes = birdTimes
                        this.recoveryTimes = recoveryTimes
                        val heartMin = min(50.0, timeseries.minOrNull() ?: 50.0).minus(20)
                        val heartMax = max(90.0, timeseries.maxOrNull() ?: 90.0).plus(20)
                        dataRange = Pair(
                            if (birdTimes.isEmpty() && recoveryTimes.isEmpty()) {
                                heartMin
                            } else {
                                heartMin - (heartMax - heartMin) * ANNOTATION_PADDING_FACTOR
                            },
                            heartMax
                        )
                        coloredRanges = listOf(
                            ColoredRange(
                                R.color.heart_red,
                                heartMin,
                                heartMax
                            )
                        )
                        thresholds = listOf(
                            heartMin + (heartMax - heartMin) / 3.0,
                            heartMin + (heartMax - heartMin) / 3.0 * 2.0
                        )
                        this.minMaxAnnotations = minMaxAnnotations
                        if (historicalDataVisible) {
                            addHistoricalAnnotations(
                                xAxis,
                                heartHistoricalAbsMinRate,
                                heartHistoricalAbsMaxRate,
                                heartHistoricalAvgMinRate,
                                heartHistoricalAvgMaxRate
                            )
                        }
                        build()
                    },
                    resources.getDimension(R.dimen.graph_at_a_glance_height)
                        .toInt()
                )
            }

            return a
        }

    }

    private val binding: ViewGraphCardBinding = DataBindingUtil.inflate(
        LayoutInflater.from(context),
        R.layout.view_graph_card,
        this, true
    )

    fun setData(data: Data) {
        binding.data = data
    }

    fun addTimeseriesView(graph: TimeseriesView, pxHeight: Int) {
        binding.resultsPreviewGraph.addView(graph)
        binding.resultsPreviewGraph.layoutParams = binding.resultsPreviewGraph.layoutParams.apply {
            this.height = pxHeight
        }
    }

    fun getTimeseriesView(): TimeseriesView? {
        return binding.resultsPreviewGraph.allViews.find { it is TimeseriesView } as? TimeseriesView
    }
}