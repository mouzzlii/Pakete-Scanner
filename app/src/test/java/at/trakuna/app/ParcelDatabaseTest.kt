package at.trakuna.app

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ParcelDatabaseTest {
    private lateinit var context: Context
    private lateinit var database: ParcelDatabase

    private val sample = ParcelRecord(
        id = "db-test-1",
        barcode = "TEST-DB-123",
        carrierId = "dpd",
        status = ParcelStatus.RECEIVED,
        createdAtEpochMillis = 1_000L,
        updatedAtEpochMillis = 1_000L
    )

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("trakuna-parcels.db")
        database = ParcelDatabase(context)
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase("trakuna-parcels.db")
    }

    @Test
    fun persistsRecordsAfterDatabaseReopen() {
        assertTrue(database.register(sample) is ParcelStoreResult.Saved)
        database.close()

        database = ParcelDatabase(context)
        val stored = database.all()
        assertEquals(1, stored.size)
        assertEquals(sample.id, stored.single().id)
        assertEquals(sample.barcode, stored.single().barcode)
        assertEquals(ParcelStatus.RECEIVED, stored.single().status)
    }

    @Test
    fun rejectsDuplicateOpenBarcodeIgnoringCase() {
        assertTrue(database.register(sample) is ParcelStoreResult.Saved)
        val duplicate = sample.copy(id = "db-test-2", barcode = "test-db-123")
        assertTrue(database.register(duplicate) is ParcelStoreResult.Rejected)
        assertEquals(1, database.all().size)
    }

    @Test
    fun allowsBarcodeReuseAfterCompletedHandover() {
        assertTrue(database.register(sample) is ParcelStoreResult.Saved)
        assertTrue(database.updateStatus(sample.id, ParcelStatus.STORED, 1_100L, "A-02") is ParcelStoreResult.Saved)
        assertTrue(database.updateStatus(sample.id, ParcelStatus.READY_FOR_PICKUP, 1_200L) is ParcelStoreResult.Saved)
        assertTrue(database.updateStatus(sample.id, ParcelStatus.HANDED_OVER, 1_300L) is ParcelStoreResult.Saved)

        val nextParcel = sample.copy(id = "db-test-2", barcode = "test-db-123")
        assertTrue(database.register(nextParcel) is ParcelStoreResult.Saved)
        assertEquals(2, database.all().size)
    }

    @Test
    fun rejectsInvalidTransitionWithoutChangingStoredRecord() {
        assertTrue(database.register(sample) is ParcelStoreResult.Saved)

        val result = database.updateStatus(sample.id, ParcelStatus.HANDED_OVER, 1_100L)
        assertTrue(result is ParcelStoreResult.Rejected)
        assertEquals(ParcelStatus.RECEIVED, database.all().single().status)
    }

    @Test
    fun requiresStorageLocationBeforeStoring() {
        assertTrue(database.register(sample) is ParcelStoreResult.Saved)

        val result = database.updateStatus(sample.id, ParcelStatus.STORED, 1_100L)
        assertTrue(result is ParcelStoreResult.Rejected)
        assertEquals(ParcelStatus.RECEIVED, database.all().single().status)
    }

    @Test
    fun rejectsUnknownRecordId() {
        val result = database.updateStatus("missing", ParcelStatus.STORED, 1_100L, "A-02")
        assertTrue(result is ParcelStoreResult.Rejected)
    }
}
