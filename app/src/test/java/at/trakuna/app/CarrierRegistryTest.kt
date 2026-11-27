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
    }

    @Test fun providersCanBeLookedUpWithoutDisplayNameCoupling() {
        assertEquals("DPD Business", CarrierRegistry.byId("dpd")?.displayName)
        assertNull(CarrierRegistry.byId("not-installed"))
    }

    @Test fun legacyPortalUiUsesTheAdapterRegistry() {
        assertEquals(CarrierRegistry.all.map { it.displayName }, CarrierPortals.all.map { it.name })
    }
}
