package com.rvishal.tvcalendar.ui.week

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.rvishal.tvcalendar.ui.CalendarViewModel
import com.rvishal.tvcalendar.ui.components.TopBar
import com.rvishal.tvcalendar.ui.components.ViewMode
import com.rvishal.tvcalendar.ui.components.eventsOnDay
import com.rvishal.tvcalendar.ui.theme.IosRed
import com.rvishal.tvcalendar.util.Dates

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WeekScreen(nav: NavHostController, vm: CalendarViewModel = viewModel { CalendarViewModel(com.rvishal.tvcalendar.di.AppGraph.repository, com.rvishal.tvcalendar.di.AppGraph.settings) }) {
    val events by vm.events.collectAsState()
    var weekStart by remember { mutableStateOf(Dates.startOfWeek(System.currentTimeMillis())) }

    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = "Week",
            current = ViewMode.WEEK,
            onNavigate = { nav.navigate(it.route) { launchSingleTop = true } },
            onSync = vm::sync,
            onSettings = { nav.navigate("settings") },
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("‹", fontSize = 30.sp, color = IosRed,
                modifier = Modifier.clickable { weekStart = Dates.plusDays(weekStart, -7) }.padding(8.dp))
            Text(
                "${Dates.shortDay(weekStart)} – ${Dates.shortDay(Dates.plusDays(weekStart, 6))}",
                fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
            )
            Text("›", fontSize = 30.sp, color = IosRed,
                modifier = Modifier.clickable { weekStart = Dates.plusDays(weekStart, 7) }.padding(8.dp))
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
                .weight(1f),
        ) {
            repeat(7) { i ->
                val dayMillis = Dates.plusDays(weekStart, i)
                val dayEvents = eventsOnDay(events, dayMillis)
                Column(
                    Modifier
                        .weight(1f)
                        .padding(2.dp)
                        .verticalScroll(rememberScrollState())
                        .clickable { nav.navigate("day/$dayMillis") },
                ) {
                    Text(
                        Dates.shortDay(dayMillis),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (Dates.isToday(dayMillis)) IosRed
                            else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    dayEvents.forEach { e ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                                .background(
                                    Color(e.color ?: e.source.color).copy(alpha = 0.85f),
                                    RoundedCornerShape(4.dp),
                                )
                                .padding(4.dp),
                        ) {
                            Text(e.title, fontSize = 10.sp, color = Color.White, maxLines = 2)
                            Text(
                                if (e.allDay) "All day" else Dates.time(e.startMillis, vm.settings.use24Hour),
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                }
            }
        }
    }
}
