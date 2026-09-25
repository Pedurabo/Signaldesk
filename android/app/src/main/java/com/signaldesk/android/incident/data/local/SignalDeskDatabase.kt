package com.signaldesk.android.incident.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        IncidentEntity::class,
        IncidentTimelineEventEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class SignalDeskDatabase : RoomDatabase() {

    abstract fun incidentDao(): IncidentDao

    companion object {

        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {
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
                }
            }
    }
}
