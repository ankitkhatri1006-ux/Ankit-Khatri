package com.example.data

import androidx.compose.ui.graphics.Color
import com.example.R

enum class WeaponCategory(val label: String) {
    AR("Assault Rifle"),
    SMG("SMG"),
    SNIPER("Sniper / DMR"),
    SHOTGUN("Shotgun / LMG"),
    SIDEARM("Sidearm / Melee")
}

data class WeaponSpec(
    val id: String,
    val name: String,
    val category: WeaponCategory,
    val ammoType: String,
    val damage: Int,
    val fireRateMs: Long,
    val recoilControl: Int,
    val rangeMeters: Int,
    val magSize: Int,
    val reloadMs: Long,
    val bulletSpeed: Float,
    val isFullAuto: Boolean,
    val accentColor: Color,
    val description: String
)

data class IndianMapSpec(
    val id: String,
    val name: String,
    val region: String,
    val sizeKm: String,
    val weather: String,
    val description: String,
    val pois: List<String>,
    val groundColor: Color,
    val structureColor: Color,
    val accentColor: Color,
    val weatherParticleColor: Color,
    val drawableRes: Int? = null
)

data class GameModeSpec(
    val id: String,
    val name: String,
    val subtitle: String,
    val squadSize: Int,
    val opponentCount: Int,
    val matchDurationSec: Int,
    val badge: String
)

data class RankTierSpec(
    val name: String,
    val minRp: Long,
    val maxRp: Long,
    val aiSkillMultiplier: Float,
    val aiReactionMs: Long,
    val color: Color
)

data class GraphicsPresetSpec(
    val name: String,
    val targetFps: Int,
    val shadowDetail: Boolean,
    val bloomEffects: Boolean,
    val particlesDensity: Float,
    val antiAliasing: String,
    val summary: String
)

data class EmoteSpec(
    val id: String,
    val name: String,
    val hindiTag: String,
    val durationMs: Long,
    val accentColor: Color
)

data class SeasonMission(
    val id: String,
    val title: String,
    val target: Int,
    val rewardXp: Int
)

object GameCatalog {

    val weapons: List<WeaponSpec> = listOf(
        // Assault Rifles
        WeaponSpec("m416", "M416 Tactical", WeaponCategory.AR, "5.56mm", 43, 95L, 88, 420, 30, 1700L, 26f, true, Color(0xFFFF9F1C), "Balanced laser-beam recoil for mid-to-long sprays."),
        WeaponSpec("akm", "AKM Desh", WeaponCategory.AR, "7.62mm", 49, 115L, 64, 380, 30, 1950L, 23f, true, Color(0xFFFF6B35), "Heavy 7.62mm punch that shreds Level 3 vests."),
        WeaponSpec("scar_l", "SCAR-L Gold", WeaponCategory.AR, "5.56mm", 42, 100L, 84, 400, 30, 1800L, 25f, true, Color(0xFFFFD166), "Reliable tactical rifle with crisp iron sights."),
        WeaponSpec("groza", "Groza Airdrop", WeaponCategory.AR, "7.62mm", 51, 82L, 76, 360, 30, 1750L, 24f, true, Color(0xFFFF3B5C), "Crate-exclusive CQC monster with blistering RPM."),
        WeaponSpec("aug", "AUG A3 Bharat", WeaponCategory.AR, "5.56mm", 44, 90L, 90, 440, 30, 1850L, 27f, true, Color(0xFF00D2B8), "Bullpup precision crate AR with near-zero horizontal kick."),
        WeaponSpec("beryl", "M762 Beryl", WeaponCategory.AR, "7.62mm", 47, 92L, 68, 370, 30, 1900L, 24f, true, Color(0xFFFB7185), "Aggressive close-range shredder for rusher squads."),

        // SMGs
        WeaponSpec("ump45", "UMP45 Stealth", WeaponCategory.SMG, ".45 ACP", 39, 98L, 92, 210, 25, 1450L, 20f, true, Color(0xFF38BDF8), "Smooth handling and tight hip-fire spread."),
        WeaponSpec("vector", "Vector Hyper", WeaponCategory.SMG, "9mm", 32, 58L, 86, 160, 19, 1250L, 21f, true, Color(0xFF22D3EE), "Fastest close-quarters TTK with extended mag."),
        WeaponSpec("uzi", "Micro UZI Pro", WeaponCategory.SMG, "9mm", 28, 52L, 82, 140, 25, 1200L, 19f, true, Color(0xFFA78BFA), "Ultra-light pocket bullet hose for building clears."),
        WeaponSpec("bizon", "PP-19 Bizon", WeaponCategory.SMG, "9mm", 35, 90L, 89, 190, 53, 1850L, 20f, true, Color(0xFF34D399), "53-round helical drum for wiping entire squads."),

        // Snipers & DMRs
        WeaponSpec("awm", "AWM Royal", WeaponCategory.SNIPER, ".300 Mag", 105, 1200L, 95, 950, 5, 2400L, 38f, false, Color(0xFFFFD166), "One-shot Level 3 helmet destroyer from airdrop crates."),
        WeaponSpec("kar98k", "Kar98k Classic", WeaponCategory.SNIPER, "7.62mm", 82, 1050L, 85, 800, 5, 2200L, 33f, false, Color(0xFFF59E0B), "Iconic bolt-action sniper for crisp headshots."),
        WeaponSpec("m24", "M24 Shadow", WeaponCategory.SNIPER, "7.62mm", 86, 1000L, 88, 840, 5, 2100L, 35f, false, Color(0xFF818CF8), "Fast muzzle velocity bolt-action with suppressed crack."),
        WeaponSpec("mini14", "Mini-14 DMR", WeaponCategory.SNIPER, "5.56mm", 48, 175L, 86, 680, 20, 1800L, 32f, false, Color(0xFF2DD4BF), "Rapid semi-auto marksman rifle for moving targets."),
        WeaponSpec("mk14", "Mk14 EBR", WeaponCategory.SNIPER, "7.62mm", 62, 140L, 70, 720, 10, 2000L, 31f, true, Color(0xFFF43F5E), "Full-auto airdrop DMR deadly at both 5m and 400m."),

        // Shotguns & LMGs
        WeaponSpec("dbs", "DBS Double", WeaponCategory.SHOTGUN, "12 Gauge", 92, 420L, 74, 60, 14, 2100L, 18f, false, Color(0xFFFB923C), "14-shell double-barrel pump monster for final circles."),
        WeaponSpec("s12k", "S12K Tactical", WeaponCategory.SHOTGUN, "12 Gauge", 78, 280L, 65, 55, 5, 1900L, 17f, false, Color(0xFFF97316), "Semi-auto AR-mag shotgun for hot drops."),
        WeaponSpec("m249", "M249 Saw", WeaponCategory.SHOTGUN, "5.56mm", 45, 78L, 72, 450, 75, 3200L, 25f, true, Color(0xFF10B981), "75-round suppressive LMG to shred vehicles and cover."),
        WeaponSpec("dp28", "DP-28 Pan", WeaponCategory.SHOTGUN, "7.62mm", 52, 118L, 84, 480, 47, 2600L, 24f, true, Color(0xFFEAB308), "47-round dinner-plate LMG with rock-solid bipod stability."),

        // Sidearms & Melee
        WeaponSpec("p18c", "P18C Auto", WeaponCategory.SIDEARM, "9mm", 24, 75L, 85, 110, 17, 1100L, 18f, true, Color(0xFF60A5FA), "Full-auto pocket sidearm when primary runs dry."),
        WeaponSpec("deagle", "Desert Eagle", WeaponCategory.SIDEARM, ".45 ACP", 60, 260L, 65, 150, 7, 1300L, 24f, false, Color(0xFFFBBF24), "High-caliber handcannon with devastating two-tap power."),
        WeaponSpec("desipan", "Desi Loha Pan", WeaponCategory.SIDEARM, "Melee", 85, 450L, 100, 35, 1, 0L, 14f, false, Color(0xFF94A3B8), "Iconic cast-iron pan — blocks rear bullets and bonks rivals.")
    )

    fun weaponById(id: String): WeaponSpec =
        weapons.firstOrNull { it.id == id } ?: weapons.first()

    val attachments: List<Pair<String, String>> = listOf(
        "red_dot" to "Red Dot Holographic (+12% ADS Speed)",
        "scope_4x" to "4x ACOG Tactical Scope",
        "scope_8x" to "8x Sniper Long-Range Optic",
        "suppressor" to "Stealth Suppressor (-40% Sound Range)",
        "compensator" to "Muzzle Compensator (+22% Recoil Control)",
        "ext_mag" to "Extended Quickdraw Mag (+10 Rounds)",
        "vert_grip" to "Vertical Foregrip (+18% Spray Stability)"
    )

    val backpackGearCatalog: List<Triple<String, String, String>> = listOf(
        Triple("vest_lv3", "Spetsnaz Vest Lv.3", "55% Armor Mitigation · 250 Durability"),
        Triple("helmet_lv3", "Titanium Helmet Lv.3", "Headshot Protection · 230 Durability"),
        Triple("medkit_x2", "First Aid Kit x2", "Restores 75 HP in 2.0s"),
        Triple("energy_x3", "Masala Chai Boost x3", "+40 Boost & Sprint Regeneration"),
        Triple("frag_x2", "Frag Grenade x2", "High-explosive area blast"),
        Triple("smoke_x2", "Smoke Grenade x2", "Instant tactical visual cover")
    )

    val indianMaps: List<IndianMapSpec> = listOf(
        IndianMapSpec(
            id = "jaipur_fort",
            name = "Jaipur Pink Fortress",
            region = "Rajasthan · 8x8 km",
            sizeKm = "8x8 km",
            weather = "Golden Dusk",
            description = "Sandstone palace ramparts, Hawa Bazaar rooftops, and sniper watchtowers.",
            pois = listOf("Amer Bastion", "Pink Bazaar", "Stepwell Yard", "Royal Gate"),
            groundColor = Color(0xFF2B2118),
            structureColor = Color(0xFFC27D52),
            accentColor = Color(0xFFFF9F1C),
            weatherParticleColor = Color(0x88FBBF24),
            drawableRes = R.drawable.img_map_jaipur_fort
        ),
        IndianMapSpec(
            id = "mumbai_docks",
            name = "Mumbai Coastal Docks",
            region = "Maharashtra · 4x4 km",
            sizeKm = "4x4 km",
            weather = "Monsoon Rain",
            description = "Shipping container mazes, Sea-Link flyover, and intense wet-asphalt CQC.",
            pois = listOf("Sassoon Pier", "Container Yard", "Marine Flyover", "Dharavi Alley"),
            groundColor = Color(0xFF141D26),
            structureColor = Color(0xFF334E68),
            accentColor = Color(0xFF38BDF8),
            weatherParticleColor = Color(0x777DD3FC)
        ),
        IndianMapSpec(
            id = "ladakh_pass",
            name = "Ladakh Frost Pass",
            region = "Himalayas · 6x6 km",
            sizeKm = "6x6 km",
            weather = "Blizzard Snow",
            description = "High-altitude snow ridges, military bunkers, and frozen river ambush points.",
            pois = listOf("Pangong Ridge", "Base Camp", "Ice Bridge", "Radar Peak"),
            groundColor = Color(0xFF1E293B),
            structureColor = Color(0xFFCBD5E1),
            accentColor = Color(0xFF93C5FD),
            weatherParticleColor = Color(0xAAFFFFFF)
        ),
        IndianMapSpec(
            id = "varanasi_ghat",
            name = "Varanasi Ghat Arena",
            region = "Uttar Pradesh · 2x2 km",
            sizeKm = "2x2 km",
            weather = "Evening Aarti Mist",
            description = "Multi-level sandstone ghats, narrow gullies, and riverboat flanks.",
            pois = listOf("Dashashwamedh", "Ghat Boats", "Stone Galli", "Bell Tower"),
            groundColor = Color(0xFF231C1A),
            structureColor = Color(0xFFB4846C),
            accentColor = Color(0xFFF59E0B),
            weatherParticleColor = Color(0x66FDE68A)
        ),
        IndianMapSpec(
            id = "sundarban_delta",
            name = "Sundarban Delta",
            region = "Bengal · 4x4 km",
            sizeKm = "4x4 km",
            weather = "Jungle Fog",
            description = "Mangrove waterways, bamboo stilts, and stealth sniper hideouts.",
            pois = listOf("Tiger Creek", "Bamboo Outpost", "Watchtower", "Delta Ferry"),
            groundColor = Color(0xFF112119),
            structureColor = Color(0xFF2D5A3F),
            accentColor = Color(0xFF2EE59D),
            weatherParticleColor = Color(0x5586EFAC)
        )
    )

    fun mapById(id: String): IndianMapSpec =
        indianMaps.firstOrNull { it.id == id } ?: indianMaps.first()

    val gameModes: List<GameModeSpec> = listOf(
        GameModeSpec("classic_br", "Classic Battle Royale", "Ranked Survival · Shrinking Blue Zone", 4, 14, 150, "RANKED"),
        GameModeSpec("tdm_warehouse", "TDM 4v4 Warehouse", "Fast Respawn · Instant Action", 4, 8, 90, "ARCADE"),
        GameModeSpec("quick_blitz", "Quick Blitz 8-Min", "Small Circle · High Loot Density", 2, 10, 80, "BLITZ"),
        GameModeSpec("sniper_duel", "Sniper Training", "Bolt-Action & DMR Only · Headshot Bonus", 1, 8, 90, "PRO")
    )

    fun modeById(id: String): GameModeSpec =
        gameModes.firstOrNull { it.id == id } ?: gameModes.first()

    val rankTiers: List<RankTierSpec> = listOf(
        RankTierSpec("Bronze", 0L, 999L, 0.72f, 650L, Color(0xFFCD7F32)),
        RankTierSpec("Silver", 1000L, 1499L, 0.85f, 520L, Color(0xFFC0C0C0)),
        RankTierSpec("Gold", 1500L, 1999L, 0.96f, 430L, Color(0xFFFFD700)),
        RankTierSpec("Platinum", 2000L, 2599L, 1.08f, 360L, Color(0xFF38BDF8)),
        RankTierSpec("Diamond", 2600L, 3199L, 1.20f, 300L, Color(0xFF818CF8)),
        RankTierSpec("Crown", 3200L, 3899L, 1.32f, 250L, Color(0xFFF472B6)),
        RankTierSpec("Ace", 3900L, 4599L, 1.45f, 210L, Color(0xFFFF3B5C)),
        RankTierSpec("Maharaja", 4600L, 50000L, 1.62f, 175L, Color(0xFFFF9F1C))
    )

    fun tierForPoints(rp: Long): RankTierSpec =
        rankTiers.lastOrNull { rp >= it.minRp } ?: rankTiers.first()

    val graphicsPresets: List<GraphicsPresetSpec> = listOf(
        GraphicsPresetSpec("Smooth", 45, false, false, 0.3f, "Off", "Battery saver, ultra-low latency"),
        GraphicsPresetSpec("Balanced", 60, true, false, 0.55f, "2x MSAA", "Crisp shadows with stable 60 FPS"),
        GraphicsPresetSpec("HD", 60, true, true, 0.75f, "2x MSAA", "High-res textures + muzzle bloom"),
        GraphicsPresetSpec("HDR", 90, true, true, 0.90f, "4x MSAA", "Rich Bharat sunset lighting & 90 FPS"),
        GraphicsPresetSpec("Ultra HD", 120, true, true, 1.2f, "4x MSAA", "Maximum shaders, dynamic sparks & extreme FPS")
    )

    fun graphicsByName(name: String): GraphicsPresetSpec =
        graphicsPresets.firstOrNull { it.name == name } ?: graphicsPresets.last()

    // Desi Character Customization Options (Boy & Girl)
    val boyHairStyles = listOf("Fauji Fade", "Desi Pompadour", "Warrior Man-Bun", "Saffron Bandana", "Spiky Commando")
    val girlHairStyles = listOf("Warrior Braid", "Sleek Tactical Bob", "High Ponytail", "Maang-Tikka Crown", "Side-Swept Waves")
    val hairColors = listOf("Jet Black", "Copper Saffron", "Ash Silver", "Midnight Indigo", "Royal Bronze")
    val boyFaceStyles = listOf("Aviators", "Battle Scar", "Night Goggles", "Tilak Mark", "Stealth Mask")
    val girlFaceStyles = listOf("Tactical Goggles", "Crimson Bindi", "Neon Visor", "War Paint", "Stealth Mask")
    val outfitStyles = listOf("Maratha Armor", "Gurkha Camo", "Mumbai Neon", "Rajput Gold", "Ladakh Parka")

    val emotes: List<EmoteSpec> = listOf(
        EmoteSpec("bhangra", "Bhangra Victory", "Dhol Beat Bounce", 2400L, Color(0xFFFF9F1C)),
        EmoteSpec("namaste", "Namaste Respect", "Tactical Bow", 2000L, Color(0xFF00D2B8)),
        EmoteSpec("chak_de", "Chak De Roar", "Power Shockwave", 2200L, Color(0xFFFF3B5C)),
        EmoteSpec("dandiya", "Dandiya Spin", "360 Agility Twirl", 2100L, Color(0xFFFFD166)),
        EmoteSpec("thalaiva", "Thalaiva Shades", "Slow-Mo Swagger", 2300L, Color(0xFF38BDF8)),
        EmoteSpec("garba", "Garba Clap", "Rhythm Taunt", 2200L, Color(0xFF2EE59D))
    )

    val seasonMissions: List<SeasonMission> = listOf(
        SeasonMission("m1", "Play 3 Ranked Matches on Indian Maps", 3, 250),
        SeasonMission("m2", "Score 10 Eliminations with AR or Sniper", 10, 400),
        SeasonMission("m3", "Win 1 Match in Jaipur Fort or Mumbai Docks", 1, 500),
        SeasonMission("m4", "Use Squad Voice Chat & Kavach Check", 1, 150)
    )

    val seasonPatchNotes: List<String> = listOf(
        "Season 07: Bharat Rising — New Jaipur Pink Fortress 8x8 km map with sandstone watchtowers!",
        "Balance Update: M416 horizontal recoil tightened +6%; AWM Royal crate drop rate increased in Final Zone.",
        "Kavach Anti-Cheat v4.2: Kernel velocity & rapid-fire cadence verifier active on all Ranked Grades.",
        "Desi Studio: Added Maang-Tikka Tactical, Maratha Sherwani Armor & 6 animated Bharat Emotes!"
    )
}
