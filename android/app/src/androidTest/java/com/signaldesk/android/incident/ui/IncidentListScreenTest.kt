package com.signaldesk.android.incident.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.signaldesk.android.incident.Incident
import com.signaldesk.android.incident.IncidentStatus
import com.signaldesk.android.incident.Severity
import org.junit.Rule
import org.junit.Test

class IncidentListScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<com.signaldesk.android.ComposeTestActivity>()

    @Test
    fun incidentIsDisplayed() {
        val incident = Incident(
            id = 501,
            title = "Payment API latency",
            description = "Payment requests are responding slowly",
            severity = Severity.HIGH,
            status = IncidentStatus.INVESTIGATING
        )

        composeRule.setContent {
            MaterialTheme {
                IncidentListScreen(
                    incidents = listOf(incident),
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                    selectedStatus = null,
                    selectedSeverity = null,
                    onStatusFilterChange = {},
                    onSeverityFilterChange = {},
                    onIncidentClick = {},
                    onCreateIncidentClick = {}
                )
            }
        }

        composeRule
            .onNodeWithText("Payment API latency")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Payment requests are responding slowly")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("HIGH")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("INVESTIGATING")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Incident #501")
            .assertIsDisplayed()
    }
    @Test
    fun clickingIncidentReturnsSelectedIncident() {
        val incident = Incident(
            id = 502,
            title = "Database connection failures",
            description = "Application cannot reach PostgreSQL",
            severity = Severity.CRITICAL,
            status = IncidentStatus.OPEN
        )

        var clickedIncident: Incident? = null

        composeRule.setContent {
            MaterialTheme {
                IncidentListScreen(
                    incidents = listOf(incident),
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                    selectedStatus = null,
                    selectedSeverity = null,
                    onStatusFilterChange = {},
                    onSeverityFilterChange = {},
                    onIncidentClick = {
                        clickedIncident = it
                    },
                    onCreateIncidentClick = {}
                )
            }
        }

        composeRule
            .onNodeWithText("Database connection failures")
            .performClick()

        composeRule.runOnIdle {
            assert(clickedIncident == incident) {
                "Expected clicked incident $incident but was $clickedIncident"
            }
        }
    }
    @Test
    fun selectingStatusFilterReturnsSelectedStatus() {
        var selectedStatus: IncidentStatus? = null

        composeRule.setContent {
            MaterialTheme {
                IncidentListScreen(
                    incidents = emptyList(),
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                    selectedStatus = null,
                    selectedSeverity = null,
                    onStatusFilterChange = {
                        selectedStatus = it
                    },
                    onSeverityFilterChange = {},
                    onIncidentClick = {},
                    onCreateIncidentClick = {}
                )
            }
        }

        composeRule
            .onAllNodesWithText("ALL")[0]
            .performClick()

        composeRule
            .onNodeWithText("INVESTIGATING")
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assert(selectedStatus == IncidentStatus.INVESTIGATING) {
                "Expected INVESTIGATING but was $selectedStatus"
            }
        }
    }

    @Test
    fun refreshingKeepsCachedIncidentVisible() {
        val incident = Incident(
            id = 503,
            title = "Cached checkout incident",
            description = "Previously cached incident remains visible",
            severity = Severity.HIGH,
            status = IncidentStatus.INVESTIGATING
        )

        composeRule.setContent {
            MaterialTheme {
                IncidentListScreen(
                    incidents = listOf(incident),
                    isLoading = false,
                    isRefreshing = true,
                    error = null,
                    selectedStatus = null,
                    selectedSeverity = null,
                    onStatusFilterChange = {},
                    onSeverityFilterChange = {},
                    onIncidentClick = {},
                    onCreateIncidentClick = {}
                )
            }
        }

        composeRule
            .onNodeWithTag("incident_refresh_indicator")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Cached checkout incident")
            .assertIsDisplayed()
    }
}
