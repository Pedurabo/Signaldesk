package com.signaldesk.android.incident.data

import com.signaldesk.android.incident.Incident
import com.signaldesk.android.incident.IncidentStatus
import com.signaldesk.android.incident.IncidentTimelineEvent
import com.signaldesk.android.incident.Severity

interface IncidentRepository {

    fun getIncidents(): List<Incident>

    fun getIncidentTimeline(
        incidentId: Long
    ): List<IncidentTimelineEvent>

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