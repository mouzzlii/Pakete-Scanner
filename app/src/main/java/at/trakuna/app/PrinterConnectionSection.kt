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
    pairedPrinters: List<BluetoothDevice>,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onTestNetwork: () -> Unit,
    onLoadBluetooth: () -> Unit,
    onTestBluetooth: (BluetoothDevice) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text("DRUCKER VERBINDEN", fontWeight = FontWeight.Bold, color = PrinterForest)
            Text(
                "Erste Teststufe: Verbindung prüfen – keine Druckdaten werden gesendet.",
                fontSize = 12.sp,
                color = Color.DarkGray
            )
            Spacer(Modifier.height(8.dp))
            Text("WLAN / LAN (manuelle IP)", fontWeight = FontWeight.SemiBold)
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
                label = { Text("Port (IPP häufig 631)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedButton(onClick = onTestNetwork, modifier = Modifier.fillMaxWidth()) {
                Text("NETZWERK-VERBINDUNG TESTEN")
            }
            HorizontalDivider()
            Text("Bluetooth Classic (SPP)", fontWeight = FontWeight.SemiBold)
            Text(
                "Drucker zuerst in Android koppeln. BLE und proprietäre Profile benötigen später eigene Adapter.",
                fontSize = 11.sp,
                color = Color.DarkGray
            )
            OutlinedButton(onClick = onLoadBluetooth, modifier = Modifier.fillMaxWidth()) {
                Text("GEKOPPELTE BLUETOOTH-GERÄTE LADEN")
            }
            pairedPrinters.forEach { device ->
                OutlinedButton(
                    onClick = { onTestBluetooth(device) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Text("SPP testen: ${device.name ?: "Bluetooth-Gerät"}")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Druckerstatus", fontWeight = FontWeight.SemiBold, color = PrinterForest)
            Text(status, fontSize = 12.sp)
        }
    }
}
