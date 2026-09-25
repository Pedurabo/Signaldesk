package com.signaldesk.backend.incident

import java.time.Instant

data class IncidentEventResponse(
    val id: Long,
    val type: IncidentEventType,
    val message: String,
    val createdAt: Instant
)

fun IncidentEvent.toResponse(): IncidentEventResponse {
    return IncidentEventResponse(
        id = requireNotNull(id),
        type = type,
        message = message,
        createdAt = createdAt
    )
}
