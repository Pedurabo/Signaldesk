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

    private fun incident(
        id: Long,
        title: String = "Incident $id",
        status: String = "OPEN"
    ): IncidentEntity {
        return IncidentEntity(
            id = id,
            title = title,
            description = "Test description",
            severity = "HIGH",
            status = status
        )
    }
}
