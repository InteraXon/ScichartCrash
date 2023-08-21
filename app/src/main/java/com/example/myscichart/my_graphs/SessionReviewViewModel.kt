package com.example.myscichart.my_graphs

import androidx.lifecycle.ViewModel
import com.example.myscichart.GlobalConstant
import com.example.myscichart.my_graphs.graph.XAxis
import io.reactivex.disposables.CompositeDisposable

class SessionReviewViewModel : ViewModel() {

    lateinit var session: UserSession

    companion object {

        fun meditationGraphs(
            sessionType: SessionType
        ): List<Graph> {
            if (sessionType == SessionType.TIMED) {
                return emptyList()
            }
            return listOf(Graph.MIND, Graph.BODY, Graph.HEART)
        }
    }


    private val disposableBag = CompositeDisposable()

    val sessionType: SessionType by lazy {
        if (GlobalConstant.isSession1) SessionType.MIND else SessionType.GUIDED
    }

    val resultsMode by lazy {
        ResultsMode.MEDITATE
    }

    val graphs: List<Graph> by lazy {
        meditationGraphs(sessionType)
    }

    val graphXLabelling: XAxis.XLabelling by lazy {
        if (sessionType == SessionType.PRESLEEP) {
            XAxis.XLabelling.ABSOLUTE_TIME
        } else {
            XAxis.XLabelling.RELATIVE_TIME
        }
    }

    val dataTrackingStart
        get() = session.startDataTrackingDatetimeLocalWithTimezone
            ?: session.startDatetimeLocalWithTimezone

    val completedSeconds
        get() = session.completedSeconds

    val mindActiveSeconds
        get() = session.mind?.activeSeconds ?: 0
    val mindNeutralSeconds
        get() = session.mind?.neutralSeconds ?: 0
    val mindCalmSeconds
        get() = session.mind?.calmSeconds ?: 0
    val mindCalmPercentage
        get() = session.mind?.calmPercentage ?: 0

    val bodyActiveSeconds
        get() = session.body?.activeSeconds ?: 0
    val bodyRelaxedSeconds
        get() = session.body?.relaxedSeconds ?: 0
    val bodyRelaxedPercentage
        get() = session.body?.relaxedPercentage ?: 0

    val averageHeartRate by lazy {
        if (heartTimeseries.isNullOrEmpty()) {
            0
        } else {
            heartTimeseries!!.filter { it >= 0 }.average().toInt()
        }
    }
    val heartHistoricalAbsMaxRate
        get() = session.heart?.historicalAbsMaxRate?.toDouble()
    val heartHistoricalAbsMinRate
        get() = session.heart?.historicalAbsMinRate?.toDouble()
    val heartHistoricalAvgMaxRate
        get() = session.heart?.historicalAvgMaxRate?.toDouble()
    val heartHistoricalAvgMinRate
        get() = session.heart?.historicalAvgMinRate?.toDouble()
    val isHeartHistoricalVisible
        get() = sessionType != SessionType.PRESLEEP && session.heart?.historicalAvgMaxRate != null

    val mindTimeseries: List<Double>?
        get() = session.timeSeries?.mindCalmDownSampled?.let {
            if (it.isEmpty()) {
                session.timeSeries?.mindCalmPerSecond
            } else {
                it
            }
        } ?: session.timeSeries?.mindCalmPerSecond
    val bodyTimeseries: List<Double>?
        get() = session.timeSeries?.bodyMovementDownSampled?.let {
            if (it.isEmpty()) {
                session.timeSeries?.bodyMovementPerSecond
            } else {
                it
            }
        } ?: session.timeSeries?.bodyMovementPerSecond
    val heartTimeseries: List<Double>?
        get() = session.timeSeries?.heartRateDownSampled?.let {
            if (it.isEmpty()) {
                session.timeSeries?.heartRatePerSecond
            } else {
                it
            }
        } ?: session.timeSeries?.heartRatePerSecond

    val birdTimestampsSecondsSinceStart: List<Int>?
        get() = session.timeSeries?.birdTimestampsSecondsSinceStart
    val recoveryTimestampsSecondsSinceStart: List<Int>?
        get() = session.timeSeries?.recoveryTimestampsSecondsSinceStart

    override fun onCleared() {
        super.onCleared()
        disposableBag.dispose()
    }
}
