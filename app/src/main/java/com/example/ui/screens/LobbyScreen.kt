package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.GameCatalog
import com.example.data.MatchRoom
import com.example.data.PlayerProfile
import com.example.ui.components.DesiCharacterPreview
import com.example.ui.components.VoiceChatBar
import com.example.ui.theme.BharatTeal
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CrimsonStrike
import com.example.ui.theme.ElevatedArmor
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.TacticalSlate
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MatchmakingUiState
import com.example.viewmodel.UiState
import com.example.viewmodel.VoiceChatUiState

@Composable
fun LobbyScreen(
    profile: PlayerProfile,
    currentUserId: String,
    matchmaking: MatchmakingUiState,
    matchRoomsState: UiState<List<MatchRoom>>,
    voiceState: VoiceChatUiState,
    onReplayIntro: () -> Unit,
    onSignOut: () -> Unit,
    onSelectMap: (String) -> Unit,
    onSelectMode: (String) -> Unit,
    onStartMatchmaking: () -> Unit,
    onCancelMatchmaking: () -> Unit,
    onQuickDeploy: () -> Unit,
    onCreateRoom: (String) -> Unit,
    onJoinOrReadyRoom: (MatchRoom, Boolean, Boolean) -> Unit,
    onLeaveRoom: (MatchRoom) -> Unit,
    onLaunchRoom: (MatchRoom) -> Unit,
    onVoicePermissionResult: (Boolean) -> Unit,
    onToggleMic: () -> Unit,
    onToggleChannel: () -> Unit
) {
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var newRoomTitle by remember { mutableStateOf("") }
    val currentTier = remember(profile.rankPoints) { GameCatalog.tierForPoints(profile.rankPoints) }
    val selectedMap = remember(matchmaking.selectedMapId) { GameCatalog.mapById(matchmaking.selectedMapId) }
    val selectedMode = remember(matchmaking.selectedModeId) { GameCatalog.modeById(matchmaking.selectedModeId) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Tactical Header: Callsign, Grade, Intro Cinema, Sign-Out
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile.callsign.uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(currentTier.color.copy(alpha = 0.18f))
                                .border(1.dp, currentTier.color, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${currentTier.name.uppercase()} · ${profile.rankPoints} RP",
                                style = MaterialTheme.typography.labelMedium,
                                color = currentTier.color
                            )
                        }
                    }
                    Text(
                        text = "K/D Wins: ${profile.wins}W / ${profile.kills}K · Kavach Trust ${profile.antiCheatTrust}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onReplayIntro,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ElevatedArmor)
                            .border(1.dp, SaffronGold.copy(alpha = 0.5f), CircleShape)
                            .testTag("replay_intro_button")
                    ) {
                        Icon(Icons.Filled.Movie, contentDescription = "Watch Intro Video", tint = SaffronGold)
                    }
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ElevatedArmor)
                            .border(1.dp, CardBorder, CircleShape)
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out", tint = TextSecondary)
                    }
                }
            }
        }

        // 2. Desi Hero Character Preview
        item {
            DesiCharacterPreview(
                profile = profile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            )
        }

        // 3. Voice Chat Integration Bar
        item {
            VoiceChatBar(
                voiceState = voiceState,
                onPermissionResult = onVoicePermissionResult,
                onToggleMic = onToggleMic,
                onToggleChannel = onToggleChannel
            )
        }

        // 4. All Competitive Grades Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "COMPETITIVE GRADES (SKILL-MATCHED OPPONENTS)",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GameCatalog.rankTiers) { tier ->
                        val isCurrent = tier.name == currentTier.name
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrent) tier.color.copy(alpha = 0.22f) else TacticalSlate)
                                .border(
                                    width = if (isCurrent) 1.5.dp else 1.dp,
                                    color = if (isCurrent) tier.color else CardBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column {
                                Text(tier.name, style = MaterialTheme.typography.labelLarge, color = tier.color)
                                Text("${tier.minRp}+ RP", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // 5. Indian Maps Selector
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MAPS OF INDIA (${GameCatalog.indianMaps.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = SaffronGold
                    )
                    Text(
                        text = selectedMap.weather,
                        style = MaterialTheme.typography.labelMedium,
                        color = BharatTeal
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(GameCatalog.indianMaps) { map ->
                        val selected = map.id == matchmaking.selectedMapId
                        Card(
                            colors = CardDefaults.cardColors(containerColor = TacticalSlate),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .width(235.dp)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) SaffronGold else CardBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelectMap(map.id) }
                                .testTag("map_card_${map.id}")
                        ) {
                            Box(modifier = Modifier.height(126.dp)) {
                                if (map.drawableRes != null) {
                                    Image(
                                        painter = painterResource(id = map.drawableRes),
                                        contentDescription = map.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(map.groundColor, map.structureColor.copy(alpha = 0.65f))
                                                )
                                            )
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.92f))
                                            )
                                        )
                                )
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = map.accentColor, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(map.region, style = MaterialTheme.typography.labelSmall, color = map.accentColor)
                                    }
                                    Text(map.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text(
                                        text = map.pois.joinToString(" · "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Game Modes Selector
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "GAME GRADES & MODES",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GameCatalog.gameModes) { mode ->
                        val selected = mode.id == matchmaking.selectedModeId
                        Column(
                            modifier = Modifier
                                .width(170.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) ElevatedArmor else TacticalSlate)
                                .border(1.5.dp, if (selected) BharatTeal else CardBorder, RoundedCornerShape(14.dp))
                                .clickable { onSelectMode(mode.id) }
                                .padding(12.dp)
                                .testTag("mode_card_${mode.id}")
                        ) {
                            Text(mode.badge, style = MaterialTheme.typography.labelSmall, color = SaffronGold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(mode.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(mode.subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 2)
                        }
                    }
                }
            }
        }

        // 7. Robust Matchmaking Engine & Deploy CTA
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TacticalSlate),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SaffronGold.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("BALANCED MATCHMAKING ENGINE", style = MaterialTheme.typography.labelLarge, color = SaffronGold)
                            Text(
                                text = "${selectedMap.name} · ${selectedMode.name}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Shield, contentDescription = null, tint = EmeraldVerified, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${matchmaking.estimatedPingMs}ms IN", style = MaterialTheme.typography.labelMedium, color = EmeraldVerified)
                        }
                    }

                    Text(
                        text = "Bracket: ${matchmaking.skillBandLabel} · Graphics: ${profile.graphicsPreset}",
                        style = MaterialTheme.typography.labelMedium,
                        color = BharatTeal
                    )

                    if (matchmaking.isSearching) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = BharatTeal,
                            trackColor = ObsidianBg
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Finding Barabar Opponents... (${matchmaking.searchElapsedSec}s · ${matchmaking.matchedPlayersCount}/${selectedMode.squadSize} Squad)",
                                style = MaterialTheme.typography.labelMedium,
                                color = SaffronGold
                            )
                            TextButton(onClick = onCancelMatchmaking) {
                                Text("CANCEL", color = CrimsonStrike)
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = onStartMatchmaking,
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronGold, contentColor = ObsidianBg),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("start_matchmaking_button")
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("FIND MATCH", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onQuickDeploy,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("instant_deploy_button")
                            ) {
                                Text("DROP NOW", style = MaterialTheme.typography.labelLarge, color = BharatTeal)
                            }
                        }
                    }
                }
            }
        }

        // 8. Real-Time Multiplayer Squad Rooms (Firestore Synced)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Group, contentDescription = null, tint = BharatTeal, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("LIVE MULTIPLAYER SQUAD ROOMS", style = MaterialTheme.typography.labelLarge, color = BharatTeal)
                }
                OutlinedButton(
                    onClick = { showCreateRoomDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp).testTag("create_room_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = SaffronGold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("HOST SQUAD", style = MaterialTheme.typography.labelMedium, color = SaffronGold)
                }
            }
        }

        when (matchRoomsState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SaffronGold, modifier = Modifier.size(28.dp))
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Text(matchRoomsState.message, style = MaterialTheme.typography.bodyMedium, color = CrimsonStrike)
                }
            }
            is UiState.Success -> {
                if (matchRoomsState.data.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = TacticalSlate),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No open squad rooms yet", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text("Tap HOST SQUAD to open a live room for friends or join ranked matchmaking above.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    }
                } else {
                    items(matchRoomsState.data, key = { it.id }) { room ->
                        val isMember = room.playerUids.contains(currentUserId)
                        val isHost = room.hostUid == currentUserId
                        val isReady = room.readyUids.contains(currentUserId)
                        val isVoiceOn = room.voiceActiveUids.contains(currentUserId)
                        val roomMap = GameCatalog.mapById(room.mapId)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = TacticalSlate),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, if (isMember) BharatTeal else CardBorder, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(room.roomTitle, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Text(
                                            text = "${roomMap.name} · ${room.rankBracket} (${room.targetMmr} MMR) · ${room.playerUids.size}/${room.maxPlayers}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Security, contentDescription = null, tint = EmeraldVerified, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(room.status, style = MaterialTheme.typography.labelSmall, color = SaffronGold)
                                    }
                                }

                                Text(
                                    text = "Squad: ${room.playerNames.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BharatTeal
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!isMember) {
                                        Button(
                                            onClick = { onJoinOrReadyRoom(room, false, false) },
                                            colors = ButtonDefaults.buttonColors(containerColor = BharatTeal, contentColor = ObsidianBg),
                                            modifier = Modifier.height(40.dp)
                                        ) {
                                            Text("JOIN", style = MaterialTheme.typography.labelMedium)
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { onJoinOrReadyRoom(room, true, false) },
                                            modifier = Modifier.height(40.dp)
                                        ) {
                                            Text(if (isReady) "READY ✓" else "READY UP", style = MaterialTheme.typography.labelMedium, color = EmeraldVerified)
                                        }
                                        OutlinedButton(
                                            onClick = { onJoinOrReadyRoom(room, false, true) },
                                            modifier = Modifier.height(40.dp)
                                        ) {
                                            Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isVoiceOn) EmeraldVerified else TextSecondary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isVoiceOn) "VOICE ON" else "VOICE", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                                        }
                                        if (isHost) {
                                            Button(
                                                onClick = { onLaunchRoom(room) },
                                                colors = ButtonDefaults.buttonColors(containerColor = SaffronGold, contentColor = ObsidianBg),
                                                modifier = Modifier.height(40.dp)
                                            ) {
                                                Text("DEPLOY", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                        TextButton(onClick = { onLeaveRoom(room) }) {
                                            Text(if (isHost) "CLOSE" else "LEAVE", style = MaterialTheme.typography.labelMedium, color = CrimsonStrike)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    if (showCreateRoomDialog) {
        AlertDialog(
            onDismissRequest = { showCreateRoomDialog = false },
            containerColor = TacticalSlate,
            title = { Text("Host Multiplayer Squad Room", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newRoomTitle,
                    onValueChange = { newRoomTitle = it.take(45) },
                    label = { Text("Squad Room Name") },
                    placeholder = { Text("Jaipur Ranked Push") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateRoom(newRoomTitle.ifBlank { "${profile.callsign}'s Squad" })
                        newRoomTitle = ""
                        showCreateRoomDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronGold, contentColor = ObsidianBg)
                ) {
                    Text("CREATE ROOM")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRoomDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }
}
