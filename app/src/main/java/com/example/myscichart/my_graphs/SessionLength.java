package com.example.myscichart.my_graphs;


import android.content.Context;

import com.example.myscichart.R;

import java.util.concurrent.TimeUnit;

public class SessionLength {
    private SessionLength() {
    }

    public static String secondsToHoursMinutesOrMinutesSeconds(int sessionLengthSeconds) {
        return secondsToHoursMinutesSeconds(sessionLengthSeconds,
                sessionLengthSeconds >= TimeUnit.HOURS.toSeconds(1), false, false, false);
    }


    private static String secondsToHoursMinutesSeconds(int sessionLengthSeconds, boolean roundSeconds, boolean hideSeconds, boolean longerTimeUnitFormat, boolean showAllZero) {
        int hours = sessionLengthSeconds / 3600;
        int minutes = (sessionLengthSeconds % 3600) / 60;
        int seconds = (sessionLengthSeconds % 3600) % 60;

        if (roundSeconds) {
            minutes += seconds >= 30 ? 1 : 0;
            seconds = 0;
        }

        Context context = ContextHolder.get();
        StringBuilder sb = new StringBuilder();
        if (hours > 0 || showAllZero) {
            sb.append(context.getString(longerTimeUnitFormat ?
                    R.string.time_format_hr_abbreviation :
                    R.string.time_format_hour_abbreviation, hours));
        }
        if (minutes > 0 || (hours > 0 && seconds > 0) || showAllZero) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(context.getString(longerTimeUnitFormat ?
                    R.string.time_format_min_abbreviation :
                    R.string.time_format_minute_abbreviation, minutes));
        }
        if ((seconds > 0 || showAllZero) && !hideSeconds) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(context.getString(longerTimeUnitFormat ?
                    R.string.time_format_sec_abbreviation :
                    R.string.time_format_second_abbreviation, seconds));

        }
        String result = sb.toString();
        if (result.isEmpty()) {
            return context.getString(longerTimeUnitFormat ?
                    R.string.time_format_min_abbreviation :
                    R.string.time_format_minute_abbreviation, 0);
        } else {
            return result;
        }
    }
}
