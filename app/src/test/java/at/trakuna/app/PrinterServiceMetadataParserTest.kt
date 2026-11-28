package at.trakuna.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class PrinterServiceMetadataParserTest {
    private fun attrs(vararg values: Pair<String, String>) =
        values.associate { it.first to it.second.toByteArray(StandardCharsets.UTF_8) }

    @Test fun parsesIppResourcePathAndDeviceIdentity() {
        val result = PrinterServiceMetadataParser.parse(
            "_ipp._tcp.",
            attrs("rp" to "ipp/print", "usb_MFG" to "Example", "usb_MDL" to "Office 4000")
        )
        assertEquals("IPP", result.protocolLabel)
        assertFalse(result.secure)
        assertEquals("/ipp/print", result.resourcePath)
        assertEquals("Example", result.manufacturer)
        assertEquals("Office 4000", result.model)
    }

    @Test fun identifiesSecureIppAndLprServices() {
        val secure = PrinterServiceMetadataParser.parse("_ipps._tcp.", emptyMap())
        assertEquals("IPP über TLS", secure.protocolLabel)
        assertTrue(secure.secure)
        assertEquals("LPD/LPR", PrinterServiceMetadataParser.parse("_printer._tcp.", emptyMap()).protocolLabel)
    }

    @Test fun rejectsUnsafeOrMalformedResourcePaths() {
        listOf("../admin", "ipp/../admin", "ipp/print?x=1", "ipp\\\\print", "ipp/print#fragment")
            .forEach { path ->
                val result = PrinterServiceMetadataParser.parse("_ipp._tcp.", attrs("rp" to path))
                assertNull("Path should be rejected: $path", result.resourcePath)
            }
    }

    @Test fun unknownServiceTypeDoesNotPretendToBeIpp() {
        val result = PrinterServiceMetadataParser.parse("_custom._tcp.", emptyMap())
        assertEquals("Unbekannter Druckerdienst", result.protocolLabel)
        assertNull(result.resourcePath)
    }
}
