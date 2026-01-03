# CallMeNot - Android Call Screening App

A production-ready Android app that enforces whitelist-only call protection. If an incoming call is not allowed by user rules, it is silently rejected (sent to voicemail). Allowed callers behave normally.

## Features

- **CallScreeningService Integration**: Uses Android's native call screening API for true call interception
- **Whitelist Management**: Add contacts from picker, manual entry, or recent calls
- **Smart Rules Engine**:
  - Allow starred/favorite contacts
  - Allow recent outgoing calls (last 7 days)
  - Emergency bypass (2 calls within 3 minutes)
  - Block unknown/private numbers
- **Cloud Sync**: Firebase authentication with phone number OTP, Firestore sync for device migration
- **Subscription System**: 7-day free trial, then $2.99/month or $19.99/year via Google Play Billing
- **Schedule Support**: Enable blocking only during specific hours

## Requirements

- Android 10+ (API 29+) - Required for CallScreeningService
- Physical Android device for testing (emulator has limited call screening support)
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17

## Project Setup

### 1. Clone/Download the Project

Download this entire `CallMeNot` folder to your local machine.

### 2. Open in Android Studio

1. Open Android Studio
2. File → Open → Select the `CallMeNot` folder
3. Wait for Gradle sync to complete (may take a few minutes first time)

### 3. Configure Firebase

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project (or use existing)
3. Add an Android app:
   - Package name: `com.callmenot.app`
   - Download `google-services.json`
4. Place `google-services.json` in the `app/` folder
5. Enable **Phone Authentication**:
   - Firebase Console → Authentication → Sign-in method → Phone → Enable
6. Create **Firestore Database**:
   - Firebase Console → Firestore Database → Create database → Start in production mode
   - Deploy these security rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### 4. Configure Google Play Billing (Optional for Development)

For subscription testing, you need:

1. A Google Play Developer account ($25 one-time fee)
2. Create an app in Play Console
3. Set up subscription products:
   - Product ID: `whitelist_calls_monthly` - $2.99/month
   - Product ID: `whitelist_calls_yearly` - $19.99/year
4. Add test accounts in Play Console for internal testing

**For initial development**, billing will show loading/error states but won't block app functionality during trial.

### 5. Build and Run

#### Debug Build (Recommended for Testing)

```bash
./gradlew assembleDebug
```

The APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

#### Install on Device

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or use Android Studio's Run button with a connected device.

## Testing Call Screening

### On a Physical Device

1. Install the app
2. Complete onboarding (grant Call Screening role)
3. Add at least one contact to whitelist
4. Have someone call your phone:
   - **Whitelisted number**: Phone rings normally
   - **Non-whitelisted number**: Phone should NOT ring, call goes to voicemail

### Test Mode (In-App)

The app includes a Test Mode in Settings → Test Mode where you can:
- Enter any phone number
- See whether it would be blocked and why
- No actual call needed

## GitHub Actions (Automated Builds)

This project includes automated build workflows. To use:

1. Push code to GitHub repository
2. Add these secrets in GitHub → Settings → Secrets:
   - `GOOGLE_SERVICES_JSON`: Contents of your google-services.json file
   - `BASE64_KEYSTORE`: Base64-encoded release keystore (for signed builds)
   - `STORE_PASSWORD`: Keystore password
   - `KEY_ALIAS`: Key alias name
   - `KEY_PASSWORD`: Key password

3. Every push to `main` triggers a build
4. Download APK from Actions → Artifacts

## Architecture

```
com.callmenot.app/
├── di/                     # Hilt dependency injection
├── data/
│   ├── local/
│   │   ├── entity/         # Room entities (WhitelistEntry, CallEvent)
│   │   ├── dao/            # Room DAOs
│   │   └── WhitelistDatabase.kt
│   ├── remote/             # Firestore sync
│   └── repository/         # Repositories
├── domain/
│   └── usecase/            # Business logic (EvaluateCallUseCase)
├── service/
│   ├── CallMeNotScreeningService.kt  # Core call screening
│   ├── BillingManager.kt   # Google Play Billing
│   └── BootReceiver.kt
├── ui/
│   ├── theme/              # Compose theming
│   ├── navigation/         # Navigation setup
│   └── screens/            # Compose screens
│       ├── onboarding/
│       ├── home/
│       ├── whitelist/
│       ├── activity/
│       ├── settings/
│       └── paywall/
└── util/                   # Helpers (PhoneNumberUtil, PermissionHelper, etc.)
```

## Call Screening Logic

```kotlin
// Simplified decision flow
fun evaluateCall(number: String, isPrivate: Boolean): Decision {
    if (!subscriptionActive && !trialActive) return ALLOW  // Don't block without subscription
    if (!masterToggleEnabled) return ALLOW
    if (scheduleEnabled && !withinSchedule) return ALLOW
    if (isPrivate && blockUnknownEnabled) return BLOCK
    if (isWhitelisted(number)) return ALLOW
    if (allowStarred && isStarredContact(number)) return ALLOW
    if (emergencyBypass && hasRecentCall(number, 3.minutes)) return ALLOW
    if (allowRecentOutgoing && hasOutgoingCall(number, 7.days)) return ALLOW
    return BLOCK
}
```

## Known Limitations

### Android Version Differences

| Feature | Android 10-12 | Android 13+ |
|---------|---------------|-------------|
| Silent blocking | ✅ Full support | ✅ Full support |
| Skip notification | ✅ | ✅ |
| Skip call log | ✅ | ✅ |
| Prevent full-screen UI | ⚠️ Best-effort | ✅ Better support |

### OEM Variations

- **Samsung One UI**: May show brief notification before blocking. Battery optimization must be disabled.
- **Xiaomi MIUI**: Requires enabling "Autostart" and disabling battery optimization.
- **Huawei EMUI**: Similar to Xiaomi, aggressive battery management may interfere.
- **Google Pixel**: Best compatibility, all features work as expected.

### General Limitations

- **Requires physical device**: Call screening can't be fully tested on emulator
- **Not default dialer**: We use CallScreeningService role, not default dialer role
- **VoIP calls**: May not intercept calls from VoIP apps (WhatsApp, Telegram, etc.)
- **Dual SIM**: Both SIMs are handled, but behavior may vary by OEM

## Play Store Compliance

Before publishing:

1. **Privacy Policy**: Required for apps that access contacts/call log
2. **Sensitive Permissions Declaration**: Explain why you need READ_CONTACTS, READ_CALL_LOG
3. **Call Screening Role**: Google may require additional verification
4. **Subscription Terms**: Include clear pricing, trial info, cancellation policy

## Support

For issues or questions:
1. Check GitHub Issues
2. Review Android call screening documentation
3. Test on multiple devices to identify OEM-specific issues

## License

This project is proprietary. All rights reserved.
