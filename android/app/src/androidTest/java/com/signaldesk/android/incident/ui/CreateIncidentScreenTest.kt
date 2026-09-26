package com.signaldesk.android.incident.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.signaldesk.android.incident.Severity
import org.junit.Rule
import org.junit.Test

class CreateIncidentScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun blankFormCannotCreateIncident() {
        var createRequests = 0

        composeRule.setContent {
            MaterialTheme {
                CreateIncidentScreen(
                    isCreating = false,
                    error = null,
                    onCreateIncident = { _, _, _ ->
                        createRequests += 1
                    },
                    onBack = {}
                )
            }
        }

        composeRule
            .onNode(
                hasText("Create incident") and
                    hasClickAction() and
                    isNotEnabled()
            )
            .assertExists()

        composeRule.runOnIdle {
            assert(createRequests == 0) {
                "Expected no create request but was $createRequests"
            }
        }
    }

    @Test
    fun validFormReturnsEnteredIncidentDetails() {
        var requestedTitle: String? = null
        var requestedDescription: String? = null
        var requestedSeverity: Severity? = null

        composeRule.setContent {
            MaterialTheme {
                CreateIncidentScreen(
                    isCreating = false,
                    error = null,
                    onCreateIncident = { title, description, severity ->
                        requestedTitle = title
                        requestedDescription = description
                        requestedSeverity = severity
                    },
                    onBack = {}
                )
            }
        }

        composeRule
            .onNodeWithText("Title")
            .performTextInput("Checkout API latency")

        composeRule
            .onNodeWithText("Description")
            .performTextInput("Checkout requests exceed latency threshold")

        composeRule
            .onNodeWithText("CRITICAL")
            .performClick()

        composeRule
            .onNode(
                hasText("Create incident") and
                    hasClickAction() and
                    isEnabled()
            )
            .performClick()

        composeRule.runOnIdle {
            assert(requestedTitle == "Checkout API latency") {
                "Unexpected title: $requestedTitle"
            }

            assert(
                requestedDescription ==
                    "Checkout requests exceed latency threshold"
            ) {
                "Unexpected description: $requestedDescription"
            }

            assert(requestedSeverity == Severity.CRITICAL) {
                "Expected CRITICAL but was $requestedSeverity"
            }
        }
    }
}


