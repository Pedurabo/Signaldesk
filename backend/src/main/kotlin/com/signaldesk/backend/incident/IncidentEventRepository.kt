package com.signaldesk.backend.incident

import org.springframework.data.jpa.repository.JpaRepository

interface IncidentEventRepository : JpaRepository<IncidentEvent, Long> {

    fun findByIncidentIdOrderByCreatedAtAsc(
        incidentId: Long
    ): List<IncidentEvent>

    fun deleteByIncidentId(
        incidentId: Long
    )
}

