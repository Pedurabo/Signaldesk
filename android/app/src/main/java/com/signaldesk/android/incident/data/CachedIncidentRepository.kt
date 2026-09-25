package com.signaldesk.android.incident.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import com.signaldesk.android.incident.Incident
import com.signaldesk.android.incident.IncidentStatus
import com.signaldesk.android.incident.IncidentTimelineEvent
import com.signaldesk.android.incident.Severity
import com.signaldesk.android.incident.data.local.IncidentDao
import com.signaldesk.android.incident.data.local.toDomain
import com.signaldesk.android.incident.data.local.toEntity

class CachedIncidentRepository(
    private val remote: IncidentRepository,
    private val local: IncidentDao
) : IncidentRepository,
    ObservableIncidentRepository,
    ObservableIncidentDetailRepository,
    ObservableIncidentTimelineRepository {
    override fun observeIncidents(
        status: IncidentStatus?,
        severity: Severity?
    ): Flow<List<Incident>> {
        return local.observeIncidents(
            status = status?.name,
            severity = severity?.name
        ).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeIncident(
        incidentId: Long
    ): Flow<Incident?> {
        return local.observeIncident(
            incidentId = incidentId
        ).map { entity ->
            entity?.toDomain()
        }
    }

    override fun getIncidents(
        status: IncidentStatus?,
        severity: Severity?
    ): List<Incident> {
        try {
            val remoteIncidents = remote.getIncidents(
                status = null,
                severity = null
            )

            local.replaceIncidents(
                remoteIncidents.map { it.toEntity() }
            )
        } catch (error: Exception) {
            if (error is CancellationException) {
                throw error
            }

            // Existing local data remains available when refresh fails.
        }

        return local.getIncidents(
            status = status?.name,
            severity = severity?.name
        ).map { it.toDomain() }
    }

    override fun getIncident(
        incidentId: Long
    ): Incident {
        return try {
            val incident =
                remote.getIncident(incidentId)

            local.upsertIncident(
                incident.toEntity()
            )

            incident
        } catch (error: Exception) {
            if (error is CancellationException) {
                throw error
            }

            local.getIncident(incidentId)
                ?.toDomain()
                ?: throw error
        }
    }

    override fun createIncident(
        title: String,
        description: String,
        severity: Severity
    ): Incident {
        val incident = remote.createIncident(
            title = title,
            description = description,
            severity = severity
        )

        local.upsertIncident(incident.toEntity())

        return incident
    }

    override fun observeIncidentTimeline(
        incidentId: Long
    ): Flow<List<IncidentTimelineEvent>> {
        return local.observeTimeline(
            incidentId = incidentId
        ).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getIncidentTimeline(
        incidentId: Long
    ): List<IncidentTimelineEvent> {
        return try {
            val events =
                remote.getIncidentTimeline(incidentId)

            local.replaceTimelineForIncident(
                incidentId = incidentId,
                events = events.map {
                    it.toEntity(incidentId)
                }
            )

            events
        } catch (error: Exception) {
            if (error is CancellationException) {
                throw error
            }

            val cachedEvents =
                local.getTimeline(incidentId)

            if (cachedEvents.isEmpty()) {
                throw error
            }

            cachedEvents.map { it.toDomain() }
        }
    }

    override fun addIncidentNote(
        incidentId: Long,
        message: String
    ): IncidentTimelineEvent {
        val event =
            remote.addIncidentNote(
                incidentId = incidentId,
                message = message
            )

        local.upsertTimelineEvents(
            listOf(
                event.toEntity(incidentId)
            )
        )

        return event
    }

    override fun updateIncidentStatus(
        incidentId: Long,
        status: IncidentStatus
    ): Incident {
        val incident = remote.updateIncidentStatus(
            incidentId = incidentId,
            status = status
        )

        local.upsertIncident(incident.toEntity())

        return incident
    }
}
