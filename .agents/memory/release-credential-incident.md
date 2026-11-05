---
name: Release credential exposure
description: Public-history credential risk that must be resolved before Play uploads.
---

Treat the historical service-account attachment as potentially compromised until the owner confirms revocation of the exposed key. Never open or copy its credential contents.

**Why:** A credential-like service-account attachment was found in Git history and the GitHub repository was public. Removing a file from the current tree does not revoke a Google Cloud key. Revocation was not confirmed during the readiness work.

**How to apply:** Before a future Play upload, obtain confirmation that the exposed key was revoked and the upload credential replaced securely if needed. Preserve the separate Android signing identity; changing an app-signing key is not a substitute for revoking a service-account credential.
