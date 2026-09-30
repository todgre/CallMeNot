package com.callmenot.app.service

import android.telecom.Call
import android.telecom.CallScreeningService
import com.callmenot.app.data.local.entity.CallAction
import com.callmenot.app.data.repository.CallEventRepository
import com.callmenot.app.domain.usecase.CallScreeningDecision
import com.callmenot.app.domain.usecase.EvaluateCallUseCase
import com.callmenot.app.util.ContactsHelper
import com.callmenot.app.util.PhoneNumberUtil
import android.util.Log
import com.callmenot.app.BuildConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@AndroidEntryPoint
class CallMeNotScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "CallMeNotScreening"
        private const val SCREENING_DEADLINE_MS = 3_500L
        // Work outlives the short-lived system-bound service long enough to
        // record the result after Telecom receives its response.
        private val screeningScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @Inject
    lateinit var callEventRepository: CallEventRepository
    
    @Inject
    lateinit var evaluateCallUseCase: EvaluateCallUseCase
    
    @Inject
    lateinit var phoneNumberUtil: PhoneNumberUtil
    
    @Inject
    lateinit var contactsHelper: ContactsHelper

    override fun onScreenCall(callDetails: Call.Details) {
        // A synchronous ContactsProvider lookup cannot be interrupted by
        // coroutine timeout. Run it on IO and stop waiting at the deadline.
        val work = screeningScope.async { processCall(callDetails) }
        val screened = runBlocking {
            try {
                if (BuildConfig.DEBUG) Log.d(TAG, "Processing incoming call")
                withTimeoutOrNull(SCREENING_DEADLINE_MS) { work.await() }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing call, allowing by default", e)
                null
            }
        }
        if (screened == null) work.cancel()
        respondToCall(callDetails, screened?.let { buildResponse(it.decision) } ?: allowCall())
        if (screened != null) {
            screeningScope.launch {
                try {
                    val name = if (!screened.isPrivateNumber && screened.normalizedNumber != null) {
                        contactsHelper.getContactName(screened.normalizedNumber) ?: screened.rawNumber
                    } else "Private Number"
                    logCallEvent(
                        screened.rawNumber, screened.normalizedNumber, name,
                        screened.decision, screened.isPrivateNumber
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Could not record screened call", e)
                }
            }
        }
    }

    private fun allowCall(): CallResponse = CallResponse.Builder()
        .setDisallowCall(false)
        .setSkipCallLog(false)
        .setSkipNotification(false)
        .build()

    private data class ScreenedCall(
        val rawNumber: String?,
        val normalizedNumber: String?,
        val isPrivateNumber: Boolean,
        val decision: CallScreeningDecision
    )

    private suspend fun processCall(callDetails: Call.Details): ScreenedCall {
        val handle = callDetails.handle
        val rawNumber = handle?.schemeSpecificPart
        val isPrivateNumber = rawNumber.isNullOrBlank() || rawNumber == "-1" || rawNumber == "0"
        
        val normalizedNumber = if (!isPrivateNumber && rawNumber != null) {
            phoneNumberUtil.normalize(rawNumber)
        } else {
            null
        }
        
        val decision = evaluateCallUseCase(
            rawNumber = rawNumber,
            normalizedNumber = normalizedNumber,
            isPrivateNumber = isPrivateNumber
        )
        
        return ScreenedCall(rawNumber, normalizedNumber, isPrivateNumber, decision)
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
        if (BuildConfig.DEBUG) Log.d(TAG, "Decision: shouldAllow=${decision.shouldAllow}, reason=${decision.reason}")
        return if (decision.shouldAllow) {
            CallResponse.Builder()
                .setDisallowCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
        } else {
            CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(true)
                .setSkipNotification(true)
                .build()
        }
    }
}
