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
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class GameRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeName(raw: String): String {
        val trimmed = raw.trim().take(24)
        return trimmed.ifEmpty { "Agent" }
    }

    // --- USER PROFILE ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        require(uid == userId) { "Cross-user profile observation forbidden." }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(defaultName: String): Result<UserProfile> {
        var docPath = "users/unknown"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val existing = snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existing != null) return Result.success(existing)
            }
            val safeName = sanitizeName(defaultName)
            val payload = mapOf(
                "userId" to uid,
                "displayName" to safeName,
                "gamesPlayed" to 0L,
                "wins" to 0L,
                "impostorRounds" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            val created = UserProfile(
                id = uid,
                userId = uid,
                displayName = safeName
            )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateNickname(newName: String): Result<Unit> {
        var docPath = "users/unknown"
        return try {
            val uid = requireUserId()
            val safeName = sanitizeName(newName)
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "displayName" to safeName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- MULTIPLAYER ROOMS ---

    fun observeMyRooms(userId: String): Flow<List<GameRoom>> = flow {
        val uid = requireUserId()
        require(uid == userId) { "Cross-user room listing forbidden." }
        val path = "rooms"
        emitAll(
            db.collection("rooms")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GameRoom::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeRoom(roomCode: String): Flow<GameRoom?> = flow {
        val uid = requireUserId()
        val cleanCode = roomCode.trim().uppercase()
        val path = "rooms/$cleanCode"
        emitAll(
            db.collection("rooms").document(cleanCode)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            GameRoom::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeMyAssignment(roomCode: String, userId: String): Flow<PlayerAssignment?> = flow {
        val uid = requireUserId()
        require(uid == userId) { "Cannot read another player's secret assignment." }
        val cleanCode = roomCode.trim().uppercase()
        val path = "rooms/$cleanCode/assignments/$uid"
        emitAll(
            db.collection("rooms").document(cleanCode)
                .collection("assignments").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            PlayerAssignment::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeHostRoundSecret(roomCode: String): Flow<HostRoundSecret?> = flow {
        val uid = requireUserId()
        val cleanCode = roomCode.trim().uppercase()
        val path = "rooms/$cleanCode/hostSecrets/current"
        emitAll(
            db.collection("rooms").document(cleanCode)
                .collection("hostSecrets").document("current")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            HostRoundSecret::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeMySecretAction(roomCode: String, userId: String): Flow<SecretRoundAction?> = flow {
        val uid = requireUserId()
        require(uid == userId) { "Cannot read another player's secret action." }
        val cleanCode = roomCode.trim().uppercase()
        val path = "rooms/$cleanCode/secretActions/$uid"
        emitAll(
            db.collection("rooms").document(cleanCode)
                .collection("secretActions").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            SecretRoundAction::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createRoom(hostNickname: String): Result<String> {
        var docPath = "rooms"
        return try {
            val uid = requireUserId()
            val safeName = sanitizeName(hostNickname)
            var roomCode = generateRoomCode()
            var attempts = 0
            while (attempts < 5) {
                val checkRef = db.collection("rooms").document(roomCode)
                docPath = checkRef.path
                val snap = checkRef.get().await()
                if (!snap.exists()) break
                roomCode = generateRoomCode()
                attempts++
            }
            val roomRef = db.collection("rooms").document(roomCode)
            docPath = roomRef.path
            val payload = mapOf(
                "roomCode" to roomCode,
                "hostId" to uid,
                "status" to RoomStatus.LOBBY.name,
                "gameMode" to GameMode.CLASSIC.name,
                "impostorCount" to 1L,
                "turnDurationSeconds" to 45L,
                "selectedCategory" to WordBank.CATEGORY_ALL,
                "hintsEnabled" to true,
                "categoryCluesEnabled" to true,
                "timerEnabled" to true,
                "impostorAbilityEnabled" to false,
                "saviorAgentEnabled" to false,
                "agentDirectorEnabled" to false,
                "agentPredictionEnabled" to false,
                "currentRound" to 0L,
                "category" to "",
                "genre" to "",
                "hint" to "",
                "memberUids" to listOf(uid),
                "playerNames" to mapOf(uid to safeName),
                "playerScores" to mapOf(uid to 0L),
                "turnOrder" to emptyList<String>(),
                "currentTurnIndex" to 0L,
                "timerPaused" to false,
                "readyUids" to emptyList<String>(),
                "votes" to emptyMap<String, String>(),
                "revealedImpostorUids" to emptyList<String>(),
                "revealedPredictions" to emptyMap<String, String>(),
                "revealedRoundDeltas" to emptyMap<String, Long>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            roomRef.set(payload).await()
            Result.success(roomCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinRoom(rawCode: String, nickname: String): Result<String> {
        val cleanCode = rawCode.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")
        if (cleanCode.length !in 4..16) {
            return Result.failure(IllegalArgumentException("Enter a valid 6-character room code."))
        }
        val docRef = db.collection("rooms").document(cleanCode)
        return try {
            val uid = requireUserId()
            val safeName = sanitizeName(nickname)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalStateException("Room $cleanCode not found."))
            }
            val room = snap.toObject(
                GameRoom::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.failure(IllegalStateException("Room $cleanCode could not be loaded."))

            if (uid in room.memberUids) {
                return Result.success(cleanCode)
            }
            if (room.roomStatus() != RoomStatus.LOBBY) {
                return Result.failure(IllegalStateException("Game already in progress in room $cleanCode."))
            }
            if (room.memberUids.size >= 20) {
                return Result.failure(IllegalStateException("Room $cleanCode is full (max 20 players)."))
            }

            docRef.update(
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(uid),
                    "playerNames.$uid" to safeName,
                    "playerScores.$uid" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(cleanCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun renamePlayerInRoom(roomCode: String, newName: String): Result<Unit> {
        val cleanCode = roomCode.trim().uppercase()
        val docRef = db.collection("rooms").document(cleanCode)
        return try {
            val uid = requireUserId()
            val safeName = sanitizeName(newName)
            docRef.update(
                mapOf(
                    "playerNames.$uid" to safeName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateRoomSettings(
        roomCode: String,
        gameMode: GameMode,
        impostorCount: Int,
        turnDurationSeconds: Int,
        selectedCategory: String,
        hintsEnabled: Boolean,
        categoryCluesEnabled: Boolean,
        timerEnabled: Boolean,
        impostorAbilityEnabled: Boolean,
        saviorAgentEnabled: Boolean,
        agentDirectorEnabled: Boolean,
        agentPredictionEnabled: Boolean
    ): Result<Unit> {
        val cleanCode = roomCode.trim().uppercase()
        val docRef = db.collection("rooms").document(cleanCode)
        return try {
            requireUserId()
            val clampedImpostors = impostorCount.coerceIn(1, 10).toLong()
            val clampedDuration = turnDurationSeconds.coerceIn(5, 300).toLong()
            val safeCategory = selectedCategory.trim().take(40).ifEmpty { WordBank.CATEGORY_ALL }
            docRef.update(
                mapOf(
                    "gameMode" to gameMode.name,
                    "impostorCount" to clampedImpostors,
                    "turnDurationSeconds" to clampedDuration,
                    "selectedCategory" to safeCategory,
                    "hintsEnabled" to hintsEnabled,
                    "categoryCluesEnabled" to categoryCluesEnabled,
                    "timerEnabled" to timerEnabled,
                    "impostorAbilityEnabled" to impostorAbilityEnabled,
                    "saviorAgentEnabled" to saviorAgentEnabled,
                    "agentDirectorEnabled" to agentDirectorEnabled,
                    "agentPredictionEnabled" to agentPredictionEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun startMultiplayerRound(
        room: GameRoom,
        wordEntry: WordEntry
    ): Result<Unit> {
        val cleanCode = room.roomCode.trim().uppercase()
        val roomRef = db.collection("rooms").document(cleanCode)
        return try {
            val uid = requireUserId()
            require(room.hostId == uid) { "Only the host can start the round." }
            val members = room.memberUids
            require(members.size >= 2) { "At least 2 players are needed to start." }

            val maxImpostors = (members.size - 1).coerceAtLeast(1)
            val actualImpostorCount = room.impostorCount.toInt().coerceIn(1, maxImpostors)
            val impostorUids = CryptoRandom.shuffled(members).take(actualImpostorCount)
            val impostorSet = impostorUids.toSet()
            val ultimateKillerUid = if (room.impostorAbilityEnabled) {
                CryptoRandom.pickOne(impostorUids)
            } else {
                null
            }
            val regularAgents = CryptoRandom.shuffled(members.filterNot { it in impostorSet })
            val saviorUid = if (room.saviorAgentEnabled && regularAgents.isNotEmpty()) {
                regularAgents.first()
            } else {
                null
            }
            val directorUid = if (room.agentDirectorEnabled && regularAgents.isNotEmpty()) {
                if (regularAgents.size >= 2) regularAgents[1] else regularAgents[0]
            } else {
                null
            }

            val nextRound = room.currentRound + 1L
            val turnOrder = CryptoRandom.shuffled(members)
            val resolvedCategory = if (room.categoryCluesEnabled) wordEntry.category else ""
            val resolvedGenre = if (room.categoryCluesEnabled) wordEntry.genre else ""
            val resolvedHint = if (room.hintsEnabled) wordEntry.hintForMode(room.resolvedGameMode()) else ""

            val batch = db.batch()
            members.forEach { memberUid ->
                val isImp = memberUid in impostorSet
                val assignmentRef = roomRef.collection("assignments").document(memberUid)
                val assignmentData = mapOf(
                    "uid" to memberUid,
                    "role" to if (isImp) PlayerRole.IMPOSTOR.name else PlayerRole.NORMAL.name,
                    "secretWord" to if (isImp) "" else wordEntry.word,
                    "category" to resolvedCategory,
                    "genre" to resolvedGenre,
                    "hint" to resolvedHint,
                    "ultimateKiller" to (isImp && memberUid == ultimateKillerUid),
                    "savior" to (!isImp && memberUid == saviorUid),
                    "director" to (!isImp && memberUid == directorUid),
                    "round" to nextRound,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(assignmentRef, assignmentData)
            }

            val hostSecretRef = roomRef.collection("hostSecrets").document("current")
            batch.set(
                hostSecretRef,
                mapOf(
                    "hostId" to uid,
                    "secretWord" to wordEntry.word,
                    "impostorUids" to impostorUids,
                    "ultimateKillerUid" to (ultimateKillerUid ?: ""),
                    "saviorUid" to (saviorUid ?: ""),
                    "directorUid" to (directorUid ?: ""),
                    "round" to nextRound,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.update(
                roomRef,
                mapOf(
                    "status" to RoomStatus.REVEAL.name,
                    "impostorCount" to actualImpostorCount.toLong(),
                    "currentRound" to nextRound,
                    "category" to resolvedCategory,
                    "genre" to resolvedGenre,
                    "hint" to resolvedHint,
                    "turnOrder" to turnOrder,
                    "currentTurnIndex" to 0L,
                    "turnStartedAt" to FieldValue.serverTimestamp(),
                    "timerPaused" to !room.timerEnabled,
                    "readyUids" to emptyList<String>(),
                    "votes" to emptyMap<String, String>(),
                    "revealedWord" to FieldValue.delete(),
                    "revealedImpostorUids" to emptyList<String>(),
                    "revealedKillerUid" to FieldValue.delete(),
                    "revealedNullifiedVoterUid" to FieldValue.delete(),
                    "revealedSaviorUid" to FieldValue.delete(),
                    "revealedProtectedUid" to FieldValue.delete(),
                    "revealedSaviorSaved" to FieldValue.delete(),
                    "revealedDirectorUid" to FieldValue.delete(),
                    "revealedPredictions" to emptyMap<String, String>(),
                    "revealedRoundDeltas" to emptyMap<String, Long>(),
                    "impostorGuess" to FieldValue.delete(),
                    "impostorGuessBy" to FieldValue.delete(),
                    "impostorGuessCorrect" to FieldValue.delete(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, roomRef.path)
            Result.failure(e)
        }
    }

    suspend fun markRoleReady(roomCode: String): Result<Unit> {
        val cleanCode = roomCode.trim().uppercase()
        val roomRef = db.collection("rooms").document(cleanCode)
        return try {
            val uid = requireUserId()
            roomRef.update(
                mapOf(
                    "readyUids" to FieldValue.arrayUnion(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            Result.failure(e)
        }
    }

    suspend fun startDiscussionTurns(roomCode: String): Result<Unit> {
        val cleanCode = roomCode.trim().uppercase()
        val roomRef = db.collection("rooms").document(cleanCode)
        return try {
            requireUserId()
            roomRef.update(
                mapOf(
                    "status" to RoomStatus.PLAYING.name,
                    "currentTurnIndex" to 0L,
                    "turnStartedAt" to FieldValue.serverTimestamp(),
                    "timerPaused" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            Result.failure(e)
        }
    }

    suspend fun advanceTurn(roomCode: String, nextIndex: Int, totalPlayers: Int): Result<Unit> {
        val cleanCode = roomCode.trim().uppercase()
        val roomRef = db.collection("rooms").document(cleanCode)
        return try {
            requireUserId()
            if (nextIndex >= totalPlayers) {
                roomRef.update(
                    mapOf(
                        "status" to RoomStatus.VOTING.name,
                        "timerPaused" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                roomRef.update(
                    mapOf(
                        "currentTurnIndex" to nextIndex.toLong(),
                        "turnStartedAt" to FieldValue.serverTimestamp(),
                        "timerPaused" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleTimerPaused(roomCode: String, paused: Boolean): Result<Unit> {
        val cleanCode = roomCode.trim().uppercase()
        val roomRef = db.collection("rooms").document(cleanCode)
        return try {
            requireUserId()
            roomRef.update(
                mapOf(
                    "timerPaused" to paused,
                    "turnStartedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            Result.failure(e)
        }
    }

    suspend fun startVoting(roomCode: String): Result<Unit> {
        val cleanCode = roomCode.trim().uppercase()
        val roomRef = db.collection("rooms").document(cleanCode)
        return try {
            requireUserId()
            roomRef.update(
                mapOf(
                    "status" to RoomStatus.VOTING.name,
                    "timerPaused" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            Result.failure(e)
        }
    }

    suspend fun castVote(roomCode: String, suspectUid: String): Result<Unit> {
        val cleanCode = roomCode.trim().uppercase()
        val roomRef = db.collection("rooms").document(cleanCode)
        return try {
            val uid = requireUserId()
            require(suspectUid.isNotBlank()) { "Suspect UID cannot be blank." }
            roomRef.update(
                mapOf(
                    "votes.$uid" to suspectUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitImpostorGuess(roomCode: String, guess: String): Result<Unit> {
        val cleanCode = roomCode.trim().uppercase()
        val roomRef = db.collection("rooms").document(cleanCode)
        return try {
            val uid = requireUserId()
            val safeGuess = guess.trim().take(80)
            require(safeGuess.isNotEmpty()) { "Enter a word guess before submitting." }
            roomRef.update(
                mapOf(
                    "impostorGuess" to safeGuess,
                    "impostorGuessBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitSecretAction(
        roomCode: String,
        round: Long,
        killedVoterUid: String?,
        protectedPlayerUid: String?,
        predictedImpostorUid: String?
    ): Result<Unit> {
        val cleanCode = roomCode.trim().uppercase()
        var docPath = "rooms/$cleanCode/secretActions"
        return try {
            val uid = requireUserId()
            require(round >= 1L) { "Round must be active before submitting an action." }
            val actionRef = db.collection("rooms").document(cleanCode)
                .collection("secretActions").document(uid)
            docPath = actionRef.path
            val payload = mapOf(
                "uid" to uid,
                "round" to round,
                "killedVoterUid" to (killedVoterUid?.trim()?.take(128) ?: ""),
                "protectedPlayerUid" to (protectedPlayerUid?.trim()?.take(128) ?: ""),
                "predictedImpostorUid" to (predictedImpostorUid?.trim()?.take(128) ?: ""),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            actionRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun revealResults(room: GameRoom, hostSecret: HostRoundSecret): Result<Unit> {
        val cleanCode = room.roomCode.trim().uppercase()
        val roomRef = db.collection("rooms").document(cleanCode)
        return try {
            val uid = requireUserId()
            require(room.hostId == uid) { "Only the host can reveal round results." }

            val impostorSet = hostSecret.impostorUids.toSet()
            val guessCorrect = !room.impostorGuess.isNullOrBlank() &&
                WordBank.isGuessMatch(room.impostorGuess, hostSecret.secretWord)

            val actions = if (room.impostorAbilityEnabled || room.saviorAgentEnabled || room.agentPredictionEnabled) {
                roomRef.collection("secretActions").get().await()
                    .toObjects(
                        SecretRoundAction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    .filter { it.round == room.currentRound }
            } else {
                emptyList()
            }

            val ultimateKillerUid = if (room.impostorAbilityEnabled) {
                hostSecret.ultimateKillerUid?.takeIf { it.isNotBlank() && it in impostorSet }
            } else {
                null
            }

            val saviorUid = if (room.saviorAgentEnabled) {
                hostSecret.saviorUid?.takeIf { it.isNotBlank() && it in room.memberUids && it !in impostorSet }
            } else {
                null
            }

            val directorUid = if (room.agentDirectorEnabled) {
                hostSecret.directorUid?.takeIf { it.isNotBlank() && it in room.memberUids && it !in impostorSet }
            } else {
                null
            }

            val nullifiedVoterUid = if (ultimateKillerUid != null) {
                actions.firstOrNull { it.uid == ultimateKillerUid }
                    ?.killedVoterUid
                    ?.takeIf { it.isNotBlank() && it != ultimateKillerUid && it in room.memberUids }
            } else {
                null
            }

            val protectedPlayerUid = if (saviorUid != null) {
                actions.firstOrNull { it.uid == saviorUid }
                    ?.protectedPlayerUid
                    ?.takeIf { it.isNotBlank() && it != saviorUid && it in room.memberUids }
            } else {
                null
            }

            val agentPredictions = if (room.agentPredictionEnabled) {
                actions.filter {
                    it.uid in room.memberUids &&
                        it.uid !in impostorSet &&
                        it.predictedImpostorUid.isNotBlank() &&
                        it.predictedImpostorUid in room.memberUids
                }.associate { it.uid to it.predictedImpostorUid }
            } else {
                emptyMap()
            }

            val eval = RoundScoring.evaluate(
                playerIds = room.memberUids,
                impostorIds = impostorSet,
                votes = room.votes,
                agentPredictions = agentPredictions,
                nullifiedVoterId = nullifiedVoterUid,
                protectedPlayerId = protectedPlayerUid,
                saviorAgentId = saviorUid,
                agentDirectorId = directorUid,
                impostorAbilityEnabled = room.impostorAbilityEnabled,
                saviorAgentEnabled = room.saviorAgentEnabled,
                agentDirectorEnabled = room.agentDirectorEnabled,
                agentPredictionEnabled = room.agentPredictionEnabled,
                impostorGuessCorrect = guessCorrect
            )

            val updatedScores = room.playerScores.toMutableMap()
            room.memberUids.forEach { memberUid ->
                val prev = updatedScores[memberUid] ?: 0L
                val delta = eval.playerDeltas[memberUid]?.toLong() ?: 0L
                updatedScores[memberUid] = prev + delta
            }

            val updates = mutableMapOf<String, Any>(
                "status" to RoomStatus.RESULTS.name,
                "revealedWord" to hostSecret.secretWord,
                "revealedImpostorUids" to hostSecret.impostorUids,
                "revealedKillerUid" to (ultimateKillerUid ?: FieldValue.delete()),
                "revealedNullifiedVoterUid" to (eval.activeNullifiedVoterId ?: FieldValue.delete()),
                "revealedSaviorUid" to (saviorUid ?: FieldValue.delete()),
                "revealedProtectedUid" to (eval.activeProtectedId ?: FieldValue.delete()),
                "revealedSaviorSaved" to eval.saviorSavedTarget,
                "revealedDirectorUid" to (eval.activeDirectorId ?: FieldValue.delete()),
                "revealedPredictions" to agentPredictions,
                "revealedRoundDeltas" to eval.playerDeltas.mapValues { it.value.toLong() },
                "impostorGuessCorrect" to guessCorrect,
                "playerScores" to updatedScores,
                "timerPaused" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            roomRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            Result.failure(e)
        }
    }

    suspend fun returnToLobby(roomCode: String): Result<Unit> {
        val cleanCode = roomCode.trim().uppercase()
        val roomRef = db.collection("rooms").document(cleanCode)
        return try {
            requireUserId()
            roomRef.update(
                mapOf(
                    "status" to RoomStatus.LOBBY.name,
                    "category" to "",
                    "genre" to "",
                    "hint" to "",
                    "turnOrder" to emptyList<String>(),
                    "currentTurnIndex" to 0L,
                    "turnStartedAt" to FieldValue.delete(),
                    "timerPaused" to false,
                    "readyUids" to emptyList<String>(),
                    "votes" to emptyMap<String, String>(),
                    "revealedWord" to FieldValue.delete(),
                    "revealedImpostorUids" to emptyList<String>(),
                    "revealedKillerUid" to FieldValue.delete(),
                    "revealedNullifiedVoterUid" to FieldValue.delete(),
                    "revealedSaviorUid" to FieldValue.delete(),
                    "revealedProtectedUid" to FieldValue.delete(),
                    "revealedSaviorSaved" to FieldValue.delete(),
                    "revealedDirectorUid" to FieldValue.delete(),
                    "revealedPredictions" to emptyMap<String, String>(),
                    "revealedRoundDeltas" to emptyMap<String, Long>(),
                    "impostorGuess" to FieldValue.delete(),
                    "impostorGuessBy" to FieldValue.delete(),
                    "impostorGuessCorrect" to FieldValue.delete(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            Result.failure(e)
        }
    }

    suspend fun leaveRoom(room: GameRoom): Result<Unit> {
        val cleanCode = room.roomCode.trim().uppercase()
        val roomRef = db.collection("rooms").document(cleanCode)
        return try {
            val uid = requireUserId()
            if (room.hostId == uid) {
                roomRef.delete().await()
            } else {
                roomRef.update(
                    mapOf(
                        "memberUids" to FieldValue.arrayRemove(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            Result.failure(e)
        }
    }

    private fun generateRoomCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString(6) {
            repeat(6) {
                append(alphabet[Random.nextInt(alphabet.length)])
            }
        }
    }
}
