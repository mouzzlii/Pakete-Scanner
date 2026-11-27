package at.trakuna.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper

internal sealed interface ParcelStoreResult {
    data class Saved(val parcel: ParcelRecord) : ParcelStoreResult
    data class Rejected(val reason: String) : ParcelStoreResult
}

/**
 * Device-local parcel store. It deliberately has no network or cloud component.
 * The database stores only the minimal fields of ParcelRecord.
 */
internal class ParcelDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE parcels (
                id TEXT PRIMARY KEY NOT NULL,
                barcode TEXT NOT NULL,
                carrier_id TEXT NOT NULL,
                status TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                storage_location TEXT
            )""".trimIndent()
        )
        db.execSQL(
            """CREATE UNIQUE INDEX idx_open_parcel_barcode
                ON parcels(barcode COLLATE NOCASE)
                WHERE status NOT IN ('HANDED_OVER', 'RETURNED_TO_CARRIER')""".trimIndent()
        )
        db.execSQL("CREATE INDEX idx_parcels_updated ON parcels(updated_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Fail closed: never silently open a database whose schema was not migrated.
        throw SQLiteException("Keine Datenbankmigration von Version $oldVersion auf $newVersion implementiert.")
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Downgrades can discard or misinterpret user data; require an explicit migration.
        throw SQLiteException("Datenbank-Downgrade von Version $oldVersion auf $newVersion ist nicht erlaubt.")
    }

    fun all(): List<ParcelRecord> {
        val records = mutableListOf<ParcelRecord>()
        readableDatabase.query(
            "parcels",
            arrayOf("id", "barcode", "carrier_id", "status", "created_at", "updated_at", "storage_location"),
            null, null, null, null, "updated_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val status = runCatching { ParcelStatus.valueOf(cursor.getString(3)) }.getOrNull() ?: continue
                records += ParcelRecord(
                    id = cursor.getString(0),
                    barcode = cursor.getString(1),
                    carrierId = cursor.getString(2),
                    status = status,
                    createdAtEpochMillis = cursor.getLong(4),
                    updatedAtEpochMillis = cursor.getLong(5),
                    storageLocation = cursor.getString(6)
                )
            }
        }
        return records
    }

    @Synchronized
    fun register(parcel: ParcelRecord): ParcelStoreResult {
        val validation = ParcelWorkflow.register(all(), parcel)
        if (validation is ParcelOperation.Rejected) return ParcelStoreResult.Rejected(validation.reason)
        val accepted = (validation as ParcelOperation.Accepted).parcel
        return try {
            writableDatabase.insertOrThrow("parcels", null, accepted.toContentValues())
            ParcelStoreResult.Saved(accepted)
        } catch (_: SQLiteConstraintException) {
            ParcelStoreResult.Rejected("Dieser Barcode oder diese Vorgangs-ID ist bereits offen vorhanden.")
        } catch (_: SQLiteException) {
            ParcelStoreResult.Rejected("Die lokale Datenbank konnte den Vorgang nicht speichern.")
        }
    }

    @Synchronized
    fun updateStatus(id: String, next: ParcelStatus, now: Long, location: String? = null): ParcelStoreResult {
        val current = all().firstOrNull { it.id == id }
            ?: return ParcelStoreResult.Rejected("Paketvorgang nicht gefunden.")
        val validation = ParcelWorkflow.transition(current, next, now, location ?: current.storageLocation)
        if (validation is ParcelOperation.Rejected) return ParcelStoreResult.Rejected(validation.reason)
        val updated = (validation as ParcelOperation.Accepted).parcel
        return try {
            // Compare-and-set prevents a second database instance from overwriting a
            // status change made after this method read the current record.
            val changed = writableDatabase.update(
                "parcels",
                updated.toContentValues(),
                "id = ? AND status = ? AND updated_at = ?",
                arrayOf(id, current.status.name, current.updatedAtEpochMillis.toString())
            )
            if (changed == 1) ParcelStoreResult.Saved(updated)
            else ParcelStoreResult.Rejected("Der Vorgang wurde zwischenzeitlich geändert. Bitte Liste aktualisieren.")
        } catch (_: SQLiteConstraintException) {
            ParcelStoreResult.Rejected("Die Änderung verletzt eine Datenbankregel.")
        } catch (_: SQLiteException) {
            ParcelStoreResult.Rejected("Die lokale Datenbank konnte den Vorgang nicht aktualisieren.")
        }
    }

    private fun ParcelRecord.toContentValues() = ContentValues().apply {
        put("id", id)
        put("barcode", barcode)
        put("carrier_id", carrierId)
        put("status", status.name)
        put("created_at", createdAtEpochMillis)
        put("updated_at", updatedAtEpochMillis)
        put("storage_location", storageLocation)
    }

    private companion object {
        const val DATABASE_NAME = "trakuna-parcels.db"
        const val DATABASE_VERSION = 1
    }
}
