package com.signaldesk.android.incident.data

import kotlinx.coroutines.CancellationException

import com.signaldesk.android.incident.Incident
import com.signaldesk.android.incident.IncidentStatus
import com.signaldesk.android.incident.IncidentTimelineEvent
import com.signaldesk.android.incident.IncidentTimelineEventType
import com.signaldesk.android.incident.Severity
import com.signaldesk.android.incident.data.local.IncidentDao
import com.signaldesk.android.incident.data.local.IncidentEntity
import com.signaldesk.android.incident.data.local.toEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CachedIncidentRepositoryTest {

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
}

private class FakeIncidentDao(
    initialIncidents: List<IncidentEntity> = emptyList()
) : IncidentDao {

    private val incidents =
        initialIncidents.associateBy { it.id }.toMutableMap()

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

    override fun getIncident(
        incidentId: Long
    ): IncidentEntity? {
        return incidents[incidentId]
    }

    override fun upsertIncidents(
        incidents: List<IncidentEntity>
    ) {
        incidents.forEach { incident ->
            this.incidents[incident.id] = incident
        }
    }

    override fun upsertIncident(
        incident: IncidentEntity
    ) {
        incidents[incident.id] = incident
    }

    override fun deleteAllIncidents() {
        incidents.clear()
    }

    override fun replaceIncidents(
        incidents: List<IncidentEntity>
    ) {
        deleteAllIncidents()
        upsertIncidents(incidents)
    }
}

private class FakeRemoteRepository(
    private val incidents: List<Incident> = emptyList(),
    private val loadError: Exception? = null,
    private val incidentLoadError: Exception? = null,
    private val updatedIncident: Incident? = null
) : IncidentRepository {

    var requestedStatus: IncidentStatus? = null
    var requestedSeverity: Severity? = null

    override fun getIncidents(
        status: IncidentStatus?,
        severity: Severity?
    ): List<Incident> {
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
        return emptyList()
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

    override fun updateIncidentStatus(
        incidentId: Long,
        status: IncidentStatus
    ): Incident {
        return updatedIncident
            ?: throw IllegalStateException(
                "No updated incident configured"
            )
    }
}
