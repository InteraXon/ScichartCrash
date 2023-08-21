package com.example.myscichart.my_graphs

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myscichart.GlobalConstant
import com.example.myscichart.R
import com.scichart.extensions.builders.SciChartBuilder

class SessionReviewActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        val sessionType = if (GlobalConstant.isSession1) SessionType.MIND else SessionType.GUIDED

        setTheme(sessionType)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_review)

        SciChartBuilder.init(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        SciChartBuilder.dispose()
    }

    private fun setTheme(sessionType: SessionType) {
        when (sessionType) {
            SessionType.BODY -> setTheme(R.style.BodyTheme)
            SessionType.HEART -> setTheme(R.style.HeartTheme)
            SessionType.GUIDED -> setTheme(R.style.GuidedTheme)
            SessionType.MIND,
            SessionType.DEMO-> setTheme(R.style.MindTheme)

            else -> {}
        }
    }
}