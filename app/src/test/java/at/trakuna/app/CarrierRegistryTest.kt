package at.trakuna.app

import org.junit.Assert.*
import org.junit.Test

class CarrierRegistryTest {
    @Test fun providerIdsAreUniqueAndStable() {
        val ids = CarrierRegistry.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.containsAll(listOf("dpd", "gls", "hermes", "ups", "dhl", "at-post")))
    }

    @Test fun everyProviderHasAnHttpsEntryPoint() {
        assertTrue(CarrierRegistry.all.all { it.portalUrl.startsWith("https://") })
        assertTrue(CarrierRegistry.all.all { it.trackingPageUrl.startsWith("https://") })
    }

    @Test fun providersCanBeLookedUpWithoutDisplayNameCoupling() {
        assertEquals("DPD Business", CarrierRegistry.byId("dpd")?.displayName)
        assertNull(CarrierRegistry.byId("not-installed"))
    }

    @Test fun legacyPortalUiUsesTheAdapterRegistry() {
        assertEquals(CarrierRegistry.all.map { it.displayName }, CarrierPortals.all.map { it.name })
    }

    @Test fun trackingLinksCarryTheNumberToTheOfficialProviderEndpoint() {
        assertTrue(CarrierRegistry.trackingUrl("dpd", "01234567890123")!!.contains("locale=de_AT&query=01234567890123"))
        assertTrue(CarrierRegistry.trackingUrl("gls", "123456789")!!.contains("match=123456789"))
        assertTrue(CarrierRegistry.trackingUrl("ups", "1Z999AA10123456784")!!.contains("tracknum=1Z999AA10123456784"))
        assertTrue(CarrierRegistry.trackingUrl("dhl", "JD014600006281234567")!!.contains("tracking-id=JD014600006281234567"))
        assertTrue(CarrierRegistry.trackingUrl("at-post", "RR123456789AT")!!.contains("/v2/tnt/sendungssuche?snr=RR123456789AT"))
    }

    @Test fun hermesUsesSafeClipboardFallbackInsteadOfInventedDeepLink() {
        assertNull(CarrierRegistry.trackingUrl("hermes", "1041606105947440140304"))
        assertTrue(CarrierRegistry.trackingPageUrl("hermes")!!.contains("myhermes.at"))
    }

    @Test fun invalidTrackingNumbersAreRejected() {
        assertNull(CarrierRegistry.trackingUrl("dpd", ""))
        assertNull(CarrierRegistry.trackingUrl("dpd", "12345\n&evil=true"))
    }
}
