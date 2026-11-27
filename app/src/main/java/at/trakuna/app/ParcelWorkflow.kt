package at.trakuna.app

/**
 * Provider-neutral parcel lifecycle. No recipient name, address, phone number,
 * or pickup secret is stored in this core model.
 */
internal enum class ParcelStatus {
    RECEIVED,
    STORED,
    READY_FOR_PICKUP,
    HANDED_OVER,
    RETURN_RECEIVED,
    RETURNED_TO_CARRIER
}

internal data class ParcelRecord(
    val id: String,
    val barcode: String,
    val carrierId: String,
    val status: ParcelStatus,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val storageLocation: String? = null
)

internal sealed interface ParcelOperation {
    data class Accepted(val parcel: ParcelRecord) : ParcelOperation
    data class Rejected(val reason: String) : ParcelOperation
}

/** Pure domain rules; persistence and UI are deliberately kept outside. */
internal object ParcelWorkflow {
    fun register(
        existing: List<ParcelRecord>,
        parcel: ParcelRecord
    ): ParcelOperation {
        if (parcel.id.isBlank()) return ParcelOperation.Rejected("Paket-ID fehlt.")
        if (parcel.barcode.isBlank()) return ParcelOperation.Rejected("Barcode fehlt.")
        if (parcel.carrierId.isBlank()) return ParcelOperation.Rejected("Paketdienst fehlt.")
        if (parcel.createdAtEpochMillis <= 0L || parcel.updatedAtEpochMillis < parcel.createdAtEpochMillis) {
            return ParcelOperation.Rejected("Zeitstempel sind ungültig.")
        }
        if (existing.any { it.id == parcel.id }) return ParcelOperation.Rejected("Paket-ID ist bereits vorhanden.")
        if (existing.any { it.barcode.equals(parcel.barcode.trim(), ignoreCase = true) && it.status != ParcelStatus.HANDED_OVER && it.status != ParcelStatus.RETURNED_TO_CARRIER }) {
            return ParcelOperation.Rejected("Dieser Barcode ist bereits als offener Vorgang erfasst.")
        }
        if (parcel.status != ParcelStatus.RECEIVED && parcel.status != ParcelStatus.RETURN_RECEIVED) {
            return ParcelOperation.Rejected("Ein neuer Vorgang muss mit Annahme oder Retoureneingang beginnen.")
        }
        return ParcelOperation.Accepted(parcel.copy(barcode = parcel.barcode.trim()))
    }

    fun transition(
        parcel: ParcelRecord,
        next: ParcelStatus,
        nowEpochMillis: Long,
        storageLocation: String? = parcel.storageLocation
    ): ParcelOperation {
        if (nowEpochMillis < parcel.updatedAtEpochMillis) {
            return ParcelOperation.Rejected("Zeit darf nicht rückwärts laufen.")
        }
        val allowed = when (parcel.status) {
            ParcelStatus.RECEIVED -> setOf(ParcelStatus.STORED)
            ParcelStatus.STORED -> setOf(ParcelStatus.READY_FOR_PICKUP)
            ParcelStatus.READY_FOR_PICKUP -> setOf(ParcelStatus.HANDED_OVER)
            ParcelStatus.RETURN_RECEIVED -> setOf(ParcelStatus.RETURNED_TO_CARRIER)
            ParcelStatus.HANDED_OVER, ParcelStatus.RETURNED_TO_CARRIER -> emptySet()
        }
        if (next !in allowed) return ParcelOperation.Rejected("Unzulässiger Statuswechsel: ${parcel.status} → $next.")
        if (next == ParcelStatus.STORED && storageLocation.isNullOrBlank()) {
            return ParcelOperation.Rejected("Für die Einlagerung fehlt der Lagerplatz.")
        }
        return ParcelOperation.Accepted(
            parcel.copy(status = next, updatedAtEpochMillis = nowEpochMillis, storageLocation = storageLocation?.trim()?.takeIf { it.isNotEmpty() })
        )
    }
}
