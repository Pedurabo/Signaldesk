package com.signaldesk.android.incident

data class IncidentTimelineEvent(
    val id: Long,
    val type: IncidentTimelineEventType,
    val message: String,
    val createdAt: String
)

enum class IncidentTimelineEventType {
    CREATED,
    STATUS_CHANGED,
    NOTE_ADDED
}