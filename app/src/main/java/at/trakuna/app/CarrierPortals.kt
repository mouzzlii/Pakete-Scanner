package at.trakuna.app

internal data class CarrierPortal(val name: String, val url: String)

/**
 * External provider entry points. Authentication and all provider-side actions
 * remain in the provider's own browser session.
 */
internal object CarrierPortals {
    val all = listOf(
        CarrierPortal("DPD Business", "https://business.dpd.at/"),
        CarrierPortal("GLS / YourGLS", "https://gls-group.com/authenticate?allowed_locales=en-AT%2Cde-AT&locale=de-AT"),
        CarrierPortal("Hermes Geschäftskundenbereich", "https://www.myhermes.at/geschaeftskunden/uebersicht/"),
        CarrierPortal("UPS Business / CampusShip", "https://www.ups.com/at/de/business-solutions/business-shipping-tools/ups-campusship"),
        CarrierPortal("DHL Geschäftskunden", "https://www.dhl.com/at-de/home/login.html"),
        CarrierPortal("Österreichische Post / ELLA", "https://ella.post.at/")
    )
}
