package com.rvishal.tvcalendar.data.prefs

import android.content.Context
import android.content.SharedPreferences
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("tv_calendar_settings", Context.MODE_PRIVATE)

    var googleAccessToken: String?
        get() = prefs.getString("google_access_token", null)
        set(v) = prefs.edit().putString("google_access_token", v).apply()

    var googleRefreshToken: String?
        get() = prefs.getString("google_refresh_token", null)
        set(v) = prefs.edit().putString("google_refresh_token", v).apply()

    var googleTokenExpiry: Long
        get() = prefs.getLong("google_token_expiry", 0L)
        set(v) = prefs.edit().putLong("google_token_expiry", v).apply()

    var googleSyncToken: String?
        get() = prefs.getString("google_sync_token", null)
        set(v) = prefs.edit().putString("google_sync_token", v).apply()

    var caldavUser: String?
        get() = prefs.getString("caldav_user", null)
        set(v) = prefs.edit().putString("caldav_user", v).apply()

    var caldavPassword: String?
        get() = prefs.getString("caldav_password", null)
        set(v) = prefs.edit().putString("caldav_password", v).apply()

    var caldavBaseUrl: String
        get() = prefs.getString("caldav_base_url", "https://caldav.icloud.com")!!
        set(v) = prefs.edit().putString("caldav_base_url", v).apply()

    var festivalIcsUrl: String
        get() = prefs.getString("festival_ics_url", DEFAULT_FESTIVAL_ICS)!!
        set(v) = prefs.edit().putString("festival_ics_url", v).apply()

    var use24Hour: Boolean
        get() = prefs.getBoolean("use_24h", false)
        set(v) = prefs.edit().putBoolean("use_24h", v).apply()

    var telegramBotToken: String?
        get() = prefs.getString("telegram_bot_token", null)
        set(v) = prefs.edit().putString("telegram_bot_token", v).apply()

    var telegramChatId: String?
        get() = prefs.getString("telegram_chat_id", null)
        set(v) = prefs.edit().putString("telegram_chat_id", v).apply()

    var telegramDigestEnabled: Boolean
        get() = prefs.getBoolean("telegram_digest", false)
        set(v) = prefs.edit().putBoolean("telegram_digest", v).apply()

    var homeChannelAsked: Boolean
        get() = prefs.getBoolean("home_channel_asked", false)
        set(v) = prefs.edit().putBoolean("home_channel_asked", v).apply()

    var homeChannelId: Long
        get() = prefs.getLong("home_channel_id", -1L)
        set(v) = prefs.edit().putLong("home_channel_id", v).apply()

    /** Program IDs we inserted into the home channel — needed because this TV's
     *  provider rejects delete-by-selection, so we delete by URI next publish. */
    var homeChannelProgramIds: Set<String>
        get() = prefs.getStringSet("home_channel_programs", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("home_channel_programs", v).apply()

    val isTelegramConfigured: Boolean
        get() = !telegramBotToken.isNullOrBlank() && !telegramChatId.isNullOrBlank()

    fun isSourceEnabled(source: String): Boolean = prefs.getBoolean("src_$source", true)
    fun setSourceEnabled(source: String, enabled: Boolean) =
        prefs.edit().putBoolean("src_$source", enabled).apply()

    val isGoogleSignedIn: Boolean get() = googleRefreshToken != null
    val isCaldavConfigured: Boolean get() = !caldavUser.isNullOrBlank() && !caldavPassword.isNullOrBlank()

    companion object {
        // Google's public "Hindu Festivals" calendar covers Karnataka festivals
        // (Ugadi, Varamahalakshmi, Ayudha Puja, Balipadyami, etc.).
        const val DEFAULT_FESTIVAL_ICS =
            "https://calendar.google.com/calendar/ical/en.hinduism%23holiday%40group.v.calendar.google.com/public/basic.ics"
    }
}
