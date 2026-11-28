package at.trakuna.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CarrierDetectorTest {
    @Test fun recognizesConfirmedHermesExampleFormat() {
        assertEquals("Hermes Geschäftskundenbereich", CarrierDetector.detect("1041606105947440140304"))
    }

    @Test fun recognizesDpdPinFormat() {
        assertEquals("DPD Business", CarrierDetector.detect("P123456789012"))
    }

    @Test fun recognizesFourteenDigitDpdParcelNumber() {
        assertEquals("DPD Business", CarrierDetector.detect("01234567890123"))
    }

    @Test fun recognizesDpdNumberWithScannerSymbologyPrefix() {
        assertEquals("DPD Business", CarrierDetector.detect("]C101234567890123"))
        assertEquals("01234567890123", CarrierDetector.normalize("]C1 01234567890123"))
    }

    @Test fun recognizesUnambiguousUps1zFormat() {
        assertEquals("UPS Business / CampusShip", CarrierDetector.detect("1Z999AA10123456784"))
    }

    @Test fun doesNotGuessAmbiguousThirteenDigit872Code() {
        assertNull(CarrierDetector.detect("8721335813708"))
    }

    @Test fun doesNotGuessGenericNumericOrS10Codes() {
        assertNull(CarrierDetector.detect("123456789012"))
        assertNull(CarrierDetector.detect("AB123456789CD"))
    }

    @Test fun explicitCarrierTextTakesPrecedence() {
        assertEquals("GLS / YourGLS", CarrierDetector.detect("GLS 8721335813708"))
    }
}
