package com.tubegroups.app.model

data class ChannelGroup(
    val id: String,
    val name: String,
    val emoji: String
)

data class Channel(
    val id: String,
    val title: String,
    val groupIds: List<String> = emptyList()
)

data class VideoItem(
    val id: String,
    val channelId: String,
    val channelTitle: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String,
    val publishedAtEpochMillis: Long,
    val publishedLabel: String,
    val isShort: Boolean
)
