package com.rvishal.tvcalendar.ui.day

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.foundation.lazy.LazyColumn
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.rvishal.tvcalendar.ui.CalendarViewModel
import com.rvishal.tvcalendar.ui.components.EventRow
import com.rvishal.tvcalendar.ui.components.eventsOnDay
import com.rvishal.tvcalendar.util.Dates

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DayScreen(nav: NavHostController, dayStartMillis: Long, vm: CalendarViewModel = viewModel { CalendarViewModel(com.rvishal.tvcalendar.di.AppGraph.repository, com.rvishal.tvcalendar.di.AppGraph.settings) }) {
    val events by vm.events.collectAsState()
    val dayStart = Dates.startOfDay(dayStartMillis)
    val dayEvents = eventsOnDay(events, dayStart).sortedBy { it.startMillis }

    Column(Modifier.fillMaxSize().padding(48.dp)) {
        Button(onClick = { nav.popBackStack() }) { Text("‹ Back") }
        Text(Dates.dayTitle(dayStart), fontSize = 30.sp, modifier = Modifier.padding(vertical = 20.dp))
        if (dayEvents.isEmpty()) {
            Text("No events.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            dayEvents.forEach { e ->
                item(key = e.id) { EventRow(e, vm.settings.use24Hour) }
            }
        }
    }
}
