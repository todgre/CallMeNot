package com.callmenot.app

import com.callmenot.app.service.isVerifiedEntitlementCacheFresh
import com.callmenot.app.ui.screens.paywall.formatBillingPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionBillingTermsTest {
    @Test
    fun formatsPlayBillingPeriodsWithoutInventingPriceTerms() {
        assertEquals("1 month", formatBillingPeriod("P1M"))
        assertEquals("3 months", formatBillingPeriod("P3M"))
        assertEquals("1 week", formatBillingPeriod("P1W"))
        assertEquals("1 year 2 months", formatBillingPeriod("P1Y2M"))
        assertEquals("invalid", formatBillingPeriod("invalid"))
    }

    @Test
    fun cachedEntitlementIsAcceptedOnlyWithinThe24HourVerificationWindow() {
        val verifiedAt = 10_000L
        assertTrue(isVerifiedEntitlementCacheFresh(verifiedAt, verifiedAt))
        assertTrue(isVerifiedEntitlementCacheFresh(verifiedAt, verifiedAt + 86_399_999L))
        assertFalse(isVerifiedEntitlementCacheFresh(verifiedAt, verifiedAt + 86_400_000L))
        assertFalse(isVerifiedEntitlementCacheFresh(verifiedAt, verifiedAt - 1L))
        assertFalse(isVerifiedEntitlementCacheFresh(0L, verifiedAt))
    }
}