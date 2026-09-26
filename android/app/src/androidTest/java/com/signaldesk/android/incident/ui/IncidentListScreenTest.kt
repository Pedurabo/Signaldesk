package com.signaldesk.android.incident.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.swipeDown
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
                    onRefresh = {},
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
                    onRefresh = {},
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
                    onRefresh = {},
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
                    onRefresh = {},
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

    @Test
    fun clickingRefreshInvokesRefreshCallback() {
        var refreshClicked = false

        composeRule.setContent {
            MaterialTheme {
                IncidentListScreen(
                    incidents = emptyList(),
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                    selectedStatus = null,
                    selectedSeverity = null,
                    onStatusFilterChange = {},
                    onSeverityFilterChange = {},
                    onIncidentClick = {},
                    onRefresh = {
                        refreshClicked = true
                    },
                    onCreateIncidentClick = {}
                )
            }
        }

        composeRule
            .onNodeWithText("Refresh")
            .performClick()

        composeRule.runOnIdle {
            assert(refreshClicked) {
                "Expected refresh callback to be invoked"
            }
        }
    }

    @Test
    fun refreshFailureKeepsCachedIncidentVisible() {
        val incident = Incident(
            id = 504,
            title = "Cached offline incident",
            description = "Cached incident remains available after refresh failure",
            severity = Severity.HIGH,
            status = IncidentStatus.INVESTIGATING
        )

        composeRule.setContent {
            MaterialTheme {
                IncidentListScreen(
                    incidents = listOf(incident),
                    isLoading = false,
                    isRefreshing = false,
                    error = "Backend unavailable",
                    selectedStatus = null,
                    selectedSeverity = null,
                    onStatusFilterChange = {},
                    onSeverityFilterChange = {},
                    onIncidentClick = {},
                    onRefresh = {},
                    onCreateIncidentClick = {}
                )
            }
        }

        composeRule
            .onNodeWithText("Cached offline incident")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Refresh failed: Backend unavailable")
            .assertIsDisplayed()
    }

    @Test
    fun pullingDownInvokesRefreshCallback() {
        var refreshCount = 0

        composeRule.setContent {
            MaterialTheme {
                IncidentListScreen(
                    incidents = listOf(
                        Incident(
                            id = 504,
                            title = "Pull refresh incident",
                            description = "Incident used for pull-to-refresh test",
                            severity = Severity.HIGH,
                            status = IncidentStatus.OPEN
                        )
                    ),
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                    selectedStatus = null,
                    selectedSeverity = null,
                    onStatusFilterChange = {},
                    onSeverityFilterChange = {},
                    onIncidentClick = {},
                    onRefresh = { refreshCount++ },
                    onCreateIncidentClick = {}
                )
            }
        }

        composeRule
            .onNodeWithTag("incident_list")
            .performTouchInput {
                swipeDown()
            }

        composeRule.runOnIdle {
            assert(refreshCount == 1) {
                "Expected one refresh callback but was $refreshCount"
            }
        }
    }


    @Test
    fun typingSearchInvokesSearchCallback() {
        var searchQuery = ""

        composeRule.setContent {
            MaterialTheme {
                IncidentListScreen(
                    incidents = emptyList(),
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                    selectedStatus = null,
                    selectedSeverity = null,
                    onStatusFilterChange = {},
                    onSeverityFilterChange = {},
                    onIncidentClick = {},
                    onRefresh = {},
                    onCreateIncidentClick = {},
                    searchQuery = searchQuery,
                    onSearchQueryChange = {
                        searchQuery = it
                    }
                )
            }
        }

        composeRule
            .onNodeWithTag("incident_search")
            .performTextInput("database")

        composeRule.runOnIdle {
            assert(searchQuery == "database") {
                "Expected database but was $searchQuery"
            }
        }
    }
}
