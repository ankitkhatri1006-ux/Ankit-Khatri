package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.GameCatalog
import com.example.data.PlayerProfile
import com.example.ui.theme.BharatTeal
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.TacticalSlate
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DesiCharacterPreview(
    profile: PlayerProfile,
    activeEmoteTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    val isGirl = profile.selectedGender == "GIRL"
    val charRes = if (isGirl) R.drawable.img_char_tara else R.drawable.img_char_veer
    val charTitle = if (isGirl) "TARA // DESI VALKYRIE" else "VEER // DESI COMMANDO"

    val outfitColor = when (profile.outfitStyle) {
        "Maratha Armor" -> SaffronGold
        "Gurkha Camo" -> Color(0xFF2EE59D)
        "Mumbai Neon" -> Color(0xFF38BDF8)
        "Rajput Gold" -> Color(0xFFFFD166)
        "Ladakh Parka" -> Color(0xFFE2E8F0)
        else -> SaffronGold
    }

    val hairTint = when (profile.hairColor) {
        "Copper Saffron" -> Color(0xFFD97706)
        "Ash Silver" -> Color(0xFFCBD5E1)
        "Midnight Indigo" -> Color(0xFF6366F1)
        "Royal Bronze" -> Color(0xFFB45309)
        else -> Color(0xFF1E293B)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "emote_anim")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emote_pulse"
    )
    val spin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "emote_spin"
    )

    val emoteYOffset = when {
        profile.selectedEmote.contains("Bhangra", ignoreCase = true) -> -12f * pulse
        profile.selectedEmote.contains("Chak De", ignoreCase = true) -> -8f * pulse
        profile.selectedEmote.contains("Garba", ignoreCase = true) -> -6f * pulse
        else -> -3f * pulse
    }
    val emoteRotationY = when {
        profile.selectedEmote.contains("Dandiya", ignoreCase = true) && activeEmoteTrigger > 0 -> spin
        profile.selectedEmote.contains("Thalaiva", ignoreCase = true) -> -6f + 12f * pulse
        else -> 0f
    }

    val primaryWeapon = GameCatalog.weaponById(profile.primaryWeaponId)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(TacticalSlate)
            .border(1.5.dp, outfitColor.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
    ) {
        Image(
            painter = painterResource(id = charRes),
            contentDescription = charTitle,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = emoteYOffset
                    rotationY = emoteRotationY
                    scaleX = if (profile.selectedEmote.contains("Chak De", ignoreCase = true)) 1f + 0.04f * pulse else 1f
                    scaleY = if (profile.selectedEmote.contains("Chak De", ignoreCase = true)) 1f + 0.04f * pulse else 1f
                },
            contentScale = ContentScale.Crop
        )

        // Dynamic Customization & Emote Aura Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Subtle outfit gradient tint on bottom armor area
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        hairTint.copy(alpha = 0.16f),
                        Color.Transparent,
                        outfitColor.copy(alpha = 0.28f),
                        ObsidianBg.copy(alpha = 0.92f)
                    )
                )
            )
            // Emote shockwave ring
            val center = Offset(size.width * 0.5f, size.height * 0.58f)
            drawCircle(
                color = outfitColor.copy(alpha = 0.45f * (1f - pulse)),
                radius = size.minDimension * (0.18f + 0.22f * pulse),
                center = center,
                style = Stroke(width = 3f)
            )
        }

        // Top Customization Badges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianBg.copy(alpha = 0.8f))
                    .border(1.dp, outfitColor.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = charTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = outfitColor
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianBg.copy(alpha = 0.8f))
                    .border(1.dp, BharatTeal.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = primaryWeapon.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = BharatTeal
                )
            }
        }

        // Bottom Outfit & Emote Strip
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "${profile.callsign} · ${profile.rankTier} (${profile.rankPoints} RP)",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${profile.hairStyle} · ${profile.faceStyle} · ${profile.outfitStyle}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Emote: ${profile.selectedEmote}",
                    style = MaterialTheme.typography.labelMedium,
                    color = SaffronGold
                )
            }
        }
    }
}
