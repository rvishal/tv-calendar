package com.rvishal.tvcalendar.data.sources

import com.rvishal.tvcalendar.data.model.CalendarEvent
import com.rvishal.tvcalendar.data.model.EventSource

interface EventDataSource {
    val source: EventSource
    val isConfigured: Boolean
    suspend fun fetch(windowStartMillis: Long, windowEndMillis: Long): List<CalendarEvent>
}
