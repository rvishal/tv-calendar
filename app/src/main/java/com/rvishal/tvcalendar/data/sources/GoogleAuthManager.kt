package com.rvishal.tvcalendar.data.sources

import com.rvishal.tvcalendar.data.prefs.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

class GoogleAuthManager(
    private val client: OkHttpClient,
    private val settings: SettingsStore,
    private val json: Json,
) {
    private val scope = "https://www.googleapis.com/auth/calendar.readonly"

    @Serializable
    data class DeviceCodeResponse(
        @SerialName("device_code") val deviceCode: String,
        @SerialName("user_code") val userCode: String,
        @SerialName("verification_url") val verificationUrl: String,
        @SerialName("expires_in") val expiresIn: Int,
        val interval: Int = 5,
    )

    @Serializable
    data class TokenResponse(
        @SerialName("access_token") val accessToken: String? = null,
        @SerialName("refresh_token") val refreshToken: String? = null,
        @SerialName("expires_in") val expiresIn: Long = 0,
        val error: String? = null,
    )

    suspend fun requestDeviceCode(clientId: String): DeviceCodeResponse = withContext(Dispatchers.IO) {
        val body = FormBody.Builder().add("client_id", clientId).add("scope", scope).build()
        val req = Request.Builder().url("https://oauth2.googleapis.com/device/code").post(body).build()
        client.newCall(req).execute().use { resp ->
            check(resp.isSuccessful) { "device code request failed: ${resp.code}" }
            json.decodeFromString(DeviceCodeResponse.serializer(), resp.body!!.string())
        }
    }

    /** Polls until the user completes sign-in or the code expires. Returns the access token. */
    suspend fun awaitSignIn(clientId: String, clientSecret: String, deviceCode: String, intervalSec: Int, timeoutSec: Int): String {
        var waited = 0
        while (waited < timeoutSec) {
            val token = poll(clientId, clientSecret, deviceCode)
            when (token.error) {
                null -> {
                    token.accessToken?.let { storeTokens(token) }
                    return token.accessToken!!
                }
                "authorization_pending" -> {}
                "slow_down" -> delay(5000)
                else -> error("sign-in failed: ${token.error}")
            }
            delay(intervalSec * 1000L)
            waited += intervalSec
        }
        error("sign-in timed out")
    }

    private suspend fun poll(clientId: String, clientSecret: String, deviceCode: String): TokenResponse = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("device_code", deviceCode)
            .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
            .build()
        val req = Request.Builder().url("https://oauth2.googleapis.com/token").post(body).build()
        client.newCall(req).execute().use { resp ->
            json.decodeFromString(TokenResponse.serializer(), resp.body!!.string())
        }
    }

    suspend fun validAccessToken(clientId: String, clientSecret: String): String? {
        val refresh = settings.googleRefreshToken ?: return null
        if (settings.googleAccessToken != null &&
            System.currentTimeMillis() < settings.googleTokenExpiry - 60_000
        ) return settings.googleAccessToken

        val body = FormBody.Builder()
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("refresh_token", refresh)
            .add("grant_type", "refresh_token")
            .build()
        val req = Request.Builder().url("https://oauth2.googleapis.com/token").post(body).build()
        return withContext(Dispatchers.IO) {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    settings.googleAccessToken = null
                    null
                } else {
                    val token = json.decodeFromString(TokenResponse.serializer(), resp.body!!.string())
                    storeTokens(token)
                    token.accessToken
                }
            }
        }
    }

    private fun storeTokens(token: TokenResponse) {
        token.accessToken?.let { settings.googleAccessToken = it }
        token.refreshToken?.let { settings.googleRefreshToken = it }
        settings.googleTokenExpiry = System.currentTimeMillis() + token.expiresIn * 1000
    }

    fun signOut() {
        settings.googleAccessToken = null
        settings.googleRefreshToken = null
        settings.googleTokenExpiry = 0
        settings.googleSyncToken = null
    }
}
