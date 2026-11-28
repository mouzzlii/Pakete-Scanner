package at.trakuna.app

import java.util.Locale

/**
 * Conservative barcode-to-carrier heuristics.
 *
 * Formats are candidates only; they are not a guarantee of carrier ownership.
 * Ambiguous formats deliberately return null and require manual selection.
 */
internal object CarrierDetector {
    fun normalize(raw: String): String {
        var value = raw.trim().uppercase(Locale.ROOT)
        // Scanner AIM identifiers describe the barcode symbology, not the parcel ID.
        if (value.startsWith("]C1") || value.startsWith("]C0") || value.startsWith("]D2")) {
            value = value.drop(3)
        }
        return value.replace(Regex("""[\s-]"""), "")
    }

    fun detect(raw: String): String? {
        val value = raw.trim().uppercase(Locale.ROOT)
        val compact = normalize(value)
        return when {
            value.contains("HERMES") -> "Hermes Geschäftskundenbereich"
            value.contains("GLS") -> "GLS / YourGLS"
            value.contains("DPD") -> "DPD Business"
            value.contains("UPS") -> "UPS Business / CampusShip"
            value.contains("DHL") -> "DHL Geschäftskunden"
            value.contains("POST") -> "Österreichische Post / ELLA"
            // Hermes: confirmed Austrian label family 104 + 19 digits.
            Regex("""^104\d{19}$""").matches(compact) -> "Hermes Geschäftskundenbereich"
            // DPD Austria accepts a P + 12-digit PIN and a 14-digit parcel number.
            Regex("""^P\d{12}$""").matches(compact) -> "DPD Business"
            Regex("""^\d{14}$""").matches(compact) -> "DPD Business"
            // UPS: unambiguous 1Z format.
            Regex("""^1Z[A-Z0-9]{16}$""").matches(compact) -> "UPS Business / CampusShip"
            // Austrian Post domestic parcel numbers are 22 digits and start with 10.
            Regex("""^10\d{20}$""").matches(compact) -> "Österreichische Post / ELLA"
            // An S10 identifier ending in AT was issued by the Austrian postal operator.
            Regex("""^[A-Z]{2}\d{9}AT$""").matches(compact) -> "Österreichische Post / ELLA"
            // Generic EAN/S10 and other numeric formats remain ambiguous.
            else -> null
        }
    }
}
