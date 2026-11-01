package com.rvishal.tvcalendar.data.sources

import com.rvishal.tvcalendar.data.model.CalendarEvent
import com.rvishal.tvcalendar.data.model.EventSource
import com.rvishal.tvcalendar.data.prefs.SettingsStore
import com.rvishal.tvcalendar.util.IcsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.TimeZone
/**
 * Minimal CalDAV client (Apple iCloud by default): principal discovery ->
 * calendar-home-set -> calendar list -> REPORT calendar-query -> ICS parse.
 */
class CalDavSource(
    private val client: OkHttpClient,
    private val settings: SettingsStore,
) : EventDataSource {

    override val source = EventSource.APPLE
    override val isConfigured: Boolean get() = settings.isCaldavConfigured

    private val xmlFactory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
    private val davDate = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'").apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    data class DavCalendar(val url: String, val name: String, val color: Long?)

    override suspend fun fetch(windowStartMillis: Long, windowEndMillis: Long): List<CalendarEvent> {
        val calendars = discoverCalendars()
        val out = mutableListOf<CalendarEvent>()
        for (cal in calendars) {
            for ((xml, parser) in listOfNotNull(
                reportCalendar(cal.url, "VEVENT", windowStartMillis, windowEndMillis)?.let { it to IcsParser::parse },
                reportCalendar(cal.url, "VTODO", windowStartMillis, windowEndMillis)?.let { it to IcsParser::parseTodos },
            )) {
                for (ics in extractCalendarData(xml)) {
                    runCatching {
                        out += parser(ics, EventSource.APPLE, cal.name, windowStartMillis, windowEndMillis)
                            .map { it.copy(color = cal.color ?: it.color) }
                    }.onFailure { android.util.Log.w("CalDavSource", "ics parse failed for ${cal.name}", it) }
                }
            }
        }
        return out
    }

    /** Pulls each <calendar-data> ICS block out of a REPORT multistatus response. */
    private fun extractCalendarData(xml: String): List<String> {
        val parser = xmlFactory.newPullParser().apply { setInput(StringReader(xml)) }
        val out = mutableListOf<String>()
        val buf = StringBuilder()
        var inside = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> if (parser.name == "calendar-data") { inside = true; buf.clear() }
                XmlPullParser.TEXT -> if (inside) buf.append(parser.text)
                XmlPullParser.END_TAG -> if (parser.name == "calendar-data" && inside) {
                    inside = false
                    val ics = buf.toString().trim()
                    if (ics.isNotEmpty()) out += ics
                }
            }
            event = parser.next()
        }
        return out
    }

    suspend fun discoverCalendars(): List<DavCalendar> {
        val base = settings.caldavBaseUrl.trimEnd('/')
        val principal = propfindSingle(
            url = base, depth = "0",
            props = listOf("current-user-principal" to "DAV:"),
            wantedHrefTag = "current-user-principal",
        ) ?: return emptyList()

        val homeSet = propfindSingle(
            url = absolute(base, principal), depth = "0",
            props = listOf("calendar-home-set" to "urn:ietf:params:xml:ns:caldav"),
            wantedHrefTag = "calendar-home-set",
        ) ?: return emptyList()

        val multistatus = propfindRaw(
            url = absolute(base, homeSet), depth = "1",
            body = propfindBody(
                "resourcetype" to "DAV:", "displayname" to "DAV:",
                "calendar-color" to "http://apple.com/ns/ical/",
            ),
        ) ?: return emptyList()

        return parseCalendarList(absolute(base, homeSet), multistatus)
    }

    private suspend fun reportCalendar(calendarUrl: String, compType: String, start: Long, end: Long): String? {
        val body = """
            <c:calendar-query xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
              <d:prop><d:getetag/><c:calendar-data/></d:prop>
              <c:filter><c:comp-filter name="VCALENDAR"><c:comp-filter name="$compType">
                <c:time-range start="${davDate.format(start)}" end="${davDate.format(end)}"/>
              </c:comp-filter></c:comp-filter></c:filter>
            </c:calendar-query>
        """.trimIndent()
        return davRequest("REPORT", calendarUrl, "1", body)
    }

    private suspend fun propfindSingle(url: String, depth: String, props: List<Pair<String, String>>, wantedHrefTag: String): String? {
        val body = propfindBody(*props.toTypedArray())
        val xml = propfindRaw(url, depth, body) ?: return null
        return extractHref(xml, wantedHrefTag)
    }

    private suspend fun propfindRaw(url: String, depth: String, body: String): String? =
        davRequest("PROPFIND", url, depth, body)

    private suspend fun davRequest(method: String, url: String, depth: String, body: String): String? =
        withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url(url)
                .method(method, body.toRequestBody("application/xml; charset=utf-8".toMediaType()))
                .header("Depth", depth)
                .header("Authorization", Credentials.basic(settings.caldavUser!!, settings.caldavPassword!!))
                .build()
            runCatching {
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.string() else {
                        android.util.Log.w("CalDavSource", "$method $url -> ${resp.code}")
                        null
                    }
                }
            }.onFailure { android.util.Log.w("CalDavSource", "$method $url failed", it) }.getOrNull()
        }

    private fun propfindBody(vararg props: Pair<String, String>): String {
        val inner = props.joinToString("") { (name, ns) ->
            if (ns == "DAV:") "<d:$name/>" else "<x:$name xmlns:x=\"$ns\"/>"
        }
        return "<d:propfind xmlns:d=\"DAV:\"><d:prop>$inner</d:prop></d:propfind>"
    }

    private fun extractHref(xml: String, wantedTag: String): String? {
        val parser = xmlFactory.newPullParser().apply { setInput(StringReader(xml)) }
        var insideWanted = false
        var insideHref = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    wantedTag -> insideWanted = true
                    "href" -> if (insideWanted) insideHref = true
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    wantedTag -> insideWanted = false
                    "href" -> insideHref = false
                }
                XmlPullParser.TEXT -> if (insideWanted && insideHref) {
                    return parser.text?.trim()
                }
            }
            event = parser.next()
        }
        return null
    }

    private fun parseCalendarList(homeSetUrl: String, xml: String): List<DavCalendar> {
        val parser = xmlFactory.newPullParser().apply { setInput(StringReader(xml)) }
        val calendars = mutableListOf<DavCalendar>()
        var href: String? = null
        var name: String? = null
        var color: Long? = null
        var isCalendar = false
        var currentTag = ""
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag == "resourcetype") isCalendar = false
                    if (currentTag == "calendar") isCalendar = true
                    if (currentTag == "response") { href = null; name = null; color = null; isCalendar = false }
                }
                XmlPullParser.TEXT -> {
                    when (currentTag) {
                        "href" -> if (href == null) href = parser.text?.trim()
                        "displayname" -> name = parser.text?.trim()
                        "calendar-color" -> color = parseColor(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "response") {
                        if (isCalendar && href != null && href != homeSetUrl && !href.endsWith("/calendars/")) {
                            calendars += DavCalendar(absolute(homeSetUrl, href), name ?: "Calendar", color)
                        }
                    }
                }
            }
            event = parser.next()
        }
        return calendars
    }

    private fun parseColor(text: String?): Long? {
        val t = text?.trim()?.removePrefix("#") ?: return null
        return runCatching { ("FF" + t.takeLast(6)).toLong(16) }.getOrNull()
    }

    private fun absolute(base: String, href: String): String {
        if (href.startsWith("http")) return href
        val schemeEnd = base.indexOf('/', base.indexOf("://") + 3)
        return if (schemeEnd > 0) base.substring(0, schemeEnd) + href else base + href
    }
}
