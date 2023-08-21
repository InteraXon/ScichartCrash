package com.example.myscichart

import com.example.myscichart.my_graphs.SessionType

val SessionType.theme: Int
    get() = when (this) {
        SessionType.BODY -> R.style.BodyTheme
        SessionType.HEART -> R.style.HeartTheme
        SessionType.GUIDED -> R.style.GuidedTheme
        SessionType.MIND -> R.style.MindTheme

        else -> {
            R.style.BodyTheme
        }
    }