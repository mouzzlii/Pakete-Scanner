package at.trakuna.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TcpEndpointValidatorTest {
    @Test fun acceptsHostnameAndStandardIppPort() {
        val result = TcpEndpointValidator.parse(" printer.local ", "631")
        assertEquals(TcpEndpointValidation.Valid(TcpEndpoint("printer.local", 631)), result)
    }

    @Test fun acceptsIpv4AndBoundaryPorts() {
        assertEquals(
            TcpEndpointValidation.Valid(TcpEndpoint("192.168.1.50", 1)),
            TcpEndpointValidator.parse("192.168.1.50", "1")
        )
        assertEquals(
            TcpEndpointValidation.Valid(TcpEndpoint("printer.local", 65535)),
            TcpEndpointValidator.parse("printer.local", "65535")
        )
    }

    @Test fun acceptsBracketedIpv6AndNormalizesBrackets() {
        assertEquals(
            TcpEndpointValidation.Valid(TcpEndpoint("2001:db8::1", 631)),
            TcpEndpointValidator.parse("[2001:db8::1]", "631")
        )
    }

    @Test fun rejectsEmptyOrMalformedHost() {
        assertTrue(TcpEndpointValidator.parse("", "631") is TcpEndpointValidation.Invalid)
        assertTrue(TcpEndpointValidator.parse("[2001:db8::1", "631") is TcpEndpointValidation.Invalid)
        assertTrue(TcpEndpointValidator.parse("printer.local/path", "631") is TcpEndpointValidation.Invalid)
        assertTrue(TcpEndpointValidator.parse("https://printer.local", "631") is TcpEndpointValidation.Invalid)
    }

    @Test fun rejectsInvalidPorts() {
        assertTrue(TcpEndpointValidator.parse("printer.local", "") is TcpEndpointValidation.Invalid)
        assertTrue(TcpEndpointValidator.parse("printer.local", "abc") is TcpEndpointValidation.Invalid)
        assertTrue(TcpEndpointValidator.parse("printer.local", "0") is TcpEndpointValidation.Invalid)
        assertTrue(TcpEndpointValidator.parse("printer.local", "65536") is TcpEndpointValidation.Invalid)
    }
}
