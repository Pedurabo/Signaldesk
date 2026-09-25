package com.signaldesk.backend.incident

import java.time.Instant

data class IncidentResponse(
    val id: Long,
    val title: String,
    val description: String,
    val severity: Severity,
    val status: IncidentStatus,
    val createdAt: Instant
)

fun Incident.toResponse(): IncidentResponse {
    return IncidentResponse(
        id = requireNotNull(id),
        title = title,
        description = description,
        severity = severity,
        status = status,
        createdAt = createdAt
    )
}
