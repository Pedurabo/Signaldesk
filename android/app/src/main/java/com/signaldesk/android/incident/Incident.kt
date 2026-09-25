package com.signaldesk.android.incident

data class Incident(
    val id: Long,
    val title: String,
    val description: String,
    val severity: Severity,
    val status: IncidentStatus
)

enum class Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class IncidentStatus {
    OPEN,
    INVESTIGATING,
    RESOLVED
}
