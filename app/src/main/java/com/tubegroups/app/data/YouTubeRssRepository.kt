package com.tubegroups.app.data

import android.util.Xml
import com.tubegroups.app.model.Channel
import com.tubegroups.app.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class YouTubeRssRepository {

    private val channelIdRegex = Regex("""UC[a-zA-Z0-9_-]{22}""")

    suspend fun resolveChannel(rawInput: String, groupIds: List<String>): Result<Channel> =
        withContext(Dispatchers.IO) {
            runCatching {
                val input = rawInput.trim()
                require(input.isNotEmpty()) { "Informe um @handle, link ou ID do canal." }

                val directMatch = channelIdRegex.find(input)?.value
                val channelId = directMatch ?: resolveIdFromWebPage(input)
                require(!channelId.isNullOrBlank()) {
                    "Não foi possível encontrar o ID (UC...) desse canal."
                }

                val title = fetchChannelTitle(channelId) ?: input.removePrefix("https://")
                    .removePrefix("http://")
                    .removePrefix("www.")
                    .take(40)

                Channel(
                    id = channelId,
                    title = title,
                    groupIds = groupIds
                )
            }
        }

    private fun resolveIdFromWebPage(input: String): String? {
        val targetUrl = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.startsWith("@") -> "https://www.youtube.com/$input"
            else -> "https://www.youtube.com/@$input"
        }

        val html = httpGet(targetUrl) ?: return null
        val patterns = listOf(
            Regex(""""externalId"\s*:\s*"(UC[a-zA-Z0-9_-]{22})""""),
            Regex(""""channelId"\s*:\s*"(UC[a-zA-Z0-9_-]{22})""""),
            Regex("""https://www\.youtube\.com/channel/(UC[a-zA-Z0-9_-]{22})""")
        )
        for (pattern in patterns) {
            val match = pattern.find(html)?.groupValues?.getOrNull(1)
            if (match != null) return match
        }
        return null
    }

    private fun fetchChannelTitle(channelId: String): String? {
        val xml = httpGet("https://www.youtube.com/feeds/videos.xml?channel_id=$channelId")
            ?: return null
        return try {
            val parser = Xml.newPullParser()
            parser.setInput(xml.reader())
            var event = parser.eventType
            var insideEntry = false
            while (event != XmlPullParser.END_DOCUMENT) {
                val name = parser.name?.substringAfterLast(':')
                if (event == XmlPullParser.START_TAG) {
                    if (name == "entry") insideEntry = true
                    if (!insideEntry && name == "title") {
                        return parser.nextText().trim()
                    }
                }
                event = parser.next()
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchChannelVideos(channel: Channel): List<VideoItem> = withContext(Dispatchers.IO) {
        val xml = httpGet("https://www.youtube.com/feeds/videos.xml?channel_id=${channel.id}")
            ?: return@withContext emptyList()

        val videos = mutableListOf<VideoItem>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(xml.reader())
            var event = parser.eventType

            var insideEntry = false
            var videoId = ""
            var title = ""
            var url = ""
            var published = ""
            var feedAuthor = channel.title

            while (event != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name?.substringAfterLast(':') ?: ""
                when (event) {
                    XmlPullParser.START_TAG -> {
                        when (tag) {
                            "entry" -> {
                                insideEntry = true
                                videoId = ""
                                title = ""
                                url = ""
                                published = ""
                            }
                            "title" -> {
                                val text = parser.nextText().trim()
                                if (insideEntry) {
                                    if (title.isEmpty()) title = text
                                } else if (text.isNotEmpty()) {
                                    feedAuthor = text
                                }
                            }
                            "videoId" -> if (insideEntry) videoId = parser.nextText().trim()
                            "published" -> if (insideEntry) published = parser.nextText().trim()
                            "link" -> if (insideEntry) {
                                val rel = parser.getAttributeValue(null, "rel")
                                val href = parser.getAttributeValue(null, "href")
                                if ((rel == null || rel == "alternate") && !href.isNullOrBlank()) {
                                    url = href
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        if (tag == "entry" && insideEntry) {
                            insideEntry = false
                            if (videoId.isNotBlank() && title.isNotBlank()) {
                                val finalUrl = url.ifBlank {
                                    "https://www.youtube.com/watch?v=$videoId"
                                }
                                val epoch = parseEpochMillis(published)
                                videos.add(
                                    VideoItem(
                                        id = videoId,
                                        channelId = channel.id,
                                        channelTitle = feedAuthor,
                                        title = title,
                                        url = finalUrl,
                                        thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                                        publishedAtEpochMillis = epoch,
                                        publishedLabel = formatRelativeDate(epoch),
                                        isShort = finalUrl.contains("/shorts/")
                                    )
                                )
                            }
                        }
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {
            return@withContext emptyList()
        }
        videos
    }

    suspend fun fetchRelatedVideos(titleQuery: String, excludeVideoId: String): List<VideoItem> =
        withContext(Dispatchers.IO) {
            try {
                val cleaned = titleQuery
                    .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
                    .split(Regex("""\s+"""))
                    .filter { it.length >= 3 }
                    .take(6)
                    .joinToString(" ")
                    .ifBlank { titleQuery }

                val encoded = URLEncoder.encode(cleaned, "UTF-8")
                val html = httpGet("https://www.youtube.com/results?search_query=$encoded")
                    ?: return@withContext emptyList()

                val marker = "var ytInitialData = "
                val start = html.indexOf(marker)
                if (start < 0) return@withContext emptyList()
                val end = html.indexOf(";</script>", start + marker.length)
                if (end < 0) return@withContext emptyList()

                val json = JSONObject(html.substring(start + marker.length, end))
                val results = mutableListOf<VideoItem>()
                collectVideoRenderers(json, results, excludeVideoId, limit = 15)
                results
            } catch (_: Exception) {
                emptyList()
            }
        }

    private fun collectVideoRenderers(
        node: Any?,
        out: MutableList<VideoItem>,
        excludeId: String,
        limit: Int
    ) {
        if (out.size >= limit || node == null) return
        when (node) {
            is JSONObject -> {
                val vr = node.optJSONObject("videoRenderer")
                if (vr != null) {
                    val id = vr.optString("videoId", "")
                    val title = vr.optJSONObject("title")
                        ?.optJSONArray("runs")
                        ?.optJSONObject(0)
                        ?.optString("text", "") ?: ""
                    val ownerRun = vr.optJSONObject("ownerText")
                        ?.optJSONArray("runs")
                        ?.optJSONObject(0)
                    val channel = ownerRun?.optString("text", "") ?: ""
                    val channelId = ownerRun?.optJSONObject("navigationEndpoint")
                        ?.optJSONObject("browseEndpoint")
                        ?.optString("browseId", "") ?: ""
                    val published = vr.optJSONObject("publishedTimeText")
                        ?.optString("simpleText", "") ?: ""

                    if (id.length == 11 && id != excludeId && title.isNotBlank() && out.none { it.id == id }) {
                        out.add(
                            VideoItem(
                                id = id,
                                channelId = channelId,
                                channelTitle = channel.ifBlank { "YouTube" },
                                title = title,
                                url = "https://www.youtube.com/watch?v=$id",
                                thumbnailUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg",
                                publishedAtEpochMillis = 0L,
                                publishedLabel = published.ifBlank { "Relacionado" },
                                isShort = false
                            )
                        )
                    }
                }
                val keys = node.keys()
                while (keys.hasNext() && out.size < limit) {
                    collectVideoRenderers(node.opt(keys.next()), out, excludeId, limit)
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    if (out.size >= limit) break
                    collectVideoRenderers(node.opt(i), out, excludeId, limit)
                }
            }
        }
    }

    fun parseTakeoutCsv(inputStream: InputStream): List<Channel> {
        val channels = mutableListOf<Channel>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        var isFirst = true

        reader.forEachLine { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEachLine
            if (isFirst) {
                isFirst = false
                if (line.contains("Channel Id", ignoreCase = true) ||
                    line.contains("ID do canal", ignoreCase = true)
                ) {
                    return@forEachLine
                }
            }

            val cols = splitCsvLine(line)
            if (cols.size >= 3) {
                val id = cols[0].trim()
                val title = cols[2].trim()
                if (channelIdRegex.matches(id)) {
                    channels.add(Channel(id = id, title = title.ifBlank { id }))
                }
            }
        }
        return channels
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }

    private fun httpGet(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                instanceFollowRedirects = true
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
                )
                setRequestProperty("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun parseEpochMillis(iso: String): Long {
        return try {
            OffsetDateTime.parse(iso).toInstant().toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }

    private fun formatRelativeDate(epochMillis: Long): String {
        if (epochMillis <= 0L) return ""
        val now = Instant.now()
        val then = Instant.ofEpochMilli(epochMillis)
        val diff = Duration.between(then, now)
        return when {
            diff.toMinutes() < 60 -> "há ${diff.toMinutes().coerceAtLeast(1)} min"
            diff.toHours() < 24 -> "há ${diff.toHours()} h"
            diff.toDays() < 7 -> "há ${diff.toDays()} d"
            else -> DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withZone(ZoneId.systemDefault())
                .format(then)
        }
    }
                  }
