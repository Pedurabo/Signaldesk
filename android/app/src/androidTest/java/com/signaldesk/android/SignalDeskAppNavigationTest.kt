package com.signaldesk.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.signaldesk.android.incident.data.FakeIncidentRepository
import com.signaldesk.android.incident.ui.IncidentDetailViewModel
import com.signaldesk.android.incident.ui.IncidentListViewModel
import com.signaldesk.android.ui.theme.SignalDeskTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test

class SignalDeskAppNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun clickingIncidentNavigatesToItsDetailScreen() {
        val repository = FakeIncidentRepository()

        val listViewModel = IncidentListViewModel(
            repository = repository,
            ioDispatcher = Dispatchers.Main
        )

        val detailViewModel = IncidentDetailViewModel(
            repository = repository,
            ioDispatcher = Dispatchers.Main
        )

        composeRule.setContent {
            SignalDeskTheme {
                SignalDeskApp(
                    listViewModel = listViewModel,
                    detailViewModel = detailViewModel
                )
            }
        }

        composeRule
            .onNodeWithText("Email delivery slowdown")
            .assertIsDisplayed()
            .performClick()

        composeRule
            .onNodeWithText("Incident #41")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Email delivery slowdown")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("INVESTIGATING")
            .assertIsDisplayed()
    }
}
