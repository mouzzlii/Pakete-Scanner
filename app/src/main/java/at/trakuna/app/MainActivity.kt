package at.trakuna.app

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.util.Locale
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

private val Forest = Color(0xFF174D39)
private val Pale = Color(0xFFF1F5F2)

private data class CarrierPortal(val name: String, val url: String)

private val portals = listOf(
    CarrierPortal("DPD Business", "https://business.dpd.at/"),
    CarrierPortal("GLS / YourGLS", "https://gls-group.com/authenticate?allowed_locales=en-AT%2Cde-AT&locale=de-AT"),
    CarrierPortal("Hermes Geschäftskundenbereich", "https://www.myhermes.at/geschaeftskunden/uebersicht/"),
    CarrierPortal("UPS Business / CampusShip", "https://www.ups.com/at/de/business-solutions/business-shipping-tools/ups-campusship"),
    CarrierPortal("DHL Geschäftskunden", "https://www.dhl.com/at-de/home/login.html"),
    CarrierPortal("Österreichische Post / ELLA", "https://ella.post.at/")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TrakunaApp() }
    }

    @Composable
    private fun TrakunaApp() {
        var selected by remember { mutableStateOf("") }
        var scannedCode by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("Bereit. Paket scannen oder Anbieter auswählen.") }
        var detected by remember { mutableStateOf(false) }
        var printerHost by remember { mutableStateOf("") }
        var printerPort by remember { mutableStateOf("631") }
        var printerStatus by remember { mutableStateOf("Noch kein Verbindungstest durchgeführt.") }
        var pairedPrinters by remember { mutableStateOf(listOf<BluetoothDevice>()) }

        val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                try {
                    val adapter = (getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter
                    pairedPrinters = adapter?.bondedDevices?.toList()?.sortedBy { it.name ?: it.address } ?: emptyList()
                    printerStatus = if (pairedPrinters.isEmpty()) "Keine gekoppelten Bluetooth-Geräte gefunden. Kopple den Drucker zuerst in den Android-Einstellungen."
                        else "Gekoppelte Geräte geladen. Wähle einen Drucker für den SPP-Verbindungstest."
                } catch (_: SecurityException) {
                    printerStatus = "Bluetooth-Berechtigung fehlt."
                }
            } else printerStatus = "Bluetooth-Zugriff wurde nicht freigegeben."
        }

        fun loadPairedPrinters() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                return
            }
            try {
                val adapter = (getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter
                pairedPrinters = adapter?.bondedDevices?.toList()?.sortedBy { it.name ?: it.address } ?: emptyList()
                printerStatus = if (pairedPrinters.isEmpty()) "Keine gekoppelten Bluetooth-Geräte gefunden. Kopple den Drucker zuerst in den Android-Einstellungen."
                    else "Gekoppelte Geräte geladen. Wähle einen Drucker für den SPP-Verbindungstest."
            } catch (_: SecurityException) {
                printerStatus = "Bluetooth-Berechtigung fehlt."
            }
        }

        fun testWifiEndpoint() {
            val host = printerHost.trim()
            val port = printerPort.toIntOrNull()
            if (host.isBlank() || port == null || port !in 1..65535) {
                printerStatus = "Bitte IP-Adresse/Host und einen gültigen Port (1–65535) eingeben."
                return
            }
            printerStatus = "Prüfe Netzwerk-Endpunkt …"
            Thread {
                val result = try {
                    Socket().use { socket -> socket.connect(InetSocketAddress(host, port), 3000) }
                    "TCP-Endpunkt $host:$port ist erreichbar. IPP-/Druckerfähigkeit wurde damit noch nicht bestätigt."
                } catch (e: Exception) {
                    "Netzwerk-Endpunkt nicht erreichbar: ${e.message ?: "Verbindung fehlgeschlagen"}"
                }
                runOnUiThread { printerStatus = result }
            }.start()
        }

        fun testBluetoothSpp(device: BluetoothDevice) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                return
            }
            printerStatus = "Prüfe Bluetooth-SPP-Verbindung zu ${device.name ?: device.address} …"
            Thread {
                var socket: BluetoothSocket? = null
                val result = try {
                    socket = device.createRfcommSocketToServiceRecord(
                        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                    )
                    socket.connect()
                    "Bluetooth-SPP-Verbindung zu ${device.name ?: device.address} erfolgreich. Es wurden keine Druckdaten gesendet."
                } catch (e: Exception) {
                    "SPP-Verbindung fehlgeschlagen: ${e.message ?: "Profil nicht unterstützt oder Gerät nicht erreichbar"}"
                } finally {
                    try { socket?.close() } catch (_: Exception) {}
                }
                runOnUiThread { printerStatus = result }
            }.start()
        }

        

        fun forwardCode(raw: String) {
            scannedCode = raw.trim()
            val match = CarrierDetector.detect(scannedCode)
            if (match != null) {
                selected = match
                detected = true
                status = "Anbieter erkannt: $match. Öffne den offiziellen Geschäftskunden-Einstieg im Browser."
                try {
                    val portal = portals.first { it.name == match }
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(portal.url)))
                } catch (_: ActivityNotFoundException) {
                    status = "Anbieter erkannt, aber kein Browser zum Öffnen gefunden."
                }
            } else {
                selected = ""
                detected = false
                status = if (Regex("""^872\d{10}$""").matches(scannedCode.uppercase(Locale.ROOT).replace(Regex("""[\s-]"""), ""))) {
                    "Mehrdeutiger 13-stelliger Code (872…): kann ein GLS- oder EAN-13-Code sein. Bitte GLS manuell auswählen."
                } else {
                    "Anbieter nicht eindeutig erkannt. Bitte Anbieter auswählen."
                }
            }
        }

        fun scan() {
            GmsBarcodeScanning.getClient(this@MainActivity)
                .startScan()
                .addOnSuccessListener { result -> forwardCode(result.rawValue.orEmpty()) }
                .addOnFailureListener {
                    status = "Scan nicht möglich. Code kann auch über einen externen Scanner eingegeben werden."
                }
        }

        fun openPortal() {
            val portal = portals.firstOrNull { it.name == selected }
            if (portal == null) {
                status = "Bitte zuerst einen Paketdienst auswählen."
                return
            }
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(portal.url)))
                status = "$selected wird im externen Android-Browser geöffnet. Anmeldung und Vorgänge erfolgen ausschließlich beim Anbieter."
            } catch (_: ActivityNotFoundException) {
                status = "Kein Browser zum Öffnen des Anbieterportals gefunden."
            }
        }

        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = Forest,
                secondary = Color(0xFF71947D),
                background = Pale,
                surface = Color.White
            )
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = Pale) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(24.dp))
                    Text("TRAKUNA", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Forest)
                    Text("PAKETDIENSTE. EIN BROWSER.", fontSize = 11.sp, color = Forest)
                    Spacer(Modifier.height(28.dp))
                    Text("Paketdienst weiterleiten", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Barcode scannen. TRAKUNA wählt den passenden Anbieter-Einstieg und öffnet den offiziellen Geschäftskunden-Einstieg im Browser.",
                        color = Color.DarkGray
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { scan() },
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("▦  PAKET SCANNEN", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = scannedCode,
                        onValueChange = { scannedCode = it },
                        label = { Text("Barcode / Zebra-Scanner") },
                        placeholder = { Text("Code scannen oder eingeben") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedButton(
                        onClick = { forwardCode(scannedCode) },
                        enabled = scannedCode.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("ANBIETER ERKENNEN & PORTAL ÖFFNEN") }
                    if (scannedCode.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text("Erfasster Code: $scannedCode", fontSize = 12.sp, color = Color.DarkGray)
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("ANBIETER", fontWeight = FontWeight.Bold, color = Forest)
                    Spacer(Modifier.height(8.dp))
                    portals.forEach { portal ->
                        val active = selected == portal.name
                        OutlinedButton(
                            onClick = {
                                selected = portal.name
                                detected = false
                                status = "${portal.name} ausgewählt."
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (active) Color(0xFFE1EEE6) else Color.White,
                                contentColor = Forest
                            )
                        ) {
                            Text(
                                if (active) "✓  ${portal.name}" else portal.name,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { openPortal() },
                        enabled = selected.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("GESCHÄFTSKUNDEN-EINSTIEG ÖFFNEN", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(12.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Status", fontWeight = FontWeight.Bold, color = Forest)
                            Text(status, fontSize = 13.sp)
                            if (detected) {
                                Text("Die Erkennung nutzt bekannte Barcode-Kandidaten. Bei Unsicherheit bitte Anbieter manuell korrigieren.", fontSize = 11.sp, color = Color.DarkGray)
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text("DRUCKER VERBINDEN", fontWeight = FontWeight.Bold, color = Forest)
                            Text("Erste Teststufe: Verbindung prüfen – keine Druckdaten werden gesendet.", fontSize = 12.sp, color = Color.DarkGray)
                            Spacer(Modifier.height(8.dp))
                            Text("WLAN / LAN (manuelle IP)", fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = printerHost,
                                onValueChange = { printerHost = it },
                                label = { Text("Drucker-IP oder Hostname") },
                                placeholder = { Text("z. B. 192.168.1.50") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = printerPort,
                                onValueChange = { printerPort = it.filter(Char::isDigit).take(5) },
                                label = { Text("Port (IPP häufig 631)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedButton(onClick = { testWifiEndpoint() }, modifier = Modifier.fillMaxWidth()) {
                                Text("NETZWERK-VERBINDUNG TESTEN")
                            }
                            HorizontalDivider()
                            Text("Bluetooth Classic (SPP)", fontWeight = FontWeight.SemiBold)
                            Text("Drucker zuerst in Android koppeln. BLE und proprietäre Profile benötigen später eigene Adapter.", fontSize = 11.sp, color = Color.DarkGray)
                            OutlinedButton(onClick = { loadPairedPrinters() }, modifier = Modifier.fillMaxWidth()) {
                                Text("GEKOPPELTE BLUETOOTH-GERÄTE LADEN")
                            }
                            pairedPrinters.forEach { device ->
                                OutlinedButton(
                                    onClick = { testBluetoothSpp(device) },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                ) {
                                    Text("SPP testen: ${device.name ?: "Bluetooth-Gerät"}")
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Druckerstatus", fontWeight = FontWeight.SemiBold, color = Forest)
                            Text(printerStatus, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Browser-Modus: Anbieterportal im externen Browser. TRAKUNA speichert keine Passwörter und führt keine Buchung, Labelbestellung oder Druckaktion aus.",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "TRAKUNA benötigt die Internetberechtigung ausschließlich für den manuellen WLAN/LAN-Verbindungstest zum selbst eingegebenen Drucker-Endpunkt. Paketcodes werden nicht an einen TRAKUNA-Server übertragen; Anmeldung und Vorgänge erfolgen im externen Browser direkt beim Anbieter.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}