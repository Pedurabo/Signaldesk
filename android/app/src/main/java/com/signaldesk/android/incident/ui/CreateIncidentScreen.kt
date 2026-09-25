package com.signaldesk.android.incident.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.signaldesk.android.incident.Severity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateIncidentScreen(
    isCreating: Boolean,
    error: String?,
    onCreateIncident: (
        title: String,
        description: String,
        severity: Severity
    ) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember {
        mutableStateOf("")
    }

    var description by remember {
        mutableStateOf("")
    }

    var severity by remember {
        mutableStateOf(Severity.MEDIUM)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        enabled = !isCreating
                    ) {
                        Text("Back")
                    }
                },
                title = {
                    Text("Create incident")
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Incident details",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCreating,
                label = {
                    Text("Title")
                },
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCreating,
                label = {
                    Text("Description")
                },
                minLines = 3
            )

            Text(
                text = "Severity",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Severity.values()
                    .take(2)
                    .forEach { option ->
                        SeverityButton(
                            severity = option,
                            selected = severity == option,
                            enabled = !isCreating,
                            onClick = {
                                severity = option
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Severity.values()
                    .drop(2)
                    .forEach { option ->
                        SeverityButton(
                            severity = option,
                            selected = severity == option,
                            enabled = !isCreating,
                            onClick = {
                                severity = option
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
            }

            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = {
                    onCreateIncident(
                        title,
                        description,
                        severity
                    )
                },
                enabled =
                    title.isNotBlank() &&
                        description.isNotBlank() &&
                        !isCreating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isCreating) {
                        "Creating incident..."
                    } else {
                        "Create incident"
                    }
                )
            }
        }
    }
}

@Composable
private fun SeverityButton(
    severity: Severity,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
        ) {
            Text(severity.name)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
        ) {
            Text(severity.name)
        }
    }
}