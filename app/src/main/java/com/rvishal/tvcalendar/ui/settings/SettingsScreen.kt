package com.rvishal.tvcalendar.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.rvishal.tvcalendar.data.model.EventSource

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavHostController): Unit {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vm: SettingsViewModel = viewModel {
        SettingsViewModel(
            context.applicationContext,
            com.rvishal.tvcalendar.di.AppGraph.settings,
            com.rvishal.tvcalendar.di.AppGraph.googleAuth,
            com.rvishal.tvcalendar.di.AppGraph.repository,
        )
    }
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize().padding(48.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Settings", fontSize = 30.sp)
            Button(onClick = { nav.popBackStack() }) { Text("Done") }
        }

        LazyColumn(Modifier.fillMaxWidth().padding(top = 24.dp)) {
            item {
                Text("Sources", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            }
            EventSource.entries.forEach { src ->
                item(key = "src_${src.name}") {
                    SettingSwitch(
                        label = "${src.label}${srcStatus(src, state)}",
                        checked = state.sourceEnabled[src.name] == true,
                    ) { vm.setSourceEnabled(src.name, it) }
                }
            }

            item {
                Text("Google Calendar", fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 24.dp))
                when (val s = state.google) {
                    is GoogleState.SignedOut -> {
                        Text(
                            "Sign in to sync your Google Calendar.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        )
                        Button(onClick = vm::startGoogleSignIn, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Sign in with Google")
                        }
                    }
                    is GoogleState.SignInFailed -> {
                        Text(
                            "Sign-in failed: ${s.message}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Button(onClick = vm::startGoogleSignIn, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Try again")
                        }
                    }
                    is GoogleState.AwaitingUser -> {
                        Text(
                            "On your phone or computer go to ${s.verificationUrl} and enter code:",
                            fontSize = 15.sp,
                        )
                        Text(s.userCode, fontSize = 34.sp, modifier = Modifier.padding(vertical = 8.dp))
                        Text("Waiting for sign-in…", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        Button(onClick = vm::cancelGoogleSignIn, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Cancel")
                        }
                    }
                    is GoogleState.SignedIn -> {
                        Text("Signed in. Google events sync automatically.", fontSize = 14.sp)
                        Button(onClick = vm::signOutGoogle, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Sign out")
                        }
                    }
                }
            }

            item {
                Text("Apple Calendar (CalDAV)", fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 24.dp))
                Text(
                    "Apple ID + app-specific password (appleid.apple.com). Server: ${state.settings.caldavBaseUrl}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
                SettingInput(
                    label = "Apple ID (email)",
                    value = state.caldavUser,
                    onChange = vm::setCaldavUser,
                )
                SettingInput(
                    label = "App-specific password",
                    value = state.caldavPassword,
                    password = true,
                    onChange = vm::setCaldavPassword,
                )
                SettingInput(
                    label = "CalDAV server",
                    value = state.caldavBaseUrl,
                    onChange = vm::setCaldavUrl,
                )
            }

            item {
                Text("Hindu festivals (Karnataka)", fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 24.dp))
                Text(
                    "ICS feed URL — default covers major Karnataka Hindu festivals.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
                SettingInput(
                    label = "Festival feed URL",
                    value = state.festivalUrl,
                    onChange = vm::setFestivalUrl,
                )
            }

            item {
                Text("Display", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 24.dp))
                SettingSwitch("24-hour time", state.use24h) { vm.set24h(it) }
            }

            item {
                Button(onClick = vm::syncNow, modifier = Modifier.padding(top = 24.dp)) {
                    Text("Sync now")
                }
                if (state.syncErrors.isNotEmpty()) {
                    Text(
                        state.syncErrors.entries.joinToString("\n") { "${it.key}: ${it.value}" },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

private fun srcStatus(src: EventSource, state: SettingsUiState): String = when (src) {
    EventSource.GOOGLE -> if (state.google is GoogleState.SignedIn) "" else " (not signed in)"
    EventSource.APPLE -> if (state.caldavUser.isNotBlank() && state.caldavPassword.isNotBlank()) "" else " (not configured)"
    else -> ""
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 16.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SettingInput(
    label: String,
    value: String,
    password: Boolean = false,
    onChange: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (password)
                androidx.compose.ui.text.input.PasswordVisualTransformation()
            else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardActions = KeyboardActions(),
        )
    }
}
