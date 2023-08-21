package com.example.myscichart.my_graphs

import org.threeten.bp.OffsetDateTime


open class UserSession(
    var utcTimestamp: Long = 0,
    var rawStartDatetimeLocalWithTimezone: String = "",
    var rawStartDataTrackingDatetimeLocalWithTimezone: String? = null,
    var rawSessionType: String = "",
    var rawResultsMode: String? = null,
    var completedSeconds: Int = 0,
    var selectedSessionLengthSeconds: Int? = null,
    var mind: MindUserSession? = null,
    var heart: HeartUserSession? = null,
    var body: BodyUserSession? = null,
    var timeSeries: UserSessionTimeSeries? = null
) {

    val startDatetimeLocalWithTimezone: OffsetDateTime
        get() {
            return OffsetDateTime.parse(rawStartDatetimeLocalWithTimezone)
        }

    val startDataTrackingDatetimeLocalWithTimezone: OffsetDateTime?
        get() {
            return if (!rawStartDataTrackingDatetimeLocalWithTimezone.isNullOrEmpty()) {
                OffsetDateTime.parse(rawStartDataTrackingDatetimeLocalWithTimezone)
            } else {
                null
            }
        }

    override fun toString(): String {
        return "utcTimestamp: $utcTimestamp\n" +
                "rawStartDatetimeLocalWithTimezone: $rawStartDatetimeLocalWithTimezone\n" +
                "rawSessionType: $rawSessionType\n" +
                "rawResultsMode: $rawResultsMode\n" +
                "completedSeconds: $completedSeconds\n" +
                "selectedSessionLengthSeconds: $selectedSessionLengthSeconds\n" +
                "mind: $mind\n" +
                "heart: $heart\n" +
                "body: $body\n" +
                "timeSeries: $timeSeries\n"
    }
}

open class MindUserSession(
    var calmPercentage: Int? = null,
    var calmSeconds: Int? = null,
    var neutralSeconds: Int? = null,
    var activeSeconds: Int? = null
){
    override fun toString(): String {
        return "calmPercentage: $calmPercentage\n" +
                "calmSeconds: $calmSeconds\n" +
                "neutralSeconds: $neutralSeconds\n" +
                "activeSeconds: $activeSeconds\n"
    }
}

open class HeartUserSession(
    var lowRatePercentage: Int? = null,
    var historicalAbsMinRate: Int? = null,
    var historicalAbsMaxRate: Int? = null,
    var historicalAvgMinRate: Int? = null,
    var historicalAvgMaxRate: Int? = null
) {
    override fun toString(): String {
        return "lowRatePercentage: $lowRatePercentage\n" +
                "historicalAbsMinRate: $historicalAbsMinRate\n" +
                "historicalAbsMaxRate: $historicalAbsMaxRate\n" +
                "historicalAvgMinRate: $historicalAvgMinRate\n" +
                "historicalAvgMaxRate: $historicalAvgMaxRate\n"
    }
}

open class BodyUserSession(
    var relaxedPercentage: Int? = null,
    var relaxedSeconds: Int? = null,
    var activeSeconds: Int? = null
)

open class UserSessionTimeSeries(
    var mindCalmPerSecond: List<Double>? = null,
    var heartRatePerSecond: List<Double>? = null,
    var bodyMovementPerSecond: List<Double>? = null,
    var birdTimestampsSecondsSinceStart: List<Int>? = null,
    var recoveryTimestampsSecondsSinceStart: List<Int>? = null,
    var mindCalmDownSampled: List<Double>? = null,
    var heartRateDownSampled: List<Double>? = null,
    var bodyMovementDownSampled: List<Double>? = null
)
