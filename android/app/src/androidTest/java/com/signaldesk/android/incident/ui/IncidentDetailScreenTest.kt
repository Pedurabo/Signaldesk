package com.signaldesk.android.incident.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.signaldesk.android.incident.Incident
import com.signaldesk.android.incident.IncidentStatus
import com.signaldesk.android.incident.Severity
import org.junit.Rule
import org.junit.Test

class IncidentDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun openIncidentCanStartInvestigation() {
        val incident = Incident(
            id = 601,
            title = "Checkout failures",
            description = "Customers cannot complete checkout",
            severity = Severity.CRITICAL,
            status = IncidentStatus.OPEN
        )

        var requestedStatus: IncidentStatus? = null

        composeRule.setContent {
            MaterialTheme {
                IncidentDetailScreen(
                    incident = incident,
                    timeline = emptyList(),
                    isTimelineLoading = false,
                    timelineError = null,
                    isAddingNote = false,
                    noteError = null,
                    isUpdating = false,
                    error = null,
                    onAddNote = {},
                    onStatusChange = {
                        requestedStatus = it
                    },
                    onBack = {}
                )
            }
        }

        composeRule
            .onNodeWithText("Checkout failures")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("OPEN")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Start investigating")
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assert(requestedStatus == IncidentStatus.INVESTIGATING) {
                "Expected INVESTIGATING but was $requestedStatus"
            }
        }
    }
    @Test
    fun addingNoteReturnsEnteredText() {
        val incident = Incident(
            id = 602,
            title = "Search service errors",
            description = "Search requests are failing",
            severity = Severity.HIGH,
            status = IncidentStatus.INVESTIGATING
        )

        var addedNote: String? = null

        composeRule.setContent {
            MaterialTheme {
                IncidentDetailScreen(
                    incident = incident,
                    timeline = emptyList(),
                    isTimelineLoading = false,
                    timelineError = null,
                    isAddingNote = false,
                    noteError = null,
                    isUpdating = false,
                    error = null,
                    onAddNote = {
                        addedNote = it
                    },
                    onStatusChange = {},
                    onBack = {}
                )
            }
        }

        composeRule
            .onNodeWithText("Note")
            .performTextInput("Database connection pool exhausted")

        composeRule
            .onNodeWithText("Add note")
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assert(addedNote == "Database connection pool exhausted") {
                "Expected entered note but was $addedNote"
            }
        }
    }
}
