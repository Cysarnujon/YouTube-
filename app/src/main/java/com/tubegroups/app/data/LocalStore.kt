package com.tubegroups.app.data

import android.content.Context
import com.tubegroups.app.model.Channel
import com.tubegroups.app.model.ChannelGroup
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class StoredData(
    val groups: List<ChannelGroup>,
    val channels: List<Channel>,
    val watchedVideoIds: Set<String>
)

class LocalStore(context: Context) {

    private val file = File(context.filesDir, "tubegroups_data.json")

    fun load(): StoredData {
        if (!file.exists()) {
            val defaultGroups = listOf(
                ChannelGroup(id = "grp_tech", name = "Tecnologia", emoji = "💻"),
                ChannelGroup(id = "grp_games", name = "Jogos", emoji = "🎮"),
                ChannelGroup(id = "grp_music", name = "Música", emoji = "🎵")
            )
            return StoredData(defaultGroups, emptyList(), emptySet())
        }

        return try {
            val root = JSONObject(file.readText())
            val groupsJson = root.optJSONArray("groups") ?: JSONArray()
            val channelsJson = root.optJSONArray("channels") ?: JSONArray()
            val watchedJson = root.optJSONArray("watched") ?: JSONArray()

            val groups = buildList {
                for (i in 0 until groupsJson.length()) {
                    val obj = groupsJson.getJSONObject(i)
                    add(
                        ChannelGroup(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            emoji = obj.optString("emoji", "📁")
                        )
                    )
                }
            }

            val channels = buildList {
                for (i in 0 until channelsJson.length()) {
                    val obj = channelsJson.getJSONObject(i)
                    val gIds = obj.optJSONArray("groupIds") ?: JSONArray()
                    val groupList = buildList {
                        for (j in 0 until gIds.length()) add(gIds.getString(j))
                    }
                    add(
                        Channel(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            groupIds = groupList
                        )
                    )
                }
            }

            val watched = buildSet {
                for (i in 0 until watchedJson.length()) {
                    add(watchedJson.getString(i))
                }
            }

            StoredData(groups, channels, watched)
        } catch (_: Exception) {
            StoredData(emptyList(), emptyList(), emptySet())
        }
    }

    fun save(data: StoredData) {
        val root = JSONObject()

        val groupsJson = JSONArray()
        data.groups.forEach { g ->
            groupsJson.put(
                JSONObject()
                    .put("id", g.id)
                    .put("name", g.name)
                    .put("emoji", g.emoji)
            )
        }

        val channelsJson = JSONArray()
        data.channels.forEach { c ->
            val gIds = JSONArray()
            c.groupIds.forEach { gIds.put(it) }
            channelsJson.put(
                JSONObject()
                    .put("id", c.id)
                    .put("title", c.title)
                    .put("groupIds", gIds)
            )
        }

        val watchedJson = JSONArray()
        data.watchedVideoIds.take(1000).forEach { watchedJson.put(it) }

        root.put("groups", groupsJson)
        root.put("channels", channelsJson)
        root.put("watched", watchedJson)

        file.writeText(root.toString())
    }
}
