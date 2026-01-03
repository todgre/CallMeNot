# Whitelist Calls ProGuard Rules

# Keep CallScreeningService
-keep class com.callmenot.app.service.WhitelistCallScreeningService { *; }

# Firebase
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class com.callmenot.app.data.remote.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }

# Google Play Billing
-keep class com.android.vending.billing.**

# libphonenumber
-keep class io.michaelrocks.libphonenumber.android.** { *; }
-keep class com.google.i18n.phonenumbers.** { *; }
