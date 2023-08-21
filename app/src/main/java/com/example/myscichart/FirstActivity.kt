package com.example.myscichart

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myscichart.databinding.ActivityFirstBinding
import com.example.myscichart.my_graphs.SessionReviewActivity
import com.scichart.charting.visuals.SciChartSurface


class FirstActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFirstBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirstBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setupSciChart()
    }

    override fun onResume() {
        super.onResume()

        android.os.Handler().postDelayed({
            val intent = Intent(this, SessionReviewActivity::class.java)
            startActivity(intent)
        }, 10)

    }

    private fun setupSciChart() {
        try {
            SciChartSurface.setRuntimeLicenseKey(getString(R.string.scichart_key))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}