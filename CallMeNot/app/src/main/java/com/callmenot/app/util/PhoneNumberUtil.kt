package com.callmenot.app.util

import android.content.Context
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil as LibPhoneNumberUtil
import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil.PhoneNumberFormat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneNumberUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val phoneNumberUtil: LibPhoneNumberUtil by lazy {
        LibPhoneNumberUtil.createInstance(context)
    }
    
    private val defaultCountryCode: String by lazy {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        tm?.simCountryIso?.uppercase() ?: tm?.networkCountryIso?.uppercase() ?: "US"
    }
    
    fun normalize(phoneNumber: String): String {
        return try {
            val parsed = phoneNumberUtil.parse(phoneNumber, defaultCountryCode)
            phoneNumberUtil.format(parsed, PhoneNumberFormat.E164)
        } catch (e: NumberParseException) {
            phoneNumber.filter { it.isDigit() || it == '+' }
        }
    }
    
    fun format(phoneNumber: String): String {
        return try {
            val parsed = phoneNumberUtil.parse(phoneNumber, defaultCountryCode)
            phoneNumberUtil.format(parsed, PhoneNumberFormat.NATIONAL)
        } catch (e: NumberParseException) {
            phoneNumber
        }
    }
    
    fun isValidNumber(phoneNumber: String): Boolean {
        val digitsOnly = phoneNumber.filter { it.isDigit() }
        if (digitsOnly.length >= 7) {
            return true
        }
        return try {
            val parsed = phoneNumberUtil.parse(phoneNumber, defaultCountryCode)
            phoneNumberUtil.isValidNumber(parsed)
        } catch (e: NumberParseException) {
            false
        }
    }
    
    fun getCountryCode(): String = defaultCountryCode
    
    fun normalizeNumber(phoneNumber: String): String = normalize(phoneNumber)
    
    fun numbersMatch(number1: String, number2: String): Boolean {
        val normalized1 = normalize(number1)
        val normalized2 = normalize(number2)
        return normalized1 == normalized2
    }
}
