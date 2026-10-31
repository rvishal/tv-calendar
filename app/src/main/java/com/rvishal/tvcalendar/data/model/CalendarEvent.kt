package com.rvishal.tvcalendar.data.model

enum class EventSource(val label: String, val color: Long) {
    GOOGLE("Google", 0xFF4285F4),
    APPLE("Apple", 0xFF8E8E93),
    SG_HOLIDAY("SG Holiday", 0xFF34A853),
    FESTIVAL("Festival", 0xFFFF9500),
}

data class CalendarEvent(
    val id: String,
    val source: EventSource,
    val calendarName: String,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean = false,
    val color: Long? = null,
)
