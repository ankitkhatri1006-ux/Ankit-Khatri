package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.TacticalAudioEngine
import com.example.ui.theme.BharatTeal
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private data class IntroScene(
    val title: String,
    val subtitle: String,
    val tag: String,
    val imageRes: Int
)

@Composable
fun IntroCinemaOverlay(
    onFinish: () -> Unit
) {
    BackHandler { onFinish() }

    val scenes = remember {
        listOf(
            IntroScene(
                title = "BHARAT KE BATTLEGROUNDS",
                subtitle = "100-Player Tactical Parachute Drop · Jaipur Fort to Ladakh Pass",
                tag = "SCENE 01 // AIRDROP INSERTION",
                imageRes = R.drawable.img_intro_battle_bharat
            ),
            IntroScene(
                title = "DESI WARRIORS: VEER & TARA",
                subtitle = "Customize Hair, Face Gear, Desi Outfits & Signature Bharat Emotes",
                tag = "SCENE 02 // SQUAD IDENTITY",
                imageRes = R.drawable.img_char_veer
            ),
            IntroScene(
                title = "BUILT FOR PRO CLUTCH PLAY",
                subtitle = "22 Authentic Guns · Skill-Matched Opponents · Ultra HD 120 FPS",
                tag = "SCENE 03 // ARSENAL & VOICE CHAT",
                imageRes = R.drawable.img_char_tara
            ),
            IntroScene(
                title = "KAVACH ANTI-CHEAT PROTECTED",
                subtitle = "Season 07 Bharat Rising · Real-Time Multiplayer & Community Forum",
                tag = "SCENE 04 // DEPLOY READY",
                imageRes = R.drawable.img_map_jaipur_fort
            )
        )
    }

    var sceneIndex by remember { mutableIntStateOf(0) }
    var audioEnabled by remember { mutableStateOf(true) }

    DisposableEffect(audioEnabled) {
        if (audioEnabled) {
            TacticalAudioEngine.startIntroTheme()
        } else {
            TacticalAudioEngine.stopIntroTheme()
        }
        onDispose {
            TacticalAudioEngine.stopIntroTheme()
        }
    }

    LaunchedEffect(Unit) {
        for (i in 1 until scenes.size) {
            delay(3000L)
            sceneIndex = i
        }
        delay(3200L)
        onFinish()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "intro_cinema")
    val kenBurnsScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ken_burns"
    )
    val radarPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_phase"
    )

    val currentScene = scenes[sceneIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        AnimatedContent(
            targetState = currentScene,
            transitionSpec = { fadeIn(tween(550)) togetherWith fadeOut(tween(450)) },
            label = "scene_transition"
        ) { scene ->
            Image(
                painter = painterResource(id = scene.imageRes),
                contentDescription = scene.title,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = kenBurnsScale
                        scaleY = kenBurnsScale
                    },
                contentScale = ContentScale.Crop
            )
        }

        // Cinematic dark vignette + ember particles + tactical HUD reticle
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xBB0A0D12),
                        Color(0x220A0D12),
                        Color(0xEE0A0D12)
                    )
                )
            )
            // Floating golden embers
            for (i in 0 until 24) {
                val seed = i * 1.37f
                val x = ((sin(radarPhase + seed) * 0.5f + 0.5f) * size.width)
                val y = ((1f - ((radarPhase * 0.35f + seed) % 1f)) * size.height)
                drawCircle(
                    color = if (i % 3 == 0) BharatTeal.copy(alpha = 0.45f) else SaffronGold.copy(alpha = 0.5f),
                    radius = (2.5f + (i % 4) * 1.2f),
                    center = Offset(x, y)
                )
            }
            // Subtle tactical crosshair ring in top-right
            val cx = size.width * 0.85f
            val cy = size.height * 0.22f
            drawCircle(
                color = SaffronGold.copy(alpha = 0.25f),
                radius = 48f,
                center = Offset(cx, cy),
                style = Stroke(width = 2f)
            )
            drawLine(
                color = SaffronGold.copy(alpha = 0.55f),
                start = Offset(cx, cy),
                end = Offset(cx + cos(radarPhase) * 48f, cy + sin(radarPhase) * 48f),
                strokeWidth = 2.5f
            )
        }

        // Top controls bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(ObsidianBg.copy(alpha = 0.72f))
                    .border(1.dp, SaffronGold.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = SaffronGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "YODHA TRAILER · HD AUDIO",
                    style = MaterialTheme.typography.labelMedium,
                    color = SaffronGold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { audioEnabled = !audioEnabled },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ObsidianBg.copy(alpha = 0.75f))
                ) {
                    Icon(
                        imageVector = if (audioEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = "Toggle Trailer Sound",
                        tint = TextPrimary
                    )
                }

                OutlinedButton(
                    onClick = {
                        TacticalAudioEngine.playUiClick()
                        onFinish()
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("skip_intro_button")
                ) {
                    Text("SKIP", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.FastForward,
                        contentDescription = null,
                        tint = SaffronGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Bottom narrative & progress bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = currentScene.tag,
                        style = MaterialTheme.typography.labelLarge,
                        color = BharatTeal
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentScene.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = currentScene.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Button(
                    onClick = {
                        TacticalAudioEngine.playUiClick()
                        onFinish()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SaffronGold,
                        contentColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("enter_lobby_button")
                ) {
                    Text(
                        text = "ENTER LOBBY",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (sceneIndex + 1f) / scenes.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = SaffronGold,
                trackColor = Color.White.copy(alpha = 0.2f)
            )
        }
    }
}
