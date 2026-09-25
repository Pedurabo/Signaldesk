package com.signaldesk.backend.incident

import org.springframework.data.jpa.repository.JpaRepository

interface IncidentRepository : JpaRepository<Incident, Long> {

    fun findByStatus(
        status: IncidentStatus
    ): List<Incident>

    fun findBySeverity(
        severity: Severity
    ): List<Incident>

    fun findByStatusAndSeverity(
        status: IncidentStatus,
        severity: Severity
    ): List<Incident>
}
