package com.signaldesk.android.incident.data

import com.signaldesk.android.incident.Incident
import com.signaldesk.android.incident.IncidentStatus
import com.signaldesk.android.incident.Severity
import kotlinx.coroutines.flow.Flow

interface ObservableIncidentRepository {

    fun observeIncidents(
        status: IncidentStatus? = null,
        severity: Severity? = null
    ): Flow<List<Incident>>
}