package at.trakuna.app

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ActivityNotFoundException
import android.content.Context
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.util.Locale
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import java.io.Closeable

private val Forest = Color(0xFF174D39)
private val Pale = Color(0xFFF1F5F2)

private data class BluetoothConnectResult(val socket: BluetoothSocket?, val message: String)

private suspend fun connectBluetoothSpp(context: Context, device: BluetoothDevice): BluetoothConnectResult {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        return BluetoothConnectResult(null, "Bluetooth-Zugriff nicht freigegeben. Bitte Berechtigung erteilen und erneut versuchen.")
    }
    return suspendCancellableCoroutine { continuation ->
        val socket = try {
            device.createRfcommSocketToServiceRecord(
                UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            )
        } catch (_: Exception) {
            continuation.resume(BluetoothConnectResult(null, "SPP-Verbindung fehlgeschlagen: Profil nicht unterstützt oder Gerät nicht erreichbar."))
            return@suspendCancellableCoroutine
        }
        continuation.invokeOnCancellation { try { socket.close() } catch (_: Exception) {} }
        Thread({
            val result = try {
                socket.connect()
                BluetoothConnectResult(socket, "Bluetooth-SPP verbunden. Die Verbindung bleibt bis zum Trennen geöffnet; es wurden keine Druckdaten gesendet.")
            } catch (_: Exception) {
                try { socket.close() } catch (_: Exception) {}
                BluetoothConnectResult(null, "SPP-Verbindung fehlgeschlagen: Profil nicht unterstützt oder Gerät nicht erreichbar.")
            }
            if (continuation.isActive) continuation.resume(result)
            else try { result.socket?.close() } catch (_: Exception) {}
        }, "trakuna-bluetooth-spp").start()
    }
}

class MainActivity : ComponentActivity() {
    private var activePrinterConnection: Closeable? = null

    override fun onDestroy() {
        try { activePrinterConnection?.close() } catch (_: Exception) {}
        activePrinterConnection = null
        super.onDestroy()
    }

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
        var printerStatus by remember { mutableStateOf("Noch keine Druckerverbindung hergestellt.") }
        var printerConnected by remember { mutableStateOf(false) }
        var pairedPrinters by remember { mutableStateOf(listOf<Pair<BluetoothDevice, String>>()) }
        val coroutineScope = rememberCoroutineScope()

        val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                try {
                    val adapter = (getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter
                    pairedPrinters = adapter?.bondedDevices?.toList()?.map { device -> device to (device.name?.takeIf { it.isNotBlank() } ?: "Bluetooth-Gerät (${device.address})") }?.sortedBy { it.second } ?: emptyList()
                    printerStatus = if (pairedPrinters.isEmpty()) "Keine gekoppelten Bluetooth-Geräte gefunden. Kopple den Drucker zuerst in den Android-Einstellungen."
                        else "Gekoppelte Geräte geladen. Wähle einen Drucker für den SPP-Verbindungstest."
                } catch (_: SecurityException) {
                    printerStatus = "Bluetooth-Berechtigung fehlt."
                }
            } else {
                printerStatus = "Bluetooth-Zugriff wurde nicht freigegeben."
                DiagnosticLog.record(this@MainActivity, "WARN", "Berechtigung", "Bluetooth-Zugriff nicht freigegeben.")
            }
        }

        fun loadPairedPrinters() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                return
            }
            try {
                val adapter = (getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter
                pairedPrinters = adapter?.bondedDevices?.toList()?.map { device -> device to (device.name?.takeIf { it.isNotBlank() } ?: "Bluetooth-Gerät (${device.address})") }?.sortedBy { it.second } ?: emptyList()
                printerStatus = if (pairedPrinters.isEmpty()) "Keine gekoppelten Bluetooth-Geräte gefunden. Kopple den Drucker zuerst in den Android-Einstellungen."
                    else "Gekoppelte Geräte geladen. Wähle einen Drucker für den SPP-Verbindungstest."
            } catch (_: SecurityException) {
                printerStatus = "Bluetooth-Berechtigung fehlt."
            }
        }

        fun connectWifiEndpoint() {
            val validation = TcpEndpointValidator.parse(printerHost, printerPort)
            if (validation is TcpEndpointValidation.Invalid) {
                printerStatus = validation.reason
                DiagnosticLog.record(this@MainActivity, "WARN", "Drucker-Netzwerk", validation.reason)
                return
            }
            val endpoint = (validation as TcpEndpointValidation.Valid).endpoint
            try { activePrinterConnection?.close() } catch (_: Exception) {}
            activePrinterConnection = null
            printerConnected = false
            printerStatus = "Verbinde mit Netzwerk-Drucker …"
            DiagnosticLog.record(this@MainActivity, "INFO", "Drucker-Netzwerk", "Netzwerk-Verbindungstest gestartet.")
            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) {
                    try {
                        val resolved = InetAddress.getAllByName(endpoint.host).toList()
                        if (resolved.isEmpty() || resolved.any { !LocalNetworkAddressPolicy.isAllowed(it) }) {
                            return@withContext "Aus Sicherheitsgründen sind nur lokale/private Netzwerkadressen als Druckerziel erlaubt."
                        }
                        // Connect to the already-validated address; do not resolve the hostname again.
                        val socket = Socket()
                        try {
                            socket.connect(InetSocketAddress(resolved.first(), endpoint.port), 5000)
                            socket
                        } catch (e: Exception) {
                            try { socket.close() } catch (_: Exception) {}
                            throw e
                        }
                    } catch (e: Exception) {
                        "Netzwerk-Endpunkt nicht erreichbar: ${e.message ?: "Verbindung fehlgeschlagen"}"
                    }
                }
                if (result is Socket) {
                    activePrinterConnection = result
                    printerConnected = true
                    printerStatus = "Mit ${endpoint.host}:${endpoint.port} verbunden. TCP-Verbindung bleibt geöffnet. Druckerprotokoll und Druckbereitschaft sind noch nicht geprüft."
                } else {
                    printerConnected = false
                    printerStatus = result as String
                }
                DiagnosticLog.record(this@MainActivity, if (printerConnected) "INFO" else "WARN", "Drucker-Netzwerk", printerStatus)
            }
        }

        fun connectBluetoothSppToDevice(device: BluetoothDevice) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    printerStatus = "Bluetooth-Berechtigung fehlt. Bitte App-Berechtigungen in Android prüfen."
                }
                return
            }
            try { activePrinterConnection?.close() } catch (_: Exception) {}
            activePrinterConnection = null
            printerConnected = false
            printerStatus = "Verbinde per Bluetooth-SPP …"
            DiagnosticLog.record(this@MainActivity, "INFO", "Drucker-Bluetooth", "Bluetooth-SPP-Verbindungsaufbau gestartet.")
            coroutineScope.launch {
                val result = try {
                    withTimeoutOrNull(12_000) {
                        connectBluetoothSpp(this@MainActivity, device)
                    } ?: BluetoothConnectResult(null, "SPP-Verbindung nach 12 Sekunden abgebrochen. Gerät und Profil prüfen.")
                } catch (_: SecurityException) {
                    BluetoothConnectResult(null, "Bluetooth-Zugriff verweigert. Berechtigung erneut prüfen.")
                }
                activePrinterConnection = result.socket
                printerConnected = result.socket != null
                printerStatus = result.message
                DiagnosticLog.record(this@MainActivity, if (printerConnected) "INFO" else "WARN", "Drucker-Bluetooth", result.message)
            }
        }

        

        fun detectCode(raw: String) {
            scannedCode = raw.trim()
            val match = CarrierDetector.detect(scannedCode)
            if (match != null) {
                DiagnosticLog.record(this@MainActivity, "INFO", "Scanner", "Barcode-Muster erkannt; Anbieter-Kandidat zugeordnet.")
                selected = match
                detected = true
                status = "Anbieter-Kandidat erkannt: $match. Prüfe die Auswahl und öffne das Portal erst mit der Portal-Schaltfläche."
            } else {
                DiagnosticLog.record(this@MainActivity, "WARN", "Scanner", "Barcode erfasst, Anbieter nicht eindeutig zuordenbar.")
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
                .addOnSuccessListener { result -> detectCode(result.rawValue.orEmpty()) }
                .addOnFailureListener {
                    status = "Scan nicht möglich. Code kann auch über einen externen Scanner eingegeben werden."
                    DiagnosticLog.record(this@MainActivity, "WARN", "Scanner", "Kamera-Scan fehlgeschlagen.")
                }
        }

        fun openPortal() {
            val portalUrl = CarrierPortals.all.firstOrNull { it.name == selected }?.url
            if (portalUrl == null) {
                status = "Bitte zuerst einen Paketdienst auswählen."
                DiagnosticLog.record(this@MainActivity, "WARN", "Portal", "Portalöffnung ohne ausgewählten Anbieter angefordert.")
                return
            }
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(portalUrl)))
                status = "$selected wird im externen Android-Browser geöffnet. Anmeldung und Vorgänge erfolgen ausschließlich beim Anbieter."
                DiagnosticLog.record(this@MainActivity, "INFO", "Portal", "Anbieterportal an Android-Browser übergeben.")
            } catch (_: ActivityNotFoundException) {
                status = "Kein Browser zum Öffnen des Anbieterportals gefunden."
                DiagnosticLog.record(this@MainActivity, "ERROR", "Portal", "Kein kompatibler Browser zum Öffnen gefunden.")
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
                        onClick = { detectCode(scannedCode); openPortal() },
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
                    CarrierPortals.all.forEach { portal ->
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
                    ParcelShopSection()
                    Spacer(Modifier.height(18.dp))
                    PrinterConnectionSection(
                        host = printerHost,
                        port = printerPort,
                        status = printerStatus,
                        pairedPrinters = pairedPrinters,
                        onHostChange = { printerHost = it },
                        onPortChange = { printerPort = it.filter(Char::isDigit).take(5) },
                        onTestNetwork = { connectWifiEndpoint() },
                        onLoadBluetooth = { loadPairedPrinters() },
                        onTestBluetooth = { device -> connectBluetoothSppToDevice(device) },
                        isConnected = printerConnected,
                        onDisconnect = {
                            try { activePrinterConnection?.close() } catch (_: Exception) {}
                            activePrinterConnection = null
                            printerConnected = false
                            printerStatus = "Druckerverbindung getrennt."
                            DiagnosticLog.record(this@MainActivity, "INFO", "Drucker", "Druckerverbindung manuell getrennt.")
                        }
                    )
                    Spacer(Modifier.height(18.dp))
                    DiagnosticLogSection(refreshKey = "$status|$printerStatus")
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