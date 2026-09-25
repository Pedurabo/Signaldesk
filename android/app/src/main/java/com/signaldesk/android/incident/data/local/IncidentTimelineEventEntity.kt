package com.signaldesk.android.incident.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.signaldesk.android.incident.IncidentTimelineEvent
import com.signaldesk.android.incident.IncidentTimelineEventType

@Entity(tableName = "incident_timeline_events")
data class IncidentTimelineEventEntity(
    @PrimaryKey
    val id: Long,
    val incidentId: Long,
    val type: String,
    val message: String,
    val createdAt: String
)

fun IncidentTimelineEvent.toEntity(
    incidentId: Long
): IncidentTimelineEventEntity {
    return IncidentTimelineEventEntity(
        id = id,
        incidentId = incidentId,
        type = type.name,
        message = message,
        createdAt = createdAt
    )
}

fun IncidentTimelineEventEntity.toDomain():
    IncidentTimelineEvent {
    return IncidentTimelineEvent(
        id = id,
        type = IncidentTimelineEventType.valueOf(type),
        message = message,
        createdAt = createdAt
    )
}