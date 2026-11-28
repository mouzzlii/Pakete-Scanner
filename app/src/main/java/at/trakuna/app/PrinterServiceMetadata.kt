package at.trakuna.app

import java.nio.charset.StandardCharsets

internal data class PrinterServiceMetadata(
    val protocolLabel: String,
    val secure: Boolean,
    val resourcePath: String?,
    val manufacturer: String?,
    val model: String?
)

/** Parses DNS-SD TXT attributes without trusting them as URLs or print commands. */
internal object PrinterServiceMetadataParser {
    fun parse(serviceType: String, attributes: Map<String, ByteArray>): PrinterServiceMetadata {
        val normalizedType = serviceType.trim().lowercase()
        val secure = normalizedType == "_ipps._tcp."
        val protocol = when (normalizedType) {
            "_ipp._tcp." -> "IPP"
            "_ipps._tcp." -> "IPP über TLS"
            "_printer._tcp." -> "LPD/LPR"
            "_pdl-datastream._tcp." -> "Direkter PDL-Datastream"
            else -> "Unbekannter Druckerdienst"
        }

        fun attribute(vararg keys: String): String? {
            val entry = attributes.entries.firstOrNull { candidate ->
                keys.any { it.equals(candidate.key, ignoreCase = true) }
            } ?: return null
            return entry.value.toString(StandardCharsets.UTF_8)
                .trim()
                .takeIf { it.isNotEmpty() && it.length <= 256 && it.none(Char::isISOControl) }
        }

        val advertisedPath = attribute("rp")
        val safePath = advertisedPath
            ?.takeIf { it.length <= 200 && !it.contains("..") && !it.contains('\\') && !it.contains('?') && !it.contains('#') }
            ?.let { if (it.startsWith('/')) it else "/$it" }

        return PrinterServiceMetadata(
            protocolLabel = protocol,
            secure = secure,
            resourcePath = safePath,
            manufacturer = attribute("usb_MFG", "manufacturer", "mfg"),
            model = attribute("usb_MDL", "product", "ty")
        )
    }
}
