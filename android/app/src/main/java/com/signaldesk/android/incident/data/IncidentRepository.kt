package com.signaldesk.android.incident.data

import com.signaldesk.android.incident.Incident
import com.signaldesk.android.incident.IncidentStatus
import com.signaldesk.android.incident.IncidentTimelineEvent
import com.signaldesk.android.incident.Severity

interface IncidentRepository {

    fun getIncidents(): List<Incident>

    fun createIncident(
        title: String,
        description: String,
        severity: Severity
    ): Incident

    fun getIncidentTimeline(
        incidentId: Long
    ): List<IncidentTimelineEvent>

    fun addIncidentNote(
        incidentId: Long,
        message: String
    ): IncidentTimelineEvent

    fun updateIncidentStatus(
        incidentId: Long,
        status: IncidentStatus
    ): Incident
}

class FakeIncidentRepository : IncidentRepository {

    private val incidents = mutableListOf(
        Incident(
            id = 39,
            title = "Payment gateway timeout",
            description = "Checkout requests are timing out for some customers",
            severity = Severity.CRITICAL,
            status = IncidentStatus.RESOLVED
        ),
        Incident(
            id = 41,
            title = "Email delivery slowdown",
            description = "Transactional emails are taking longer than normal to reach users",
            severity = Severity.MEDIUM,
            status = IncidentStatus.INVESTIGATING
        ),
        Incident(
            id = 42,
            title = "Analytics export delayed",
            description = "Analytics exports are taking longer than expected",
            severity = Severity.HIGH,
            status = IncidentStatus.INVESTIGATING
        )
    )

    override fun getIncidents(): List<Incident> {
        return incidents.toList()
    }

    override fun getIncidentTimeline(
        incidentId: Long
    ): List<IncidentTimelineEvent> {
        return emptyList()
    }

    override fun addIncidentNote(
        incidentId: Long,
        message: String
    ): IncidentTimelineEvent {
        return IncidentTimelineEvent(
            id = 1,
            type = com.signaldesk.android.incident.IncidentTimelineEventType.NOTE_ADDED,
            message = message,
            createdAt = ""
        )
    }

    override fun createIncident(
        title: String,
        description: String,
        severity: Severity
    ): Incident {
        val nextId = (incidents.maxOfOrNull { it.id } ?: 0L) + 1L

        val incident = Incident(
            id = nextId,
            title = title,
            description = description,
            severity = severity,
            status = IncidentStatus.OPEN
        )

        incidents.add(incident)

        return incident
    }

    override fun updateIncidentStatus(
        incidentId: Long,
        status: IncidentStatus
    ): Incident {
        val index = incidents.indexOfFirst {
            it.id == incidentId
        }

        if (index == -1) {
            throw IllegalArgumentException(
                "Incident $incidentId was not found"
            )
        }

        val updatedIncident = incidents[index].copy(
            status = status
        )

        incidents[index] = updatedIncident

        return updatedIncident
    }
}