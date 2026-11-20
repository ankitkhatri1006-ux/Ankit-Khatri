package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class PlayerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val callsign: String = "YodhaWarrior",
    val rankPoints: Long = 1200L,
    val rankTier: String = "Silver",
    val kills: Long = 0L,
    val wins: Long = 0L,
    val matchesPlayed: Long = 0L,
    val seasonXp: Long = 150L,
    val selectedGender: String = "BOY",
    val hairStyle: String = "Fauji Fade",
    val hairColor: String = "Jet Black",
    val faceStyle: String = "Aviators",
    val outfitStyle: String = "Maratha Armor",
    val selectedEmote: String = "Bhangra Victory",
    val primaryWeaponId: String = "m416",
    val secondaryWeaponId: String = "awm",
    val sidearmWeaponId: String = "p18c",
    val meleeWeaponId: String = "desipan",
    val unlockedAttachments: List<String> = listOf("red_dot", "suppressor", "ext_mag", "vert_grip"),
    val backpackItems: List<String> = listOf("vest_lv3", "helmet_lv3", "medkit_x2", "energy_x3", "frag_x2", "smoke_x2"),
    val graphicsPreset: String = "Ultra HD",
    val antiCheatTrust: Long = 100L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "callsign" to callsign.take(24).ifBlank { "YodhaWarrior" },
        "rankPoints" to rankPoints,
        "rankTier" to rankTier,
        "kills" to kills,
        "wins" to wins,
        "matchesPlayed" to matchesPlayed,
        "seasonXp" to seasonXp,
        "selectedGender" to selectedGender,
        "hairStyle" to hairStyle,
        "hairColor" to hairColor,
        "faceStyle" to faceStyle,
        "outfitStyle" to outfitStyle,
        "selectedEmote" to selectedEmote,
        "primaryWeaponId" to primaryWeaponId,
        "secondaryWeaponId" to secondaryWeaponId,
        "sidearmWeaponId" to sidearmWeaponId,
        "meleeWeaponId" to meleeWeaponId,
        "unlockedAttachments" to unlockedAttachments.take(20),
        "backpackItems" to backpackItems.take(20),
        "graphicsPreset" to graphicsPreset,
        "antiCheatTrust" to antiCheatTrust,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class AntiCheatLog(
    @DocumentId val id: String = "",
    val eventType: String = "SESSION_HASH",
    val status: String = "VERIFIED",
    val latencyMs: Long = 18L,
    val integrityScore: Long = 100L,
    val detail: String = "Kavach kernel integrity verified",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MatchRoom(
    @DocumentId val id: String = "",
    val hostUid: String = "",
    val hostName: String = "",
    val roomTitle: String = "",
    val mapId: String = "jaipur_fort",
    val modeId: String = "classic_br",
    val rankBracket: String = "Silver",
    val targetMmr: Long = 1200L,
    val roomState: String = "OPEN",
    val status: String = "WAITING",
    val playerUids: List<String> = emptyList(),
    val playerNames: List<String> = emptyList(),
    val readyUids: List<String> = emptyList(),
    val voiceActiveUids: List<String> = emptyList(),
    val maxPlayers: Long = 4L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ForumPost(
    @DocumentId val id: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorRank: String = "Silver",
    val category: String = "Squad Up",
    val title: String = "",
    val body: String = "",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val likedByUids: List<String> = emptyList(),
    val moderationStatus: String = "PUBLISHED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ForumComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val body: String = "",
    val moderationStatus: String = "PUBLISHED",
    @ServerTimestamp val createdAt: Timestamp? = null
)
