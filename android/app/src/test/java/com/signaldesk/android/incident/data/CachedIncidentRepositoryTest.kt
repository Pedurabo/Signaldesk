package com.signaldesk.android.incident.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException

import com.signaldesk.android.incident.Incident
import com.signaldesk.android.incident.IncidentStatus
import com.signaldesk.android.incident.IncidentTimelineEvent
import com.signaldesk.android.incident.IncidentTimelineEventType
import com.signaldesk.android.incident.Severity
import com.signaldesk.android.incident.data.local.IncidentDao
import com.signaldesk.android.incident.data.local.IncidentEntity
import com.signaldesk.android.incident.data.local.IncidentTimelineEventEntity
import com.signaldesk.android.incident.data.local.toEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import com.signaldesk.android.incident.data.local.PendingIncidentMutationEntity
import com.signaldesk.android.incident.sync.IncidentSyncScheduling

class CachedIncidentRepositoryTest {

    @Test
    fun observedTimelineComesFromLocalCache() = runBlocking {
        val local = FakeIncidentDao()

        local.upsertTimelineEvents(
            listOf(
                IncidentTimelineEventEntity(
                    id = 801,
                    incidentId = 71,
                    type = "NOTE_ADDED",
                    message = "Cached timeline event",
                    createdAt = "2026-09-26T00:00:00Z"
                )
            )
        )

        val repository = CachedIncidentRepository(
            remote = FakeRemoteRepository(),
            local = local
        )

        val timeline =
            repository.observeIncidentTimeline(
                incidentId = 71
            ).first()

        assertEquals(
            listOf(801L),
            timeline.map { it.id }
        )

        assertEquals(
            IncidentTimelineEventType.NOTE_ADDED,
            timeline.single().type
        )

        assertEquals(
            "Cached timeline event",
            timeline.single().message
        )
    }

    @Test
    fun observedIncidentComesFromLocalCache() = runBlocking {
        val cachedIncident = incident(
            id = 71,
            status = IncidentStatus.INVESTIGATING
        )

        val local = FakeIncidentDao(
            initialIncidents = listOf(
                cachedIncident.toEntity()
            )
        )

        val repository = CachedIncidentRepository(
            remote = FakeRemoteRepository(),
            local = local
        )

        val observedIncident =
            repository.observeIncident(
                incidentId = cachedIncident.id
            ).first()

        assertEquals(
            cachedIncident,
            observedIncident
        )
    }

    @Test
    fun networkSuccessRefreshesCompleteLocalCache() {
        val remote = FakeRemoteRepository(
            incidents = listOf(
                incident(
                    id = 41,
                    status = IncidentStatus.OPEN
                ),
                incident(
                    id = 42,
                    status = IncidentStatus.INVESTIGATING
                )
            )
        )

        val local = FakeIncidentDao(
            initialIncidents = listOf(
                incident(id = 99).toEntity()
            )
        )

        val repository =
            CachedIncidentRepository(remote, local)

        val incidents = repository.getIncidents()

        assertEquals(
            listOf(42L, 41L),
            incidents.map { it.id }
        )

        assertEquals(
            listOf(42L, 41L),
            local.getIncidents().map { it.id }
        )
    }

    @Test
    fun networkFailureReturnsExistingLocalCache() {
        val remote = FakeRemoteRepository(
            loadError = IllegalStateException("Offline")
        )

        val local = FakeIncidentDao(
            initialIncidents = listOf(
                incident(
                    id = 41,
                    status = IncidentStatus.OPEN
                ).toEntity()
            )
        )

        val repository =
            CachedIncidentRepository(remote, local)

        val incidents = repository.getIncidents()

        assertEquals(
            listOf(41L),
            incidents.map { it.id }
        )
    }

    @Test
    fun filteredRequestRefreshesCompleteRemoteListThenFiltersLocally() {
        val remote = FakeRemoteRepository(
            incidents = listOf(
                incident(
                    id = 41,
                    status = IncidentStatus.OPEN,
                    severity = Severity.HIGH
                ),
                incident(
                    id = 42,
                    status = IncidentStatus.OPEN,
                    severity = Severity.CRITICAL
                ),
                incident(
                    id = 43,
                    status = IncidentStatus.RESOLVED,
                    severity = Severity.CRITICAL
                )
            )
        )

        val local = FakeIncidentDao()

        val repository =
            CachedIncidentRepository(remote, local)

        val incidents = repository.getIncidents(
            status = IncidentStatus.OPEN,
            severity = Severity.CRITICAL
        )

        assertEquals(null, remote.requestedStatus)
        assertEquals(null, remote.requestedSeverity)

        assertEquals(
            listOf(42L),
            incidents.map { it.id }
        )

        assertEquals("OPEN", local.requestedStatus)
        assertEquals(
            "CRITICAL",
            local.requestedSeverity
        )
    }

    @Test
    fun incidentLoadWritesRemoteIncidentToLocalCache() {
        val remoteIncident = incident(
            id = 41,
            status = IncidentStatus.INVESTIGATING
        )

        val remote = FakeRemoteRepository(
            incidents = listOf(remoteIncident)
        )

        val local = FakeIncidentDao()

        val repository =
            CachedIncidentRepository(remote, local)

        val loaded =
            repository.getIncident(41)

        assertEquals(
            IncidentStatus.INVESTIGATING,
            loaded.status
        )

        assertEquals(
            "INVESTIGATING",
            local.getIncident(41)?.status
        )
    }

    @Test
    fun incidentLoadFallsBackToLocalCacheWhenRemoteFails() {
        val remote = FakeRemoteRepository(
            incidentLoadError =
                IllegalStateException("Offline")
        )

        val local = FakeIncidentDao(
            initialIncidents = listOf(
                incident(
                    id = 41,
                    status = IncidentStatus.INVESTIGATING
                ).toEntity()
            )
        )

        val repository =
            CachedIncidentRepository(remote, local)

        val loaded =
            repository.getIncident(41)

        assertEquals(41L, loaded.id)

        assertEquals(
            IncidentStatus.INVESTIGATING,
            loaded.status
        )
    }

    @Test
    fun listCancellationIsNotConvertedToCacheFallback() {
        val remote = FakeRemoteRepository(
            loadError = CancellationException(
                "Superseded"
            )
        )

        val local = FakeIncidentDao(
            initialIncidents = listOf(
                incident(id = 41).toEntity()
            )
        )

        val repository =
            CachedIncidentRepository(remote, local)

        assertThrows(CancellationException::class.java) {
            repository.getIncidents()
        }
    }

    @Test
    fun detailCancellationIsNotConvertedToCacheFallback() {
        val remote = FakeRemoteRepository(
            incidentLoadError = CancellationException(
                "Superseded"
            )
        )

        val local = FakeIncidentDao(
            initialIncidents = listOf(
                incident(id = 41).toEntity()
            )
        )

        val repository =
            CachedIncidentRepository(remote, local)

        assertThrows(CancellationException::class.java) {
            repository.getIncident(41)
        }
    }

    @Test
    fun successfulEmptyRefreshClearsExistingLocalCache() {
        val remote = FakeRemoteRepository(
            incidents = emptyList()
        )

        val local = FakeIncidentDao(
            initialIncidents = listOf(
                incident(id = 41).toEntity(),
                incident(id = 42).toEntity()
            )
        )

        val repository =
            CachedIncidentRepository(remote, local)

        val incidents = repository.getIncidents()

        assertEquals(
            emptyList<Incident>(),
            incidents
        )

        assertEquals(
            emptyList<IncidentEntity>(),
            local.getIncidents()
        )
    }

    @Test
    fun createdIncidentIsWrittenToLocalCache() {
        val remote = FakeRemoteRepository()
        val local = FakeIncidentDao()

        val repository =
            CachedIncidentRepository(remote, local)

        val created = repository.createIncident(
            title = "Database latency",
            description = "Queries are slow",
            severity = Severity.CRITICAL
        )

        assertEquals(100L, created.id)

        assertEquals(
            "Database latency",
            local.getIncident(100)?.title
        )

        assertEquals(
            "CRITICAL",
            local.getIncident(100)?.severity
        )

        assertEquals(
            "OPEN",
            local.getIncident(100)?.status
        )
    }

    @Test
    fun incidentLoadRethrowsRemoteFailureWhenCacheIsMissing() {
        val remoteError =
            IllegalStateException("Offline")

        val remote = FakeRemoteRepository(
            incidentLoadError = remoteError
        )

        val local = FakeIncidentDao()

        val repository =
            CachedIncidentRepository(remote, local)

        val thrown = assertThrows(
            IllegalStateException::class.java
        ) {
            repository.getIncident(41)
        }

        assertEquals(
            remoteError,
            thrown
        )
    }

    @Test
    fun statusUpdateIsWrittenToLocalCache() {
        val updated = incident(
            id = 41,
            status = IncidentStatus.INVESTIGATING
        )

        val remote = FakeRemoteRepository(
            updatedIncident = updated
        )

        val local = FakeIncidentDao(
            initialIncidents = listOf(
                incident(
                    id = 41,
                    status = IncidentStatus.OPEN
                ).toEntity()
            )
        )

        val repository =
            CachedIncidentRepository(remote, local)

        repository.updateIncidentStatus(
            incidentId = 41,
            status = IncidentStatus.INVESTIGATING
        )

        assertEquals(
            "INVESTIGATING",
            local.getIncident(41)?.status
        )
    }

    @Test
    fun offlineStatusUpdateIsWrittenToLocalCache() {
        val local = FakeIncidentDao(
            initialIncidents = listOf(
                incident(
                    id = 41,
                    status = IncidentStatus.OPEN
                ).toEntity()
            )
        )

        val repository = CachedIncidentRepository(
            remote = FakeRemoteRepository(
                statusUpdateError =
                    IllegalStateException("offline")
            ),
            local = local
        )

        repository.updateIncidentStatus(
            incidentId = 41,
            status = IncidentStatus.INVESTIGATING
        )

        assertEquals(
            "INVESTIGATING",
            local.getIncident(41)?.status
        )
    }

    @Test
    fun offlineStatusUpdateQueuesPendingMutation() {
        val local = FakeIncidentDao(
            initialIncidents = listOf(
                incident(
                    id = 41,
                    status = IncidentStatus.OPEN
                ).toEntity()
            )
        )

        val repository = CachedIncidentRepository(
            remote = FakeRemoteRepository(
                statusUpdateError =
                    IllegalStateException("offline")
            ),
            local = local
        )

        repository.updateIncidentStatus(
            incidentId = 41,
            status = IncidentStatus.INVESTIGATING
        )

        val mutations =
            local.getPendingMutations()

        assertEquals(1, mutations.size)
        assertEquals(41, mutations.single().incidentId)
        assertEquals(
            "STATUS_CHANGE",
            mutations.single().type
        )
        assertEquals(
            "INVESTIGATING",
            mutations.single().payload
        )
    }

    @Test
    fun refreshPreservesPendingStatusMutation() {
        val local = FakeIncidentDao(
            initialIncidents = listOf(
                incident(
                    id = 41,
                    status = IncidentStatus.INVESTIGATING
                ).toEntity()
            )
        )

        local.insertPendingMutation(
            PendingIncidentMutationEntity(
                incidentId = 41,
                type = "STATUS_CHANGE",
                payload = "INVESTIGATING",
                createdAt = 1L
            )
        )

        val remote = FakeRemoteRepository(
            incidents = listOf(
                incident(
                    id = 41,
                    status = IncidentStatus.OPEN
                )
            )
        )

        val repository =
            CachedIncidentRepository(remote, local)

        repository.getIncidents()

        assertEquals(
            "INVESTIGATING",
            local.getIncident(41)?.status
        )
    }

    @Test
    fun refreshUsesLatestPendingStatusMutation() {
        val local = FakeIncidentDao(
            initialIncidents = listOf(
                incident(
                    id = 41,
                    status = IncidentStatus.RESOLVED
                ).toEntity()
            )
        )

        local.insertPendingMutation(
            PendingIncidentMutationEntity(
                incidentId = 41,
                type = "STATUS_CHANGE",
                payload = "INVESTIGATING",
                createdAt = 1L
            )
        )

        local.insertPendingMutation(
            PendingIncidentMutationEntity(
                incidentId = 41,
                type = "STATUS_CHANGE",
                payload = "RESOLVED",
                createdAt = 2L
            )
        )

        val repository = CachedIncidentRepository(
            remote = FakeRemoteRepository(
                incidents = listOf(
                    incident(
                        id = 41,
                        status = IncidentStatus.OPEN
                    )
                )
            ),
            local = local
        )

        repository.getIncidents()

        assertEquals(
            "RESOLVED",
            local.getIncident(41)?.status
        )
    }

    @Test
    fun successfulSyncRemovesPendingStatusMutation() {
        val local = FakeIncidentDao(
            initialIncidents = listOf(
                incident(
                    id = 41,
                    status = IncidentStatus.INVESTIGATING
                ).toEntity()
            )
        )

        local.insertPendingMutation(
            PendingIncidentMutationEntity(
                incidentId = 41,
                type = "STATUS_CHANGE",
                payload = "INVESTIGATING",
                createdAt = 1L
            )
        )

        val remote = FakeRemoteRepository(
            updatedIncident = incident(
                id = 41,
                status = IncidentStatus.INVESTIGATING
            )
        )

        val repository =
            CachedIncidentRepository(remote, local)

        val result =
            repository.syncPendingMutations()

        assertEquals(
            PendingMutationSyncResult.COMPLETED,
            result
        )

        assertEquals(
            0,
            local.getPendingMutations().size
        )
        assertEquals(
            "INVESTIGATING",
            local.getIncident(41)?.status
        )
    }

    @Test
    fun failedSyncKeepsPendingStatusMutation() {
        val local = FakeIncidentDao(
            initialIncidents = listOf(
                incident(
                    id = 41,
                    status = IncidentStatus.INVESTIGATING
                ).toEntity()
            )
        )

        local.insertPendingMutation(
            PendingIncidentMutationEntity(
                incidentId = 41,
                type = "STATUS_CHANGE",
                payload = "INVESTIGATING",
                createdAt = 1L
            )
        )

        val repository = CachedIncidentRepository(
            remote = FakeRemoteRepository(
                statusUpdateError =
                    IllegalStateException("offline")
            ),
            local = local
        )

        val result =
            repository.syncPendingMutations()

        assertEquals(
            PendingMutationSyncResult.RETRY_NEEDED,
            result
        )

        val mutations = local.getPendingMutations()

        assertEquals(1, mutations.size)
        assertEquals(41, mutations.single().incidentId)
        assertEquals(
            "STATUS_CHANGE",
            mutations.single().type
        )
        assertEquals(
            "INVESTIGATING",
            mutations.single().payload
        )
        assertEquals(
            "INVESTIGATING",
            local.getIncident(41)?.status
        )
    }

    @Test
    fun syncReplaysStatusMutationsInOrder() {
        val local = FakeIncidentDao(
            initialIncidents = listOf(
                incident(
                    id = 41,
                    status = IncidentStatus.RESOLVED
                ).toEntity()
            )
        )

        local.insertPendingMutation(
            PendingIncidentMutationEntity(
                incidentId = 41,
                type = "STATUS_CHANGE",
                payload = "INVESTIGATING",
                createdAt = 1L
            )
        )

        local.insertPendingMutation(
            PendingIncidentMutationEntity(
                incidentId = 41,
                type = "STATUS_CHANGE",
                payload = "RESOLVED",
                createdAt = 2L
            )
        )

        val remote = FakeRemoteRepository(
            updatedIncident = incident(
                id = 41,
                status = IncidentStatus.RESOLVED
            )
        )

        val repository =
            CachedIncidentRepository(remote, local)

        repository.syncPendingMutations()

        assertEquals(
            listOf(
                41L to IncidentStatus.INVESTIGATING,
                41L to IncidentStatus.RESOLVED
            ),
            remote.statusUpdateCalls
        )
        assertEquals(
            0,
            local.getPendingMutations().size
        )
        assertEquals(
            "RESOLVED",
            local.getIncident(41)?.status
        )
    }

    @Test
    fun syncStopsAfterFirstFailedMutation() {
        val local = FakeIncidentDao(
            initialIncidents = listOf(
                incident(
                    id = 41,
                    status = IncidentStatus.RESOLVED
                ).toEntity()
            )
        )

        local.insertPendingMutation(
            PendingIncidentMutationEntity(
                incidentId = 41,
                type = "STATUS_CHANGE",
                payload = "INVESTIGATING",
                createdAt = 1L
            )
        )

        local.insertPendingMutation(
            PendingIncidentMutationEntity(
                incidentId = 41,
                type = "STATUS_CHANGE",
                payload = "RESOLVED",
                createdAt = 2L
            )
        )

        val remote = FakeRemoteRepository(
            statusUpdateError =
                IllegalStateException("offline")
        )

        val repository =
            CachedIncidentRepository(remote, local)

        repository.syncPendingMutations()

        assertEquals(
            listOf(
                41L to IncidentStatus.INVESTIGATING
            ),
            remote.statusUpdateCalls
        )
        assertEquals(
            2,
            local.getPendingMutations().size
        )
        assertEquals(
            "RESOLVED",
            local.getIncident(41)?.status
        )
    }

    @Test
    fun refreshSyncsPendingMutationsBeforeRemoteFetch() {
        val local = FakeIncidentDao(
            initialIncidents = listOf(
                incident(
                    id = 41,
                    status = IncidentStatus.INVESTIGATING
                ).toEntity()
            )
        )

        local.insertPendingMutation(
            PendingIncidentMutationEntity(
                incidentId = 41,
                type = "STATUS_CHANGE",
                payload = "INVESTIGATING",
                createdAt = 1L
            )
        )

        val remote = FakeRemoteRepository(
            incidents = listOf(
                incident(
                    id = 41,
                    status = IncidentStatus.INVESTIGATING
                )
            ),
            updatedIncident = incident(
                id = 41,
                status = IncidentStatus.INVESTIGATING
            )
        )

        val repository =
            CachedIncidentRepository(remote, local)

        repository.getIncidents()

        assertEquals(
            listOf("PATCH_STATUS", "GET"),
            remote.remoteCalls
        )
        assertEquals(
            0,
            local.getPendingMutations().size
        )
    }

    @Test
    fun offlineRefreshPreservesOptimisticStatusAndMutation() {
        val local = FakeIncidentDao(
            initialIncidents = listOf(
                incident(
                    id = 41,
                    status = IncidentStatus.INVESTIGATING
                ).toEntity()
            )
        )

        local.insertPendingMutation(
            PendingIncidentMutationEntity(
                incidentId = 41,
                type = "STATUS_CHANGE",
                payload = "INVESTIGATING",
                createdAt = 1L
            )
        )

        val remote = FakeRemoteRepository(
            loadError =
                IllegalStateException("offline GET"),
            statusUpdateError =
                IllegalStateException("offline PATCH")
        )

        val repository =
            CachedIncidentRepository(remote, local)

        val incidents = repository.getIncidents()

        assertEquals(
            listOf("PATCH_STATUS", "GET"),
            remote.remoteCalls
        )
        assertEquals(
            1,
            local.getPendingMutations().size
        )
        assertEquals(
            "INVESTIGATING",
            local.getIncident(41)?.status
        )
        assertEquals(
            IncidentStatus.INVESTIGATING,
            incidents.single().status
        )
    }

    @Test
    fun offlineStatusUpdateSchedulesBackgroundSync() {
        val local = FakeIncidentDao(
            initialIncidents = listOf(
                incident(
                    id = 41,
                    status = IncidentStatus.OPEN
                ).toEntity()
            )
        )

        val remote = FakeRemoteRepository(
            statusUpdateError =
                IllegalStateException("offline")
        )

        val scheduler = FakeIncidentSyncScheduler()

        val repository = CachedIncidentRepository(
            remote = remote,
            local = local,
            syncScheduler = scheduler
        )

        repository.updateIncidentStatus(
            incidentId = 41,
            status = IncidentStatus.INVESTIGATING
        )

        assertEquals(1, scheduler.scheduleCalls)
    }

    private fun incident(
        id: Long,
        status: IncidentStatus = IncidentStatus.OPEN,
        severity: Severity = Severity.HIGH
    ): Incident {
        return Incident(
            id = id,
            title = "Incident $id",
            description = "Test description",
            severity = severity,
            status = status
        )
    }

    @Test
    fun timelineSuccessReplacesLocalTimeline() {
        val local = FakeIncidentDao()

        local.upsertTimelineEvents(
            listOf(
                IncidentTimelineEventEntity(
                    id = 1,
                    incidentId = 41,
                    type = "CREATED",
                    message = "Stale event",
                    createdAt = "old"
                )
            )
        )

        val remoteTimeline = listOf(
            IncidentTimelineEvent(
                id = 2,
                type = IncidentTimelineEventType.STATUS_CHANGED,
                message = "Fresh event",
                createdAt = "new"
            )
        )

        val repository = CachedIncidentRepository(
            remote = FakeRemoteRepository(
                timelineEvents = remoteTimeline
            ),
            local = local
        )

        val result =
            repository.getIncidentTimeline(41)

        assertEquals(remoteTimeline, result)

        assertEquals(
            listOf(2L),
            local.getTimeline(41).map { it.id }
        )
    }

    @Test
    fun timelineFailureReturnsExistingLocalTimeline() {
        val local = FakeIncidentDao()

        local.upsertTimelineEvents(
            listOf(
                IncidentTimelineEventEntity(
                    id = 7,
                    incidentId = 41,
                    type = "CREATED",
                    message = "Cached event",
                    createdAt = "cached"
                )
            )
        )

        val repository = CachedIncidentRepository(
            remote = FakeRemoteRepository(
                timelineLoadError =
                    IllegalStateException("offline")
            ),
            local = local
        )

        val result =
            repository.getIncidentTimeline(41)

        assertEquals(
            listOf(7L),
            result.map { it.id }
        )

        assertEquals(
            "Cached event",
            result.single().message
        )
    }

    @Test
    fun timelineFailureRethrowsRemoteErrorWhenCacheIsMissing() {
        val remoteError =
            IllegalStateException("offline")

        val repository = CachedIncidentRepository(
            remote = FakeRemoteRepository(
                timelineLoadError = remoteError
            ),
            local = FakeIncidentDao()
        )

        val thrown =
            assertThrows(
                IllegalStateException::class.java
            ) {
                repository.getIncidentTimeline(41)
            }

        assertEquals(
            remoteError,
            thrown
        )
    }
    @Test
    fun timelineCancellationIsNotConvertedToCacheFallback() {
        val local = FakeIncidentDao()

        local.upsertTimelineEvents(
            listOf(
                IncidentTimelineEventEntity(
                    id = 7,
                    incidentId = 41,
                    type = "CREATED",
                    message = "Cached event",
                    createdAt = "cached"
                )
            )
        )

        val repository = CachedIncidentRepository(
            remote = FakeRemoteRepository(
                timelineLoadError =
                    CancellationException("cancelled")
            ),
            local = local
        )

        assertThrows(
            CancellationException::class.java
        ) {
            repository.getIncidentTimeline(41)
        }
    }

    @Test
    fun addedNoteIsWrittenToLocalTimelineCache() {
        val local = FakeIncidentDao()

        val repository = CachedIncidentRepository(
            remote = FakeRemoteRepository(),
            local = local
        )

        val event =
            repository.addIncidentNote(
                incidentId = 41,
                message = "Investigating logs"
            )

        assertEquals(
            event.id,
            local.getTimeline(41).single().id
        )

        assertEquals(
            "Investigating logs",
            local.getTimeline(41).single().message
        )
    }


    @Test
    fun observedIncidentsComeFromLocalCache() = runBlocking {
        val local = FakeIncidentDao(
            initialIncidents = listOf(
                incident(
                    id = 41,
                    status = IncidentStatus.OPEN,
                    severity = Severity.HIGH
                ).toEntity(),
                incident(
                    id = 42,
                    status = IncidentStatus.RESOLVED,
                    severity = Severity.CRITICAL
                ).toEntity()
            )
        )

        val repository =
            CachedIncidentRepository(
                remote = FakeRemoteRepository(),
                local = local
            )

        val incidents =
            repository.observeIncidents(
                status = IncidentStatus.OPEN,
                severity = Severity.HIGH
            ).first()

        assertEquals(
            listOf(41L),
            incidents.map { it.id }
        )

        assertEquals(
            IncidentStatus.OPEN,
            incidents.single().status
        )

        assertEquals(
            Severity.HIGH,
            incidents.single().severity
        )
    }
}

private class FakeIncidentDao(
    initialIncidents: List<IncidentEntity> = emptyList()
) : IncidentDao {

    private val pendingMutations =
        mutableListOf<PendingIncidentMutationEntity>()


    private val incidents =
        initialIncidents.associateBy { it.id }.toMutableMap()

    private val incidentState =
        MutableStateFlow(incidents.values.toList())

    var requestedStatus: String? = null
    var requestedSeverity: String? = null

    override fun getIncidents(
        status: String?,
        severity: String?
    ): List<IncidentEntity> {
        requestedStatus = status
        requestedSeverity = severity

        return incidents.values
            .filter { incident ->
                (status == null || incident.status == status) &&
                    (severity == null || incident.severity == severity)
            }
            .sortedByDescending { it.id }
    }

    override fun observeIncidents(
        status: String?,
        severity: String?
    ): Flow<List<IncidentEntity>> {
        return incidentState.map { current ->
            current
                .filter { incident ->
                    (status == null || incident.status == status) &&
                        (severity == null || incident.severity == severity)
                }
                .sortedByDescending { it.id }
        }
    }
    override fun getIncident(
        incidentId: Long
    ): IncidentEntity? {
        return incidents[incidentId]
    }

    override fun observeIncident(
        incidentId: Long
    ): Flow<IncidentEntity?> {
        return incidentState.map { current ->
            current.firstOrNull { incident ->
                incident.id == incidentId
            }
        }
    }

    override fun upsertIncidents(
        incidents: List<IncidentEntity>
    ) {
        incidents.forEach { incident ->
            this.incidents[incident.id] = incident
        }

        incidentState.value =
            this.incidents.values.toList()
    }

    override fun upsertIncident(
        incident: IncidentEntity
    ) {
        incidents[incident.id] = incident
        incidentState.value =
            incidents.values.toList()
    }

    override fun deleteAllIncidents() {
        incidents.clear()
        incidentState.value = emptyList()
    }

    private val timelineEvents =
        mutableMapOf<Long, IncidentTimelineEventEntity>()

    override fun observeTimeline(
        incidentId: Long
    ): Flow<List<IncidentTimelineEventEntity>> {
        return incidentState.map {
            timelineEvents.values
                .filter { event ->
                    event.incidentId == incidentId
                }
                .sortedBy { event ->
                    event.id
                }
        }
    }

    override fun getTimeline(
        incidentId: Long
    ): List<IncidentTimelineEventEntity> {
        return timelineEvents.values
            .filter { it.incidentId == incidentId }
            .sortedBy { it.id }
    }

    override fun upsertTimelineEvents(
        events: List<IncidentTimelineEventEntity>
    ) {
        events.forEach { event ->
            timelineEvents[event.id] = event
        }
    }

    override fun deleteTimelineForIncident(
        incidentId: Long
    ) {
        timelineEvents.entries.removeAll {
            it.value.incidentId == incidentId
        }
    }

    override fun replaceIncidents(
        incidents: List<IncidentEntity>
    ) {
        deleteAllIncidents()
        upsertIncidents(incidents)
    }

    override fun queueStatusMutation(
        incident: IncidentEntity,
        mutation: PendingIncidentMutationEntity
    ) {
        upsertIncident(incident)
        insertPendingMutation(mutation)
    }

    override fun insertPendingMutation(
        mutation: PendingIncidentMutationEntity
    ): Long {
        val id =
            (pendingMutations.maxOfOrNull { it.id } ?: 0L) + 1L

        pendingMutations += mutation.copy(id = id)
        return id
    }

    override fun getPendingMutations():
        List<PendingIncidentMutationEntity> {
        return pendingMutations.toList()
    }

    override fun deletePendingMutation(
        mutationId: Long
    ) {
        pendingMutations.removeAll { it.id == mutationId }
    }
}

private class FakeIncidentSyncScheduler :
    IncidentSyncScheduling {

    var scheduleCalls = 0
        private set

    override fun schedule() {
        scheduleCalls++
    }
}

private class FakeRemoteRepository(
    private val incidents: List<Incident> = emptyList(),
    private val loadError: Exception? = null,
    private val incidentLoadError: Exception? = null,
    private val updatedIncident: Incident? = null,
    private val timelineEvents: List<IncidentTimelineEvent> = emptyList(),
    private val timelineLoadError: Exception? = null,
    private val statusUpdateError: Exception? = null
) : IncidentRepository {
    val remoteCalls = mutableListOf<String>()

    var requestedStatus: IncidentStatus? = null
    var requestedSeverity: Severity? = null

    override fun getIncidents(
        status: IncidentStatus?,
        severity: Severity?
    ): List<Incident> {
        remoteCalls += "GET"
        requestedStatus = status
        requestedSeverity = severity

        loadError?.let { throw it }

        return incidents
    }

    override fun getIncident(
        incidentId: Long
    ): Incident {
        incidentLoadError?.let {
            throw it
        }

        return incidents.first {
            it.id == incidentId
        }
    }

    override fun createIncident(
        title: String,
        description: String,
        severity: Severity
    ): Incident {
        return Incident(
            id = 100,
            title = title,
            description = description,
            severity = severity,
            status = IncidentStatus.OPEN
        )
    }

    override fun getIncidentTimeline(
        incidentId: Long
    ): List<IncidentTimelineEvent> {
        timelineLoadError?.let {
            throw it
        }

        return timelineEvents
    }

    override fun addIncidentNote(
        incidentId: Long,
        message: String
    ): IncidentTimelineEvent {
        return IncidentTimelineEvent(
            id = 1,
            type = IncidentTimelineEventType.NOTE_ADDED,
            message = message,
            createdAt = ""
        )
    }

    val statusUpdateCalls =
        mutableListOf<Pair<Long, IncidentStatus>>()

    override fun updateIncidentStatus(
        incidentId: Long,
        status: IncidentStatus
    ): Incident {
        remoteCalls += "PATCH_STATUS"
        statusUpdateCalls += incidentId to status
        statusUpdateError?.let { throw it }

        return updatedIncident
            ?: throw IllegalStateException(
                "No updated incident configured"
            )
    }
}
