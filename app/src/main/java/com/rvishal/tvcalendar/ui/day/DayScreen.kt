package com.rvishal.tvcalendar.ui.day

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedTextField
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.rvishal.tvcalendar.data.model.ItemKind
import com.rvishal.tvcalendar.ui.CalendarViewModel
import com.rvishal.tvcalendar.ui.components.EventRow
import com.rvishal.tvcalendar.ui.components.eventsOnDay
import com.rvishal.tvcalendar.util.Dates

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DayScreen(nav: NavHostController, dayStartMillis: Long, vm: CalendarViewModel = viewModel { CalendarViewModel(com.rvishal.tvcalendar.di.AppGraph.repository, com.rvishal.tvcalendar.di.AppGraph.settings) }) {
    val events by vm.events.collectAsState()
    val dayStart = Dates.startOfDay(dayStartMillis)
    val dayEvents = eventsOnDay(events, dayStart).sortedWith(
        compareBy({ it.kind == ItemKind.TODO }, { it.startMillis })
    )
    var newTodo by remember { mutableStateOf("") }
    var addingTodo by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(48.dp)) {
        Button(onClick = { nav.popBackStack() }) { Text("‹ Back") }
        Text(Dates.dayTitle(dayStart), fontSize = 30.sp, modifier = Modifier.padding(vertical = 20.dp))
        if (dayEvents.isEmpty() && !addingTodo) {
            Text("No events.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            dayEvents.forEach { e ->
                item(key = e.id) {
                    EventRow(
                        e,
                        vm.settings.use24Hour,
                        onClick = if (e.kind == ItemKind.TODO
                            && e.source == com.rvishal.tvcalendar.data.model.EventSource.LOCAL
                        ) {
                            { vm.toggleTodo(e) }
                        } else null,
                    )
                }
            }
            item(key = "add_todo") {
                if (addingTodo) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = newTodo,
                            onValueChange = { newTodo = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { androidx.compose.material3.Text("To-do title") },
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    vm.addTodo(newTodo, dayStart)
                                    newTodo = ""
                                    addingTodo = false
                                }
                            ),
                        )
                        Spacer(Modifier.width(12.dp))
                        Button(onClick = {
                            vm.addTodo(newTodo, dayStart)
                            newTodo = ""
                            addingTodo = false
                        }) { Text("Add") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { addingTodo = false; newTodo = "" }) { Text("Cancel") }
                    }
                } else {
                    Button(onClick = { addingTodo = true }, modifier = Modifier.padding(vertical = 8.dp)) {
                        Text("＋ Add to-do")
                    }
                }
            }
        }
    }
}
