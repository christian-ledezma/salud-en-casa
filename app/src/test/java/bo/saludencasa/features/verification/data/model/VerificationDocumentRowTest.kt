package bo.saludencasa.features.verification.data.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VerificationDocumentRowTest {
    @Test
    fun resubmissionSendsClearedReviewFieldsExplicitly() {
        val row =
            VerificationDocumentRow(
                profileId = "299073bc-5ac5-4991-ae6d-a0a8e0c5949b",
                documentType = "ID_FRONT",
                storagePath = "299073bc-5ac5-4991-ae6d-a0a8e0c5949b/ID_FRONT.jpg",
                caption = null,
                status = "PENDING",
                reviewedBy = null,
                reviewedAt = null,
                rejectionReason = null,
            )

        val encoded = Json.encodeToString(VerificationDocumentRow.serializer(), row)
        val payload = Json.parseToJsonElement(encoded).jsonObject

        // An omitted key keeps the old value on an upsert, so a rejection reason
        // would survive the reopening and break the row check (HU-08).
        assertEquals("PENDING", payload.getValue("status").jsonPrimitive.content)
        assertTrue(payload.containsKey("rejection_reason"))
        assertEquals(JsonNull, payload.getValue("rejection_reason"))
        assertEquals(JsonNull, payload.getValue("reviewed_by"))
        assertEquals(JsonNull, payload.getValue("reviewed_at"))
    }
}
