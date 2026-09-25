package com.signaldesk.android.incident.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface IncidentDao {

    @Query(
        """
        SELECT * FROM incidents
        WHERE (:status IS NULL OR status = :status)
          AND (:severity IS NULL OR severity = :severity)
        ORDER BY id DESC
        """
    )
    fun getIncidents(
        status: String? = null,
        severity: String? = null
    ): List<IncidentEntity>

    @Query("SELECT * FROM incidents WHERE id = :incidentId")
    fun getIncident(incidentId: Long): IncidentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertIncidents(incidents: List<IncidentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertIncident(incident: IncidentEntity)

    @Query("DELETE FROM incidents")
    fun deleteAllIncidents()

    @Transaction
    fun replaceIncidents(
        incidents: List<IncidentEntity>
    ) {
        deleteAllIncidents()
        upsertIncidents(incidents)
    }
}
