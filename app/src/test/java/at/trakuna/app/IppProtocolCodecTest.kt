package at.trakuna.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class IppProtocolCodecTest {
    @Test fun buildsGetPrinterAttributesRequestWithExpectedHeaderAndOperation() {
        val request = IppProtocolCodec.buildGetPrinterAttributesRequest("ipp://192.168.1.20:631/ipp/print", 42)
        assertArrayEquals(byteArrayOf(2, 0, 0, 11, 0, 0, 0, 42), request.copyOfRange(0, 8))
        assertTrue(request.containsAscii("printer-uri"))
        assertTrue(request.containsAscii("ipp://192.168.1.20:631/ipp/print"))
        assertTrue(request.containsAscii("document-format-supported"))
        assertEquals(0x03, request.last().toInt() and 0xFF)
    }

    @Test fun rejectsUnsupportedOrCredentialBearingPrinterUris() {
        assertThrows(IllegalArgumentException::class.java) {
            IppProtocolCodec.buildGetPrinterAttributesRequest("http://192.168.1.20/ipp/print")
        }
        assertThrows(IllegalArgumentException::class.java) {
            IppProtocolCodec.buildGetPrinterAttributesRequest("ipp://user:secret@192.168.1.20/ipp/print")
        }
        assertThrows(IllegalArgumentException::class.java) {
            IppProtocolCodec.buildGetPrinterAttributesRequest("ipp://192.168.1.20/ipp/../admin")
        }
    }

    @Test fun parsesTextAndRepeatedAttributeValues() {
        val response = fakeTextResponse(0x0000, 7, listOf(
            Triple(0x41, "printer-name", "TRAKUNA Test"),
            Triple(0x44, "document-format-supported", "application/pdf"),
            Triple(0x44, "", "image/pwg-raster")
        ))
        val parsed = IppProtocolCodec.parseResponse(response)
        assertEquals(0, parsed.statusCode)
        assertEquals(7, parsed.requestId)
        assertEquals("TRAKUNA Test", parsed.first("printer-name"))
        assertEquals(listOf("application/pdf", "image/pwg-raster"), parsed.attributes["document-format-supported"])
    }

    @Test fun decodesPrinterStateEnumAndBooleanAsTypedValues() {
        val response = fakeResponse(0, 9, listOf(
            Triple(0x23, "printer-state", byteArrayOf(0, 0, 0, 3)),
            Triple(0x22, "printer-is-accepting-jobs", byteArrayOf(1))
        ))
        val parsed = IppProtocolCodec.parseResponse(response)
        assertEquals(3, parsed.firstInteger("printer-state"))
        assertEquals(true, parsed.firstBoolean("printer-is-accepting-jobs"))
        assertEquals("3", parsed.first("printer-state"))
    }

    @Test fun preservesOpaqueBinaryValuesWithoutUtf8Corruption() {
        val response = fakeResponse(0, 10, listOf(
            Triple(0x30, "printer-config-change-time", byteArrayOf(0, 0xFF.toByte(), 2))
        ))
        val parsed = IppProtocolCodec.parseResponse(response)
        assertNull(parsed.first("printer-config-change-time"))
        assertArrayEquals(byteArrayOf(0, 0xFF.toByte(), 2),
            parsed.typedAttributes["printer-config-change-time"]!!.single().rawValue)
    }

    @Test fun rejectsMalformedTypedValues() {
        assertThrows(IllegalArgumentException::class.java) {
            IppProtocolCodec.parseResponse(fakeTextResponse(0, 1, listOf(
                Triple(0x23, "printer-state", byteArrayOf(0, 1))
            )))
        }
        assertThrows(IllegalArgumentException::class.java) {
            IppProtocolCodec.parseResponse(fakeResponse(0, 1, listOf(
                Triple(0x22, "printer-is-accepting-jobs", byteArrayOf(2))
            )))
        }
    }

    @Test fun rejectsTruncatedResponsesAndMissingEndTag() {
        assertThrows(IllegalArgumentException::class.java) {
            IppProtocolCodec.parseResponse(byteArrayOf(2, 0, 0, 0, 0, 0, 0, 1))
        }
        assertFalse(runCatching {
            IppProtocolCodec.parseResponse(byteArrayOf(2, 0, 0, 0, 0, 0, 0, 1, 1))
        }.isSuccess)
    }

    private fun fakeTextResponse(status: Int, requestId: Int, attributes: List<Triple<Int, String, String>>): ByteArray {
        return fakeRawResponse(status, requestId, attributes.map { (tag, name, value) ->
            Triple(tag, name, value.toByteArray(StandardCharsets.UTF_8))
        })
    }

    private fun fakeRawResponse(status: Int, requestId: Int, attributes: List<Triple<Int, String, ByteArray>>): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(2, 0, (status ushr 8).toByte(), status.toByte(),
            (requestId ushr 24).toByte(), (requestId ushr 16).toByte(),
            (requestId ushr 8).toByte(), requestId.toByte(), 0x01))
        attributes.forEach { (tag, name, value) ->
            val n = name.toByteArray(StandardCharsets.UTF_8)
            out.write(tag)
            out.write((n.size ushr 8) and 0xFF); out.write(n.size and 0xFF); out.write(n)
            out.write((value.size ushr 8) and 0xFF); out.write(value.size and 0xFF); out.write(value)
        }
        out.write(0x03)
        return out.toByteArray()
    }

    private fun ByteArray.containsAscii(value: String): Boolean =
        String(this, StandardCharsets.ISO_8859_1).contains(value)
}
