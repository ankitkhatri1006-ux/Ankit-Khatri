package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.TacticalAudioEngine
import com.example.ui.theme.BharatTeal
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CrimsonStrike
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.TacticalSlate
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.VoiceChatUiState

@Composable
fun VoiceChatBar(
    voiceState: VoiceChatUiState,
    onPermissionResult: (Boolean) -> Unit,
    onToggleMic: () -> Unit,
    onToggleChannel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val micLevel by TacticalAudioEngine.micLevel.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        onPermissionResult(granted)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            onPermissionResult(granted)
            if (granted && !voiceState.micEnabled) {
                onToggleMic()
            }
        }
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(TacticalSlate.copy(alpha = 0.92f))
            .border(
                1.dp,
                if (voiceState.micEnabled) EmeraldVerified.copy(alpha = 0.7f) else CardBorder,
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (voiceState.micEnabled) EmeraldVerified.copy(alpha = 0.18f) else ObsidianBg)
                .clickable {
                    if (!voiceState.permissionGranted) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        onToggleMic()
                    }
                }
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .testTag("voice_mic_toggle"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (voiceState.micEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
                contentDescription = if (voiceState.micEnabled) "Mute Voice Chat" else "Enable Voice Chat",
                tint = if (voiceState.micEnabled) EmeraldVerified else CrimsonStrike,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when {
                    !voiceState.permissionGranted -> "MIC ALLOW"
                    voiceState.micEnabled -> "MIC LIVE"
                    else -> "MUTED"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (voiceState.micEnabled) EmeraldVerified else TextPrimary
            )
        }

        // Live AudioRecord VAD bar visualization
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Filled.HeadsetMic,
                contentDescription = "Voice Activity",
                tint = BharatTeal,
                modifier = Modifier.size(16.dp)
            )
            for (bar in 0 until 8) {
                val threshold = (bar + 1) / 8f
                val active = voiceState.micEnabled && micLevel >= threshold * 0.6f
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height((8 + bar * 2).dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (active) EmeraldVerified else CardBorder)
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(ObsidianBg)
                .border(1.dp, SaffronGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .clickable { onToggleChannel() }
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .testTag("voice_channel_toggle")
        ) {
            Text(
                text = voiceState.channel,
                style = MaterialTheme.typography.labelMedium,
                color = if (voiceState.channel == "SQUAD") SaffronGold else TextSecondary
            )
        }
    }
}
