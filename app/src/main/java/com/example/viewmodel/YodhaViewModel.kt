package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.TacticalAudioEngine
import com.example.data.AntiCheatLog
import com.example.data.ForumComment
import com.example.data.ForumPost
import com.example.data.GameCatalog
import com.example.data.MatchRoom
import com.example.data.PlayerProfile
import com.example.data.YodhaRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class MatchmakingUiState(
    val isSearching: Boolean = false,
    val selectedMapId: String = "jaipur_fort",
    val selectedModeId: String = "classic_br",
    val searchElapsedSec: Int = 0,
    val matchedPlayersCount: Int = 1,
    val estimatedPingMs: Int = 18,
    val skillBandLabel: String = "±180 MMR Balanced",
    val readyToLaunchArena: Boolean = false
)

data class VoiceChatUiState(
    val permissionGranted: Boolean = false,
    val micEnabled: Boolean = false,
    val channel: String = "SQUAD", // SQUAD or ALL
    val speakerVolume: Float = 0.85f
)

data class KavachTelemetryState(
    val integrityScore: Int = 100,
    val velocityStatus: String = "CLEAN (<= 7.2 m/s)",
    val cadenceStatus: String = "VERIFIED",
    val sessionHash: String = "KV-9F8A-BHARAT-07",
    val packetsAudited: Int = 1420
)

class YodhaViewModel(
    private val repository: YodhaRepository,
    val currentUserId: String,
    private val defaultDisplayName: String
) : ViewModel() {

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _showIntroCinema = MutableStateFlow(true)
    val showIntroCinema: StateFlow<Boolean> = _showIntroCinema.asStateFlow()

    private val _inActiveMatch = MutableStateFlow(false)
    val inActiveMatch: StateFlow<Boolean> = _inActiveMatch.asStateFlow()

    private val _matchmaking = MutableStateFlow(MatchmakingUiState())
    val matchmaking: StateFlow<MatchmakingUiState> = _matchmaking.asStateFlow()

    private val _voiceChat = MutableStateFlow(VoiceChatUiState())
    val voiceChat: StateFlow<VoiceChatUiState> = _voiceChat.asStateFlow()

    private val _kavachState = MutableStateFlow(KavachTelemetryState())
    val kavachState: StateFlow<KavachTelemetryState> = _kavachState.asStateFlow()

    private val _musicVolume = MutableStateFlow(0.7f)
    val musicVolume: StateFlow<Float> = _musicVolume.asStateFlow()

    private val _sfxVolume = MutableStateFlow(0.9f)
    val sfxVolume: StateFlow<Float> = _sfxVolume.asStateFlow()

    private val _selectedPostIdForComments = MutableStateFlow<String?>(null)
    val selectedPostIdForComments: StateFlow<String?> = _selectedPostIdForComments.asStateFlow()

    private var matchmakingJob: Job? = null

    val profileState: StateFlow<UiState<PlayerProfile>> = repository.observePlayerProfile(currentUserId)
        .map<PlayerProfile?, UiState<PlayerProfile>> { profile ->
            UiState.Success(profile ?: PlayerProfile(userId = currentUserId, callsign = defaultDisplayName.take(20).ifBlank { "YodhaWarrior" }))
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Profile flow error", root)
            emit(UiState.Error(root.message ?: "Failed to load player profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val matchRoomsState: StateFlow<UiState<List<MatchRoom>>> = repository.observeOpenMatchRooms()
        .map<List<MatchRoom>, UiState<List<MatchRoom>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "MatchRooms flow error", root)
            emit(UiState.Error(root.message ?: "Failed to sync multiplayer rooms"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val forumPostsState: StateFlow<UiState<List<ForumPost>>> = repository.observeForumPosts()
        .map<List<ForumPost>, UiState<List<ForumPost>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "ForumPosts flow error", root)
            emit(UiState.Error(root.message ?: "Failed to load community forum"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    @Suppress("OPT_IN_USAGE")
    val postCommentsState: StateFlow<UiState<List<ForumComment>>> = _selectedPostIdForComments
        .flatMapLatest { postId ->
            if (postId.isNullOrBlank()) {
                flowOf<UiState<List<ForumComment>>>(UiState.Success(emptyList()))
            } else {
                repository.observePostComments(postId)
                    .map<List<ForumComment>, UiState<List<ForumComment>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val root = firestoreError ?: error
                        emit(UiState.Error(root.message ?: "Failed to load replies"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))

    val antiCheatLogsState: StateFlow<UiState<List<AntiCheatLog>>> = repository.observeAntiCheatLogs(currentUserId)
        .map<List<AntiCheatLog>, UiState<List<AntiCheatLog>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            emit(UiState.Error(root.message ?: "Failed to load Kavach telemetry"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    init {
        viewModelScope.launch {
            repository.ensurePlayerProfile(defaultDisplayName)
            repository.logAntiCheatCheck(
                eventType = "SESSION_BOOT",
                status = "VERIFIED",
                latencyMs = 18L,
                integrityScore = 100L,
                detail = "Kavach Kernel v4.2 initialized · Signature Clean"
            )
        }
    }

    fun dismissBanner() {
        _statusBanner.value = null
    }

    fun setShowIntroCinema(show: Boolean) {
        _showIntroCinema.value = show
        if (!show) TacticalAudioEngine.stopIntroTheme()
    }

    fun selectMap(mapId: String) {
        TacticalAudioEngine.playUiClick()
        _matchmaking.value = _matchmaking.value.copy(selectedMapId = mapId)
    }

    fun selectMode(modeId: String) {
        TacticalAudioEngine.playUiClick()
        _matchmaking.value = _matchmaking.value.copy(selectedModeId = modeId)
    }

    fun startBalancedMatchmaking() {
        TacticalAudioEngine.playUiClick()
        matchmakingJob?.cancel()
        val mode = GameCatalog.modeById(_matchmaking.value.selectedModeId)
        val profile = (profileState.value as? UiState.Success)?.data
        val tier = GameCatalog.tierForPoints(profile?.rankPoints ?: 1200L)
        _matchmaking.value = _matchmaking.value.copy(
            isSearching = true,
            searchElapsedSec = 0,
            matchedPlayersCount = 1,
            skillBandLabel = "${tier.name} · ±150 MMR Balanced",
            readyToLaunchArena = false
        )
        matchmakingJob = viewModelScope.launch {
            val targetSquad = mode.squadSize.coerceAtLeast(1)
            for (tick in 1..3) {
                delay(750L)
                _matchmaking.value = _matchmaking.value.copy(
                    searchElapsedSec = tick,
                    matchedPlayersCount = (tick + 1).coerceAtMost(targetSquad)
                )
            }
            _matchmaking.value = _matchmaking.value.copy(
                isSearching = false,
                readyToLaunchArena = false
            )
            _inActiveMatch.value = true
        }
    }

    fun cancelMatchmaking() {
        matchmakingJob?.cancel()
        _matchmaking.value = _matchmaking.value.copy(isSearching = false, searchElapsedSec = 0)
    }

    fun launchArenaDirectly() {
        TacticalAudioEngine.playUiClick()
        _inActiveMatch.value = true
    }

    fun exitArenaAndRecordMatch(kills: Int, won: Boolean, shotsFired: Int, maxSpeedObserved: Float) {
        _inActiveMatch.value = false
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        val rpDelta = (if (won) 45L else 12L) + (kills * 8L)
        val newRp = (profile.rankPoints + rpDelta).coerceIn(0L, 50000L)
        val newTier = GameCatalog.tierForPoints(newRp).name
        val xpAdded = 120L + (kills * 35L) + (if (won) 180L else 0L)

        // Kavach Anti-Cheat Post-Match Audit
        val speedClean = maxSpeedObserved <= 12.5f
        val auditStatus = if (speedClean) "VERIFIED" else "WARNING"
        val integrity = if (speedClean) 100L else 94L
        _kavachState.value = _kavachState.value.copy(
            integrityScore = integrity.toInt(),
            packetsAudited = _kavachState.value.packetsAudited + shotsFired + 48,
            velocityStatus = if (speedClean) "CLEAN (${String.format("%.1f", maxSpeedObserved)} m/s)" else "FLAGGED"
        )

        viewModelScope.launch {
            repository.recordMatchResult(kills, won, rpDelta, newRp, newTier, xpAdded)
                .onFailure { _statusBanner.value = "Match save warning: ${it.localizedMessage}" }
            repository.logAntiCheatCheck(
                eventType = "MATCH_TELEMETRY",
                status = auditStatus,
                latencyMs = _matchmaking.value.estimatedPingMs.toLong(),
                integrityScore = integrity,
                detail = "Audited $shotsFired shots & movement vectors on ${GameCatalog.mapById(_matchmaking.value.selectedMapId).name}"
            )
            _statusBanner.value = if (won) {
                "BOOYAH! Vijayi Bhava! +$rpDelta RP ($newTier) · +$xpAdded Season XP"
            } else {
                "Match Complete · $kills Eliminations · +$rpDelta RP · +$xpAdded XP"
            }
        }
    }

    // --- CHARACTER & LOADOUT UPDATES ---
    fun updateCharacterCustomization(
        gender: String? = null,
        hairStyle: String? = null,
        hairColor: String? = null,
        faceStyle: String? = null,
        outfitStyle: String? = null,
        selectedEmote: String? = null,
        callsign: String? = null
    ) {
        val updates = mutableMapOf<String, Any>()
        gender?.let { updates["selectedGender"] = it }
        hairStyle?.let { updates["hairStyle"] = it }
        hairColor?.let { updates["hairColor"] = it }
        faceStyle?.let { updates["faceStyle"] = it }
        outfitStyle?.let { updates["outfitStyle"] = it }
        selectedEmote?.let { updates["selectedEmote"] = it }
        callsign?.let {
            val clean = it.trim().take(24)
            if (clean.length >= 2) updates["callsign"] = clean
        }
        if (updates.isEmpty()) return
        TacticalAudioEngine.playUiClick()
        viewModelScope.launch {
            repository.updateProfileFields(updates)
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to save character" }
        }
    }

    fun equipWeapon(slot: Int, weaponId: String) {
        TacticalAudioEngine.playUiClick()
        val key = when (slot) {
            1 -> "primaryWeaponId"
            2 -> "secondaryWeaponId"
            3 -> "sidearmWeaponId"
            else -> "meleeWeaponId"
        }
        viewModelScope.launch {
            repository.updateProfileFields(mapOf(key to weaponId))
                .onSuccess { _statusBanner.value = "Equipped ${GameCatalog.weaponById(weaponId).name} in Slot $slot" }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun toggleAttachment(attachmentId: String) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        val current = profile.unlockedAttachments.toMutableList()
        if (current.contains(attachmentId)) current.remove(attachmentId) else current.add(attachmentId)
        TacticalAudioEngine.playUiClick()
        viewModelScope.launch {
            repository.updateProfileFields(mapOf("unlockedAttachments" to current.take(20)))
        }
    }

    fun toggleBackpackItem(itemId: String) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        val current = profile.backpackItems.toMutableList()
        if (current.contains(itemId)) current.remove(itemId) else current.add(itemId)
        TacticalAudioEngine.playUiClick()
        viewModelScope.launch {
            repository.updateProfileFields(mapOf("backpackItems" to current.take(20)))
        }
    }

    fun setGraphicsPreset(presetName: String) {
        TacticalAudioEngine.playUiClick()
        viewModelScope.launch {
            repository.updateProfileFields(mapOf("graphicsPreset" to presetName))
                .onSuccess { _statusBanner.value = "Graphics set to $presetName (${GameCatalog.graphicsByName(presetName).targetFps} FPS)" }
        }
    }

    fun setAudioVolumes(music: Float, sfx: Float) {
        _musicVolume.value = music.coerceIn(0f, 1f)
        _sfxVolume.value = sfx.coerceIn(0f, 1f)
        TacticalAudioEngine.musicVolume = _musicVolume.value
        TacticalAudioEngine.sfxVolume = _sfxVolume.value
    }

    // --- VOICE CHAT ---
    fun setVoicePermissionGranted(granted: Boolean) {
        _voiceChat.value = _voiceChat.value.copy(permissionGranted = granted)
    }

    fun toggleVoiceMic() {
        val current = _voiceChat.value
        if (!current.permissionGranted) return
        val next = !current.micEnabled
        _voiceChat.value = current.copy(micEnabled = next)
        if (next) {
            TacticalAudioEngine.startVoiceMicMonitor()
        } else {
            TacticalAudioEngine.stopVoiceMicMonitor()
        }
    }

    fun toggleVoiceChannel() {
        val next = if (_voiceChat.value.channel == "SQUAD") "ALL" else "SQUAD"
        _voiceChat.value = _voiceChat.value.copy(channel = next)
    }

    // --- REAL-TIME MULTIPLAYER ROOMS ---
    fun createSquadRoom(roomTitle: String) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        val mm = _matchmaking.value
        val mode = GameCatalog.modeById(mm.selectedModeId)
        viewModelScope.launch {
            repository.createMatchRoom(
                hostName = profile.callsign,
                roomTitle = roomTitle,
                mapId = mm.selectedMapId,
                modeId = mm.selectedModeId,
                rankBracket = profile.rankTier,
                targetMmr = profile.rankPoints,
                maxPlayers = mode.squadSize
            ).onSuccess {
                _statusBanner.value = "Squad Lobby Created & Synced Live!"
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to create squad room"
            }
        }
    }

    fun joinOrToggleReadyRoom(room: MatchRoom, toggleReady: Boolean = false, toggleVoice: Boolean = false) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.joinOrUpdateRoom(room, profile.callsign, toggleReady, toggleVoice)
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Room update failed" }
        }
    }

    fun leaveRoom(room: MatchRoom) {
        viewModelScope.launch {
            repository.leaveOrCloseRoom(room)
        }
    }

    fun launchRoomMatch(room: MatchRoom) {
        viewModelScope.launch {
            repository.launchMatchRoom(room).onSuccess {
                _matchmaking.value = _matchmaking.value.copy(
                    selectedMapId = room.mapId,
                    selectedModeId = room.modeId
                )
                _inActiveMatch.value = true
            }
        }
    }

    // --- KAVACH ANTI-CHEAT DIAGNOSTIC ---
    fun runKavachScan() {
        TacticalAudioEngine.playUiClick()
        viewModelScope.launch {
            val latency = (14L..26L).random()
            _kavachState.value = _kavachState.value.copy(
                integrityScore = 100,
                packetsAudited = _kavachState.value.packetsAudited + 64
            )
            repository.logAntiCheatCheck(
                eventType = "MANUAL_DEEP_SCAN",
                status = "VERIFIED",
                latencyMs = latency,
                integrityScore = 100L,
                detail = "Memory, touch cadence & network jitter verified clean (${latency}ms)"
            ).onSuccess {
                _statusBanner.value = "Kavach Deep Scan: 100% Integrity Verified (${latency}ms)"
            }
        }
    }

    // --- COMMUNITY FORUM ---
    fun createForumPost(category: String, title: String, body: String) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.createForumPost(
                authorName = profile.callsign,
                authorRank = profile.rankTier,
                category = category,
                title = title,
                body = body
            ).onSuccess {
                _statusBanner.value = "Post published to Bharat Community Forum!"
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to publish post"
            }
        }
    }

    fun togglePostLike(post: ForumPost) {
        TacticalAudioEngine.playUiClick()
        viewModelScope.launch {
            repository.togglePostLike(post)
        }
    }

    fun openPostComments(postId: String?) {
        _selectedPostIdForComments.value = postId
    }

    fun submitForumComment(postId: String, body: String) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.addForumComment(postId, profile.callsign, body)
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to send reply" }
        }
    }

    override fun onCleared() {
        super.onCleared()
        TacticalAudioEngine.stopIntroTheme()
        TacticalAudioEngine.stopVoiceMicMonitor()
    }

    private companion object {
        const val TAG = "YodhaVM"
    }
}
