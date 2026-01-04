package com.callmenot.app.service

import android.telecom.Call
import android.telecom.CallScreeningService
import com.callmenot.app.data.local.entity.CallAction
import com.callmenot.app.data.local.entity.CallReason
import com.callmenot.app.data.repository.CallEventRepository
import com.callmenot.app.data.repository.SettingsRepository
import com.callmenot.app.data.repository.WhitelistRepository
import com.callmenot.app.domain.usecase.CallScreeningDecision
import com.callmenot.app.domain.usecase.EvaluateCallUseCase
import com.callmenot.app.util.ContactsHelper
import com.callmenot.app.util.PhoneNumberUtil
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class CallMeNotScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "CallMeNotScreening"
    }

    @Inject
    lateinit var whitelistRepository: WhitelistRepository
    
    @Inject
    lateinit var callEventRepository: CallEventRepository
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    @Inject
    lateinit var billingManager: BillingManager
    
    @Inject
    lateinit var evaluateCallUseCase: EvaluateCallUseCase
    
    @Inject
    lateinit var phoneNumberUtil: PhoneNumberUtil
    
    @Inject
    lateinit var contactsHelper: ContactsHelper

    override fun onScreenCall(callDetails: Call.Details) {
        val response = runBlocking {
            try {
                val phoneNumber = callDetails.handle?.schemeSpecificPart
                Log.d(TAG, "Processing incoming call: $phoneNumber")
                processCall(callDetails)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing call, allowing by default", e)
                CallResponse.Builder()
                    .setDisallowCall(false)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                    .build()
            }
        }
        respondToCall(callDetails, response)
    }

    private suspend fun processCall(callDetails: Call.Details): CallResponse {
        val handle = callDetails.handle
        val rawNumber = handle?.schemeSpecificPart
        val isPrivateNumber = rawNumber.isNullOrBlank() || rawNumber == "-1" || rawNumber == "0"
        
        val normalizedNumber = if (!isPrivateNumber && rawNumber != null) {
            phoneNumberUtil.normalize(rawNumber)
        } else {
            null
        }
        
        val displayName = if (!isPrivateNumber && normalizedNumber != null) {
            contactsHelper.getContactName(normalizedNumber) ?: rawNumber
        } else {
            "Private Number"
        }
        
        val decision = evaluateCallUseCase(
            rawNumber = rawNumber,
            normalizedNumber = normalizedNumber,
            isPrivateNumber = isPrivateNumber
        )
        
        logCallEvent(
            phoneNumber = rawNumber,
            normalizedNumber = normalizedNumber,
            displayName = displayName,
            decision = decision,
            isPrivateNumber = isPrivateNumber
        )
        
        return buildResponse(decision)
    }

    private suspend fun logCallEvent(
        phoneNumber: String?,
        normalizedNumber: String?,
        displayName: String?,
        decision: CallScreeningDecision,
        isPrivateNumber: Boolean
    ) {
        val action = if (decision.shouldAllow) CallAction.ALLOWED else CallAction.BLOCKED
        
        callEventRepository.logCallEvent(
            phoneNumber = phoneNumber,
            normalizedNumber = normalizedNumber,
            displayName = displayName,
            action = action,
            reason = decision.reason,
            matchedWhitelistId = decision.matchedWhitelistId,
            isPrivateNumber = isPrivateNumber
        )
    }

    private fun buildResponse(decision: CallScreeningDecision): CallResponse {
        Log.d(TAG, "Building response: shouldAllow=${decision.shouldAllow}, reason=${decision.reason}")
        return if (decision.shouldAllow) {
            Log.d(TAG, "ALLOWING call")
            CallResponse.Builder()
                .setDisallowCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
        } else {
            Log.d(TAG, "BLOCKING call")
            CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(true)
                .setSkipNotification(true)
                .build()
        }
    }
}
