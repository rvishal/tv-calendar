package com.rvishal.tvcalendar.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvishal.tvcalendar.R
import com.rvishal.tvcalendar.data.prefs.SettingsStore
import com.rvishal.tvcalendar.data.repo.EventRepository
import com.rvishal.tvcalendar.data.sources.GoogleAuthManager
import com.rvishal.tvcalendar.data.sync.DailyDigestWorker
import com.rvishal.tvcalendar.data.sync.TelegramDigest
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
sealed interface GoogleState {
    data object SignedOut : GoogleState
    data class AwaitingUser(val userCode: String, val verificationUrl: String) : GoogleState
    data class SignInFailed(val message: String) : GoogleState
    data object SignedIn : GoogleState
}

data class SettingsUiState(
    val settings: SettingsStore,
    val google: GoogleState,
    val caldavUser: String,
    val caldavPassword: String,
    val caldavBaseUrl: String,
    val festivalUrl: String,
    val syncErrors: Map<String, String>,
    val sourceEnabled: Map<String, Boolean>,
    val use24h: Boolean,
    val telegramToken: String,
    val telegramChatId: String,
    val telegramEnabled: Boolean,
    val telegramStatus: String?,
)

class SettingsViewModel(
    private val context: Context,
    val settings: SettingsStore,
    private val auth: GoogleAuthManager,
    private val repository: EventRepository,
    private val telegramDigest: TelegramDigest,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(
            settings = settings,
            google = if (settings.isGoogleSignedIn) GoogleState.SignedIn else GoogleState.SignedOut,
            caldavUser = settings.caldavUser.orEmpty(),
            caldavPassword = settings.caldavPassword.orEmpty(),
            caldavBaseUrl = settings.caldavBaseUrl,
            festivalUrl = settings.festivalIcsUrl,
            syncErrors = emptyMap(),
            sourceEnabled = sourceEnabledMap(),
            use24h = settings.use24Hour,
            telegramToken = settings.telegramBotToken.orEmpty(),
            telegramChatId = settings.telegramChatId.orEmpty(),
            telegramEnabled = settings.telegramDigestEnabled,
            telegramStatus = null,
        )
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private var signInJob: Job? = null

    fun startGoogleSignIn() {
        val clientId = context.getString(R.string.google_client_id)
        val clientSecret = context.getString(R.string.google_client_secret)
        if (clientId == "YOUR_GOOGLE_TV_CLIENT_ID") {
            _state.update { it.copy(google = GoogleState.SignInFailed("Client ID not configured")) }
            return
        }
        signInJob?.cancel()
        signInJob = viewModelScope.launch {
            try {
                val code = auth.requestDeviceCode(clientId)
                _state.update {
                    it.copy(google = GoogleState.AwaitingUser(code.userCode, code.verificationUrl))
                }
                auth.awaitSignIn(clientId, clientSecret, code.deviceCode, code.interval, code.expiresIn)
                _state.update { it.copy(google = GoogleState.SignedIn) }
                repository.syncAll()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("SettingsViewModel", "Google sign-in failed", e)
                _state.update {
                    it.copy(
                        google = if (settings.isGoogleSignedIn) GoogleState.SignedIn
                                 else GoogleState.SignInFailed(e.message ?: "sign-in failed"),
                    )
                }
            }
        }
    }

    fun cancelGoogleSignIn() {
        signInJob?.cancel()
        _state.update {
            it.copy(google = if (settings.isGoogleSignedIn) GoogleState.SignedIn else GoogleState.SignedOut)
        }
    }

    fun signOutGoogle() {
        auth.signOut()
        _state.update { it.copy(google = GoogleState.SignedOut) }
    }

    fun setCaldavUser(v: String) {
        settings.caldavUser = v.trim()
        _state.update { it.copy(caldavUser = v) }
    }

    fun setCaldavPassword(v: String) {
        settings.caldavPassword = v.trim()
        _state.update { it.copy(caldavPassword = v) }
    }

    fun setCaldavUrl(v: String) {
        settings.caldavBaseUrl = v.trim()
        _state.update { it.copy(caldavBaseUrl = v) }
    }

    fun setFestivalUrl(v: String) {
        settings.festivalIcsUrl = v.trim()
        _state.update { it.copy(festivalUrl = v) }
    }

    fun set24h(v: Boolean) {
        settings.use24Hour = v
        _state.update { it.copy(use24h = v) }
    }

    fun setTelegramToken(v: String) {
        settings.telegramBotToken = v.trim()
        _state.update { it.copy(telegramToken = v) }
    }

    fun setTelegramChatId(v: String) {
        settings.telegramChatId = v.trim()
        _state.update { it.copy(telegramChatId = v) }
    }

    fun setTelegramEnabled(v: Boolean) {
        settings.telegramDigestEnabled = v
        DailyDigestWorker.schedule(context, v)
        _state.update { it.copy(telegramEnabled = v) }
    }

    /** Sends today's agenda immediately — also serves as a "test" button. */
    fun sendTelegramNow() {
        if (!settings.isTelegramConfigured) {
            _state.update { it.copy(telegramStatus = "Enter bot token + chat ID first") }
            return
        }
        _state.update { it.copy(telegramStatus = "Sending…") }
        viewModelScope.launch {
            try {
                telegramDigest.send(repository.todayEvents())
                _state.update { it.copy(telegramStatus = "Sent ✓") }
            } catch (e: Exception) {
                _state.update { it.copy(telegramStatus = "Failed: ${e.message}") }
            }
        }
    }

    fun setSourceEnabled(name: String, enabled: Boolean) {
        settings.setSourceEnabled(name, enabled)
        _state.update { it.copy(sourceEnabled = sourceEnabledMap()) }
    }

    private fun sourceEnabledMap(): Map<String, Boolean> =
        com.rvishal.tvcalendar.data.model.EventSource.entries
            .associate { it.name to settings.isSourceEnabled(it.name) }

    fun syncNow() {
        viewModelScope.launch {
            val errors = repository.syncAll()
            val channelId = settings.homeChannelId
            if (channelId > 0) com.rvishal.tvcalendar.di.AppGraph.homeChannel.publishToday(channelId)
            _state.update { it.copy(syncErrors = errors) }
        }
    }
}
