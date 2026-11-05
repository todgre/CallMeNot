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

- **Language**: Kotlin 2.1
- **UI**: Jetpack Compose with Material3
- **Architecture**: MVVM + Repository pattern
- **DI**: Hilt
- **Database**: Room
- **Settings**: DataStore
- **Auth/Cloud Sync**: Firebase SDKs retained, but no sign-in or active sync flow
- **Billing**: Google Play Billing Library 8
- **Min SDK**: 29 (Android 10+)
- **Target SDK**: 36

## Key Features

1. **CallScreeningService**: Intercepts incoming calls at OS level
2. **Whitelist Management**: Add/remove numbers, import from contacts
3. **Rules Engine**: Allow starred contacts, emergency bypass, recent outgoing
4. **Local data**: Cloud sync is disabled; existing cloud records are untouched
5. **Subscription**: Local 7-day trial, then Play-priced monthly/yearly plans

## How to Build

### Option 1: GitHub Actions (Recommended - No Local Setup)

1. Push this code to a GitHub repository
2. Pushes and pull requests test/debug-build and validate an unsigned minified release bundle
3. Manually dispatch the workflow for a signed AAB; Alpha upload is an explicit option, default off

### Option 2: Local Android Studio

1. Download the `CallMeNot` folder
2. Open in Android Studio
3. Keep the existing Android Google Services configuration
4. Sync Gradle
5. Run on physical device (API 29+)

## Configuration Required

Before building for production:

1. **Google Play Billing**: Verify the existing subscription products, base plans, offers and license testers
2. **Signing**: Preserve the existing release upload key and GitHub Actions secrets
3. **Security**: Confirm revocation of the service-account key previously found in public Git history before Play uploads
4. **External readiness**: See `RELEASE_READINESS.md` for evidence and outstanding console/device checks

## Current Runtime Behavior

- Android binds the screening service through the call-screening role. Legacy foreground notification, boot startup and Samsung fallback services are not registered in the manifest.
- Onboarding persists trial start before navigating; a missing start date does not activate a trial.
- A previous call within the configured emergency window allows a repeat caller unless an earlier rule blocks them.
- Billing initializes at application startup. A successful Play query confirms entitlement; transient failures do not mean a confirmed absence. Cached confirmation has a bounded seven-day offline grace period.
- Screening stops waiting for decision work after 3.5 seconds and fails open. Activity logging follows the response.

## Current State

- Code and CI checks are not proof of production approval
- No Firebase account is needed for local call screening
- Subscriptions need periodic Play verification; the local trial is not server-enforced
- Requires real device testing for call screening functionality
- Unit tests included for rules engine logic
