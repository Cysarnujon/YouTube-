package com.tubegroups.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tubegroups.app.data.LocalStore
import com.tubegroups.app.data.StoredData
import com.tubegroups.app.data.YouTubeRssRepository
import com.tubegroups.app.model.Channel
import com.tubegroups.app.model.ChannelGroup
import com.tubegroups.app.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.UUID

data class MainUiState(
    val groups: List<ChannelGroup> = emptyList(),
    val channels: List<Channel> = emptyList(),
    val videos: List<VideoItem> = emptyList(),
    val watchedIds: Set<String> = emptySet(),
    val selectedGroupId: String? = null, // null = Todos, "ungrouped" = Sem grupo
    val searchQuery: String = "",
    val hideShorts: Boolean = false,
    val hideWatched: Boolean = false,
    val isLoadingFeed: Boolean = false,
    val isAddingChannel: Boolean = false,
    val statusMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val store = LocalStore(application)
    private val repo = YouTubeRssRepository()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        val initial = store.load()
        _uiState.update {
            it.copy(
                groups = initial.groups,
                channels = initial.channels,
                watchedIds = initial.watchedVideoIds
            )
        }
        if (initial.channels.isNotEmpty()) {
            refreshFeed()
        }
    }

    fun refreshFeed() {
        val channels = _uiState.value.channels
        if (channels.isEmpty()) {
            _uiState.update { it.copy(videos = emptyList(), isLoadingFeed = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFeed = true) }
            val semaphore = Semaphore(6)
            val allVideos = channels.map { channel ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        repo.fetchChannelVideos(channel)
                    }
                }
            }.awaitAll().flatten()
                .distinctBy { it.id }
                .sortedByDescending { it.publishedAtEpochMillis }

            _uiState.update {
                it.copy(
                    videos = allVideos,
                    isLoadingFeed = false
                )
            }
        }
    }

    fun selectGroup(groupId: String?) {
        _uiState.update { it.copy(selectedGroupId = groupId) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleHideShorts() {
        _uiState.update { it.copy(hideShorts = !it.hideShorts) }
    }

    fun toggleHideWatched() {
        _uiState.update { it.copy(hideWatched = !it.hideWatched) }
    }

    fun markVideoWatched(videoId: String) {
        _uiState.update { state ->
            val updated = state.watchedIds + videoId
            persist(state.copy(watchedIds = updated))
            state.copy(watchedIds = updated)
        }
    }

    fun toggleVideoWatched(videoId: String) {
        _uiState.update { state ->
            val updated = if (videoId in state.watchedIds) {
                state.watchedIds - videoId
            } else {
                state.watchedIds + videoId
            }
            persist(state.copy(watchedIds = updated))
            state.copy(watchedIds = updated)
        }
    }

    fun addChannel(rawInput: String, groupIds: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingChannel = true) }
            val result = repo.resolveChannel(rawInput, groupIds)
            result.onSuccess { channel ->
                val exists = _uiState.value.channels.any { it.id == channel.id }
                if (exists) {
                    _uiState.update {
                        it.copy(
                            isAddingChannel = false,
                            statusMessage = "Canal '${channel.title}' já está cadastrado."
                        )
                    }
                } else {
                    _uiState.update { state ->
                        val updatedChannels = (state.channels + channel).sortedBy { it.title.lowercase() }
                        val nextState = state.copy(
                            channels = updatedChannels,
                            isAddingChannel = false,
                            statusMessage = "Canal '${channel.title}' adicionado!"
                        )
                        persist(nextState)
                        nextState
                    }
                    refreshFeed()
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isAddingChannel = false,
                        statusMessage = err.message ?: "Erro ao adicionar canal."
                    )
                }
            }
        }
    }

    fun updateChannelGroups(channelId: String, groupIds: List<String>) {
        _uiState.update { state ->
            val updated = state.channels.map {
                if (it.id == channelId) it.copy(groupIds = groupIds) else it
            }
            val next = state.copy(channels = updated)
            persist(next)
            next
        }
    }

    fun removeChannel(channelId: String) {
        _uiState.update { state ->
            val updatedChannels = state.channels.filterNot { it.id == channelId }
            val updatedVideos = state.videos.filterNot { it.channelId == channelId }
            val next = state.copy(
                channels = updatedChannels,
                videos = updatedVideos,
                statusMessage = "Canal removido."
            )
            persist(next)
            next
        }
    }

    fun createGroup(name: String, emoji: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val newGroup = ChannelGroup(
            id = "grp_${UUID.randomUUID().toString().take(8)}",
            name = cleanName,
            emoji = emoji.ifBlank { "📁" }
        )
        _uiState.update { state ->
            val next = state.copy(
                groups = state.groups + newGroup,
                statusMessage = "Grupo '$cleanName' criado!"
            )
            persist(next)
            next
        }
    }

    fun deleteGroup(groupId: String) {
        _uiState.update { state ->
            val updatedGroups = state.groups.filterNot { it.id == groupId }
            val updatedChannels = state.channels.map { ch ->
                ch.copy(groupIds = ch.groupIds.filterNot { it == groupId })
            }
            val selected = if (state.selectedGroupId == groupId) null else state.selectedGroupId
            val next = state.copy(
                groups = updatedGroups,
                channels = updatedChannels,
                selectedGroupId = selected,
                statusMessage = "Grupo removido."
            )
            persist(next)
            next
        }
    }

    fun importTakeoutCsv(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = getApplication<Application>()
                    .contentResolver
                    .openInputStream(uri) ?: return@launch

                val imported = inputStream.use { repo.parseTakeoutCsv(it) }
                if (imported.isEmpty()) {
                    _uiState.update {
                        it.copy(statusMessage = "Nenhum canal válido encontrado no CSV.")
                    }
                    return@launch
                }

                var addedCount = 0
                _uiState.update { state ->
                    val existingIds = state.channels.map { it.id }.toSet()
                    val newChannels = imported.filterNot { it.id in existingIds }
                    addedCount = newChannels.size
                    val merged = (state.channels + newChannels).sortedBy { it.title.lowercase() }
                    val next = state.copy(
                        channels = merged,
                        statusMessage = "$addedCount canais importados do CSV!"
                    )
                    persist(next)
                    next
                }
                if (addedCount > 0) refreshFeed()
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(statusMessage = "Erro ao ler o arquivo CSV.")
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun persist(state: MainUiState) {
        store.save(
            StoredData(
                groups = state.groups,
                channels = state.channels,
                watchedVideoIds = state.watchedIds
            )
        )
    }
}
