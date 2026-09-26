package com.signaldesk.android

import android.app.Application
import androidx.room.Room
import com.signaldesk.android.incident.data.CachedIncidentRepository
import com.signaldesk.android.incident.data.NetworkIncidentRepository
import com.signaldesk.android.incident.data.local.SignalDeskDatabase
import com.signaldesk.android.incident.sync.IncidentSyncScheduler

class SignalDeskApplication : Application() {

    val database: SignalDeskDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            SignalDeskDatabase::class.java,
            "signaldesk.db"
        )
            .addMigrations(
                SignalDeskDatabase.MIGRATION_1_2,
                SignalDeskDatabase.MIGRATION_2_3,
                SignalDeskDatabase.MIGRATION_3_4
            )
            .build()
    }

    val incidentRepository: CachedIncidentRepository by lazy {
        CachedIncidentRepository(
            remote = NetworkIncidentRepository(),
            local = database.incidentDao(),
            syncScheduler = IncidentSyncScheduler(
                applicationContext
            )
        )
    }
}
