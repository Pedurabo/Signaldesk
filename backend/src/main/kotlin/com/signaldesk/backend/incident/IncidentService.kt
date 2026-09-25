package com.signaldesk.backend.incident

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class IncidentService(
    private val incidentRepository: IncidentRepository,
    private val incidentEventRepository: IncidentEventRepository
) {

    fun getIncidents(
        status: IncidentStatus?,
        severity: Severity?
    ): List<IncidentResponse> {

        val incidents = when {
            status != null && severity != null ->
                incidentRepository.findByStatusAndSeverity(status, severity)

            status != null ->
                incidentRepository.findByStatus(status)

            severity != null ->
                incidentRepository.findBySeverity(severity)

            else ->
                incidentRepository.findAll()
        }

        return incidents.map { it.toResponse() }
    }

    fun getIncident(id: Long): IncidentResponse? {
        return incidentRepository.findById(id)
            .orElse(null)
            ?.toResponse()
    }

    @Transactional
    fun createIncident(request: CreateIncidentRequest): IncidentResponse {

        val now = Instant.now()

        val incident = Incident(
            title = request.title,
            description = request.description,
            severity = request.severity,
            status = IncidentStatus.OPEN,
            createdAt = now
        )

        val savedIncident = incidentRepository.save(incident)

        val event = IncidentEvent(
            incidentId = requireNotNull(savedIncident.id),
            type = IncidentEventType.CREATED,
            message = "Incident created",
            createdAt = now
        )

        incidentEventRepository.save(event)

        return savedIncident.toResponse()
    }

    @Transactional
    fun updateStatus(
        id: Long,
        request: UpdateIncidentStatusRequest
    ): IncidentResponse? {

        val incident = incidentRepository.findById(id).orElse(null)
            ?: return null

        val previousStatus = incident.status
        val newStatus = request.status

        if (previousStatus == newStatus) {
            return incident.toResponse()
        }

        incident.status = newStatus

        val savedIncident = incidentRepository.save(incident)

        val event = IncidentEvent(
            incidentId = requireNotNull(savedIncident.id),
            type = IncidentEventType.STATUS_CHANGED,
            message = "Status changed from $previousStatus to $newStatus",
            createdAt = Instant.now()
        )

        incidentEventRepository.save(event)

        return savedIncident.toResponse()
    }

    @Transactional
    fun addNote(
        id: Long,
        request: AddIncidentNoteRequest
    ): IncidentEventResponse? {

        if (!incidentRepository.existsById(id)) {
            return null
        }

        val event = IncidentEvent(
            incidentId = id,
            type = IncidentEventType.NOTE_ADDED,
            message = request.message,
            createdAt = Instant.now()
        )

        return incidentEventRepository.save(event).toResponse()
    }
    fun getTimeline(id: Long): List<IncidentEventResponse>? {

        if (!incidentRepository.existsById(id)) {
            return null
        }

        return incidentEventRepository
            .findByIncidentIdOrderByCreatedAtAsc(id)
            .map { it.toResponse() }
    }
    @Transactional
    fun deleteIncident(id: Long): Boolean {

        if (!incidentRepository.existsById(id)) {
            return false
        }

        incidentEventRepository.deleteByIncidentId(id)
        incidentRepository.deleteById(id)

        return true
    }
}







