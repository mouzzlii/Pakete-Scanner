package at.trakuna.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticLogTest {
    @Test
    fun masksIpv4BluetoothAddressesAndLongNumericCodes() {
        val safe = DiagnosticLog.redact(
            "Drucker 192.168.1.50; Bluetooth AA:BB:CC:DD:EE:FF; Paket 123456789012"
        )
        assertTrue(safe.contains("[IP]"))
        assertTrue(safe.contains("[BT-ADRESSE]"))
        assertTrue(safe.contains("[ZAHLENCODE]"))
        assertFalse(safe.contains("192.168.1.50"))
        assertFalse(safe.contains("AA:BB:CC:DD:EE:FF"))
        assertFalse(safe.contains("123456789012"))
    }

    @Test
    fun keepsUsefulShortDiagnosticText() {
        val safe = DiagnosticLog.redact("Bluetooth-Verbindung fehlgeschlagen (Timeout).")
        assertEquals("Bluetooth-Verbindung fehlgeschlagen (Timeout).", safe)
    }
}
