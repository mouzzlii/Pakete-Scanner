package at.trakuna.app

/**
 * Extension point for carrier-specific integrations.
 * A carrier adapter owns its identity and official entry point; later API,
 * tracking, label and booking capabilities can be added without changing the
 * provider-neutral parcel lifecycle.
 */
internal interface CarrierAdapter {
    val id: String
    val displayName: String
    val portalUrl: String
}

private data class PortalCarrierAdapter(
    override val id: String,
    override val displayName: String,
    override val portalUrl: String
) : CarrierAdapter

internal object CarrierRegistry {
    /** Add new providers here or register implementations from future modules. */
    val all: List<CarrierAdapter> = listOf(
        PortalCarrierAdapter("dpd", "DPD Business", "https://business.dpd.at/"),
        PortalCarrierAdapter("gls", "GLS / YourGLS", "https://gls-group.com/authenticate?allowed_locales=en-AT%2Cde-AT&locale=de-AT"),
        PortalCarrierAdapter("hermes", "Hermes Geschäftskundenbereich", "https://www.myhermes.at/geschaeftskunden/uebersicht/"),
        PortalCarrierAdapter("ups", "UPS Business / CampusShip", "https://www.ups.com/at/de/business-solutions/business-shipping-tools/ups-campusship"),
        PortalCarrierAdapter("dhl", "DHL Geschäftskunden", "https://www.dhl.com/at-de/home/login.html"),
        PortalCarrierAdapter("at-post", "Österreichische Post / ELLA", "https://ella.post.at/")
    )

    fun byId(id: String): CarrierAdapter? = all.firstOrNull { it.id == id }
    fun byDisplayName(name: String): CarrierAdapter? = all.firstOrNull { it.displayName == name }
}

/** Compatibility view used by the current portal-selection UI. */
internal data class CarrierPortal(val name: String, val url: String)

internal object CarrierPortals {
    val all: List<CarrierPortal>
        get() = CarrierRegistry.all.map { CarrierPortal(it.displayName, it.portalUrl) }
}
