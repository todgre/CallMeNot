package com.whitelistcalls.app.util

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val phoneNumberUtil: PhoneNumberUtil
) {
    private val contentResolver: ContentResolver get() = context.contentResolver
    
    fun getContactName(phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )
            
            if (cursor?.moveToFirst() == true) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    return cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            // Permission denied or other error
        } finally {
            cursor?.close()
        }
        
        return null
    }
    
    fun isStarredContact(phoneNumber: String): Boolean {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.STARRED),
                null,
                null,
                null
            )
            
            if (cursor?.moveToFirst() == true) {
                val starredIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.STARRED)
                if (starredIndex >= 0) {
                    return cursor.getInt(starredIndex) == 1
                }
            }
        } catch (e: Exception) {
            // Permission denied or other error
        } finally {
            cursor?.close()
        }
        
        return false
    }
    
    fun hasRecentOutgoingCall(phoneNumber: String, withinDays: Int): Boolean {
        val normalizedNumber = phoneNumberUtil.normalize(phoneNumber)
        val cutoffTime = System.currentTimeMillis() - (withinDays * 24 * 60 * 60 * 1000L)
        
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE),
                "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.DATE} > ?",
                arrayOf(CallLog.Calls.OUTGOING_TYPE.toString(), cutoffTime.toString()),
                null
            )
            
            cursor?.let {
                while (it.moveToNext()) {
                    val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                    if (numberIndex >= 0) {
                        val callNumber = it.getString(numberIndex)
                        val normalizedCallNumber = phoneNumberUtil.normalize(callNumber)
                        if (normalizedCallNumber == normalizedNumber) {
                            return true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Permission denied or other error
        } finally {
            cursor?.close()
        }
        
        return false
    }
    
    data class Contact(
        val id: String,
        val name: String,
        val phoneNumbers: List<String>,
        val isStarred: Boolean
    )
    
    fun getAllContacts(): List<Contact> {
        val contacts = mutableMapOf<String, Contact>()
        
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.STARRED
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            
            cursor?.let {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val starredIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
                
                while (it.moveToNext()) {
                    val id = it.getString(idIndex) ?: continue
                    val name = it.getString(nameIndex) ?: continue
                    val number = it.getString(numberIndex) ?: continue
                    val isStarred = it.getInt(starredIndex) == 1
                    
                    val existing = contacts[id]
                    if (existing != null) {
                        contacts[id] = existing.copy(
                            phoneNumbers = existing.phoneNumbers + number
                        )
                    } else {
                        contacts[id] = Contact(
                            id = id,
                            name = name,
                            phoneNumbers = listOf(number),
                            isStarred = isStarred
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Permission denied or other error
        } finally {
            cursor?.close()
        }
        
        return contacts.values.toList()
    }
}
