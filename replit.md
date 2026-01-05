# CallMeNot - Native Android Project

## Project Overview

This is a **native Android application** written in Kotlin with Jetpack Compose. It implements a whitelist-only call screening system using Android's CallScreeningService API.

**Important**: This project is designed as a code repository that must be built externally using Android Studio or GitHub Actions. Replit does not have Android SDK or Gradle tooling to compile this code.

## Project Structure

```
CallMeNot/
├── app/
│   ├── build.gradle.kts          # App module build config
│   ├── proguard-rules.pro        # ProGuard rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/callmenot/app/
│       │   │   ├── CallMeNotApp.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── di/           # Hilt DI modules
│       │   │   ├── data/         # Database, repositories
│       │   │   ├── domain/       # Use cases
│       │   │   ├── service/      # CallScreeningService, BillingManager
│       │   │   ├── ui/           # Compose screens
│       │   │   └── util/         # Helpers
│       │   └── res/              # Android resources
│       └── test/                 # Unit tests
├── .github/workflows/            # GitHub Actions for automated builds
├── build.gradle.kts              # Root build config
├── settings.gradle.kts           # Gradle settings
└── README.md                     # Setup instructions
```

## Tech Stack

- **Language**: Kotlin 1.9+
- **UI**: Jetpack Compose with Material3
- **Architecture**: MVVM + Repository pattern
- **DI**: Hilt
- **Database**: Room
- **Settings**: DataStore
- **Auth**: Firebase Phone Authentication
- **Cloud Sync**: Firestore
- **Billing**: Google Play Billing Library 6.1
- **Min SDK**: 29 (Android 10+)
- **Target SDK**: 34

## Key Features

1. **CallScreeningService**: Intercepts incoming calls at OS level
2. **Whitelist Management**: Add/remove numbers, import from contacts
3. **Rules Engine**: Allow starred contacts, emergency bypass, recent outgoing
4. **Cloud Sync**: Firebase-based whitelist sync for device migration
5. **Subscription**: 7-day trial, then $2.99/mo or $19.99/yr

## How to Build

### Option 1: GitHub Actions (Recommended - No Local Setup)

1. Push this code to a GitHub repository
2. GitHub Actions will automatically build the APK
3. Download from Actions → Artifacts

### Option 2: Local Android Studio

1. Download the `CallMeNot` folder
2. Open in Android Studio
3. Add `google-services.json` from Firebase Console
4. Sync Gradle
5. Run on physical device (API 29+)

## Configuration Required

Before building for production:

1. **Firebase**: Create project, enable Phone Auth, create Firestore database
2. **Google Play Billing**: Set up subscription products in Play Console
3. **Signing**: Create release keystore for Play Store distribution

## Recent Changes (January 2026)

- **Notification Bug Fix**: Service now properly starts/stops based on protection toggle, checks saved state on device boot
- **Call Log Filtering**: Added time period filter (Today, 1 Week, 1 Month, 1 Year, All Time) with dropdown UI
- **Optional Firebase**: App works fully locally without account; cloud sync is optional for backing up settings and syncing whitelist across devices
- **What's New Dialog**: Shows changelog after app updates, tracks last seen version in DataStore
- **App Icon**: Clean shield with checkmark design representing call protection

## Current State

- All core code is generated and complete
- Requires Firebase configuration (google-services.json) for cloud sync only
- App works fully offline without Firebase account
- Requires real device testing for call screening functionality
- Unit tests included for rules engine logic
