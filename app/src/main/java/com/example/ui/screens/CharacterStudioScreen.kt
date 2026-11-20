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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.audio.TacticalAudioEngine
import com.example.data.GameCatalog
import com.example.data.PlayerProfile
import com.example.ui.components.DesiCharacterPreview
import com.example.ui.theme.BharatTeal
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ElevatedArmor
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.TacticalSlate
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CharacterStudioScreen(
    profile: PlayerProfile,
    onUpdateCharacter: (
        gender: String?,
        hairStyle: String?,
        hairColor: String?,
        faceStyle: String?,
        outfitStyle: String?,
        selectedEmote: String?,
        callsign: String?
    ) -> Unit
) {
    var callsignInput by remember(profile.callsign) { mutableStateOf(profile.callsign) }
    var emoteTrigger by remember { mutableIntStateOf(0) }

    val hairOptions = if (profile.selectedGender == "GIRL") GameCatalog.girlHairStyles else GameCatalog.boyHairStyles
    val faceOptions = if (profile.selectedGender == "GIRL") GameCatalog.girlFaceStyles else GameCatalog.boyFaceStyles

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("DESI YODHA STUDIO", style = MaterialTheme.typography.displayMedium, color = TextPrimary)
            Text(
                text = "Customize Ladka (Veer) & Ladki (Tara) — Baal, Chehra, Kapde & Diverse Emotes",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Live Animated Preview
        item {
            DesiCharacterPreview(
                profile = profile,
                activeEmoteTrigger = emoteTrigger,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            )
        }

        // Ladka (Veer) vs Ladki (Tara) Selector
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GenderSelectCard(
                    title = "VEER (DESI LADKA)",
                    subtitle = "Commando Tactical",
                    selected = profile.selectedGender == "BOY",
                    onClick = {
                        onUpdateCharacter("BOY", GameCatalog.boyHairStyles.first(), null, GameCatalog.boyFaceStyles.first(), null, null, null)
                    },
                    modifier = Modifier.weight(1f).testTag("select_gender_boy")
                )
                GenderSelectCard(
                    title = "TARA (DESI LADKI)",
                    subtitle = "Valkyrie Striker",
                    selected = profile.selectedGender == "GIRL",
                    onClick = {
                        onUpdateCharacter("GIRL", GameCatalog.girlHairStyles.first(), null, GameCatalog.girlFaceStyles.first(), null, null, null)
                    },
                    modifier = Modifier.weight(1f).testTag("select_gender_girl")
                )
            }
        }

        // Callsign Editor
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = callsignInput,
                    onValueChange = { callsignInput = it.take(24) },
                    label = { Text("In-Game Callsign") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { onUpdateCharacter(null, null, null, null, null, null, callsignInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronGold, contentColor = ObsidianBg),
                    modifier = Modifier.height(54.dp)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = "Save Callsign")
                }
            }
        }

        // Baal (Hair Style)
        item {
            CustomizationRow(
                title = "BAAL (HAIRSTYLE)",
                options = hairOptions,
                selectedOption = profile.hairStyle,
                accent = SaffronGold,
                onSelect = { onUpdateCharacter(null, it, null, null, null, null, null) }
            )
        }

        // Hair Color
        item {
            CustomizationRow(
                title = "HAIR COLOR",
                options = GameCatalog.hairColors,
                selectedOption = profile.hairColor,
                accent = BharatTeal,
                onSelect = { onUpdateCharacter(null, null, it, null, null, null, null) }
            )
        }

        // Chehra (Face Gear / Bindi / Aviators / Tilak)
        item {
            CustomizationRow(
                title = "CHEHRA (FACE & GEAR)",
                options = faceOptions,
                selectedOption = profile.faceStyle,
                accent = SaffronGold,
                onSelect = { onUpdateCharacter(null, null, null, it, null, null, null) }
            )
        }

        // Kapde (Desi Outfits)
        item {
            CustomizationRow(
                title = "KAPDE (DESI OUTFITS)",
                options = GameCatalog.outfitStyles,
                selectedOption = profile.outfitStyle,
                accent = BharatTeal,
                onSelect = { onUpdateCharacter(null, null, null, null, it, null, null) }
            )
        }

        // Diverse Emotes & Animations
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.EmojiEmotions, contentDescription = null, tint = SaffronGold)
                Spacer(modifier = Modifier.width(6.dp))
                Text("DIVERSE EMOTES & ANIMATIONS", style = MaterialTheme.typography.labelLarge, color = SaffronGold)
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(GameCatalog.emotes, key = { it.id }) { emote ->
                    val selected = profile.selectedEmote == emote.name
                    Column(
                        modifier = Modifier
                            .width(155.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) emote.accentColor.copy(alpha = 0.2f) else TacticalSlate)
                            .border(1.5.dp, if (selected) emote.accentColor else CardBorder, RoundedCornerShape(14.dp))
                            .clickable {
                                emoteTrigger++
                                TacticalAudioEngine.playUiClick()
                                onUpdateCharacter(null, null, null, null, null, emote.name, null)
                            }
                            .padding(12.dp)
                            .testTag("emote_card_${emote.id}")
                    ) {
                        Text(emote.name, style = MaterialTheme.typography.titleMedium, color = if (selected) emote.accentColor else TextPrimary)
                        Text(emote.hindiTag, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(if (selected) "ACTIVE ANIMATION" else "TAP TO PREVIEW", style = MaterialTheme.typography.labelSmall, color = emote.accentColor)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }
    }
}

@Composable
private fun GenderSelectCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) ElevatedArmor else TacticalSlate)
            .border(1.5.dp, if (selected) SaffronGold else CardBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Face, contentDescription = null, tint = if (selected) SaffronGold else TextSecondary)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun CustomizationRow(
    title: String,
    options: List<String>,
    selectedOption: String,
    accent: androidx.compose.ui.graphics.Color,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { opt ->
                val selected = opt == selectedOption
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) accent.copy(alpha = 0.22f) else TacticalSlate)
                        .border(1.5.dp, if (selected) accent else CardBorder, RoundedCornerShape(10.dp))
                        .clickable { onSelect(opt) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = opt,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) accent else TextPrimary
                    )
                }
            }
        }
    }
}
