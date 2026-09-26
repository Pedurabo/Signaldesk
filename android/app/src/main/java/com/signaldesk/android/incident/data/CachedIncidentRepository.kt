package com.signaldesk.android.incident.data
import com.signaldesk.android.incident.sync.IncidentSyncScheduling
import com.signaldesk.android.incident.sync.NoOpIncidentSyncScheduler

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
import com.signaldesk.android.incident.data.local.PendingIncidentMutationEntity

enum class PendingMutationSyncResult {
    COMPLETED,
    RETRY_NEEDED
}

class CachedIncidentRepository(
    private val remote: IncidentRepository,
    private val local: IncidentDao,
    private val syncScheduler: IncidentSyncScheduling = NoOpIncidentSyncScheduler
) : IncidentRepository,
    ObservableIncidentRepository,
    ObservableIncidentDetailRepository,
    ObservableIncidentTimelineRepository,
    ObservableIncidentSyncRepository {
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

    override fun observeIncidentSyncState(
        incidentId: Long
    ): Flow<IncidentSyncState> {
        return local.observePendingMutationsForIncident(
            incidentId = incidentId
        ).map { mutations ->
            val latestMutation =
                mutations.lastOrNull()

            IncidentSyncState(
                hasPendingMutations = mutations.isNotEmpty(),
                attemptCount =
                    latestMutation?.attemptCount ?: 0,
                lastAttemptAt =
                    latestMutation?.lastAttemptAt,
                lastError =
                    latestMutation?.lastError
            )
        }
    }

    override fun observeIncidentHasPendingMutations(
        incidentId: Long
    ): Flow<Boolean> {
        return local.observeHasPendingMutations(
            incidentId = incidentId
        )
    }

    override fun getIncidents(
        status: IncidentStatus?,
        severity: Severity?
    ): List<Incident> {
        try {
            syncPendingMutations()

            val remoteIncidents = remote.getIncidents(
                status = null,
                severity = null
            )

            val pendingStatusByIncident =
                local.getPendingMutations()
                    .asSequence()
                    .filter { it.type == "STATUS_CHANGE" }
                    .groupBy { it.incidentId }
                    .mapValues { (_, mutations) ->
                        mutations.maxByOrNull { it.id }
                            ?.payload
                    }

            val mergedIncidents =
                remoteIncidents.map { incident ->
                    val pendingStatus =
                        pendingStatusByIncident[incident.id]

                    if (pendingStatus == null) {
                        incident
                    } else {
                        incident.copy(
                            status = IncidentStatus.valueOf(
                                pendingStatus
                            )
                        )
                    }
                }

            local.replaceIncidents(
                mergedIncidents.map { it.toEntity() }
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
        return try {
            val incident = remote.updateIncidentStatus(
                incidentId = incidentId,
                status = status
            )

            local.upsertIncident(incident.toEntity())

            incident
        } catch (error: Exception) {
            if (error is CancellationException) {
                throw error
            }

            val cachedIncident =
                local.getIncident(incidentId)
                    ?.toDomain()
                    ?: throw error

            val updatedIncident =
                cachedIncident.copy(status = status)

            local.queueStatusMutation(
                incident = updatedIncident.toEntity(),
                mutation = PendingIncidentMutationEntity(
                    incidentId = incidentId,
                    type = "STATUS_CHANGE",
                    payload = status.name,
                    createdAt = System.currentTimeMillis()
                )
            )

            syncScheduler.schedule()

            updatedIncident
        }
    }

    fun syncPendingMutations(): PendingMutationSyncResult {
        val mutations = local.getPendingMutations()

        for (mutation in mutations) {
            if (mutation.type != "STATUS_CHANGE") {
                continue
            }

            try {
                val incident = remote.updateIncidentStatus(
                    incidentId = mutation.incidentId,
                    status = IncidentStatus.valueOf(
                        mutation.payload
                    )
                )

                local.upsertIncident(
                    incident.toEntity()
                )

                local.deletePendingMutation(
                    mutation.id
                )
            } catch (error: Exception) {
                if (error is CancellationException) {
                    throw error
                }

                local.recordPendingMutationFailure(
                    mutationId = mutation.id,
                    attemptedAt = System.currentTimeMillis(),
                    error = error.message
                        ?: error::class.java.simpleName
                )

                // Keep the mutation queued for a later retry.
                return PendingMutationSyncResult.RETRY_NEEDED
            }
        }

        return PendingMutationSyncResult.COMPLETED
    }
}
