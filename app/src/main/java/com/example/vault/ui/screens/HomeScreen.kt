package com.example.vault.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vault.ui.home.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onImport: () -> Unit,
    onViewToday: () -> Unit,
    onSettings: () -> Unit
) {
    val total by viewModel.totalCount.collectAsState()
    val remaining by viewModel.remainingToday.collectAsState()
    // Refresh counts when this screen appears
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }
    Column(modifier = Modifier.padding(24.dp)) {
        Text(text = "Total files: $total")
        Text(text = "Remaining today: $remaining")
        Button(
            onClick = onImport,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) { Text("Import Files") }
        Button(
            onClick = onViewToday,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) { Text("View Today") }
        Button(
            onClick = onSettings,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) { Text("Settings") }
    }
}