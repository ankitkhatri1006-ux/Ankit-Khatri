package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AntiCheatLog
import com.example.data.GameCatalog
import com.example.data.PlayerProfile
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
import com.example.viewmodel.KavachTelemetryState
import com.example.viewmodel.UiState

@Composable
fun SeasonAndAntiCheatScreen(
    profile: PlayerProfile,
    kavachState: KavachTelemetryState,
    antiCheatLogsState: UiState<List<AntiCheatLog>>,
    musicVolume: Float,
    sfxVolume: Float,
    onSelectGraphics: (String) -> Unit,
    onAudioChanged: (Float, Float) -> Unit,
    onRunKavachScan: () -> Unit
) {
    val seasonLevel = ((profile.seasonXp / 200L) + 1L).coerceAtMost(50L)
    val levelProgress = (profile.seasonXp % 200L) / 200f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("SEASON 07 & KAVACH", style = MaterialTheme.typography.displayMedium, color = TextPrimary)
            Text(
                text = "Bharat Rising Royale Pass · Ultra Graphics & Audio · Anti-Cheat Engine",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // 1. Season 07 Royale Pass Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TacticalSlate),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().border(1.5.dp, SaffronGold.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = SaffronGold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SEASON 07: BHARAT RISING", style = MaterialTheme.typography.titleLarge, color = SaffronGold)
                        }
                        Text("TIER $seasonLevel / 50", style = MaterialTheme.typography.labelLarge, color = BharatTeal)
                    }
                    LinearProgressIndicator(
                        progress = { levelProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = SaffronGold,
                        trackColor = ObsidianBg
                    )
                    Text(
                        text = "Season XP: ${profile.seasonXp} · Next Seasonal Drop in 14d 08h",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // 2. Seasonal Weekly Missions
        item {
            Text("SEASONAL MISSIONS", style = MaterialTheme.typography.labelLarge, color = BharatTeal)
        }
        items(GameCatalog.seasonMissions, key = { it.id }) { mission ->
            val currentProgress = when (mission.id) {
                "m1" -> profile.matchesPlayed.toInt().coerceAtMost(mission.target)
                "m2" -> profile.kills.toInt().coerceAtMost(mission.target)
                "m3" -> profile.wins.toInt().coerceAtMost(mission.target)
                else -> 1
            }
            val done = currentProgress >= mission.target
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TacticalSlate)
                    .border(1.dp, if (done) EmeraldVerified.copy(alpha = 0.6f) else CardBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(mission.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("Progress: $currentProgress / ${mission.target} · +${mission.rewardXp} XP", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (done) EmeraldVerified else CardBorder
                )
            }
        }

        // 3. Regular Seasonal Updates / Patch Notes
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Update, contentDescription = null, tint = SaffronGold, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("SEASONAL PATCH NOTES", style = MaterialTheme.typography.labelLarge, color = SaffronGold)
            }
        }
        items(GameCatalog.seasonPatchNotes) { note ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElevatedArmor)
                    .padding(12.dp)
            ) {
                Text("• $note", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
        }

        // 4. Ultra-High Graphics Presets
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Speed, contentDescription = null, tint = BharatTeal, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("HIGH GRAPHICS & FPS ENGINE", style = MaterialTheme.typography.labelLarge, color = BharatTeal)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GameCatalog.graphicsPresets, key = { it.name }) { preset ->
                    val selected = profile.graphicsPreset == preset.name
                    Column(
                        modifier = Modifier
                            .width(155.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) BharatTeal.copy(alpha = 0.2f) else TacticalSlate)
                            .border(1.5.dp, if (selected) BharatTeal else CardBorder, RoundedCornerShape(14.dp))
                            .clickable { onSelectGraphics(preset.name) }
                            .padding(12.dp)
                            .testTag("gfx_${preset.name}")
                    ) {
                        Text(preset.name, style = MaterialTheme.typography.titleMedium, color = if (selected) BharatTeal else TextPrimary)
                        Text("${preset.targetFps} FPS · ${preset.antiAliasing}", style = MaterialTheme.typography.labelSmall, color = SaffronGold)
                        Text(preset.summary, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 2)
                    }
                }
            }
        }

        // 5. Audio Engine Sliders
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TacticalSlate),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = SaffronGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TACTICAL AUDIO MIXER", style = MaterialTheme.typography.labelLarge, color = SaffronGold)
                    }
                    Text("Intro & Theme Music: ${(musicVolume * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                    Slider(
                        value = musicVolume,
                        onValueChange = { onAudioChanged(it, sfxVolume) },
                        colors = SliderDefaults.colors(thumbColor = SaffronGold, activeTrackColor = SaffronGold)
                    )
                    Text("Gunfire & Combat SFX: ${(sfxVolume * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                    Slider(
                        value = sfxVolume,
                        onValueChange = { onAudioChanged(musicVolume, it) },
                        colors = SliderDefaults.colors(thumbColor = BharatTeal, activeTrackColor = BharatTeal)
                    )
                }
            }
        }

        // 6. Kavach Advanced Anti-Cheat System
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TacticalSlate),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().border(1.5.dp, EmeraldVerified.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Security, contentDescription = null, tint = EmeraldVerified)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("KAVACH ANTI-CHEAT SYSTEM", style = MaterialTheme.typography.titleLarge, color = EmeraldVerified)
                        }
                        Text("${kavachState.integrityScore}%", style = MaterialTheme.typography.headlineMedium, color = EmeraldVerified)
                    }

                    Text("Session Hash: ${kavachState.sessionHash}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text("Velocity Guard: ${kavachState.velocityStatus} · Cadence: ${kavachState.cadenceStatus}", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                    Text("Telemetry Packets Audited: ${kavachState.packetsAudited}", style = MaterialTheme.typography.labelSmall, color = BharatTeal)

                    Button(
                        onClick = onRunKavachScan,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldVerified, contentColor = ObsidianBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("run_kavach_scan_button")
                    ) {
                        Text("RUN KAVACH DEEP INTEGRITY SCAN", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        when (antiCheatLogsState) {
            is UiState.Success -> {
                items(antiCheatLogsState.data.take(6), key = { it.id }) { log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElevatedArmor)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${log.eventType} · ${log.status}", style = MaterialTheme.typography.labelMedium, color = if (log.status == "VERIFIED") EmeraldVerified else CrimsonStrike)
                            Text(log.detail, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Text("${log.latencyMs}ms", style = MaterialTheme.typography.labelSmall, color = SaffronGold)
                    }
                }
            }
            is UiState.Error -> {
                item { Text(antiCheatLogsState.message, color = CrimsonStrike, style = MaterialTheme.typography.bodyMedium) }
            }
            is UiState.Loading -> Unit
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }
    }
}
