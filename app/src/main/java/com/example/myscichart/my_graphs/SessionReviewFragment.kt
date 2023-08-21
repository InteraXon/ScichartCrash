package com.example.myscichart.my_graphs

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myscichart.GlobalConstant
import com.example.myscichart.R
import com.example.myscichart.SessionObj1
import com.example.myscichart.SessionObj2
import com.example.myscichart.databinding.ContentMeditateBodyLegendBinding
import com.example.myscichart.databinding.ContentMeditateHeartLegendBinding
import com.example.myscichart.databinding.ContentMeditateMindLegendBinding
import com.example.myscichart.databinding.FragmentSessionReviewBinding
import com.example.myscichart.databinding.LayoutChartZoomingBinding
import com.example.myscichart.my_graphs.graph.GraphCardView
import com.example.myscichart.my_graphs.graph.Timeseries
import com.example.myscichart.my_graphs.graph.TimeseriesView
import java.util.Date
import java.util.EnumMap


class SessionReviewFragment : Fragment(R.layout.fragment_session_review) {

    private var session1 = SessionObj1().userSession
    private var session2 = SessionObj2().userSession

    private val viewModel: SessionReviewViewModel by lazy {
        ViewModelProvider(this)[SessionReviewViewModel::class.java]
    }

    private val graphCardViewLayoutParameters by lazy {
        val topMargin = resources.getDimension(R.dimen._10sdp).toInt()
        LinearLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, topMargin, 0, 0)
        }
    }

    private val legendViewLayoutParameters by lazy {
        val topMargin = resources.getDimension(R.dimen._10sdp).toInt()
        val bottomMargin = resources.getDimension(R.dimen._12sdp).toInt()
        LinearLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, topMargin, 0, bottomMargin)
        }
    }

    private var _binding: FragmentSessionReviewBinding? = null
    private val binding: FragmentSessionReviewBinding get() = _binding!!

    private class TimeseriesGraphGestureSynchronizer(
        private val binding: FragmentSessionReviewBinding
    ) : TimeseriesView.GestureCallback {

        private val graphs = ArrayList<TimeseriesView>()

        var panCount = 0
        var zoomCount = 0

        fun addGraph(chart: TimeseriesView) {
            graphs.add(chart)
        }

        override fun onPerformingZoom(point: PointF, xValue: Double, yValue: Double) {
            binding.scrollView.requestDisallowInterceptTouchEvent(true)
            graphs.forEach { it.performZoom(point, xValue, yValue) }
        }

        override fun onFinishedZoom() {
            zoomCount++
            panCount-- // Zooming over-counts pans
        }

        override fun onPerformingPan(xDelta: Float, yDelta: Float, isSecondHalf: Boolean) {
            binding.scrollView.requestDisallowInterceptTouchEvent(true)
            graphs.forEach { it.performPan(xDelta, yDelta, isSecondHalf) }
        }

        override fun onFinishedPan() {
            panCount++
        }

        fun setVisibleRange(start: Date) {
            graphs.forEach { it.setVisibleRange(start) }
        }
    }

    private lateinit var graphGestureSynchronizer: TimeseriesGraphGestureSynchronizer

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        if (GlobalConstant.isSession1) {
            viewModel.session = session1
        } else {
            viewModel.session = session2
        }

        ContextHolder.set(requireContext())

        _binding = FragmentSessionReviewBinding.inflate(inflater, container, false)
        graphGestureSynchronizer = TimeseriesGraphGestureSynchronizer(binding)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGraphRows(viewModel.graphs)

        requireActivity().onBackPressedDispatcher.addCallback(requireActivity()) {
            activity?.finish()
        }

        android.os.Handler().postDelayed({
            requireActivity().finish()
            GlobalConstant.isSession1 = !GlobalConstant.isSession1
        }, 100)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupGraphRows(graphs: List<Graph>) {
        if (graphs.isEmpty()) {
            return
        }
        binding.sessionResultsGraphCollection.root.visibility = View.VISIBLE
        val graphCardViews = EnumMap<Graph, GraphCardView>(Graph::class.java)
        graphs.forEach { graph ->
            graphCardViews[graph] = when (graph) {
                Graph.MIND -> addMindGraph()
                Graph.BODY -> addBodyGraph()
                Graph.HEART -> addHeartGraph()
                else -> {
                    addMindGraph()
                }
            }
        }

        graphCardViews[Graph.MIND]?.getTimeseriesView()?.timeseries?.let {
            val startDate = findFirstNonNanYValueDate(it)
            if (startDate != null) {
                graphGestureSynchronizer.setVisibleRange(startDate)
            }
        }
    }

    private fun findFirstNonNanYValueDate(timeseries: Timeseries): Date? {
        for (i in 0 until timeseries.x.size) {
            if (timeseries.y[i].isNaN()) continue
            if (i <= 0) return null
            return timeseries.x[i]
        }
        return null
    }

    private fun addMindGraph(): GraphCardView? {
        val mindTimeseries = viewModel.mindTimeseries
        val birdTimes = when (viewModel.sessionType) {
            SessionType.GUIDED,
            SessionType.MIND -> viewModel.birdTimestampsSecondsSinceStart ?: emptyList()

            else -> emptyList()
        }
        val recoveryTimes = when (viewModel.sessionType) {
            SessionType.GUIDED,
            SessionType.MIND -> viewModel.recoveryTimestampsSecondsSinceStart ?: emptyList()

            else -> emptyList()
        }
        if (!mindTimeseries.isNullOrEmpty()) {
            val legend = ContentMeditateMindLegendBinding.inflate(layoutInflater).apply {
                mapOf(
                    activeTimeText to viewModel.mindActiveSeconds,
                    neutralTimeText to viewModel.mindNeutralSeconds,
                    calmTimeText to viewModel.mindCalmSeconds
                ).forEach { (textView, seconds) ->
                    textView.text = SessionLength.secondsToHoursMinutesOrMinutesSeconds(seconds)
                }
            }
            val graphCardView = GraphCardView.createMindGraphCardView(
                requireContext(),
                mindTimeseries,
                birdTimes,
                recoveryTimes,
                viewModel.mindCalmPercentage,
                viewModel.dataTrackingStart,
                viewModel.completedSeconds,
                viewModel.graphXLabelling
            ).apply {
                setupGraphCardView(this, legend.zoomingView, Graph.MIND)
            }
            binding.sessionResultsGraphCollection.mixedGraphContainer.addView(
                graphCardView,
                graphCardViewLayoutParameters
            )
            binding.sessionResultsGraphCollection.mixedGraphContainer.addView(
                legend.root,
                legendViewLayoutParameters
            )
            return graphCardView
        } else {
            return null
        }
    }

    private fun addBodyGraph(): GraphCardView? {
        val bodyTimeseries = viewModel.bodyTimeseries
        val birdTimes = when (viewModel.sessionType) {
            SessionType.BODY -> viewModel.birdTimestampsSecondsSinceStart ?: emptyList()
            else -> emptyList()
        }
        val recoveryTimes = when (viewModel.sessionType) {
            SessionType.BODY -> viewModel.recoveryTimestampsSecondsSinceStart ?: emptyList()
            else -> emptyList()
        }
        if (!bodyTimeseries.isNullOrEmpty()) {
            val legend = ContentMeditateBodyLegendBinding.inflate(layoutInflater).apply {
                mapOf(
                    activeTimeText to viewModel.bodyActiveSeconds,
                    relaxedTimeText to viewModel.bodyRelaxedSeconds
                ).forEach { (textView, seconds) ->
                    textView.text = SessionLength.secondsToHoursMinutesOrMinutesSeconds(seconds)
                }
            }
            val graphCardView = GraphCardView.createBodyGraphCardView(
                requireContext(),
                bodyTimeseries,
                birdTimes,
                recoveryTimes,
                viewModel.bodyRelaxedPercentage,
                viewModel.dataTrackingStart,
                viewModel.completedSeconds,
                viewModel.graphXLabelling
            ).apply {
                setupGraphCardView(this, legend.zoomingView, Graph.BODY)
            }
            binding.sessionResultsGraphCollection.mixedGraphContainer.addView(
                graphCardView,
                graphCardViewLayoutParameters
            )
            binding.sessionResultsGraphCollection.mixedGraphContainer.addView(
                legend.root,
                legendViewLayoutParameters
            )
            return graphCardView
        } else {
            return null
        }
    }

    private fun addHeartGraph(): GraphCardView? {
        val heartTimeseries = viewModel.heartTimeseries
        val birdTimes = when (viewModel.sessionType) {
            SessionType.HEART -> viewModel.birdTimestampsSecondsSinceStart ?: emptyList()
            else -> emptyList()
        }
        val recoveryTimes = when (viewModel.sessionType) {
            SessionType.HEART -> viewModel.recoveryTimestampsSecondsSinceStart ?: emptyList()
            else -> emptyList()
        }
        if (!heartTimeseries.isNullOrEmpty()) {
            val legend = ContentMeditateHeartLegendBinding.inflate(layoutInflater)
            val graphCardView = GraphCardView.createHeartGraphCardView(
                requireContext(),
                heartTimeseries,
                birdTimes,
                recoveryTimes,
                viewModel.averageHeartRate,
                if (viewModel.resultsMode == ResultsMode.MEDITATE) {
                    TimeseriesView.MinMaxAnnotations.MIN_MAX
                } else {
                    TimeseriesView.MinMaxAnnotations.MIN_ONLY
                },
                viewModel.isHeartHistoricalVisible,
                viewModel.dataTrackingStart,
                viewModel.completedSeconds,
                viewModel.graphXLabelling,
                viewModel.heartHistoricalAbsMinRate,
                viewModel.heartHistoricalAbsMaxRate,
                viewModel.heartHistoricalAvgMinRate,
                viewModel.heartHistoricalAvgMaxRate
            ).apply {
                setupGraphCardView(this, legend.zoomingView, Graph.HEART)
            }
            binding.sessionResultsGraphCollection.mixedGraphContainer.addView(
                graphCardView,
                graphCardViewLayoutParameters
            )
            binding.sessionResultsGraphCollection.mixedGraphContainer.addView(
                legend.root,
                legendViewLayoutParameters
            )
            if (viewModel.isHeartHistoricalVisible) {
                legend.averageLegend.visibility = View.VISIBLE
                legend.historicalLegend.visibility = View.VISIBLE
            } else {
                legend.averageLegend.visibility = View.GONE
                legend.historicalLegend.visibility = View.GONE
            }
            return graphCardView
        } else {
            return null
        }
    }

    private fun setupGraphCardView(
        graphCard: GraphCardView,
        zoomingView: LayoutChartZoomingBinding,
        graph: Graph
    ) {
        if (viewModel.resultsMode == ResultsMode.MEDITATE) {
            val timeseriesView = graphCard.getTimeseriesView()
            if (timeseriesView != null) {
                if (timeseriesView.zoomingEnabled) {
                    graphGestureSynchronizer.addGraph(timeseriesView)
                    timeseriesView.setupGestures(
                        graphGestureSynchronizer,
                        TimeseriesView.ZoomInstructionCallback { showZoomIn ->
                            if (showZoomIn) {
                                zoomingView.zoomInLayout.visibility = View.VISIBLE
                                zoomingView.zoomOutLayout.visibility = View.GONE
                            } else {
                                zoomingView.zoomInLayout.visibility = View.GONE
                                zoomingView.zoomOutLayout.visibility = View.VISIBLE
                            }
                        })
                } else {
                    zoomingView.root.visibility = View.GONE
                }
            }
        } else {
            zoomingView.root.visibility = View.GONE
        }
    }
}
