package com.example.vault.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vault.ui.importing.ImportViewModel

@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    onDone: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                viewModel.importUris(uris)
            }
        }
    )

    LaunchedEffect(state) {
        if (state is ImportViewModel.ImportState.Success) {
            onDone()
            viewModel.reset()
        }
    }

    Column(modifier = Modifier.padding(24.dp)) {
        when (state) {
            is ImportViewModel.ImportState.Importing -> {
                CircularProgressIndicator()
                Text(text = "Importing...")
            }
            is ImportViewModel.ImportState.Error -> {
                val msg = (state as ImportViewModel.ImportState.Error).message
                Text(text = "Error: $msg")
            }
            else -> {
                Button(
                    onClick = {
                        launcher.launch(arrayOf("image/*", "video/*"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Select files to import")
                }
            }
        }
    }
}