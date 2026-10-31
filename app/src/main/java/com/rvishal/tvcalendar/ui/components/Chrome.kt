package com.rvishal.tvcalendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.rvishal.tvcalendar.data.model.CalendarEvent
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

@Composable
fun EventRow(event: CalendarEvent, use24h: Boolean, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            Modifier
                .width(6.dp)
                .height(36.dp)
                .background(Color(event.color ?: event.source.color))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(event.title, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                event.calendarName + if (event.location != null) " · ${event.location}" else "",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            if (event.allDay) "All day"
            else "${Dates.time(event.startMillis, use24h)} – ${Dates.time(event.endMillis, use24h)}",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        )
    }
}
