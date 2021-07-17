package com.cosmos.unreddit.util

import android.content.Context
import com.cosmos.unreddit.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtil {
    enum class Unit(val value: Long) {
        SECOND(1000),
        MINUTE(SECOND.value * 60),
        HOURS(MINUTE.value * 60),
        DAY(HOURS.value * 24),
        YEAR(DAY.value * 365)
    }

    @JvmStatic
    @JvmOverloads
    fun getTimeDifference(
        context: Context,
        timeInMillis: Long,
        withSuffix: Boolean = true
    ): String {
        val elapsedTime = System.currentTimeMillis() - timeInMillis
        return when {
            elapsedTime < Unit.MINUTE.value -> context.getString(R.string.time_difference_now)
            elapsedTime < Unit.HOURS.value -> {
                val timeString = TimeUnit.MILLISECONDS.toMinutes(elapsedTime)
                if (withSuffix) {
                    context.getString(R.string.time_difference_minute, timeString)
                } else {
                    context.getString(R.string.time_difference_minute_short, timeString)
                }
            }
            elapsedTime < Unit.DAY.value -> {
                val timeString = TimeUnit.MILLISECONDS.toHours(elapsedTime)
                if (withSuffix) {
                    context.getString(R.string.time_difference_hour, timeString)
                } else {
                    context.getString(R.string.time_difference_hour_short, timeString)
                }
            }
            elapsedTime < Unit.YEAR.value -> {
                val timeString = TimeUnit.MILLISECONDS.toDays(elapsedTime)
                if (withSuffix) {
                    context.getString(R.string.time_difference_day, timeString)
                } else {
                    context.getString(R.string.time_difference_day_short, timeString)
                }
            }
            else -> {
                val timeString = elapsedTime.div(Unit.YEAR.value)
                if (withSuffix) {
                    context.getString(R.string.time_difference_year, timeString)
                } else {
                    context.getString(R.string.time_difference_year_short, timeString)
                }
            }
        }
    }

    fun getFormattedDate(timeInMillis: Long): String {
        return DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())
            .format(Date(timeInMillis))
    }

    fun getFormattedDate(pattern: String, date: Date): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
    }
}
