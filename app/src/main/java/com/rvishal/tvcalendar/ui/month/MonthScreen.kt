package com.rvishal.tvcalendar.ui.month

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import java.util.Calendar

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MonthScreen(nav: NavHostController, vm: CalendarViewModel = viewModel { CalendarViewModel(com.rvishal.tvcalendar.di.AppGraph.repository, com.rvishal.tvcalendar.di.AppGraph.settings) }) {
    val events by vm.events.collectAsState()
    val now = System.currentTimeMillis()
    var shownMillis by remember {
        mutableStateOf(Dates.startOfMonth(
            Dates.field(now, Calendar.YEAR), Dates.field(now, Calendar.MONTH)))
    }
    val year = Dates.field(shownMillis, Calendar.YEAR)
    val month = Dates.field(shownMillis, Calendar.MONTH)

    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = "Calendar",
            current = ViewMode.MONTH,
            onNavigate = { mode -> nav.navigate(mode.route) { launchSingleTop = true } },
            onSync = vm::sync,
            onSettings = { nav.navigate("settings") },
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("‹", fontSize = 30.sp, color = IosRed,
                modifier = Modifier.clickable {
                    shownMillis = shift(shownMillis, -1)
                }.padding(8.dp))
            Text(Dates.monthTitle(shownMillis), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text("›", fontSize = 30.sp, color = IosRed,
                modifier = Modifier.clickable {
                    shownMillis = shift(shownMillis, 1)
                }.padding(8.dp))
        }

        // Weekday header
        Row(Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 8.dp)) {
            Dates.WEEKDAY_HEADER.forEach {
                Text(it, fontSize = 14.sp, modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
            }
        }

        // 6-row grid
        val gridStart = Dates.monthGridStart(year, month)
        Column(Modifier.fillMaxWidth().padding(horizontal = 48.dp)) {
            repeat(6) { row ->
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    repeat(7) { col ->
                        val dayMillis = Dates.plusDays(gridStart, row * 7 + col)
                        val inMonth = Dates.field(dayMillis, Calendar.MONTH) == month
                        val dayEvents = eventsOnDay(events, dayMillis)
                        DayCell(
                            dayMillis = dayMillis,
                            dayNum = Dates.field(dayMillis, Calendar.DAY_OF_MONTH),
                            inMonth = inMonth,
                            isToday = Dates.isToday(dayMillis),
                            events = dayEvents,
                            modifier = Modifier.weight(1f),
                            onOpen = { nav.navigate("day/$dayMillis") },
                        )
                    }
                }
            }
        }
    }
}

private fun shift(monthStart: Long, months: Int): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = monthStart
    c.add(Calendar.MONTH, months)
    return c.timeInMillis
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DayCell(
    dayMillis: Long,
    dayNum: Int,
    inMonth: Boolean,
    isToday: Boolean,
    events: List<com.rvishal.tvcalendar.data.model.CalendarEvent>,
    modifier: Modifier,
    onOpen: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .aspectRatio(1.45f)
            .padding(2.dp)
            .background(
                if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                else Color.Transparent,
                RoundedCornerShape(6.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onOpen)
            .padding(4.dp),
    ) {
        Box(
            Modifier.size(26.dp).background(
                if (isToday) IosRed else Color.Transparent, CircleShape
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$dayNum",
                fontSize = 14.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isToday -> Color.White
                    !inMonth -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.onBackground
                },
            )
        }
        events.take(3).forEach { e ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .background(
                        Color(e.color ?: e.source.color).copy(alpha = 0.9f),
                        RoundedCornerShape(3.dp),
                    )
                    .padding(horizontal = 3.dp, vertical = 1.dp),
            ) {
                Text(e.title, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = Color.White)
            }
        }
        if (events.size > 3) {
            Text("+${events.size - 3} more", fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        }
    }
}


