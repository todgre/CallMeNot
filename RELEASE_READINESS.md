# CallMeNot release readiness

Review date: November 5, 2026.

**Status: code hardening completed; public production release is not approved or verified.**
Version prepared: **1.0.7 (version code 9)**. No new Play upload or production promotion was performed in this work.

## Evidence

| Area | Verified result | Remaining limit |
| --- | --- | --- |
| Android CI | [Latest unit tests (including Active-state offline expiry) and debug APK compilation passed](https://github.com/todgre/CallMeNot/actions/runs/42071001481); [earlier routine unsigned-release validation passed](https://github.com/todgre/CallMeNot/actions/runs/36676226236) | Compilation does not prove behavior on a phone |
| Signed bundle | [Latest signed minified AAB built successfully; artifact saved; Play upload skipped](https://github.com/todgre/CallMeNot/actions/runs/42071001481) | A successful build is not a successful Play review or device test |
| Call screening | Decision work runs on IO with a 3.5-second wait deadline; fail-open response on failure; logging after response; trial starts before onboarding navigation | Real cellular calls, reboot/app-closed behavior, OEMs, permission revocation, and carrier voicemail are not device-verified |
| Background startup | Billing starts with the Application; unsupported foreground-service/boot/Samsung fallback startup paths are no longer registered | A retained screening role is required; Android binds the screening service, not a permanent foreground service |
| Billing UI | Play-provided price/offer phases, asynchronous restore, pending state, acknowledgement failure reporting, complete-inventory reconciliation for pending plan changes | Live products/base plans/offers, restore/renewal/refund/expiry and real purchase callbacks are unverified |
| Offline entitlement | Last successful local Play confirmation has a seven-day grace period; successful confirmation of no subscription clears it | Not server-validated; device-clock or rooted-device tampering is not prevented; after grace expires without verification, screening fails open |
| Trial | Seven days from persisted on-device onboarding start; no trial when start date is missing | Not a server-enforced or Play-offer trial |
| Cloud features | No sign-in flow; legacy Firestore APIs disabled to prevent accidental overwrite/deletion; local data preserved | Existing Firebase accounts/records and live rules were not inspected |
| Public policies | [Privacy policy](https://todgre.github.io/CallMeNot/privacy-policy.html) and [deletion information](https://todgre.github.io/CallMeNot/delete-data.html) respond successfully; privacy page visually checked | Owner must verify legal accuracy, contact mailbox and handling of legacy deletion requests |
| Release automation | Tests and unsigned release validation on pushes/PRs; signed AAB on manual dispatch; Alpha upload explicitly opt-in; no automatic production promotion | Owner should protect the main branch and review permissions before releases |
| Release signing | Existing upload-keystore secrets retained; credential-download generation workflow removed | Never replace the existing app-signing identity casually |
| Credential exposure | A credential-like service-account attachment appeared in public Git history; values were not opened or copied | Treat as compromised until the owner confirms the exposed key is revoked; deleting source files does not revoke keys |
| Google Play | An earlier 1.0.6 Alpha upload succeeded; a Google Play connection was offered and deferred | Current tester count, qualification period, production access, listing, declarations, geographic availability, billing catalog and warnings are **unknown** |

## Required owner actions outside Replit

1. **Revoke the exposed service-account key before any Play upload.** In Google Cloud Console, select the relevant project, open **IAM & Admin → Service Accounts → the Play upload account → Keys**, identify and revoke the exposed key. If that key is still used, securely replace **GitHub → repository Settings → Secrets and variables → Actions → GOOGLE_PLAY_SERVICE_ACCOUNT_JSON**. Do not paste credentials into chat or source control. Do not rotate the app-signing key as a substitute.
2. **Complete the trader declaration and legal review.** In Play Console, use **Manage account → Account details → About you → Trader status**, then **CallMeNot → Policy and programs → App content**. Declare accurately, review the public contact details, and approve/provide a Terms of Service URL. Google's current declaration deadline is February 1, 2027: [official guidance](https://support.google.com/googleplay/android-developer/answer/17594752?hl=en).
3. **Verify current Play launch settings when you return to the console.** Check the closed-test eligibility shown on the dashboard rather than assuming prior tester screenshots still qualify. Verify `callmenot_monthly` / `callmenot_yearly`, active base plans/offers, license testers, target countries, store copy/screenshots, call-log permission eligibility, Data Safety, app access, content rating and the two public policy URLs. Do not advertise cloud sync.
4. **Run the device acceptance pass.** On a Play-installed build, check permitted/blocked/private/repeat callers, contacts and blacklist precedence, screening after app closure/reboot, denied/revoked permissions, schedules, and purchases/restore/pending/cancellation/renewal/expiry. Include at least a Pixel-class device and a Samsung device if both are supported. Record results; emulated rules tests are not a substitute.
5. **Check legacy Firebase and support operations.** In Firebase Console inspect **Authentication**, **Firestore data**, and **Firestore rules** for prior users/records and inappropriate access. Confirm the privacy contact mailbox works and that legitimate deletion requests can be processed. Do not delete records indiscriminately.
6. **Approve the production release only after these gates pass.** Review Google's pre-launch report and policy warnings, complete any production-access application, then authorize the final staged rollout. Google review/approval is not an agent-controlled step.

The Google Play console review can resume later with authorized access; no obligation to connect was inferred from dismissing the connection card.
