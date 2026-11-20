package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.TacticalAudioEngine
import com.example.data.GameCatalog
import com.example.data.PlayerProfile
import com.example.ui.components.VoiceChatBar
import com.example.ui.theme.BharatTeal
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CrimsonStrike
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.StormBlue
import com.example.ui.theme.TacticalSlate
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.VoiceChatUiState
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private data class ArenaObstacle(val x: Float, val y: Float, val w: Float, val h: Float)
private data class ArenaBullet(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val fromPlayer: Boolean,
    val damage: Int,
    val color: Color,
    var life: Int = 55
)
private data class ArenaOpponent(
    val id: Int,
    val name: String,
    var x: Float,
    var y: Float,
    var hp: Float,
    val maxHp: Float,
    var aimAngle: Float,
    var lastShotMs: Long,
    var strafeDir: Float,
    val tierColor: Color
)
private data class ArenaLootCrate(val id: Int, val x: Float, val y: Float, var collected: Boolean = false, val isAirdrop: Boolean = false)
private data class ArenaParticle(var x: Float, var y: Float, var vx: Float, var vy: Float, val color: Color, var life: Int)

@Composable
fun GameArenaScreen(
    profile: PlayerProfile,
    mapId: String,
    modeId: String,
    voiceState: VoiceChatUiState,
    onVoicePermissionResult: (Boolean) -> Unit,
    onToggleMic: () -> Unit,
    onToggleChannel: () -> Unit,
    onExitMatch: (kills: Int, won: Boolean, shotsFired: Int, maxSpeed: Float) -> Unit
) {
    val mapSpec = remember(mapId) { GameCatalog.mapById(mapId) }
    val modeSpec = remember(modeId) { GameCatalog.modeById(modeId) }
    val rankTier = remember(profile.rankPoints) { GameCatalog.tierForPoints(profile.rankPoints) }
    val gfx = remember(profile.graphicsPreset) { GameCatalog.graphicsByName(profile.graphicsPreset) }

    val primaryWep = remember(profile.primaryWeaponId) { GameCatalog.weaponById(profile.primaryWeaponId) }
    val secondaryWep = remember(profile.secondaryWeaponId) { GameCatalog.weaponById(profile.secondaryWeaponId) }
    val meleeWep = remember(profile.meleeWeaponId) { GameCatalog.weaponById(profile.meleeWeaponId) }

    var activeSlot by remember { mutableIntStateOf(1) }
    val activeWeapon = when (activeSlot) {
        1 -> primaryWep
        2 -> secondaryWep
        else -> meleeWep
    }

    // World coordinates: 2000 x 2000 arena
    val worldSize = 2000f
    var playerX by remember { mutableFloatStateOf(1000f) }
    var playerY by remember { mutableFloatStateOf(1050f) }
    var playerAimAngle by remember { mutableFloatStateOf(-1.57f) }
    var playerHp by remember { mutableFloatStateOf(100f) }
    var playerArmor by remember { mutableFloatStateOf(100f) }
    var medkits by remember { mutableIntStateOf(2) }
    var grenades by remember { mutableIntStateOf(2) }
    var isAdsZoom by remember { mutableStateOf(false) }
    var isCrouched by remember { mutableStateOf(false) }
    var emoteBanner by remember { mutableStateOf<String?>(null) }

    var ammoInMag by remember { mutableIntStateOf(primaryWep.magSize) }
    var isReloading by remember { mutableStateOf(false) }
    var kills by remember { mutableIntStateOf(0) }
    var shotsFired by remember { mutableIntStateOf(0) }
    var maxSpeedObserved by remember { mutableFloatStateOf(0f) }
    var zoneRadius by remember { mutableFloatStateOf(950f) }
    var matchFinished by remember { mutableStateOf(false) }
    var matchWon by remember { mutableStateOf(false) }
    var killFeedText by remember { mutableStateOf("Deployed to ${mapSpec.name} · ${rankTier.name} Lobby") }

    // Smooth dual-thumb joystick state
    var moveVecX by remember { mutableFloatStateOf(0f) }
    var moveVecY by remember { mutableFloatStateOf(0f) }
    var aimVecX by remember { mutableFloatStateOf(0f) }
    var aimVecY by remember { mutableFloatStateOf(0f) }
    var isFiring by remember { mutableStateOf(false) }
    var lastShotTimeMs by remember { mutableLongStateOf(0L) }
    var frameTick by remember { mutableLongStateOf(0L) }

    val obstacles = remember(mapId) {
        listOf(
            ArenaObstacle(420f, 420f, 180f, 70f),
            ArenaObstacle(1340f, 440f, 200f, 80f),
            ArenaObstacle(760f, 760f, 120f, 120f),
            ArenaObstacle(1160f, 860f, 160f, 70f),
            ArenaObstacle(500f, 1280f, 190f, 80f),
            ArenaObstacle(1320f, 1340f, 170f, 95f),
            ArenaObstacle(920f, 1440f, 160f, 65f),
            ArenaObstacle(360f, 880f, 80f, 180f),
            ArenaObstacle(1560f, 820f, 85f, 190f)
        )
    }

    val opponents = remember(modeId, profile.rankPoints) {
        val desiBotNames = listOf(
            "RajaSniper", "MumbaiKing", "JaipurBlade", "SheraOP", "KaaliStrike",
            "BhangraRush", "ChaiClutch", "Vikram99", "TaraFury", "ArjunAce",
            "GhatShadow", "LadakhGhost", "DilliDon", "BengalTiger"
        )
        mutableStateListOf<ArenaOpponent>().apply {
            for (i in 0 until modeSpec.opponentCount) {
                val angle = (i.toFloat() / modeSpec.opponentCount) * 6.28318f
                val dist = 380f + (i % 3) * 190f
                add(
                    ArenaOpponent(
                        id = i,
                        name = desiBotNames[i % desiBotNames.size],
                        x = (1000f + cos(angle) * dist).coerceIn(140f, worldSize - 140f),
                        y = (1000f + sin(angle) * dist).coerceIn(140f, worldSize - 140f),
                        hp = 90f * rankTier.aiSkillMultiplier,
                        maxHp = 90f * rankTier.aiSkillMultiplier,
                        aimAngle = angle + 3.14f,
                        lastShotMs = 0L,
                        strafeDir = if (i % 2 == 0) 1f else -1f,
                        tierColor = rankTier.color
                    )
                )
            }
        }
    }

    val crates = remember {
        mutableStateListOf(
            ArenaLootCrate(1, 860f, 640f, isAirdrop = true),
            ArenaLootCrate(2, 1220f, 1080f, isAirdrop = false),
            ArenaLootCrate(3, 620f, 1140f, isAirdrop = false),
            ArenaLootCrate(4, 1440f, 680f, isAirdrop = true)
        )
    }
    val bullets = remember { mutableStateListOf<ArenaBullet>() }
    val particles = remember { mutableStateListOf<ArenaParticle>() }

    BackHandler {
        onExitMatch(kills, matchWon, shotsFired, maxSpeedObserved)
    }

    LaunchedEffect(activeSlot) {
        ammoInMag = activeWeapon.magSize
        isReloading = false
    }

    // Main 60 FPS Simulation Loop
    LaunchedEffect(matchFinished) {
        if (matchFinished) return@LaunchedEffect
        while (!matchFinished) {
            val now = System.currentTimeMillis()
            val baseSpeed = if (isCrouched) 4.2f else 6.6f
            val moveMag = hypot(moveVecX, moveVecY).coerceIn(0f, 1f)
            if (moveMag > 0.05f) {
                val dx = moveVecX * baseSpeed
                val dy = moveVecY * baseSpeed
                val nextX = (playerX + dx).coerceIn(60f, worldSize - 60f)
                val nextY = (playerY + dy).coerceIn(60f, worldSize - 60f)
                val blocked = obstacles.any { o ->
                    nextX in (o.x - 18f)..(o.x + o.w + 18f) && nextY in (o.y - 18f)..(o.y + o.h + 18f)
                }
                if (!blocked) {
                    playerX = nextX
                    playerY = nextY
                }
                maxSpeedObserved = maxOf(maxSpeedObserved, moveMag * baseSpeed)
                if (hypot(aimVecX, aimVecY) < 0.1f) {
                    playerAimAngle = atan2(moveVecY, moveVecX)
                }
            }

            if (hypot(aimVecX, aimVecY) > 0.15f) {
                playerAimAngle = atan2(aimVecY, aimVecX)
            }

            // Player Firing
            if (isFiring && !isReloading && now - lastShotTimeMs >= activeWeapon.fireRateMs) {
                if (ammoInMag > 0 || activeSlot == 3) {
                    lastShotTimeMs = now
                    shotsFired++
                    if (activeSlot != 3) ammoInMag--
                    val spread = if (isAdsZoom) 0.02f else 0.08f
                    val shotAngle = playerAimAngle + (Random.nextFloat() - 0.5f) * spread
                    bullets.add(
                        ArenaBullet(
                            x = playerX + cos(shotAngle) * 24f,
                            y = playerY + sin(shotAngle) * 24f,
                            vx = cos(shotAngle) * activeWeapon.bulletSpeed,
                            vy = sin(shotAngle) * activeWeapon.bulletSpeed,
                            fromPlayer = true,
                            damage = activeWeapon.damage,
                            color = activeWeapon.accentColor
                        )
                    )
                    TacticalAudioEngine.playGunshot(activeWeapon.category.name)
                    if (gfx.bloomEffects) {
                        for (p in 0 until 4) {
                            val pa = shotAngle + (Random.nextFloat() - 0.5f) * 0.8f
                            particles.add(
                                ArenaParticle(
                                    x = playerX + cos(shotAngle) * 26f,
                                    y = playerY + sin(shotAngle) * 26f,
                                    vx = cos(pa) * 5f,
                                    vy = sin(pa) * 5f,
                                    color = SaffronGold,
                                    life = 8
                                )
                            )
                        }
                    }
                    if (ammoInMag == 0 && activeSlot != 3) {
                        isReloading = true
                    }
                }
            }

            // Auto-reload completion
            if (isReloading && now - lastShotTimeMs >= activeWeapon.reloadMs) {
                ammoInMag = activeWeapon.magSize
                isReloading = false
                TacticalAudioEngine.playReload()
            }

            // Loot crate pickup
            crates.forEach { crate ->
                if (!crate.collected && hypot(playerX - crate.x, playerY - crate.y) < 48f) {
                    crate.collected = true
                    playerHp = (playerHp + 35f).coerceAtMost(100f)
                    playerArmor = (playerArmor + 40f).coerceAtMost(100f)
                    medkits++
                    grenades++
                    killFeedText = if (crate.isAirdrop) "Airdrop Looted! Armor & Medkits +1" else "Supply Crate Looted!"
                    TacticalAudioEngine.playReload()
                }
            }

            // Skill-matched AI Opponents ("Barabar ke Opponents")
            opponents.forEach { bot ->
                val dist = hypot(playerX - bot.x, playerY - bot.y)
                val angleToPlayer = atan2(playerY - bot.y, playerX - bot.x)
                bot.aimAngle = angleToPlayer

                // Move / strafe toward optimal engagement ring
                val moveSpeed = 2.6f * rankTier.aiSkillMultiplier
                val dirX = if (dist > 260f) cos(angleToPlayer) else -sin(angleToPlayer) * bot.strafeDir
                val dirY = if (dist > 260f) sin(angleToPlayer) else cos(angleToPlayer) * bot.strafeDir
                val nx = (bot.x + dirX * moveSpeed).coerceIn(80f, worldSize - 80f)
                val ny = (bot.y + dirY * moveSpeed).coerceIn(80f, worldSize - 80f)
                val hitWall = obstacles.any { o ->
                    nx in (o.x - 16f)..(o.x + o.w + 16f) && ny in (o.y - 16f)..(o.y + o.h + 16f)
                }
                if (!hitWall) {
                    bot.x = nx
                    bot.y = ny
                } else {
                    bot.strafeDir *= -1f
                }

                // AI firing scaled by player's Competitive Rank Grade
                if (dist < 460f && now - bot.lastShotMs >= rankTier.aiReactionMs) {
                    bot.lastShotMs = now
                    val botSpread = (0.24f / rankTier.aiSkillMultiplier).coerceIn(0.05f, 0.30f)
                    val shotAngle = angleToPlayer + (Random.nextFloat() - 0.5f) * botSpread
                    bullets.add(
                        ArenaBullet(
                            x = bot.x + cos(shotAngle) * 20f,
                            y = bot.y + sin(shotAngle) * 20f,
                            vx = cos(shotAngle) * 16f,
                            vy = sin(shotAngle) * 16f,
                            fromPlayer = false,
                            damage = (7 * rankTier.aiSkillMultiplier).toInt().coerceAtLeast(5),
                            color = CrimsonStrike
                        )
                    )
                }
            }

            // Bullet physics & hits
            val bulletIter = bullets.iterator()
            while (bulletIter.hasNext()) {
                val b = bulletIter.next()
                b.x += b.vx
                b.y += b.vy
                b.life--
                val hitObstacle = obstacles.any { o ->
                    b.x in o.x..(o.x + o.w) && b.y in o.y..(o.y + o.h)
                }
                if (b.life <= 0 || hitObstacle || b.x < 0f || b.x > worldSize || b.y < 0f || b.y > worldSize) {
                    bulletIter.remove()
                    continue
                }

                if (b.fromPlayer) {
                    val target = opponents.firstOrNull { hypot(it.x - b.x, it.y - b.y) < 24f }
                    if (target != null) {
                        val isHeadshot = Random.nextFloat() < 0.28f
                        val dmg = if (isHeadshot) (b.damage * 1.45f) else b.damage.toFloat()
                        target.hp -= dmg
                        TacticalAudioEngine.playHitMarker(isHeadshot)
                        for (p in 0 until 6) {
                            particles.add(
                                ArenaParticle(
                                    x = target.x,
                                    y = target.y,
                                    vx = (Random.nextFloat() - 0.5f) * 7f,
                                    vy = (Random.nextFloat() - 0.5f) * 7f,
                                    color = if (isHeadshot) SaffronGold else CrimsonStrike,
                                    life = 14
                                )
                            )
                        }
                        if (target.hp <= 0f) {
                            kills++
                            killFeedText = "You eliminated ${target.name} with ${activeWeapon.name}!"
                            opponents.remove(target)
                            if (opponents.isEmpty()) {
                                matchWon = true
                                matchFinished = true
                                TacticalAudioEngine.playVictoryFanfare()
                            }
                        }
                        bulletIter.remove()
                    }
                } else {
                    if (hypot(playerX - b.x, playerY - b.y) < 22f) {
                        val absorbed = if (playerArmor > 0f) b.damage * 0.45f else 0f
                        playerArmor = (playerArmor - absorbed).coerceAtLeast(0f)
                        playerHp = (playerHp - (b.damage - absorbed)).coerceAtLeast(0f)
                        if (playerHp <= 0f) {
                            matchWon = false
                            matchFinished = true
                        }
                        bulletIter.remove()
                    }
                }
            }

            // Particles & Blue Zone shrink
            val partIter = particles.iterator()
            while (partIter.hasNext()) {
                val p = partIter.next()
                p.x += p.vx
                p.y += p.vy
                p.life--
                if (p.life <= 0) partIter.remove()
            }

            if (zoneRadius > 260f) {
                zoneRadius -= 0.16f
            }
            if (hypot(playerX - 1000f, playerY - 1000f) > zoneRadius) {
                playerHp = (playerHp - 0.08f).coerceAtLeast(0f)
                if (playerHp <= 0f) {
                    matchWon = false
                    matchFinished = true
                }
            }

            frameTick++
            delay(16L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(mapSpec.groundColor)
    ) {
        // 60 FPS Tactical Arena Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Read frameTick so Canvas invalidates every frame
            val tick = frameTick
            val camZoom = if (isAdsZoom) 1.38f else 1.0f
            val cx = size.width / 2f
            val cy = size.height / 2f

            fun toScreen(wx: Float, wy: Float): Offset =
                Offset(cx + (wx - playerX) * camZoom, cy + (wy - playerY) * camZoom)

            // Ground grid lines
            val gridStep = 140f
            val startGX = ((playerX - cx / camZoom) / gridStep).toInt() * gridStep
            val startGY = ((playerY - cy / camZoom) / gridStep).toInt() * gridStep
            var gx = startGX
            while (gx <= playerX + cx / camZoom + gridStep) {
                val sx = cx + (gx - playerX) * camZoom
                drawLine(Color.White.copy(alpha = 0.05f), Offset(sx, 0f), Offset(sx, size.height), 1f)
                gx += gridStep
            }
            var gy = startGY
            while (gy <= playerY + cy / camZoom + gridStep) {
                val sy = cy + (gy - playerY) * camZoom
                drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, sy), Offset(size.width, sy), 1f)
                gy += gridStep
            }

            // Blue Zone circle
            val zoneCenter = toScreen(1000f, 1000f)
            drawCircle(
                color = StormBlue.copy(alpha = 0.7f),
                radius = zoneRadius * camZoom,
                center = zoneCenter,
                style = Stroke(
                    width = 5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f)
                )
            )

            // Indian Map Cover Structures (with optional shadows for Balanced+ graphics)
            obstacles.forEach { o ->
                val topLeft = toScreen(o.x, o.y)
                if (gfx.shadowDetail) {
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.35f),
                        topLeft = topLeft + Offset(8f, 10f),
                        size = Size(o.w * camZoom, o.h * camZoom),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                }
                drawRoundRect(
                    color = mapSpec.structureColor,
                    topLeft = topLeft,
                    size = Size(o.w * camZoom, o.h * camZoom),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                drawRoundRect(
                    color = mapSpec.accentColor.copy(alpha = 0.55f),
                    topLeft = topLeft,
                    size = Size(o.w * camZoom, o.h * camZoom),
                    cornerRadius = CornerRadius(10f, 10f),
                    style = Stroke(width = 2.5f)
                )
            }

            // Loot & Airdrop Crates
            crates.filter { !it.collected }.forEach { crate ->
                val pos = toScreen(crate.x, crate.y)
                val boxColor = if (crate.isAirdrop) CrimsonStrike else SaffronGold
                drawRect(
                    color = boxColor,
                    topLeft = pos - Offset(14f * camZoom, 14f * camZoom),
                    size = Size(28f * camZoom, 28f * camZoom)
                )
                drawCircle(
                    color = boxColor.copy(alpha = 0.35f),
                    radius = (26f + (tick % 20)) * camZoom,
                    center = pos,
                    style = Stroke(2f)
                )
            }

            // Opponents
            opponents.forEach { bot ->
                val bPos = toScreen(bot.x, bot.y)
                drawCircle(
                    color = bot.tierColor.copy(alpha = 0.25f),
                    radius = 22f * camZoom,
                    center = bPos
                )
                drawCircle(
                    color = CrimsonStrike,
                    radius = 15f * camZoom,
                    center = bPos
                )
                // Gun barrel direction
                drawLine(
                    color = Color.White,
                    start = bPos,
                    end = bPos + Offset(cos(bot.aimAngle) * 24f * camZoom, sin(bot.aimAngle) * 24f * camZoom),
                    strokeWidth = 4f
                )
                // Enemy HP bar
                val hpRatio = (bot.hp / bot.maxHp).coerceIn(0f, 1f)
                drawRect(
                    color = Color.Black.copy(alpha = 0.65f),
                    topLeft = bPos + Offset(-20f * camZoom, -28f * camZoom),
                    size = Size(40f * camZoom, 5f)
                )
                drawRect(
                    color = CrimsonStrike,
                    topLeft = bPos + Offset(-20f * camZoom, -28f * camZoom),
                    size = Size(40f * camZoom * hpRatio, 5f)
                )
            }

            // Bullets
            bullets.forEach { b ->
                val bPos = toScreen(b.x, b.y)
                drawLine(
                    color = b.color,
                    start = bPos,
                    end = bPos - Offset(b.vx * 0.9f, b.vy * 0.9f),
                    strokeWidth = if (gfx.bloomEffects) 5f else 3f
                )
            }

            // Particles & Map Weather
            particles.forEach { p ->
                drawCircle(
                    color = p.color.copy(alpha = (p.life / 14f).coerceIn(0f, 1f)),
                    radius = 4.5f * camZoom,
                    center = toScreen(p.x, p.y)
                )
            }
            val weatherCount = (18 * gfx.particlesDensity).toInt()
            for (w in 0 until weatherCount) {
                val wx = ((w * 137 + tick * 4) % size.width.toInt()).toFloat()
                val wy = ((w * 251 + tick * 6) % size.height.toInt()).toFloat()
                drawCircle(mapSpec.weatherParticleColor, radius = 2.2f, center = Offset(wx, wy))
            }

            // Player Avatar (Center)
            val pCenter = Offset(cx, cy)
            drawCircle(
                color = BharatTeal.copy(alpha = 0.25f),
                radius = (if (isCrouched) 22f else 28f) * camZoom,
                center = pCenter
            )
            drawCircle(
                color = SaffronGold,
                radius = (if (isCrouched) 14f else 17f) * camZoom,
                center = pCenter
            )
            // Laser sight / aim line
            drawLine(
                color = SaffronGold.copy(alpha = if (isAdsZoom) 0.7f else 0.35f),
                start = pCenter,
                end = pCenter + Offset(cos(playerAimAngle) * 140f * camZoom, sin(playerAimAngle) * 140f * camZoom),
                strokeWidth = if (isAdsZoom) 3f else 2f
            )
        }

        // Top Tactical HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Alive + Kills + Kavach Shield
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HudPill("ALIVE ${opponents.size + 1}", CrimsonStrike)
                    HudPill("KILLS $kills", SaffronGold)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianBg.copy(alpha = 0.8f))
                            .border(1.dp, EmeraldVerified.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Security, contentDescription = "Kavach Verified", tint = EmeraldVerified, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("KAVACH", style = MaterialTheme.typography.labelSmall, color = EmeraldVerified)
                    }
                }

                // Minimap + Exit
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mini Radar
                    Canvas(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianBg.copy(alpha = 0.85f))
                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                    ) {
                        val scale = size.width / worldSize
                        drawCircle(
                            color = StormBlue.copy(alpha = 0.7f),
                            radius = zoneRadius * scale,
                            center = Offset(1000f * scale, 1000f * scale),
                            style = Stroke(1.5f)
                        )
                        opponents.forEach { bot ->
                            drawCircle(CrimsonStrike, radius = 2.5f, center = Offset(bot.x * scale, bot.y * scale))
                        }
                        drawCircle(EmeraldVerified, radius = 3.5f, center = Offset(playerX * scale, playerY * scale))
                    }

                    IconButton(
                        onClick = { onExitMatch(kills, matchWon, shotsFired, maxSpeedObserved) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ObsidianBg.copy(alpha = 0.8f))
                            .testTag("arena_exit_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Exit Arena", tint = TextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Kill Feed & Compact Voice Chat Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Text(
                        text = emoteBanner ?: killFeedText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (emoteBanner != null) SaffronGold else BharatTeal,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ObsidianBg.copy(alpha = 0.72f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${gfx.name} · ${gfx.targetFps} FPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                VoiceChatBar(
                    voiceState = voiceState,
                    onPermissionResult = onVoicePermissionResult,
                    onToggleMic = onToggleMic,
                    onToggleChannel = onToggleChannel,
                    modifier = Modifier.width(260.dp)
                )
            }
        }

        // Bottom Controls Layer: Dual Thumb Joysticks + Action Buttons + Weapon Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // HP & Armor Bars + Quick Medkit / Grenade / Emote Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("ARMOR ${playerArmor.toInt()}% · HP ${playerHp.toInt()}", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { (playerHp / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                        color = if (playerHp > 35f) EmeraldVerified else CrimsonStrike,
                        trackColor = ObsidianBg.copy(alpha = 0.8f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TacticalActionChip("MED ($medkits)", Icons.Filled.Favorite, EmeraldVerified) {
                        if (medkits > 0 && playerHp < 100f) {
                            medkits--
                            playerHp = (playerHp + 45f).coerceAtMost(100f)
                            TacticalAudioEngine.playReload()
                        }
                    }
                    TacticalActionChip("FRAG ($grenades)", Icons.Filled.LocalFireDepartment, SaffronGold) {
                        if (grenades > 0) {
                            grenades--
                            val blastX = playerX + cos(playerAimAngle) * 180f
                            val blastY = playerY + sin(playerAimAngle) * 180f
                            for (p in 0 until 18) {
                                particles.add(
                                    ArenaParticle(blastX, blastY, (Random.nextFloat() - 0.5f) * 12f, (Random.nextFloat() - 0.5f) * 12f, SaffronGold, 16)
                                )
                            }
                            TacticalAudioEngine.playGunshot("SHOTGUN")
                            val hitBots = opponents.filter { hypot(it.x - blastX, it.y - blastY) < 130f }
                            hitBots.forEach { bot ->
                                bot.hp -= 85f
                                if (bot.hp <= 0f) {
                                    kills++
                                    killFeedText = "Frag Grenade eliminated ${bot.name}!"
                                    opponents.remove(bot)
                                    if (opponents.isEmpty()) {
                                        matchWon = true
                                        matchFinished = true
                                        TacticalAudioEngine.playVictoryFanfare()
                                    }
                                }
                            }
                        }
                    }
                    TacticalActionChip("EMOTE", Icons.Filled.EmojiEmotions, BharatTeal) {
                        emoteBanner = "${profile.callsign} used ${profile.selectedEmote}!"
                        TacticalAudioEngine.playUiClick()
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dual Thumb Joysticks + Scope/Reload + Weapon Slots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Left Movement Joystick
                VirtualJoystick(
                    label = "MOVE",
                    accent = BharatTeal,
                    onVectorChanged = { x, y ->
                        moveVecX = x
                        moveVecY = y
                    },
                    modifier = Modifier.testTag("arena_move_joystick")
                )

                // Center Weapon Switcher & Crouch/Reload
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { isAdsZoom = !isAdsZoom },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isAdsZoom) SaffronGold else TacticalSlate.copy(alpha = 0.9f))
                                .border(1.dp, SaffronGold, CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.CenterFocusStrong,
                                contentDescription = "Toggle ADS Scope",
                                tint = if (isAdsZoom) ObsidianBg else SaffronGold
                            )
                        }
                        IconButton(
                            onClick = {
                                if (!isReloading && ammoInMag < activeWeapon.magSize) {
                                    isReloading = true
                                    lastShotTimeMs = System.currentTimeMillis()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(TacticalSlate.copy(alpha = 0.9f))
                                .border(1.dp, BharatTeal, CircleShape)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Reload Weapon", tint = BharatTeal)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WeaponSlotPill("1: ${primaryWep.name.substringBefore(" ")}", activeSlot == 1) { activeSlot = 1 }
                        WeaponSlotPill("2: ${secondaryWep.name.substringBefore(" ")}", activeSlot == 2) { activeSlot = 2 }
                        WeaponSlotPill("PAN", activeSlot == 3) { activeSlot = 3 }
                    }
                    Text(
                        text = if (isReloading) "RELOADING..." else if (activeSlot == 3) "MELEE READY" else "AMMO $ammoInMag / ${activeWeapon.magSize}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isReloading) SaffronGold else TextPrimary
                    )
                }

                // Right Aim & Fire Joystick
                VirtualJoystick(
                    label = "FIRE",
                    accent = CrimsonStrike,
                    onVectorChanged = { x, y ->
                        aimVecX = x
                        aimVecY = y
                        isFiring = hypot(x, y) > 0.12f
                    },
                    onTapFire = {
                        isFiring = true
                    },
                    onRelease = {
                        isFiring = false
                    },
                    modifier = Modifier.testTag("arena_fire_joystick")
                )
            }
        }

        // Post-Match Victory / Elimination Overlay
        if (matchFinished) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TacticalSlate),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                        .border(2.dp, if (matchWon) SaffronGold else CrimsonStrike, RoundedCornerShape(22.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (matchWon) "VIJAYI BHAVA! #1" else "ELIMINATED",
                            style = MaterialTheme.typography.displayMedium,
                            color = if (matchWon) SaffronGold else CrimsonStrike
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${mapSpec.name} · ${modeSpec.name}",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Eliminations: $kills  |  Shots: $shotsFired  |  Kavach Verified",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldVerified
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                onExitMatch(kills, matchWon, shotsFired, maxSpeedObserved)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronGold, contentColor = ObsidianBg),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("claim_match_rewards_button")
                        ) {
                            Text(
                                text = "CLAIM RANK POINTS & RETURN TO LOBBY",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HudPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianBg.copy(alpha = 0.82f))
            .border(1.dp, color.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun TacticalActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(TacticalSlate.copy(alpha = 0.92f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
    }
}

@Composable
private fun WeaponSlotPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) SaffronGold else TacticalSlate.copy(alpha = 0.92f))
            .border(1.dp, if (selected) SaffronGold else CardBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) ObsidianBg else TextPrimary
        )
    }
}

@Composable
private fun VirtualJoystick(
    label: String,
    accent: Color,
    onVectorChanged: (Float, Float) -> Unit,
    onTapFire: (() -> Unit)? = null,
    onRelease: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadius = 46f

    Box(
        modifier = modifier
            .size(116.dp)
            .clip(CircleShape)
            .background(ObsidianBg.copy(alpha = 0.6f))
            .border(2.dp, accent.copy(alpha = 0.55f), CircleShape)
            .clickable(enabled = onTapFire != null) { onTapFire?.invoke() }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onTapFire?.invoke() },
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onVectorChanged(0f, 0f)
                        onRelease?.invoke()
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        onVectorChanged(0f, 0f)
                        onRelease?.invoke()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val next = knobOffset + dragAmount
                        val dist = hypot(next.x, next.y)
                        knobOffset = if (dist > maxRadius) {
                            Offset(next.x / dist * maxRadius, next.y / dist * maxRadius)
                        } else next
                        onVectorChanged(knobOffset.x / maxRadius, knobOffset.y / maxRadius)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = accent.copy(alpha = 0.85f),
                radius = 22.dp.toPx(),
                center = center + knobOffset
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
        )
    }
}
