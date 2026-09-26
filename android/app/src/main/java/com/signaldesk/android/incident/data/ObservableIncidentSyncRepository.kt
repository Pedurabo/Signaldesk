package com.signaldesk.android.incident.data

import kotlinx.coroutines.flow.Flow

interface ObservableIncidentSyncRepository {
    fun observeIncidentHasPendingMutations(
        incidentId: Long
    ): Flow<Boolean>
}
