package com.signaldesk.android.incident.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.signaldesk.android.incident.Incident
import com.signaldesk.android.incident.IncidentStatus
import com.signaldesk.android.incident.Severity

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey
    val id: Long,
    val title: String,
    val description: String,
    val severity: String,
    val status: String
)

fun Incident.toEntity(): IncidentEntity {
    return IncidentEntity(
        id = id,
        title = title,
        description = description,
        severity = severity.name,
        status = status.name
    )
}

fun IncidentEntity.toDomain(): Incident {
    return Incident(
        id = id,
        title = title,
        description = description,
        severity = Severity.valueOf(severity),
        status = IncidentStatus.valueOf(status)
    )
}
