package com.signaldesk.android.incident.data

import com.signaldesk.android.incident.IncidentTimelineEvent
import kotlinx.coroutines.flow.Flow

interface ObservableIncidentTimelineRepository {
    fun observeIncidentTimeline(
        incidentId: Long
    ): Flow<List<IncidentTimelineEvent>>
}