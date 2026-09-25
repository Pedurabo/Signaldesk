package com.signaldesk.android.incident.data

import com.signaldesk.android.incident.Incident
import kotlinx.coroutines.flow.Flow

interface ObservableIncidentDetailRepository {
    fun observeIncident(
        incidentId: Long
    ): Flow<Incident?>
}