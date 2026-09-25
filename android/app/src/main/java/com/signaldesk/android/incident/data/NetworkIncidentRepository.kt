package com.signaldesk.android.incident.data

import com.signaldesk.android.incident.Incident
import com.signaldesk.android.incident.IncidentStatus
import com.signaldesk.android.incident.IncidentTimelineEvent
import com.signaldesk.android.incident.IncidentTimelineEventType
import com.signaldesk.android.incident.Severity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class NetworkIncidentRepository(
    private val baseUrl: String = "http://10.0.2.2:8082"
) : IncidentRepository {

    override fun getIncidents(
        status: IncidentStatus?,
        severity: Severity?
    ): List<Incident> {
        val queryParameters = mutableListOf<String>()

        if (status != null) {
            queryParameters += "status=${status.name}"
        }

        if (severity != null) {
            queryParameters += "severity=${severity.name}"
        }

        val queryString =
            if (queryParameters.isEmpty()) {
                ""
            } else {
                "?${queryParameters.joinToString("&")}"
            }

        val connection =
            URL("$baseUrl/api/incidents$queryString")
                .openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            if (connection.responseCode !in 200..299) {
                throw IllegalStateException(
                    "Failed to load incidents: HTTP ${connection.responseCode}"
                )
            }

            val response = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            parseIncidents(response)
        } finally {
            connection.disconnect()
        }
    }

    override fun getIncident(
        incidentId: Long
    ): Incident {
        val connection =
            URL("$baseUrl/api/incidents/$incidentId")
                .openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            if (connection.responseCode !in 200..299) {
                throw IllegalStateException(
                    "Failed to load incident: HTTP ${connection.responseCode}"
                )
            }

            val response = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            val json = JSONObject(response)

            Incident(
                id = json.getLong("id"),
                title = json.getString("title"),
                description = json.getString("description"),
                severity = Severity.valueOf(
                    json.getString("severity")
                ),
                status = IncidentStatus.valueOf(
                    json.getString("status")
                )
            )
        } finally {
            connection.disconnect()
        }
    }

    override fun getIncidentTimeline(
        incidentId: Long
    ): List<IncidentTimelineEvent> {
        val connection =
            URL("$baseUrl/api/incidents/$incidentId/timeline")
                .openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            if (connection.responseCode !in 200..299) {
                throw IllegalStateException(
                    "Failed to load incident timeline: HTTP ${connection.responseCode}"
                )
            }

            val response = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            parseTimeline(response)
        } finally {
            connection.disconnect()
        }
    }

    override fun createIncident(
        title: String,
        description: String,
        severity: Severity
    ): Incident {
        val connection =
            URL("$baseUrl/api/incidents")
                .openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.doOutput = true

            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            val requestBody = JSONObject()
                .put("title", title)
                .put("description", description)
                .put("severity", severity.name)
                .toString()

            connection.outputStream
                .bufferedWriter()
                .use { writer ->
                    writer.write(requestBody)
                }

            if (connection.responseCode !in 200..299) {
                throw IllegalStateException(
                    "Failed to create incident: HTTP ${connection.responseCode}"
                )
            }

            val response = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            parseIncident(
                JSONObject(response)
            )
        } finally {
            connection.disconnect()
        }
    }

    override fun addIncidentNote(
        incidentId: Long,
        message: String
    ): IncidentTimelineEvent {
        val connection =
            URL("$baseUrl/api/incidents/$incidentId/notes")
                .openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.doOutput = true

            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            val requestBody = JSONObject()
                .put("message", message)
                .toString()

            connection.outputStream
                .bufferedWriter()
                .use { writer ->
                    writer.write(requestBody)
                }

            if (connection.responseCode !in 200..299) {
                throw IllegalStateException(
                    "Failed to add incident note: HTTP ${connection.responseCode}"
                )
            }

            val response = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            parseTimelineEvent(
                JSONObject(response)
            )
        } finally {
            connection.disconnect()
        }
    }

    override fun updateIncidentStatus(
        incidentId: Long,
        status: IncidentStatus
    ): Incident {
        val connection =
            URL("$baseUrl/api/incidents/$incidentId/status")
                .openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "PATCH"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.doOutput = true

            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            val requestBody = JSONObject()
                .put("status", status.name)
                .toString()

            connection.outputStream
                .bufferedWriter()
                .use { writer ->
                    writer.write(requestBody)
                }

            if (connection.responseCode !in 200..299) {
                throw IllegalStateException(
                    "Failed to update incident status: HTTP ${connection.responseCode}"
                )
            }

            val response = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            parseIncident(JSONObject(response))
        } finally {
            connection.disconnect()
        }
    }

    private fun parseIncidents(
        json: String
    ): List<Incident> {
        val array = JSONArray(json)

        return buildList {
            for (index in 0 until array.length()) {
                add(
                    parseIncident(
                        array.getJSONObject(index)
                    )
                )
            }
        }
    }

    private fun parseIncident(
        item: JSONObject
    ): Incident {
        return Incident(
            id = item.getLong("id"),
            title = item.getString("title"),
            description = item.getString("description"),
            severity = Severity.valueOf(
                item.getString("severity")
            ),
            status = IncidentStatus.valueOf(
                item.getString("status")
            )
        )
    }

    private fun parseTimeline(
        json: String
    ): List<IncidentTimelineEvent> {
        val array = JSONArray(json)

        return buildList {
            for (index in 0 until array.length()) {
                add(
                    parseTimelineEvent(
                        array.getJSONObject(index)
                    )
                )
            }
        }
    }

    private fun parseTimelineEvent(
        item: JSONObject
    ): IncidentTimelineEvent {
        return IncidentTimelineEvent(
            id = item.getLong("id"),
            type = IncidentTimelineEventType.valueOf(
                item.getString("type")
            ),
            message = item.getString("message"),
            createdAt = item.getString("createdAt")
        )
    }
}