package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Test Subject",
    val totalChambersCompleted: Long = 0L,
    val totalPortalsFired: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class GameSave(
    @DocumentId val id: String = "",
    val userId: String = "",
    val chamberId: String = "",
    val chamberName: String = "",
    val playerPosX: Double = 0.0,
    val playerPosY: Double = 0.0,
    val playerVelX: Double = 0.0,
    val playerVelY: Double = 0.0,
    val bluePortalActive: Boolean = false,
    val orangePortalActive: Boolean = false,
    val bluePortalX: Double = 0.0,
    val bluePortalY: Double = 0.0,
    val bluePortalNormalX: Double = 0.0,
    val bluePortalNormalY: Double = 0.0,
    val orangePortalX: Double = 0.0,
    val orangePortalY: Double = 0.0,
    val orangePortalNormalX: Double = 0.0,
    val orangePortalNormalY: Double = 0.0,
    val elapsedSeconds: Double = 0.0,
    val portalsFired: Long = 0L,
    val isCompleted: Boolean = false,
    val cubesStateJson: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChamberScore(
    @DocumentId val id: String = "",
    val userId: String = "",
    val chamberId: String = "",
    val bestTimeSeconds: Double = 0.0,
    val minPortals: Long = 0L,
    @ServerTimestamp val completedAt: Timestamp? = null
)
