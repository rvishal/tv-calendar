package com.rvishal.tvcalendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvishal.tvcalendar.data.model.CalendarEvent
import com.rvishal.tvcalendar.data.prefs.SettingsStore
import com.rvishal.tvcalendar.data.repo.EventRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val repository: EventRepository,
    val settings: SettingsStore,
) : ViewModel() {

    private val window = MutableStateFlow(repository.syncWindow())

    val events: StateFlow<List<CalendarEvent>> = window
        .flatMapLatest { (s, e) -> repository.observe(s, e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncing = MutableStateFlow(false)
    val syncErrors = MutableStateFlow<Map<String, String>>(emptyMap())

    fun sync() {
        if (syncing.value) return
        syncing.value = true
        viewModelScope.launch {
            try {
                syncErrors.value = repository.syncAll()
            } finally {
                syncing.value = false
            }
        }
    }
}
