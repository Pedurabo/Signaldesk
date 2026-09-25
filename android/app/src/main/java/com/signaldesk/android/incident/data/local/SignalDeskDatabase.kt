package com.signaldesk.android.incident.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        IncidentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SignalDeskDatabase : RoomDatabase() {

    abstract fun incidentDao(): IncidentDao
}
