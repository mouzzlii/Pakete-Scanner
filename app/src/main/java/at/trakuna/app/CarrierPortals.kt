package at.trakuna.app

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Carrier-specific official entry points. Tracking links are built only for
 * providers whose public tracking URL accepts a parcel-number parameter.
 */
internal interface CarrierAdapter {
    val id: String
    val displayName: String
    val portalUrl: String
    val trackingPageUrl: String
}

private data class PortalCarrierAdapter(
    override val id: String,
    override val displayName: String,
    override val portalUrl: String,
    override val trackingPageUrl: String
) : CarrierAdapter

internal object CarrierRegistry {
    val all: List<CarrierAdapter> = listOf(
        PortalCarrierAdapter("dpd", "DPD Business", "https://business.dpd.at/", "https://www.dpd.com/at/de/empfangen/paketverfolgung/"),
        PortalCarrierAdapter("gls", "GLS / YourGLS", "https://gls-group.com/authenticate?allowed_locales=en-AT%2Cde-AT&locale=de-AT", "https://gls-group.com/AT/de/paket-verfolgen/"),
        PortalCarrierAdapter("hermes", "Hermes Geschäftskundenbereich", "https://www.myhermes.at/geschaeftskunden/uebersicht/", "https://www.myhermes.at/sendungsverfolgung/"),
        PortalCarrierAdapter("ups", "UPS Business / CampusShip", "https://www.ups.com/at/de/business-solutions/business-shipping-tools/ups-campusship", "https://www.ups.com/track?loc=de_AT"),
        PortalCarrierAdapter("dhl", "DHL Geschäftskunden", "https://www.dhl.com/at-de/home/login.html", "https://www.dhl.com/at-de/home/tracking.html"),
        PortalCarrierAdapter("at-post", "Österreichische Post / ELLA", "https://ella.post.at/", "https://www.post.at/v2/tnt/sendungssuche")
    )

    fun byId(id: String): CarrierAdapter? = all.firstOrNull { it.id == id }
    fun byDisplayName(name: String): CarrierAdapter? = all.firstOrNull { it.displayName == name }
    fun trackingPageUrl(id: String): String? = byId(id)?.trackingPageUrl

    fun trackingUrl(id: String, trackingNumber: String): String? {
        val number = trackingNumber.trim()
        if (number.length !in 5..60 || number.any { it.isISOControl() }) return null
        val encoded = URLEncoder.encode(number, StandardCharsets.UTF_8.name()).replace("+", "%20")
        return when (id) {
            "dpd" -> "https://tracking.dpd.de/parcelstatus?locale=de_AT&query=$encoded"
            "gls" -> "https://gls-group.com/AT/de/paket-verfolgen/?match=$encoded"
            "ups" -> "https://www.ups.com/track?loc=de_AT&tracknum=$encoded&requester=WT%2Ftrackdetails"
            "dhl" -> "https://www.dhl.com/at-de/home/tracking.html?submit=1&tracking-id=$encoded"
            "at-post" -> "https://www.post.at/v2/tnt/sendungssuche?snr=$encoded"
            // Hermes Austria has no verified public deep-link parameter. Use
            // the official form and copy the number rather than guessing a URL.
            else -> null
        }
    }
}

/** Compatibility view used by the current portal-selection UI. */
internal data class CarrierPortal(val name: String, val url: String)

internal object CarrierPortals {
    val all: List<CarrierPortal>
        get() = CarrierRegistry.all.map { CarrierPortal(it.displayName, it.portalUrl) }
}
