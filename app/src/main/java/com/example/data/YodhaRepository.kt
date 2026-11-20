package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class YodhaRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observePlayerProfile(userId: String): Flow<PlayerProfile?> = flow {
        val uid = requireUserId()
        if (uid != userId) throw IllegalStateException("Session user mismatch.")
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(PlayerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exToLog = firestoreError ?: (error as? Exception)
                    if (exToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensurePlayerProfile(defaultCallsign: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeCallsign = defaultCallsign.replace(Regex("[^a-zA-Z0-9_ ]"), "").trim()
                    .take(20).ifBlank { "Yodha_${uid.take(5)}" }
                val initialProfile = PlayerProfile(
                    userId = uid,
                    callsign = safeCallsign
                )
                docRef.set(initialProfile.toCreateMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfileFields(fields: Map<String, Any>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val payload = fields.toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("users").document(uid).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordMatchResult(
        killsAdded: Int,
        wonMatch: Boolean,
        rpDelta: Long,
        newRankPoints: Long,
        newRankTier: String,
        xpAdded: Long
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val clampedRp = newRankPoints.coerceIn(0L, 50000L)
            val updates = mapOf(
                "kills" to FieldValue.increment(killsAdded.coerceAtLeast(0).toLong()),
                "wins" to FieldValue.increment(if (wonMatch) 1L else 0L),
                "matchesPlayed" to FieldValue.increment(1L),
                "seasonXp" to FieldValue.increment(xpAdded.coerceAtLeast(0L)),
                "rankPoints" to clampedRp,
                "rankTier" to newRankTier,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- KAVACH ANTI-CHEAT LOGS ---
    fun observeAntiCheatLogs(userId: String): Flow<List<AntiCheatLog>> = flow {
        val uid = requireUserId()
        if (uid != userId) throw IllegalStateException("Session user mismatch.")
        val path = "users/$userId/anticheat_logs"
        emitAll(
            db.collection("users").document(userId).collection("anticheat_logs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
                .snapshots()
                .map { it.toObjects(AntiCheatLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exToLog = firestoreError ?: (error as? Exception)
                    if (exToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun logAntiCheatCheck(
        eventType: String,
        status: String,
        latencyMs: Long,
        integrityScore: Long,
        detail: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val logId = "chk_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "users/$uid/anticheat_logs/$logId"
        return try {
            val payload = mapOf(
                "eventType" to eventType.take(40),
                "status" to status,
                "latencyMs" to latencyMs.coerceIn(1L, 9999L),
                "integrityScore" to integrityScore.coerceIn(0L, 100L),
                "detail" to detail.take(200),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("anticheat_logs").document(logId)
                .set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- REAL-TIME MULTIPLAYER MATCH ROOMS ---
    fun observeOpenMatchRooms(): Flow<List<MatchRoom>> = flow {
        requireUserId()
        val path = "match_rooms"
        emitAll(
            db.collection("match_rooms")
                .whereEqualTo("roomState", "OPEN")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(MatchRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exToLog = firestoreError ?: (error as? Exception)
                    if (exToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createMatchRoom(
        hostName: String,
        roomTitle: String,
        mapId: String,
        modeId: String,
        rankBracket: String,
        targetMmr: Long,
        maxPlayers: Int
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val roomId = "room_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "match_rooms/$roomId"
        return try {
            val safeTitle = roomTitle.trim().take(50).ifBlank { "$hostName's Bharat Squad" }
            val safeHost = hostName.trim().take(24).ifBlank { "Yodha" }
            val payload = mapOf(
                "hostUid" to uid,
                "hostName" to safeHost,
                "roomTitle" to safeTitle,
                "mapId" to mapId,
                "modeId" to modeId,
                "rankBracket" to rankBracket,
                "targetMmr" to targetMmr.coerceIn(0L, 50000L),
                "roomState" to "OPEN",
                "status" to "WAITING",
                "playerUids" to listOf(uid),
                "playerNames" to listOf(safeHost),
                "readyUids" to listOf(uid),
                "voiceActiveUids" to emptyList<String>(),
                "maxPlayers" to maxPlayers.coerceIn(1, 4).toLong(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("match_rooms").document(roomId).set(payload).await()
            Result.success(roomId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinOrUpdateRoom(room: MatchRoom, playerName: String, toggleReady: Boolean = false, toggleVoice: Boolean = false): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "match_rooms/${room.id}"
        return try {
            val newUids = room.playerUids.toMutableList()
            val newNames = room.playerNames.toMutableList()
            val newReady = room.readyUids.toMutableList()
            val newVoice = room.voiceActiveUids.toMutableList()

            if (!newUids.contains(uid)) {
                if (newUids.size >= room.maxPlayers) {
                    return Result.failure(IllegalStateException("Squad room is full"))
                }
                newUids.add(uid)
                newNames.add(playerName.take(24).ifBlank { "Yodha" })
            }
            if (toggleReady) {
                if (newReady.contains(uid)) newReady.remove(uid) else newReady.add(uid)
            }
            if (toggleVoice) {
                if (newVoice.contains(uid)) newVoice.remove(uid) else newVoice.add(uid)
            }

            db.collection("match_rooms").document(room.id).update(
                mapOf(
                    "playerUids" to newUids.take(4),
                    "playerNames" to newNames.take(4),
                    "readyUids" to newReady.take(4),
                    "voiceActiveUids" to newVoice.take(4),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun leaveOrCloseRoom(room: MatchRoom): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "match_rooms/${room.id}"
        return try {
            if (room.hostUid == uid) {
                db.collection("match_rooms").document(room.id).delete().await()
            } else {
                val idx = room.playerUids.indexOf(uid)
                val newUids = room.playerUids.filterNot { it == uid }
                val newNames = if (idx in room.playerNames.indices) {
                    room.playerNames.filterIndexed { i, _ -> i != idx }
                } else room.playerNames
                db.collection("match_rooms").document(room.id).update(
                    mapOf(
                        "playerUids" to newUids,
                        "playerNames" to newNames,
                        "readyUids" to room.readyUids.filterNot { it == uid },
                        "voiceActiveUids" to room.voiceActiveUids.filterNot { it == uid },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun launchMatchRoom(room: MatchRoom): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (room.hostUid != uid) return Result.failure(IllegalStateException("Only host can launch match"))
        val path = "match_rooms/${room.id}"
        return try {
            db.collection("match_rooms").document(room.id).update(
                mapOf(
                    "status" to "IN_MATCH",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- COMMUNITY FORUM ---
    fun observeForumPosts(): Flow<List<ForumPost>> = flow {
        requireUserId()
        val path = "forum_posts"
        emitAll(
            db.collection("forum_posts")
                .whereEqualTo("moderationStatus", "PUBLISHED")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ForumPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exToLog = firestoreError ?: (error as? Exception)
                    if (exToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createForumPost(
        authorName: String,
        authorRank: String,
        category: String,
        title: String,
        body: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val postId = "post_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "forum_posts/$postId"
        return try {
            val cleanTitle = title.trim().take(80)
            val cleanBody = body.trim().take(800)
            require(cleanTitle.length >= 3) { "Title must be at least 3 characters" }
            require(cleanBody.length >= 5) { "Message must be at least 5 characters" }
            val payload = mapOf(
                "authorUid" to uid,
                "authorName" to authorName.trim().take(24).ifBlank { "Yodha" },
                "authorRank" to authorRank.take(20),
                "category" to category,
                "title" to cleanTitle,
                "body" to cleanBody,
                "likesCount" to 0L,
                "commentsCount" to 0L,
                "likedByUids" to emptyList<String>(),
                "moderationStatus" to "PUBLISHED",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("forum_posts").document(postId).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun togglePostLike(post: ForumPost): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "forum_posts/${post.id}"
        return try {
            val alreadyLiked = post.likedByUids.contains(uid)
            val newLikedBy = if (alreadyLiked) {
                post.likedByUids.filterNot { it == uid }
            } else {
                (post.likedByUids + uid).take(200)
            }
            val newLikes = if (alreadyLiked) (post.likesCount - 1L).coerceAtLeast(0L) else post.likesCount + 1L
            db.collection("forum_posts").document(post.id).update(
                mapOf(
                    "likesCount" to newLikes,
                    "likedByUids" to newLikedBy,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observePostComments(postId: String): Flow<List<ForumComment>> = flow {
        requireUserId()
        val path = "forum_posts/$postId/comments"
        emitAll(
            db.collection("forum_posts").document(postId).collection("comments")
                .whereEqualTo("moderationStatus", "PUBLISHED")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ForumComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exToLog = firestoreError ?: (error as? Exception)
                    if (exToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addForumComment(postId: String, authorName: String, body: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val commentId = "c_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "forum_posts/$postId/comments/$commentId"
        return try {
            val cleanBody = body.trim().take(400)
            require(cleanBody.isNotEmpty()) { "Comment cannot be empty" }
            val batch = db.batch()
            val commentRef = db.collection("forum_posts").document(postId).collection("comments").document(commentId)
            val postRef = db.collection("forum_posts").document(postId)
            batch.set(
                commentRef,
                mapOf(
                    "postId" to postId,
                    "authorUid" to uid,
                    "authorName" to authorName.trim().take(24).ifBlank { "Yodha" },
                    "body" to cleanBody,
                    "moderationStatus" to "PUBLISHED",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                postRef,
                mapOf(
                    "commentsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
