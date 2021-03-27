package com.cosmos.unreddit.util

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cosmos.unreddit.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DateUtilTest {

    private val context: Context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }

    @Test
    fun nowTimeDifference() {
        val time: Long = getTimeFromElapsedTime(DateUtil.Unit.SECOND.value * 45)
        assertEquals(context.getString(R.string.time_difference_now),
            DateUtil.getTimeDifference(
                context,
                time
            )
        )
    }

    @Test
    fun minuteTimeDifference() {
        val time: Long = getTimeFromElapsedTime(DateUtil.Unit.MINUTE.value * 6)
        assertEquals(context.getString(R.string.time_difference_minute, 6),
            DateUtil.getTimeDifference(
                context,
                time
            )
        )
    }

    @Test
    fun hourTimeDifference() {
        val time: Long = getTimeFromElapsedTime(DateUtil.Unit.HOURS.value * 14)
        assertEquals(context.getString(R.string.time_difference_hour, 14),
            DateUtil.getTimeDifference(
                context,
                time
            )
        )
    }

    @Test
    fun dayTimeDifference() {
        val time: Long = getTimeFromElapsedTime(DateUtil.Unit.DAY.value * 62)
        assertEquals(context.getString(R.string.time_difference_day, 62),
            DateUtil.getTimeDifference(
                context,
                time
            )
        )
    }

    @Test
    fun yearTimeDifference() {
        val time1: Long = getTimeFromElapsedTime(DateUtil.Unit.YEAR.value * 2)
        assertEquals(context.getString(R.string.time_difference_year, 2),
            DateUtil.getTimeDifference(
                context,
                time1
            )
        )

        val time2: Long = getTimeFromElapsedTime(DateUtil.Unit.DAY.value * 367)
        assertEquals(context.getString(R.string.time_difference_year, 1),
            DateUtil.getTimeDifference(
                context,
                time2
            )
        )
    }

    private fun getTimeFromElapsedTime(elapsedTime: Long) = System.currentTimeMillis() - elapsedTime
}