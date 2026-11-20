
package de.trakuna.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Forest = Color(0xFF164B38)
private val LightGreen = Color(0xFFE8F1EB)
private val Background = Color(0xFFF5F7F5)

data class ParcelItem(
    val number: String,
    val customer: String,
    val type: String,
    val status: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrakunaApp()
        }
    }
}

@Composable
fun TrakunaApp() {
    var selectedFlow by remember { mutableStateOf("Annahme") }
    var trackingNumber by remember { mutableStateOf("") }
    var customer by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Willkommen bei TRAKUNA.") }

    val parcels = remember { mutableStateListOf<ParcelItem>() }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Forest,
            background = Background
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {
            Text(
                text = "TRAKUNA",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Forest
            )

            Text(
                text = "ALLE PAKETDIENSTE. EIN SMARTPHONE.",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Forest
            )

            Spacer(modifier = Modifier.height(22.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Forest),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "SCAN-ZENTRALE",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Was möchtest du erledigen?",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Alle wichtigen Paketshop-Abläufe an einem Ort.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "PAKETSHOP-FUNKTIONEN",
                color = Forest,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            val flows = listOf(
                "Annahme" to "Paket entgegennehmen",
                "Abholung" to "Paket an Kunden ausgeben",
                "Retoure" to "Rücksendung erfassen",
                "Versand" to "Übergabe vorbereiten"
            )

            flows.chunked(2).forEach { rowFlows ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowFlows.forEach { flow ->
                        val active = selectedFlow == flow.first

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(112.dp),
                            onClick = {
                                selectedFlow = flow.first
                                message = "${flow.first} ausgewählt."
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (active) LightGreen else Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = if (active) {
                                androidx.compose.foundation.BorderStroke(2.dp, Forest)
                            } else null
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(13.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = flow.first,
                                    color = Forest,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = flow.second,
                                    color = Color.DarkGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = selectedFlow.uppercase(),
                        color = Forest,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = trackingNumber,
                        onValueChange = { trackingNumber = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Sendungsnummer / Barcode") },
                        singleLine = true
                    )

                    if (selectedFlow == "Annahme" || selectedFlow == "Retoure") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customer,
                            onValueChange = { customer = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Kundenname (optional)") },
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val number = trackingNumber.trim()

                            if (number.isBlank()) {
                                message = "Bitte zuerst eine Sendungsnummer eingeben."
                            } else {
                                when (selectedFlow) {
                                    "Annahme" -> {
                                        parcels.add(
                                            ParcelItem(
                                                number,
                                                customer.trim(),
                                                "Annahme",
                                                "Eingelagert"
                                            )
                                        )
                                        message = "Paket wurde lokal erfasst."
                                    }

                                    "Retoure" -> {
                                        parcels.add(
                                            ParcelItem(
                                                number,
                                                customer.trim(),
                                                "Retoure",
                                                "Zur Übergabe vorgemerkt"
                                            )
                                        )
                                        message = "Retoure wurde lokal erfasst."
                                    }

                                    "Abholung" -> {
                                        val index = parcels.indexOfFirst {
                                            it.number == number &&
                                            it.status == "Eingelagert"
                                        }

                                        if (index >= 0) {
                                            parcels[index] = parcels[index].copy(
                                                status = "Abgeholt"
                                            )
                                            message = "Paket wurde als abgeholt markiert."
                                        } else {
                                            message = "Kein eingelagerter Treffer gefunden."
                                        }
                                    }

                                    "Versand" -> {
                                        val index = parcels.indexOfFirst {
                                            it.number == number
                                        }

                                        if (index >= 0) {
                                            parcels[index] = parcels[index].copy(
                                                status = "Zur Übergabe vorgemerkt"
                                            )
                                            message = "Paket für die Übergabe vorgemerkt."
                                        } else {
                                            message = "Paket nicht gefunden. Bitte zuerst erfassen."
                                        }
                                    }
                                }

                                if (message.contains("lokal erfasst") ||
                                    message.contains("markiert") ||
                                    message.contains("vorgemerkt")
                                ) {
                                    trackingNumber = ""
                                    customer = ""
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "VORGANG AUSFÜHREN",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = message,
                        color = Forest,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "PAKETÜBERSICHT (${parcels.size})",
                color = Forest,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (parcels.isEmpty()) {
                Text(
                    text = "Noch keine Pakete erfasst.",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            } else {
                parcels.reversed().forEach { parcel ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = parcel.number,
                                color = Forest,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${parcel.type} • ${parcel.status}",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                            if (parcel.customer.isNotBlank()) {
                                Text(
                                    text = "Kunde: ${parcel.customer}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "TRAKUNA 2.0 • TESTVERSION",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
