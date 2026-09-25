package com.signaldesk.android.incident.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncidentDaoTest {

    private lateinit var database: SignalDeskDatabase
    private lateinit var dao: IncidentDao

    @Before
    fun createDatabase() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(
            context,
            SignalDeskDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        dao = database.incidentDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertedIncidentsCanBeLoaded() {
        dao.upsertIncidents(
            listOf(
                incident(
                    id = 41,
                    title = "Email delivery slowdown"
                ),
                incident(
                    id = 42,
                    title = "Analytics export delayed"
                )
            )
        )

        val incidents = dao.getIncidents()

        assertEquals(2, incidents.size)
        assertEquals(42L, incidents[0].id)
        assertEquals(41L, incidents[1].id)
    }

    @Test
    fun incidentCanBeLoadedById() {
        dao.upsertIncident(
            incident(
                id = 41,
                title = "Email delivery slowdown"
            )
        )

        val loaded = dao.getIncident(41)

        assertEquals(
            "Email delivery slowdown",
            loaded?.title
        )

        assertNull(dao.getIncident(999))
    }

    @Test
    fun insertingSameIdReplacesExistingIncident() {
        dao.upsertIncident(
            incident(
                id = 41,
                title = "Original title",
                status = "OPEN"
            )
        )

        dao.upsertIncident(
            incident(
                id = 41,
                title = "Updated title",
                status = "INVESTIGATING"
            )
        )

        val loaded = dao.getIncident(41)

        assertEquals("Updated title", loaded?.title)
        assertEquals("INVESTIGATING", loaded?.status)
        assertEquals(1, dao.getIncidents().size)
    }

    @Test
    fun deletingAllIncidentsClearsDatabase() {
        dao.upsertIncidents(
            listOf(
                incident(id = 41),
                incident(id = 42)
            )
        )

        dao.deleteAllIncidents()

        assertEquals(
            emptyList<IncidentEntity>(),
            dao.getIncidents()
        )
    }

    @Test
    fun replacingIncidentsRemovesStaleRows() {
        dao.upsertIncidents(
            listOf(
                incident(id = 41),
                incident(id = 42)
            )
        )

        dao.replaceIncidents(
            listOf(
                incident(id = 43)
            )
        )

        assertEquals(
            listOf(43L),
            dao.getIncidents().map { it.id }
        )

        assertNull(dao.getIncident(41))
        assertNull(dao.getIncident(42))
    }

    @Test
    fun replacingIncidentsWithEmptyListClearsDatabase() {
        dao.upsertIncidents(
            listOf(
                incident(id = 41),
                incident(id = 42)
            )
        )

        dao.replaceIncidents(emptyList())

        assertEquals(
            emptyList<IncidentEntity>(),
            dao.getIncidents()
        )
    }

    @Test
    fun incidentsCanBeFilteredByStatus() {
        dao.upsertIncidents(
            listOf(
                incident(
                    id = 41,
                    status = "OPEN"
                ),
                incident(
                    id = 42,
                    status = "INVESTIGATING"
                ),
                incident(
                    id = 43,
                    status = "OPEN"
                )
            )
        )

        val incidents =
            dao.getIncidents(status = "OPEN")

        assertEquals(
            listOf(43L, 41L),
            incidents.map { it.id }
        )
    }

    @Test
    fun incidentsCanBeFilteredBySeverity() {
        dao.upsertIncidents(
            listOf(
                incident(
                    id = 41,
                    severity = "HIGH"
                ),
                incident(
                    id = 42,
                    severity = "CRITICAL"
                ),
                incident(
                    id = 43,
                    severity = "HIGH"
                )
            )
        )

        val incidents =
            dao.getIncidents(severity = "CRITICAL")

        assertEquals(
            listOf(42L),
            incidents.map { it.id }
        )
    }

    @Test
    fun incidentsCanBeFilteredByStatusAndSeverity() {
        dao.upsertIncidents(
            listOf(
                incident(
                    id = 41,
                    status = "OPEN",
                    severity = "HIGH"
                ),
                incident(
                    id = 42,
                    status = "OPEN",
                    severity = "CRITICAL"
                ),
                incident(
                    id = 43,
                    status = "RESOLVED",
                    severity = "CRITICAL"
                )
            )
        )

        val incidents =
            dao.getIncidents(
                status = "OPEN",
                severity = "CRITICAL"
            )

        assertEquals(
            listOf(42L),
            incidents.map { it.id }
        )
    }

    private fun incident(
        id: Long,
        title: String = "Incident $id",
        status: String = "OPEN",
        severity: String = "HIGH"
    ): IncidentEntity {
        return IncidentEntity(
            id = id,
            title = title,
            description = "Test description",
            severity = severity,
            status = status
        )
    }
}
