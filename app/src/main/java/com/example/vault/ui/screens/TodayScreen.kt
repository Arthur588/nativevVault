package com.example.vault.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.vault.ui.today.TodayViewModel

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onViewItem: (String) -> Unit,
    onBack: () -> Unit
) {
    val media by viewModel.media.collectAsState()
    val viewedCount by viewModel.viewedCount.collectAsState()
    // Refresh today's list when screen appears
    LaunchedEffect(Unit) { viewModel.refresh() }
    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = onBack) { Text("Back") }
        LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
            itemsIndexed(media) { index, item ->
                val viewed = index < viewedCount
                val color = if (viewed) Color.Gray else Color.Black
                val decoration = if (viewed) TextDecoration.LineThrough else TextDecoration.None
                Text(
                    text = item.originalName,
                    color = color,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !viewed) { onViewItem(item.id) }
                        .padding(vertical = 8.dp),
                    textDecoration = decoration
                )
            }
        }
    }
}