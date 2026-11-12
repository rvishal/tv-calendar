package com.rvishal.tvcalendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.ui.text.style.TextDecoration
import com.rvishal.tvcalendar.data.model.CalendarEvent
import com.rvishal.tvcalendar.data.model.EventSource
import com.rvishal.tvcalendar.data.model.ItemKind
import com.rvishal.tvcalendar.util.Dates

enum class ViewMode(val route: String, val label: String) {
    MONTH("month", "Month"),
    WEEK("week", "Week"),
    AGENDA("agenda", "Agenda"),
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    current: ViewMode,
    onNavigate: (ViewMode) -> Unit,
    onSync: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, fontSize = 28.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.weight(1f))
        ViewMode.entries.forEach { mode ->
            Button(onClick = { onNavigate(mode) }) {
                Text(if (mode == current) "● ${mode.label}" else mode.label)
            }
        }
        Button(onClick = onSync) { Text("Sync") }
        Button(onClick = onSettings) { Text("Settings") }
    }
}

/** Events visible on the local calendar day at [dayMillis] (a start-of-day anchor). */
fun eventsOnDay(events: List<CalendarEvent>, dayMillis: Long): List<CalendarEvent> {
    val dayStart = Dates.startOfDay(dayMillis)
    return events.filter { e ->
        if (e.allDay) {
            val first = Dates.dayKeyMillis(e.startMillis, allDay = true)
            val last = Dates.dayKeyMillis(e.endMillis - 1, allDay = true)
            dayStart in first..last
        } else {
            e.startMillis < dayStart + 86_400_000 && e.endMillis > dayStart
        }
    }
}

@Composable
fun EventRow(event: CalendarEvent, use24h: Boolean, onClick: (() -> Unit)? = null) {
    var focused by remember { mutableStateOf(false) }
    val isTodo = event.kind == ItemKind.TODO
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(
                if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else Color.Transparent,
                RoundedCornerShape(6.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isTodo) {
            Text(
                if (event.completed) "✓" else "○",
                fontSize = 20.sp,
                color = Color(event.color ?: event.source.color),
                modifier = Modifier.width(24.dp),
            )
        } else {
            Spacer(
                Modifier
                    .width(6.dp)
                    .height(36.dp)
                    .background(Color(event.color ?: event.source.color))
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                event.title,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (event.completed) TextDecoration.LineThrough else TextDecoration.None,
            )
            Text(
                event.calendarName + if (event.location != null) " · ${event.location}" else "",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            if (isTodo) "To-do"
            else if (event.allDay) "All day"
            else "${Dates.time(event.startMillis, use24h)} – ${Dates.time(event.endMillis, use24h)}",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        )
    }
}
