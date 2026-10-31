package com.rvishal.tvcalendar.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object Dates {

    fun startOfDay(millis: Long, tz: TimeZone = TimeZone.getDefault()): Long {
        val c = Calendar.getInstance(tz)
        c.timeInMillis = millis
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun plusDays(millis: Long, days: Int, tz: TimeZone = TimeZone.getDefault()): Long {
        val c = Calendar.getInstance(tz)
        c.timeInMillis = millis
        c.add(Calendar.DAY_OF_YEAR, days)
        return c.timeInMillis
    }

    fun startOfMonth(year: Int, month: Int, tz: TimeZone = TimeZone.getDefault()): Long {
        val c = Calendar.getInstance(tz)
        c.clear()
        c.set(year, month, 1)
        return c.timeInMillis
    }

    /** Millis of the Sunday/Monday cell anchoring the month grid's first row. */
    fun monthGridStart(year: Int, month: Int, tz: TimeZone = TimeZone.getDefault()): Long {
        val first = startOfMonth(year, month, tz)
        val c = Calendar.getInstance(tz)
        c.timeInMillis = first
        val dow = c.get(Calendar.DAY_OF_WEEK) // SUNDAY=1
        c.add(Calendar.DAY_OF_YEAR, -(dow - Calendar.SUNDAY))
        return c.timeInMillis
    }

    fun startOfWeek(millis: Long, tz: TimeZone = TimeZone.getDefault()): Long {
        val c = Calendar.getInstance(tz)
        c.timeInMillis = startOfDay(millis, tz)
        val dow = c.get(Calendar.DAY_OF_WEEK)
        c.add(Calendar.DAY_OF_YEAR, -(dow - Calendar.SUNDAY))
        return c.timeInMillis
    }

    fun field(millis: Long, field: Int, tz: TimeZone = TimeZone.getDefault()): Int {
        val c = Calendar.getInstance(tz); c.timeInMillis = millis
        return c.get(field)
    }

    fun monthTitle(millis: Long): String =
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(millis)

    fun dayTitle(millis: Long): String =
        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(millis)

    fun shortDay(millis: Long): String =
        SimpleDateFormat("EEE d", Locale.getDefault()).format(millis)

    fun time(millis: Long, use24h: Boolean): String =
        SimpleDateFormat(if (use24h) "HH:mm" else "h:mm a", Locale.getDefault()).format(millis)

    /**
     * The local-tz day a date belongs to for display purposes. All-day events are
     * anchored at UTC midnight, so their "day" is the UTC calendar date rendered
     * on the matching local calendar date — never shifted by the device timezone.
     */
    fun dayKeyMillis(millis: Long, allDay: Boolean): Long {
        if (!allDay) return startOfDay(millis)
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.timeInMillis = millis
        val local = Calendar.getInstance()
        local.clear()
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
        return local.timeInMillis
    }

    fun isToday(millis: Long): Boolean =
        startOfDay(millis) == startOfDay(System.currentTimeMillis())

    val WEEKDAY_HEADER = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
}
