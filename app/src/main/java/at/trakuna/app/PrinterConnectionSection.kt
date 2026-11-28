package at.trakuna.app

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PrinterForest = Color(0xFF174D39)

/**
 * Printer connection UI only. Connection logic and permission handling stay
 * in MainActivity; this composable never sends print data.
 */
@Composable
internal fun PrinterConnectionSection(
    host: String,
    port: String,
    status: String,
    pairedPrinters: List<Pair<BluetoothDevice, String>>,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onTestNetwork: () -> Unit,
    onLoadBluetooth: () -> Unit,
    onTestBluetooth: (BluetoothDevice) -> Unit,
    isConnected: Boolean,
    onDisconnect: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text("DRUCKER VERBINDEN", fontWeight = FontWeight.Bold, color = PrinterForest)
            Text(
                "Modellunabhängige Transportverbindung: WLAN/LAN über RAW-TCP und Bluetooth Classic über SPP. Die Verbindung bleibt bis zum manuellen Trennen geöffnet. Noch kein Druckauftrag.",
                fontSize = 12.sp,
                color = Color.DarkGray
            )
            Spacer(Modifier.height(8.dp))
            Text("WLAN / LAN – RAW TCP", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = host,
                onValueChange = onHostChange,
                label = { Text("Drucker-IP oder Hostname") },
                placeholder = { Text("z. B. 192.168.1.50") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = port,
                onValueChange = onPortChange,
                label = { Text("RAW-TCP-Port (typisch 9100)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedButton(onClick = onTestNetwork, modifier = Modifier.fillMaxWidth()) {
                Text("MIT NETZWERK-DRUCKER VERBINDEN")
            }
            HorizontalDivider()
            Text("Bluetooth Classic – SPP", fontWeight = FontWeight.SemiBold)
            Text(
                "Drucker zuerst in Android koppeln. SPP funktioniert bei Geräten mit serieller Bluetooth-Schnittstelle; BLE/GATT und herstellerspezifische Profile sind nicht automatisch kompatibel.",
                fontSize = 11.sp,
                color = Color.DarkGray
            )
            OutlinedButton(onClick = onLoadBluetooth, modifier = Modifier.fillMaxWidth()) {
                Text("GEKOPPELTE BLUETOOTH-GERÄTE LADEN")
            }
            pairedPrinters.forEach { (device, displayName) ->
                OutlinedButton(
                    onClick = { onTestBluetooth(device) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Text("$displayName – per SPP verbinden")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Druckerstatus", fontWeight = FontWeight.SemiBold, color = PrinterForest)
            Text(if (isConnected) "● Verbunden" else "○ Nicht verbunden", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (isConnected) PrinterForest else Color.DarkGray)
            Text(status, fontSize = 12.sp)
            if (isConnected) {
                OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) {
                    Text("DRUCKER TRENNEN")
                }
            }
        }
    }
}
