package com.signaldesk.android.incident.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_incident_mutations")
data class PendingIncidentMutationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val incidentId: Long,
    val type: String,
    val payload: String,
    val createdAt: Long,
    val attemptCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val lastError: String? = null
)
