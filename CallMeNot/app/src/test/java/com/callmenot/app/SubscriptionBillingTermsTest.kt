package com.callmenot.app

import com.callmenot.app.service.isVerifiedEntitlementCacheFresh
import com.callmenot.app.service.isSubscriptionEntitlementUsable
import com.callmenot.app.service.SubscriptionStatus
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
    fun cachedEntitlementIsAcceptedOnlyWithinTheSevenDayGraceWindow() {
        val verifiedAt = 10_000L
        assertTrue(isVerifiedEntitlementCacheFresh(verifiedAt, verifiedAt))
        assertTrue(isVerifiedEntitlementCacheFresh(verifiedAt, verifiedAt + 604_799_999L))
        assertFalse(isVerifiedEntitlementCacheFresh(verifiedAt, verifiedAt + 604_800_000L))
        assertFalse(isVerifiedEntitlementCacheFresh(verifiedAt, verifiedAt - 1L))
        assertFalse(isVerifiedEntitlementCacheFresh(0L, verifiedAt))
    }

    @Test
    fun activeSubscriptionCannotOutliveVerificationDuringProlongedPlayOutage() {
        val verifiedAt = 10_000L
        val active = SubscriptionStatus.Active(isYearly = true, purchaseTime = 1L)
        assertTrue(isSubscriptionEntitlementUsable(active, verifiedAt, verifiedAt))
        // A failed query/disconnect may leave the last observed status Active.
        assertTrue(isSubscriptionEntitlementUsable(active, verifiedAt, verifiedAt + 604_799_999L))
        assertFalse(isSubscriptionEntitlementUsable(active, verifiedAt, verifiedAt + 604_800_000L))
        assertFalse(isSubscriptionEntitlementUsable(active, verifiedAt, verifiedAt + 700_000_000L))
        val error = SubscriptionStatus.Error("Play unavailable")
        assertTrue(isSubscriptionEntitlementUsable(error, verifiedAt, verifiedAt + 1L))
        assertFalse(isSubscriptionEntitlementUsable(error, verifiedAt, verifiedAt + 604_800_000L))
        assertFalse(isSubscriptionEntitlementUsable(SubscriptionStatus.NotSubscribed, verifiedAt, verifiedAt))
        assertFalse(isSubscriptionEntitlementUsable(SubscriptionStatus.Pending, verifiedAt, verifiedAt))
    }
}