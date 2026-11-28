package at.trakuna.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    }

    @Test fun parsesResponseAndRepeatedAttributeValues() {
        val response = fakeResponse(
            0x0000,
            7,
            listOf(
                Triple(0x41, "printer-name", "TRAKUNA Test"),
                Triple(0x44, "document-format-supported", "application/pdf"),
                Triple(0x44, "", "image/pwg-raster")
            )
        )
        val parsed = IppProtocolCodec.parseResponse(response)
        assertEquals(0, parsed.statusCode)
        assertEquals(7, parsed.requestId)
        assertEquals("TRAKUNA Test", parsed.first("printer-name"))
        assertEquals(listOf("application/pdf", "image/pwg-raster"), parsed.attributes["document-format-supported"])
    }

    @Test fun rejectsTruncatedResponsesAndMissingEndTag() {
        assertThrows(IllegalArgumentException::class.java) {
            IppProtocolCodec.parseResponse(byteArrayOf(2, 0, 0, 0, 0, 0, 0, 1))
        }
        assertFalse(runCatching {
            IppProtocolCodec.parseResponse(byteArrayOf(2, 0, 0, 0, 0, 0, 0, 1, 1))
        }.isSuccess)
    }

    private fun fakeResponse(status: Int, requestId: Int, attributes: List<Triple<Int, String, String>>): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(2, 0, (status ushr 8).toByte(), status.toByte(),
            (requestId ushr 24).toByte(), (requestId ushr 16).toByte(),
            (requestId ushr 8).toByte(), requestId.toByte(), 0x01))
        attributes.forEach { (tag, name, value) ->
            val n = name.toByteArray(StandardCharsets.UTF_8)
            val v = value.toByteArray(StandardCharsets.UTF_8)
            out.write(tag)
            out.write((n.size ushr 8) and 0xFF); out.write(n.size and 0xFF); out.write(n)
            out.write((v.size ushr 8) and 0xFF); out.write(v.size and 0xFF); out.write(v)
        }
        out.write(0x03)
        return out.toByteArray()
    }

    private fun ByteArray.containsAscii(value: String): Boolean =
        String(this, StandardCharsets.ISO_8859_1).contains(value)
}
