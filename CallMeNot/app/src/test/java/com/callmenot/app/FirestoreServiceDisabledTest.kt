package com.callmenot.app

import com.callmenot.app.data.local.entity.WhitelistEntry
import com.callmenot.app.data.remote.FirestoreService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class FirestoreServiceDisabledTest {

    private val service = FirestoreService()

    @Test
    fun `cloud operations stay disabled and do not expose cloud data`() = runBlocking {
        val entry = WhitelistEntry(
            id = "local-entry",
            displayName = "Local",
            phoneNumber = "+15555550100",
            normalizedNumber = "+15555550100"
        )

        assertFalse(service.isAvailable)
        assertFalse(service.syncWhitelist("user", listOf(entry)))
        assertEquals(emptyList<WhitelistEntry>(), service.getWhitelist("user"))
        assertNull(service.getSettings("user"))

        // These legacy APIs must remain harmless: they must not issue cloud deletes/writes.
        service.deleteWhitelistEntry("user", entry.id)
        service.syncSettings("user", mapOf("enabled" to true))
    }
}