package com.rvishal.tvcalendar.ui.agenda

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
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.rvishal.tvcalendar.ui.CalendarViewModel
import com.rvishal.tvcalendar.ui.components.EventRow
import com.rvishal.tvcalendar.ui.components.TopBar
import com.rvishal.tvcalendar.ui.components.ViewMode
import com.rvishal.tvcalendar.util.Dates

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AgendaScreen(nav: NavHostController, vm: CalendarViewModel = viewModel { CalendarViewModel(com.rvishal.tvcalendar.di.AppGraph.repository, com.rvishal.tvcalendar.di.AppGraph.settings) }) {
    val events by vm.events.collectAsState()
    val now = System.currentTimeMillis()
    val todayStart = Dates.startOfDay(now)
    val upcoming = events.filter { e ->
        if (e.allDay) Dates.dayKeyMillis(e.startMillis, allDay = true) >= todayStart
        else e.endMillis >= now
    }.sortedBy { it.startMillis }
    val grouped = upcoming.groupBy { Dates.dayKeyMillis(it.startMillis, it.allDay) }.toSortedMap()

    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = "Upcoming",
            current = ViewMode.AGENDA,
            onNavigate = { nav.navigate(it.route) { launchSingleTop = true } },
            onSync = vm::sync,
            onSettings = { nav.navigate("settings") },
        )
        if (grouped.isEmpty()) {
            Text(
                "No upcoming events. Add sources in Settings, then Sync.",
                modifier = Modifier.padding(48.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 48.dp)) {
            grouped.forEach { (day, dayEvents) ->
                item(key = "h$day") {
                    Text(
                        Dates.dayTitle(day),
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    )
                }
                dayEvents.forEach { e ->
                    item(key = e.id) {
                        EventRow(e, vm.settings.use24Hour) {
                            nav.navigate("day/${Dates.dayKeyMillis(e.startMillis, e.allDay)}")
                        }
                    }
                }
            }
        }
    }
}
