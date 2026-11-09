package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.GameSave
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PortalSaveRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveGame_validSave_createsDocumentAndReturnsId(): Unit = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = PortalSaveRepository(firestore)

        val save = GameSave(
            chamberId = "chamber_01",
            chamberName = "Introduction to Portals",
            playerPosX = 120.0,
            playerPosY = 350.0,
            playerVelX = 45.0,
            playerVelY = -10.0,
            bluePortalActive = true,
            orangePortalActive = false,
            elapsedSeconds = 12.3,
            portalsFired = 2
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveGame(save, "alice_save_1")
        }
        assertTrue(result.isSuccess)
        assertEquals("alice_save_1", result.getOrThrow())
    }

    @Test
    fun observeSaves_authenticatedOwner_returnsSavedGame(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = PortalSaveRepository(firestore)

        val save = GameSave(
            chamberId = "chamber_02",
            chamberName = "Momentum Leap",
            playerPosX = 200.0,
            playerPosY = 400.0,
            bluePortalActive = true,
            orangePortalActive = true,
            elapsedSeconds = 25.0
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveGame(save, "save_02")
        }

        val saves = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSaves(aliceUid).first { list -> list.isNotEmpty() }
        }
        assertTrue(saves.any { it.chamberId == "chamber_02" })
    }

    @Test
    fun crossUserAccess_bobCannotReadAliceSave_failsWithPermissionDenied(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = PortalSaveRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveGame(
                GameSave(chamberId = "chamber_secret", playerPosX = 10.0, playerPosY = 10.0),
                "secret_save"
            )
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = PortalSaveRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getSave(aliceUid, "secret_save")
        }
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        val firestoreException = exception?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun observeSaves_unauthenticated_failsWithPermissionDenied(): Unit = runBlocking {
        auth.signOut()
        val repository = PortalSaveRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeSaves("random_user_123").first()
            }
            fail("Expected exception due to unauthenticated access")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_test@aperture.lab"
        const val BOB_EMAIL = "bob_test@aperture.lab"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 6000L
    }
}
