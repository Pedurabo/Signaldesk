package com.signaldesk.android.incident.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface IncidentDao {

    @Query("SELECT * FROM incidents ORDER BY id DESC")
    fun getIncidents(): List<IncidentEntity>

    @Query("SELECT * FROM incidents WHERE id = :incidentId")
    fun getIncident(incidentId: Long): IncidentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertIncidents(incidents: List<IncidentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertIncident(incident: IncidentEntity)

    @Query("DELETE FROM incidents")
    fun deleteAllIncidents()
}
