package co.candyhouse.app.ext.webview.util

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.candyhouse.sesame.db.model.CHDeviceHistory
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LocalHistoryView(
    historyList: List<CHDeviceHistory>,
    modifier: Modifier = Modifier
) {
    if (historyList.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(text = "No local history available")
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(historyList) { item ->
                HistoryItem(item)
            }
        }
    }
}

@Composable
fun HistoryItem(history: CHDeviceHistory) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val date = Date(history.timestamp)
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            Text(
                text = format.format(date),
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Event Data: ${history.historyData}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
