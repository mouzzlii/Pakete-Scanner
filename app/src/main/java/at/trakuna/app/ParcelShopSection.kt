package at.trakuna.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
internal fun ParcelShopSection() {
    val context = LocalContext.current
    val database = remember(context) { ParcelDatabase(context) }
    val scope = rememberCoroutineScope()
    var parcels by remember { mutableStateOf(emptyList<ParcelRecord>()) }
    var barcode by remember { mutableStateOf("") }
    var carrierId by remember { mutableStateOf(CarrierRegistry.all.firstOrNull()?.id.orEmpty()) }
    var carrierMenuExpanded by remember { mutableStateOf(false) }
    val storageLocations = remember { mutableStateMapOf<String, String>() }
    var isReturn by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("Lokale Demo-Verwaltung – Daten bleiben auf diesem Gerät.") }

    suspend fun refresh() {
        parcels = withContext(Dispatchers.IO) { database.all() }
    }

    LaunchedEffect(database) { refresh() }
    DisposableEffect(database) { onDispose { database.close() } }

    fun perform(action: suspend () -> ParcelStoreResult) {
        scope.launch {
            val result = withContext(Dispatchers.IO) { action() }
            when (result) {
                is ParcelStoreResult.Saved -> {
                    feedback = "Gespeichert: ${result.parcel.barcode}"
                    refresh()
                }
                is ParcelStoreResult.Rejected -> feedback = result.reason
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("PAKETVERWALTUNG", color = Color(0xFF174D39), fontWeight = FontWeight.Bold)
            Text("Lokale Demo · keine Carrier-API · keine Cloud-Synchronisierung", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Sendungsnummer / Barcode") },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { carrierMenuExpanded = true }) {
                    Text("Paketdienst: ${CarrierRegistry.byId(carrierId)?.displayName ?: "Auswählen"}")
                }
                DropdownMenu(expanded = carrierMenuExpanded, onDismissRequest = { carrierMenuExpanded = false }) {
                    CarrierRegistry.all.forEach { carrier ->
                        DropdownMenuItem(
                            text = { Text(carrier.displayName) },
                            onClick = {
                                carrierId = carrier.id
                                carrierMenuExpanded = false
                            }
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { isReturn = false }, modifier = Modifier.weight(1f)) {
                    Text(if (!isReturn) "✓ Paket" else "Paket")
                }
                OutlinedButton(onClick = { isReturn = true }, modifier = Modifier.weight(1f)) {
                    Text(if (isReturn) "✓ Retoure" else "Retoure")
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    val parcel = ParcelRecord(
                        id = UUID.randomUUID().toString(),
                        barcode = barcode.trim(),
                        carrierId = carrierId,
                        status = if (isReturn) ParcelStatus.RETURN_RECEIVED else ParcelStatus.RECEIVED,
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now
                    )
                    perform { database.register(parcel) }
                    barcode = ""
                },
                enabled = barcode.isNotBlank() && carrierId.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (isReturn) "RETOURE ANNEHMEN" else "PAKET ANNEHMEN") }

            Spacer(Modifier.height(8.dp))
            Text(feedback, style = MaterialTheme.typography.bodySmall, color = Color(0xFF174D39))
            Spacer(Modifier.height(12.dp))
            val openCount = parcels.count { it.status != ParcelStatus.HANDED_OVER && it.status != ParcelStatus.RETURNED_TO_CARRIER }
            Text("PAKETVORGÄNGE · $openCount offen · ${parcels.size - openCount} abgeschlossen", fontWeight = FontWeight.Bold, color = Color(0xFF174D39))
            if (parcels.isEmpty()) {
                Text("Noch keine Pakete erfasst.", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
            } else {
                parcels.forEach { parcel ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F2))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(parcel.barcode, fontWeight = FontWeight.Bold)
                            Text(
                                "${CarrierRegistry.byId(parcel.carrierId)?.displayName ?: parcel.carrierId} · ${parcel.status.toGermanLabel()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (!parcel.storageLocation.isNullOrBlank()) {
                                Text("Lagerplatz: ${parcel.storageLocation}", style = MaterialTheme.typography.bodySmall)
                            }
                            when (parcel.status) {
                                ParcelStatus.RECEIVED -> {
                                    OutlinedTextField(
                                        value = storageLocations[parcel.id].orEmpty(),
                                        onValueChange = { storageLocations[parcel.id] = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Lagerplatz (z. B. Regal A-02)") },
                                        singleLine = true
                                    )
                                    Button(
                                        onClick = {
                                            val location = storageLocations[parcel.id].orEmpty().trim()
                                            perform { database.updateStatus(parcel.id, ParcelStatus.STORED, System.currentTimeMillis(), location) }
                                        },
                                        enabled = !storageLocations[parcel.id].isNullOrBlank(),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("EINLAGERN") }
                                }
                                ParcelStatus.STORED -> Button(
                                    onClick = { perform { database.updateStatus(parcel.id, ParcelStatus.READY_FOR_PICKUP, System.currentTimeMillis()) } },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("ALS ABHOLBEREIT MARKIEREN") }
                                ParcelStatus.READY_FOR_PICKUP -> Button(
                                    onClick = { perform { database.updateStatus(parcel.id, ParcelStatus.HANDED_OVER, System.currentTimeMillis()) } },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("ÜBERGABE BESTÄTIGEN") }
                                ParcelStatus.RETURN_RECEIVED -> Button(
                                    onClick = { perform { database.updateStatus(parcel.id, ParcelStatus.RETURNED_TO_CARRIER, System.currentTimeMillis()) } },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("AN PAKETDIENST ÜBERGEBEN") }
                                ParcelStatus.HANDED_OVER, ParcelStatus.RETURNED_TO_CARRIER -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ParcelStatus.toGermanLabel(): String = when (this) {
    ParcelStatus.RECEIVED -> "Angenommen"
    ParcelStatus.STORED -> "Eingelagert"
    ParcelStatus.READY_FOR_PICKUP -> "Abholbereit"
    ParcelStatus.HANDED_OVER -> "Ausgegeben"
    ParcelStatus.RETURN_RECEIVED -> "Retoure angenommen"
    ParcelStatus.RETURNED_TO_CARRIER -> "Retoure übergeben"
}
