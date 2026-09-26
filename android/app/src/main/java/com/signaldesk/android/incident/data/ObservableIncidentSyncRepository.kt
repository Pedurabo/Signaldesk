package com.signaldesk.android.incident.data

import kotlinx.coroutines.flow.Flow

data class IncidentSyncState(
    val hasPendingMutations: Boolean,
    val attemptCount: Int,
    val lastAttemptAt: Long?,
    val lastError: String?
)

interface ObservableIncidentSyncRepository {
    fun observeIncidentSyncState(
        incidentId: Long
    ): Flow<IncidentSyncState>

    fun observeIncidentHasPendingMutations(
        incidentId: Long
    ): Flow<Boolean>
}
