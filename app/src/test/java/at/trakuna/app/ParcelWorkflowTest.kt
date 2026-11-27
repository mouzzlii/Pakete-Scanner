package at.trakuna.app

import org.junit.Assert.*
import org.junit.Test

class ParcelWorkflowTest {
    private val sample = ParcelRecord(
        id = "test-1",
        barcode = "  TEST-123  ",
        carrierId = "dpd",
        status = ParcelStatus.RECEIVED,
        createdAtEpochMillis = 100L,
        updatedAtEpochMillis = 100L
    )

    @Test fun registersAndNormalizesBarcode() {
        val result = ParcelWorkflow.register(emptyList(), sample)
        assertTrue(result is ParcelOperation.Accepted)
        assertEquals("TEST-123", (result as ParcelOperation.Accepted).parcel.barcode)
    }

    @Test fun rejectsDuplicateOpenBarcodeIgnoringCase() {
        val existing = sample.copy(barcode = "TEST-123")
        val result = ParcelWorkflow.register(listOf(existing), sample.copy(id = "test-2"))
        assertTrue(result is ParcelOperation.Rejected)
    }

    @Test fun allowsBarcodeReuseAfterCompletedHandover() {
        val existing = sample.copy(barcode = "TEST-123", status = ParcelStatus.HANDED_OVER)
        assertTrue(ParcelWorkflow.register(listOf(existing), sample.copy(id = "test-2")) is ParcelOperation.Accepted)
    }

    @Test fun requiresStorageLocationBeforeStored() {
        assertTrue(ParcelWorkflow.transition(sample, ParcelStatus.STORED, 110L) is ParcelOperation.Rejected)
        val accepted = ParcelWorkflow.transition(sample, ParcelStatus.STORED, 110L, "Regal A-02")
        assertTrue(accepted is ParcelOperation.Accepted)
        assertEquals("Regal A-02", (accepted as ParcelOperation.Accepted).parcel.storageLocation)
    }

    @Test fun enforcesLinearPickupLifecycle() {
        val stored = (ParcelWorkflow.transition(sample, ParcelStatus.STORED, 110L, "A-02") as ParcelOperation.Accepted).parcel
        val ready = (ParcelWorkflow.transition(stored, ParcelStatus.READY_FOR_PICKUP, 120L) as ParcelOperation.Accepted).parcel
        val handedOver = ParcelWorkflow.transition(ready, ParcelStatus.HANDED_OVER, 130L)
        assertTrue(handedOver is ParcelOperation.Accepted)
        assertTrue(ParcelWorkflow.transition(sample, ParcelStatus.HANDED_OVER, 110L) is ParcelOperation.Rejected)
        assertTrue(ParcelWorkflow.transition((handedOver as ParcelOperation.Accepted).parcel, ParcelStatus.STORED, 140L) is ParcelOperation.Rejected)
    }

    @Test fun supportsSeparateReturnLifecycle() {
        val returned = sample.copy(status = ParcelStatus.RETURN_RECEIVED)
        assertTrue(ParcelWorkflow.transition(returned, ParcelStatus.RETURNED_TO_CARRIER, 110L) is ParcelOperation.Accepted)
        assertTrue(ParcelWorkflow.transition(returned, ParcelStatus.STORED, 110L) is ParcelOperation.Rejected)
    }

    @Test fun rejectsTimeTravelAndMissingRequiredFields() {
        assertTrue(ParcelWorkflow.transition(sample, ParcelStatus.STORED, 99L, "A-02") is ParcelOperation.Rejected)
        assertTrue(ParcelWorkflow.register(emptyList(), sample.copy(carrierId = " ")) is ParcelOperation.Rejected)
    }
}
