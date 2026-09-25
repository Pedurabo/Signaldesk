package com.signaldesk.android.incident.data

import com.signaldesk.android.incident.Incident
import com.signaldesk.android.incident.IncidentStatus
import com.signaldesk.android.incident.Severity
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class NetworkIncidentRepository(
    private val baseUrl: String = "http://10.0.2.2:8082"
) : IncidentRepository {

    override fun getIncidents(): List<Incident> {
        val connection = URL("$baseUrl/api/incidents")
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

    private fun parseIncidents(json: String): List<Incident> {
        val array = JSONArray(json)

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)

                add(
                    Incident(
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
                )
            }
        }
    }
}