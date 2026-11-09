package com.example.data.repository

import com.example.data.model.ChamberScore
import com.example.data.model.GameSave
import com.example.data.model.UserProfile
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PortalSaveRepository(
    private val db: FirebaseFirestore
) {
    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing cloud saves.")
    }

    fun observeSaves(userId: String): Flow<List<GameSave>> {
        val path = "users/$userId/saves"
        return db.collection("users").document(userId).collection("saves")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(GameSave::class.java) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun saveGame(save: GameSave, customSaveId: String? = null): Result<String> {
        val uid = requireUserId()
        val docId = customSaveId ?: if (save.id.isNotBlank()) save.id else "save_${save.chamberId}"
        val docRef = db.collection("users").document(uid).collection("saves").document(docId)

        val payload = mapOf(
            "userId" to uid,
            "chamberId" to save.chamberId,
            "chamberName" to save.chamberName,
            "playerPosX" to save.playerPosX,
            "playerPosY" to save.playerPosY,
            "playerVelX" to save.playerVelX,
            "playerVelY" to save.playerVelY,
            "bluePortalActive" to save.bluePortalActive,
            "orangePortalActive" to save.orangePortalActive,
            "bluePortalX" to save.bluePortalX,
            "bluePortalY" to save.bluePortalY,
            "bluePortalNormalX" to save.bluePortalNormalX,
            "bluePortalNormalY" to save.bluePortalNormalY,
            "orangePortalX" to save.orangePortalX,
            "orangePortalY" to save.orangePortalY,
            "orangePortalNormalX" to save.orangePortalNormalX,
            "orangePortalNormalY" to save.orangePortalNormalY,
            "elapsedSeconds" to save.elapsedSeconds,
            "portalsFired" to save.portalsFired,
            "isCompleted" to save.isCompleted,
            "cubesStateJson" to save.cubesStateJson,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getSave(userId: String, saveId: String): Result<GameSave?> {
        val docRef = db.collection("users").document(userId).collection("saves").document(saveId)
        return try {
            val snapshot = docRef.get().await()
            Result.success(snapshot.toObject(GameSave::class.java))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteSave(userId: String, saveId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("saves").document(saveId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeRecords(userId: String): Flow<List<ChamberScore>> {
        val path = "users/$userId/records"
        return db.collection("users").document(userId).collection("records")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(ChamberScore::class.java) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun recordCompletion(chamberId: String, timeSeconds: Double, portalsUsed: Long): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("records").document(chamberId)
        val payload = mapOf(
            "userId" to uid,
            "chamberId" to chamberId,
            "bestTimeSeconds" to timeSeconds,
            "minPortals" to portalsUsed,
            "completedAt" to FieldValue.serverTimestamp()
        )

        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val currentBest = existing.getDouble("bestTimeSeconds") ?: Double.MAX_VALUE
                if (timeSeconds < currentBest) {
                    docRef.set(payload).await()
                }
            } else {
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncProfile(displayName: String, completedChambers: Long, portalsFired: Long): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val payload = mapOf(
            "userId" to uid,
            "displayName" to displayName,
            "totalChambersCompleted" to completedChambers,
            "totalPortalsFired" to portalsFired,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }
}
