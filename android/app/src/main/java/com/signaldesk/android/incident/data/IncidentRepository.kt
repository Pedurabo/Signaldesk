package com.signaldesk.android.incident.data

import com.signaldesk.android.incident.Incident
import com.signaldesk.android.incident.IncidentStatus
import com.signaldesk.android.incident.Severity

interface IncidentRepository {
    fun getIncidents(): List<Incident>
}

class FakeIncidentRepository : IncidentRepository {

    override fun getIncidents(): List<Incident> {
        return listOf(
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
    }
}