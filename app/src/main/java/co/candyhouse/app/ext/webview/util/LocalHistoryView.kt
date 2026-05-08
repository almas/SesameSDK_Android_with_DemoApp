package co.candyhouse.app.ext.webview.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.candyhouse.sesame.db.model.CHDeviceHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LocalHistoryView(
    historyList: List<CHDeviceHistory>,
    modifier: Modifier = Modifier
) {
    // Filter out unknown events (NONE type)
    val filteredHistoryList = historyList.filter { history ->
        val historyType = try {
            val bytes = history.historyData.chunked(2).map { it.toInt(16).toByte() }
            if (bytes.size >= 5) {
                SesameHistoryType.fromByte(bytes[4])
            } else {
                SesameHistoryType.NONE
            }
        } catch (e: Exception) {
            SesameHistoryType.NONE
        }
        historyType != SesameHistoryType.NONE
    }
    
    if (filteredHistoryList.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(text = "No local history available")
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredHistoryList) { item ->
                HistoryItem(item)
            }
        }
    }
}

@Composable
fun HistoryItem(history: CHDeviceHistory) {
    val historyType = try {
        val bytes = history.historyData.chunked(2).map { it.toInt(16).toByte() }
        if (bytes.size >= 5) {
            SesameHistoryType.fromByte(bytes[4])
        } else {
            SesameHistoryType.NONE
        }
    } catch (e: Exception) {
        SesameHistoryType.NONE
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val date = Date(history.timestamp)
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                Text(
                    text = format.format(date),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = if (historyType != SesameHistoryType.NONE) historyType.description else "Unknown Event",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Data: ${history.historyData}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

enum class SesameHistoryType(val value: Byte, val description: String) {
    NONE(0, "None"),
    BLE_LOCK(1, "Locked by Bluetooth"),
    BLE_UNLOCK(2, "Unlocked by Bluetooth"),
    TIME_CHANGED(3, "Time Changed"),
    AUTOLOCK_UPDATED(4, "Auto-lock Updated"),
    MECH_SETTING_UPDATED(5, "Mechanical Setting Updated"),
    AUTOLOCK(6, "Auto-locked"),
    MANUAL_LOCKED(7, "Locked Manually"),
    MANUAL_UNLOCKED(8, "Unlocked Manually"),
    MANUAL_ELSE(9, "Manual Event"),
    DRIVE_LOCKED(10, "Drive Locked"),
    DRIVE_UNLOCKED(11, "Drive Unlocked"),
    DRIVE_FAILED(12, "Drive Failed"),
    BLE_ADV_PARAM_UPDATED(13, "Bluetooth Adv Param Updated"),
    WM2_LOCK(14, "Locked by WiFi Module"),
    WM2_UNLOCK(15, "Unlocked by WiFi Module"),
    WEB_LOCK(16, "Locked by Web"),
    WEB_UNLOCK(17, "Unlocked by Web");

    companion object {
        fun fromByte(value: Byte): SesameHistoryType {
            return values().find { it.value == value } ?: NONE
        }
    }
}

// Helper function to clean local history - this can be called from settings
// This is a placeholder that demonstrates how the clean functionality would be implemented
fun cleanLocalHistory(deviceUUID: String, onComplete: () -> Unit) {
    // In a real implementation, this would call the database to delete history records
    // Example implementation:
    /*
    GlobalScope.launch(Dispatchers.IO) {
        try {
            CHDB.CHHistoryModel.deleteByDeviceUUID(deviceUUID) { result ->
                if (result.isSuccess) {
                    // History cleared successfully
                    onComplete()
                } else {
                    // Handle error
                    onComplete()
                }
            }
        } catch (e: Exception) {
            // Handle exception
            onComplete()
        }
    }
    */
    // For now, we're just demonstrating the concept
    onComplete()
}
