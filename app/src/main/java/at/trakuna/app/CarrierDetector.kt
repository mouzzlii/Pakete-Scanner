package at.trakuna.app

import java.util.Locale

/**
 * Conservative barcode-to-carrier heuristics.
 *
 * Formats are candidates only; they are not a guarantee of carrier ownership.
 * Ambiguous formats deliberately return null and require manual selection.
 */
internal object CarrierDetector {
    fun detect(raw: String): String? {
        val value = raw.trim().uppercase(Locale.ROOT)
        val compact = value.replace(Regex("""[\s-]"""), "")
        return when {
            value.contains("HERMES") -> "Hermes Geschäftskundenbereich"
            value.contains("GLS") -> "GLS / YourGLS"
            value.contains("DPD") -> "DPD Business"
            value.contains("UPS") -> "UPS Business / CampusShip"
            value.contains("DHL") -> "DHL Geschäftskunden"
            value.contains("POST") -> "Österreichische Post / ELLA"
            // Hermes: nur das vom Nutzer bestätigte 104+19-Format automatisch zuordnen.
            Regex("""^104\d{19}$""").matches(compact) -> "Hermes Geschäftskundenbereich"
            // DPD Österreich: offizielles PIN-Beispiel P + 12 Ziffern.
            Regex("""^P\d{12}$""").matches(compact) -> "DPD Business"
            // UPS: eindeutiges 1Z-Format. Andere Nummernlängen können kollidieren.
            Regex("""^1Z[A-Z0-9]{16}$""").matches(compact) -> "UPS Business / CampusShip"
            // DHL, Österreichische Post und nicht eindeutig zuordenbare Formate:
            // manuelle Anbieterauswahl statt unsicherer automatischer Vermutung.
            else -> null
        }
    }
}
