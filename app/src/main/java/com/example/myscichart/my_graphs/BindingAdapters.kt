package com.example.myscichart.my_graphs

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.example.myscichart.CircularSeekBar
import com.example.myscichart.R
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.*

@BindingAdapter("visibleGone")
fun showHide(view: View, show: Boolean = false) {
    view.visibility = if (show) View.VISIBLE else View.GONE
}

@BindingAdapter("progDrawable")
fun setProgressDrawable(view: ProgressBar, type: SessionType) {
    view.progressDrawable = when (type) {
        SessionType.MIND -> ContextCompat.getDrawable(
            view.context,
            R.drawable.mind_percentage_progress
        )
        SessionType.BODY -> ContextCompat.getDrawable(
            view.context,
            R.drawable.body_percentage_progress
        )
        else -> null
    }
}

@BindingAdapter("presleepProgDrawable")
fun setPresleepProgressDrawable(view: CircularSeekBar, color: Int) {
    view.circleProgressColor = ContextCompat.getColor(view.context, color)
}

@BindingAdapter("appLocaleFormat")
fun getEnglishLocale(view: TextView, startTime: OffsetDateTime?) {
    view.text =
        startTime?.format(DateTimeFormatter.ofPattern("h:mma", appLocale()))?.lowercase()
}

