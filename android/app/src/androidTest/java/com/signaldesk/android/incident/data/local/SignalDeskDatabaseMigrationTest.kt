package com.signaldesk.android.incident.data.local

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignalDeskDatabaseMigrationTest {

    private lateinit var context: Context

    private val databaseName =
        "signaldesk-migration-test.db"

    @Before
    fun setUp() {
        context =
            ApplicationProvider.getApplicationContext()

        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migration2To3PreservesExistingDataAndCreatesOutbox() {
        createVersion2Database()

        val database =
            Room.databaseBuilder(
                context,
                SignalDeskDatabase::class.java,
                databaseName
            )
                .addMigrations(
                    SignalDeskDatabase.MIGRATION_2_3,
                    SignalDeskDatabase.MIGRATION_3_4
                )
                .allowMainThreadQueries()
                .build()

        try {
            val dao = database.incidentDao()

            val incident =
                requireNotNull(
                    dao.getIncident(41)
                )

            assertEquals(
                "Existing incident",
                incident.title
            )
            assertEquals(
                "INVESTIGATING",
                incident.status
            )

            val timeline =
                dao.getTimeline(41)

            assertEquals(1, timeline.size)
            assertEquals(
                "Existing timeline event",
                timeline.single().message
            )

            val mutationId =
                dao.insertPendingMutation(
                    PendingIncidentMutationEntity(
                        incidentId = 41,
                        type = "STATUS_CHANGE",
                        payload = "RESOLVED",
                        createdAt = 123456789L
                    )
                )

            assertTrue(mutationId > 0)

            val mutations =
                dao.getPendingMutations()

            assertEquals(1, mutations.size)
            assertEquals(
                41,
                mutations.single().incidentId
            )
            assertEquals(
                "STATUS_CHANGE",
                mutations.single().type
            )
            assertEquals(
                "RESOLVED",
                mutations.single().payload
            )
        } finally {
            database.close()
        }
    }


    @Test
    fun migration3To4PreservesPendingMutationAndAddsRetryMetadata() {
        createVersion3Database()

        val database =
            Room.databaseBuilder(
                context,
                SignalDeskDatabase::class.java,
                databaseName
            )
                .addMigrations(
                    SignalDeskDatabase.MIGRATION_3_4
                )
                .allowMainThreadQueries()
                .build()

        try {
            val mutation =
                database.incidentDao()
                    .getPendingMutations()
                    .single()

            assertEquals(41, mutation.incidentId)
            assertEquals(
                "STATUS_CHANGE",
                mutation.type
            )
            assertEquals(
                "RESOLVED",
                mutation.payload
            )
            assertEquals(
                0,
                mutation.attemptCount
            )
            assertEquals(
                null,
                mutation.lastAttemptAt
            )
            assertEquals(
                null,
                mutation.lastError
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun pendingMutationObserverEmitsUpdatedRetryMetadata() =
        runBlocking {
            val database =
                Room.databaseBuilder(
                    context,
                    SignalDeskDatabase::class.java,
                    databaseName
                )
                    .allowMainThreadQueries()
                    .build()

            try {
                val dao =
                    database.incidentDao()

                val mutationId =
                    dao.insertPendingMutation(
                        PendingIncidentMutationEntity(
                            incidentId = 41,
                            type = "STATUS_CHANGE",
                            payload = "RESOLVED",
                            createdAt = 123456789L
                        )
                    )

                val initial =
                    dao.observePendingMutationsForIncident(
                        incidentId = 41
                    )
                        .first()
                        .single()

                assertEquals(
                    0,
                    initial.attemptCount
                )
                assertEquals(
                    null,
                    initial.lastAttemptAt
                )
                assertEquals(
                    null,
                    initial.lastError
                )

                val retriedDeferred =
                    async(
                        start = CoroutineStart.UNDISPATCHED
                    ) {
                        dao.observePendingMutationsForIncident(
                            incidentId = 41
                        )
                            .first { mutations ->
                                mutations
                                    .singleOrNull()
                                    ?.attemptCount == 1
                            }
                            .single()
                    }

                dao.recordPendingMutationFailure(
                    mutationId = mutationId,
                    attemptedAt = 987654321L,
                    error = "offline"
                )

                val retried =
                    retriedDeferred.await()

                assertEquals(
                    1,
                    retried.attemptCount
                )
                assertEquals(
                    987654321L,
                    retried.lastAttemptAt
                )
                assertEquals(
                    "offline",
                    retried.lastError
                )
            } finally {
                database.close()
            }
        }

    private fun createVersion3Database() {
        val configuration =
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(databaseName)
                .callback(
                    object :
                        androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(3) {

                        override fun onCreate(
                            db: SupportSQLiteDatabase
                        ) {
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS incidents (
                                    id INTEGER NOT NULL,
                                    title TEXT NOT NULL,
                                    description TEXT NOT NULL,
                                    severity TEXT NOT NULL,
                                    status TEXT NOT NULL,
                                    PRIMARY KEY(id)
                                )
                                """.trimIndent()
                            )

                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS incident_timeline_events (
                                    id INTEGER NOT NULL,
                                    incidentId INTEGER NOT NULL,
                                    type TEXT NOT NULL,
                                    message TEXT NOT NULL,
                                    createdAt TEXT NOT NULL,
                                    PRIMARY KEY(id)
                                )
                                """.trimIndent()
                            )

                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS pending_incident_mutations (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    incidentId INTEGER NOT NULL,
                                    type TEXT NOT NULL,
                                    payload TEXT NOT NULL,
                                    createdAt INTEGER NOT NULL
                                )
                                """.trimIndent()
                            )

                            db.execSQL(
                                """
                                INSERT INTO pending_incident_mutations (
                                    incidentId,
                                    type,
                                    payload,
                                    createdAt
                                ) VALUES (
                                    41,
                                    'STATUS_CHANGE',
                                    'RESOLVED',
                                    123456789
                                )
                                """.trimIndent()
                            )
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int
                        ) = Unit
                    }
                )
                .build()

        val helper =
            FrameworkSQLiteOpenHelperFactory()
                .create(configuration)

        helper.writableDatabase.close()
        helper.close()
    }
    private fun createVersion2Database() {
        val configuration =
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(databaseName)
                .callback(
                    object :
                        androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {

                        override fun onCreate(
                            db: SupportSQLiteDatabase
                        ) {
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS incidents (
                                    id INTEGER NOT NULL,
                                    title TEXT NOT NULL,
                                    description TEXT NOT NULL,
                                    severity TEXT NOT NULL,
                                    status TEXT NOT NULL,
                                    PRIMARY KEY(id)
                                )
                                """.trimIndent()
                            )

                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS incident_timeline_events (
                                    id INTEGER NOT NULL,
                                    incidentId INTEGER NOT NULL,
                                    type TEXT NOT NULL,
                                    message TEXT NOT NULL,
                                    createdAt TEXT NOT NULL,
                                    PRIMARY KEY(id)
                                )
                                """.trimIndent()
                            )

                            db.execSQL(
                                """
                                INSERT INTO incidents (
                                    id,
                                    title,
                                    description,
                                    severity,
                                    status
                                ) VALUES (
                                    41,
                                    'Existing incident',
                                    'Must survive migration',
                                    'HIGH',
                                    'INVESTIGATING'
                                )
                                """.trimIndent()
                            )

                            db.execSQL(
                                """
                                INSERT INTO incident_timeline_events (
                                    id,
                                    incidentId,
                                    type,
                                    message,
                                    createdAt
                                ) VALUES (
                                    1001,
                                    41,
                                    'STATUS_CHANGED',
                                    'Existing timeline event',
                                    '2026-09-26T08:00:00'
                                )
                                """.trimIndent()
                            )
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int
                        ) = Unit
                    }
                )
                .build()

        val helper =
            FrameworkSQLiteOpenHelperFactory()
                .create(configuration)

        helper.writableDatabase.close()
        helper.close()
    }
}
