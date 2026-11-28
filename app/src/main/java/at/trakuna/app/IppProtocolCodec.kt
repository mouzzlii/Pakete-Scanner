package at.trakuna.app

import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.charset.StandardCharsets

internal data class IppResponse(
    val statusCode: Int,
    val requestId: Int,
    val attributes: Map<String, List<String>>
) {
    fun first(name: String): String? = attributes[name]?.firstOrNull()
}

/**
 * Small, dependency-free IPP codec for capability probes.
 * It deliberately does not implement Print-Job or trust printer-supplied URLs.
 */
internal object IppProtocolCodec {
    private val requestedAttributes = listOf(
        "printer-name",
        "printer-make-and-model",
        "printer-state",
        "printer-state-reasons",
        "document-format-supported",
        "media-supported",
        "printer-resolution-supported"
    )

    fun buildGetPrinterAttributesRequest(printerUri: String, requestId: Int = 1): ByteArray {
        require(requestId > 0) { "IPP request id must be positive." }
        val uri = URI(printerUri)
        require(uri.scheme.equals("ipp", true) || uri.scheme.equals("ipps", true)) {
            "Only ipp:// and ipps:// printer URIs are supported."
        }
        require(uri.host != null && uri.userInfo == null && uri.fragment == null) {
            "Printer URI must contain a host and no credentials or fragment."
        }
        require(uri.rawPath.orEmpty().isNotEmpty() && !uri.rawPath.contains("..")) {
            "Printer URI must contain a safe resource path."
        }

        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(2, 0)) // IPP/2.0
        writeU16(out, 0x000B) // Get-Printer-Attributes
        writeU32(out, requestId)
        out.write(0x01) // operation-attributes-tag
        writeAttribute(out, 0x47, "attributes-charset", "utf-8")
        writeAttribute(out, 0x48, "attributes-natural-language", "en")
        writeAttribute(out, 0x45, "printer-uri", printerUri)
        requestedAttributes.forEachIndexed { index, value ->
            writeAttribute(out, 0x44, if (index == 0) "requested-attributes" else "", value)
        }
        out.write(0x03) // end-of-attributes-tag
        return out.toByteArray()
    }

    fun parseResponse(bytes: ByteArray): IppResponse {
        require(bytes.size >= 9) { "IPP response is too short." }
        val status = readU16(bytes, 2)
        val requestId = readU32(bytes, 4)
        var offset = 8
        var currentName = ""
        val attributes = linkedMapOf<String, MutableList<String>>()

        while (offset < bytes.size) {
            val tag = bytes[offset++].toInt() and 0xFF
            if (tag == 0x03) {
                return IppResponse(status, requestId, attributes.mapValues { it.value.toList() })
            }
            if (tag in 0x01..0x0F) {
                currentName = ""
                continue
            }
            require(offset + 2 <= bytes.size) { "Truncated IPP attribute name length." }
            val nameLength = readU16(bytes, offset)
            offset += 2
            require(offset + nameLength + 2 <= bytes.size) { "Truncated IPP attribute name." }
            if (nameLength > 0) {
                currentName = String(bytes, offset, nameLength, StandardCharsets.UTF_8)
            }
            offset += nameLength
            val valueLength = readU16(bytes, offset)
            offset += 2
            require(offset + valueLength <= bytes.size) { "Truncated IPP attribute value." }
            val value = String(bytes, offset, valueLength, StandardCharsets.UTF_8)
            offset += valueLength
            if (currentName.isNotBlank()) {
                attributes.getOrPut(currentName) { mutableListOf() }.add(value)
            }
        }
        throw IllegalArgumentException("IPP response is missing end-of-attributes-tag.")
    }

    private fun writeAttribute(out: ByteArrayOutputStream, tag: Int, name: String, value: String) {
        val nameBytes = name.toByteArray(StandardCharsets.UTF_8)
        val valueBytes = value.toByteArray(StandardCharsets.UTF_8)
        require(nameBytes.size <= 0xFFFF && valueBytes.size <= 0xFFFF)
        out.write(tag)
        writeU16(out, nameBytes.size)
        out.write(nameBytes)
        writeU16(out, valueBytes.size)
        out.write(valueBytes)
    }

    private fun writeU16(out: ByteArrayOutputStream, value: Int) {
        out.write((value ushr 8) and 0xFF)
        out.write(value and 0xFF)
    }

    private fun writeU32(out: ByteArrayOutputStream, value: Int) {
        out.write((value ushr 24) and 0xFF)
        out.write((value ushr 16) and 0xFF)
        out.write((value ushr 8) and 0xFF)
        out.write(value and 0xFF)
    }

    private fun readU16(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)

    private fun readU32(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
}
