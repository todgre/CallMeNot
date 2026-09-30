# CallMeNot for Android

CallMeNot is a native Android call-screening app. It keeps whitelist and settings data locally on the device. This repository is under development and is **not a claim of production or Play Store readiness**.

## Current app behavior

- Uses Android's `CallScreeningService` role to apply local allow/block rules to cellular calls. Call screening depends on Android and device behavior; it is not a default dialer and does not screen calls from VoIP apps.
- Whitelist entries can be added manually, from contacts, or from recent calls. Rules include starred contacts, recent outgoing calls, unknown/private numbers, an emergency repeat-call bypass, and an optional schedule.
- Cloud sync is disabled in this version. Firebase sign-in and Firestore sync are not available; app data does not migrate between devices through the app.
- The app has a **local 7-day trial**. Google Play subscriptions use product IDs `callmenot_monthly` and `callmenot_yearly`. Prices and subscription offers are provided by Google Play, not hardcoded in the app.

## Requirements and local setup

- Android Studio with JDK 17 (or JDK 17 and Android SDK/build tools installed)
- Android 10 (API 29) or later; use a physical Android phone to test call screening

Open the `CallMeNot` directory in Android Studio and allow Gradle to sync. The project uses the `com.callmenot.app` application ID and includes its Google Services configuration; no Firebase project, login, or Firestore setup is needed for the currently disabled cloud features.

Build a debug APK from the `CallMeNot` directory:

```bash
./gradlew testDebugUnitTest assembleDebug
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`. Install it with Android Studio or:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

On first launch, follow onboarding and grant the Android call-screening role and requested permissions. Verify behavior on the intended devices; do not rely on emulator behavior or assume identical call UI, notifications, call-log handling, or background behavior across Android versions and manufacturers.

## Play Billing setup and testing

For real billing tests, create/configure the app in Google Play Console and create subscription products with these exact product IDs:

- `callmenot_monthly`
- `callmenot_yearly`

Configure availability, pricing, and any Play subscription offers in Play Console. The app displays the product details and pricing returned by Play. Add license testers and install a build through a Play testing track to test Play Billing; an APK installed directly with `adb` is useful for local app testing but is not a substitute for Play billing tests.

## GitHub Actions builds

`.github/workflows/android-build.yml` runs unit tests and compiles the debug app on pushes and pull requests to `main`. It does **not** automatically publish a release or upload a debug APK artifact.

Manually dispatch the workflow to build a signed release AAB and save it as a workflow artifact. Configure these GitHub Actions secrets for that step:

- `KEYSTORE_BASE64` — base64-encoded release keystore
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

The AAB is uploaded to Google Play's **Alpha** track only when dispatching the workflow with the `upload_alpha` input explicitly set to `true`. That upload also requires:

- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` — service-account JSON contents with the required Play Console access

Protect signing and service-account credentials as secrets; never commit them to the repository.

### Credential incident

A service-account key file was previously present in public Git history. Treat that credential as compromised: revoke/delete the exposed key in Google Cloud IAM, create and securely distribute a replacement if still needed, and update or remove the `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` GitHub secret accordingly. Removing a file from the current tree or rewriting history does **not** revoke an exposed key.

## Release considerations

Before any public release, validate call screening and billing on supported devices and Play test tracks, review permissions and Play policy requirements, and provide the required privacy policy and subscription terms. Device/OEM call behavior can vary; no manufacturer-specific behavior is guaranteed. This repository does not establish that these release, policy, or device-validation steps have been completed.