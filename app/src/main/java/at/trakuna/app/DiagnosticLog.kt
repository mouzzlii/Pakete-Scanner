package at.trakuna.app

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class DiagnosticEntry(
    val timestamp: Long,
    val severity: String,
    val category: String,
    val message: String
)

internal object DiagnosticLog {
    private const val PREFS = "trakuna_diagnostics"
    private const val KEY = "events"
    private const val MAX_ENTRIES = 150

    @Synchronized
    fun record(context: Context, severity: String, category: String, message: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = readJson(prefs.getString(KEY, null))
        val entry = JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("severity", severity.take(12))
            put("category", category.take(32))
            put("message", redact(message).take(220))
        }
        current.put(entry)
        while (current.length() > MAX_ENTRIES) current.remove(0)
        prefs.edit().putString(KEY, current.toString()).apply()
    }

    @Synchronized
    fun entries(context: Context): List<DiagnosticEntry> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val array = readJson(prefs.getString(KEY, null))
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            DiagnosticEntry(
                timestamp = item.optLong("timestamp"),
                severity = item.optString("severity", "INFO"),
                category = item.optString("category", "App"),
                message = item.optString("message")
            )
        }.reversed()
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY).apply()
    }

    fun exportText(context: Context): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY)
        return buildString {
            appendLine("TRAKUNA – lokales Fehlerprotokoll")
            appendLine("Export: ${formatter.format(Date())}")
            appendLine("Es werden keine Paketcodes, IP-Adressen oder Bluetooth-Adressen protokolliert.")
            appendLine()
            entries(context).asReversed().forEach { event ->
                appendLine("${formatter.format(Date(event.timestamp))} [${event.severity}] [${event.category}] ${event.message}")
            }
        }
    }

    private fun readJson(raw: String?): JSONArray = try {
        if (raw.isNullOrBlank()) JSONArray() else JSONArray(raw)
    } catch (_: Exception) {
        JSONArray()
    }

    private fun redact(value: String): String {
        return value
            .replace(Regex("""(?i)\b(?:\d{1,3}\.){3}\d{1,3}\b"""), "[IP]")
            .replace(Regex("""(?i)\b[0-9a-f]{2}(?::[0-9a-f]{2}){5}\b"""), "[BT-ADRESSE]")
            .replace(Regex("""\b\d{8,}\b"""), "[ZAHLENCODE]")
    }
}

@Composable
internal fun DiagnosticLogSection(status: String) {
    val context = LocalContext.current
    var events by remember { mutableStateOf(DiagnosticLog.entries(context)) }

    LaunchedEffect(status) {
        if (status.isNotBlank()) {
            val failure = status.contains("fehl", ignoreCase = true) ||
                status.contains("nicht erreichbar", ignoreCase = true) ||
                status.contains("abgebrochen", ignoreCase = true) ||
                status.contains("verweigert", ignoreCase = true) ||
                status.contains("Fehler", ignoreCase = true)
            DiagnosticLog.record(
                context,
                if (failure) "WARN" else "INFO",
                "Drucker",
                status
            )
            events = DiagnosticLog.entries(context)
        }
    }

    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text("FEHLERPROTOKOLL", fontWeight = FontWeight.Bold, color = Color(0xFF174D39))
            Text(
                "Lokale Diagnoseereignisse – maximal 150 Einträge. Paketcodes und Geräteadressen werden vor dem Speichern maskiert.",
                fontSize = 11.sp,
                color = Color.DarkGray
            )
            Spacer(Modifier.height(8.dp))
            if (events.isEmpty()) {
                Text("Noch keine Diagnoseereignisse vorhanden.", fontSize = 12.sp)
            } else {
                events.take(12).forEach { event ->
                    Text(
                        "${SimpleDateFormat("dd.MM. HH:mm:ss", Locale.GERMANY).format(Date(event.timestamp))} · ${event.severity} · ${event.message}",
                        fontSize = 11.sp,
                        color = if (event.severity == "WARN") Color(0xFF9A4D00) else Color.DarkGray,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                if (events.size > 12) {
                    Text("… und ${events.size - 12} ältere Einträge", fontSize = 10.sp, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "TRAKUNA Fehlerprotokoll")
                        putExtra(Intent.EXTRA_TEXT, DiagnosticLog.exportText(context))
                    }
                    context.startActivity(Intent.createChooser(send, "Fehlerprotokoll teilen"))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = events.isNotEmpty()
            ) { Text("PROTOKOLL EXPORTIEREN / TEILEN") }
            OutlinedButton(
                onClick = {
                    DiagnosticLog.clear(context)
                    events = emptyList()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = events.isNotEmpty()
            ) { Text("PROTOKOLL LÖSCHEN") }
        }
    }
}
