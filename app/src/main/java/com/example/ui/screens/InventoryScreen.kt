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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.WeaponCategory
import com.example.data.WeaponSpec
import com.example.ui.theme.BharatTeal
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ElevatedArmor
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.TacticalSlate
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun InventoryScreen(
    profile: PlayerProfile,
    onEquipWeapon: (slot: Int, weaponId: String) -> Unit,
    onToggleAttachment: (String) -> Unit,
    onToggleBackpackItem: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<WeaponCategory?>(null) }

    val filteredWeapons = remember(selectedCategory) {
        if (selectedCategory == null) GameCatalog.weapons
        else GameCatalog.weapons.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("ARSENAL & INVENTORY", style = MaterialTheme.typography.displayMedium, color = TextPrimary)
            Text(
                text = "All ${GameCatalog.weapons.size} Guns · Attachments · Level 3 Backpack Gear",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Equipped Loadout Summary Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TacticalSlate),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, SaffronGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ACTIVE COMBAT LOADOUT", style = MaterialTheme.typography.labelLarge, color = SaffronGold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        LoadoutBadge("SLOT 1", GameCatalog.weaponById(profile.primaryWeaponId).name, Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        LoadoutBadge("SLOT 2", GameCatalog.weaponById(profile.secondaryWeaponId).name, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        LoadoutBadge("SIDEARM", GameCatalog.weaponById(profile.sidearmWeaponId).name, Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        LoadoutBadge("MELEE", GameCatalog.weaponById(profile.meleeWeaponId).name, Modifier.weight(1f))
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    CategoryChip("ALL (${GameCatalog.weapons.size})", selectedCategory == null) {
                        selectedCategory = null
                    }
                }
                items(WeaponCategory.entries) { cat ->
                    CategoryChip(cat.label, selectedCategory == cat) {
                        selectedCategory = cat
                    }
                }
            }
        }

        // All Weapons Cards
        items(filteredWeapons, key = { it.id }) { weapon ->
            WeaponArsenalCard(
                weapon = weapon,
                isSlot1 = profile.primaryWeaponId == weapon.id,
                isSlot2 = profile.secondaryWeaponId == weapon.id,
                isSidearm = profile.sidearmWeaponId == weapon.id,
                isMelee = profile.meleeWeaponId == weapon.id,
                onTestFire = { TacticalAudioEngine.playGunshot(weapon.category.name) },
                onEquipSlot = { slot -> onEquipWeapon(slot, weapon.id) }
            )
        }

        // Attachments Section
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Tune, contentDescription = null, tint = BharatTeal, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("OPTICS, MUZZLES & MAG ATTACHMENTS", style = MaterialTheme.typography.labelLarge, color = BharatTeal)
            }
        }

        items(GameCatalog.attachments, key = { it.first }) { (id, label) ->
            val equipped = profile.unlockedAttachments.contains(id)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TacticalSlate)
                    .border(1.dp, if (equipped) BharatTeal else CardBorder, RoundedCornerShape(12.dp))
                    .clickable { onToggleAttachment(id) }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = if (equipped) "Equipped" else "Unequipped",
                    tint = if (equipped) EmeraldVerified else CardBorder
                )
            }
        }

        // Backpack Gear Section
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Backpack, contentDescription = null, tint = SaffronGold, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("LEVEL 3 BACKPACK & UTILITY GEAR", style = MaterialTheme.typography.labelLarge, color = SaffronGold)
            }
        }

        items(GameCatalog.backpackGearCatalog, key = { it.first }) { (id, name, desc) ->
            val carried = profile.backpackItems.contains(id)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TacticalSlate)
                    .border(1.dp, if (carried) SaffronGold else CardBorder, RoundedCornerShape(12.dp))
                    .clickable { onToggleBackpackItem(id) }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(desc, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                Text(
                    text = if (carried) "PACKED" else "ADD",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (carried) EmeraldVerified else SaffronGold
                )
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }
    }
}

@Composable
private fun LoadoutBadge(slotLabel: String, weaponName: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ElevatedArmor)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(slotLabel, style = MaterialTheme.typography.labelSmall, color = BharatTeal)
        Text(weaponName, style = MaterialTheme.typography.labelLarge, color = TextPrimary, maxLines = 1)
    }
}

@Composable
private fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) SaffronGold else TacticalSlate)
            .border(1.dp, if (selected) SaffronGold else CardBorder, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) ObsidianBg else TextPrimary
        )
    }
}

@Composable
private fun WeaponArsenalCard(
    weapon: WeaponSpec,
    isSlot1: Boolean,
    isSlot2: Boolean,
    isSidearm: Boolean,
    isMelee: Boolean,
    onTestFire: () -> Unit,
    onEquipSlot: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TacticalSlate),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isSlot1 || isSlot2 || isSidearm || isMelee) weapon.accentColor else CardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${weapon.category.label} · ${weapon.ammoType}", style = MaterialTheme.typography.labelSmall, color = weapon.accentColor)
                    Text(weapon.name, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                }
                IconButton(
                    onClick = onTestFire,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElevatedArmor)
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Test Fire Sound", tint = weapon.accentColor)
                }
            }

            Text(weapon.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatBar("DMG ${weapon.damage}", weapon.damage / 105f, weapon.accentColor, Modifier.weight(1f))
                StatBar("CTRL ${weapon.recoilControl}", weapon.recoilControl / 100f, BharatTeal, Modifier.weight(1f))
                StatBar("MAG ${weapon.magSize}", (weapon.magSize / 75f).coerceIn(0.15f, 1f), SaffronGold, Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (weapon.category == WeaponCategory.SIDEARM) {
                    val isMeleeWeapon = weapon.ammoType == "Melee"
                    val targetSlot = if (isMeleeWeapon) 4 else 3
                    val equipped = if (isMeleeWeapon) isMelee else isSidearm
                    Button(
                        onClick = { onEquipSlot(targetSlot) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (equipped) EmeraldVerified else SaffronGold,
                            contentColor = ObsidianBg
                        ),
                        modifier = Modifier.height(40.dp).testTag("equip_${weapon.id}")
                    ) {
                        Text(if (equipped) "EQUIPPED ✓" else if (isMeleeWeapon) "EQUIP MELEE" else "EQUIP SIDEARM", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Button(
                        onClick = { onEquipSlot(1) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSlot1) EmeraldVerified else SaffronGold,
                            contentColor = ObsidianBg
                        ),
                        modifier = Modifier.height(40.dp).testTag("equip_s1_${weapon.id}")
                    ) {
                        Text(if (isSlot1) "SLOT 1 ✓" else "EQUIP SLOT 1", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { onEquipSlot(2) },
                        modifier = Modifier.height(40.dp).testTag("equip_s2_${weapon.id}")
                    ) {
                        Text(if (isSlot2) "SLOT 2 ✓" else "EQUIP SLOT 2", style = MaterialTheme.typography.labelMedium, color = BharatTeal)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBar(label: String, progress: Float, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = ObsidianBg
        )
    }
}
