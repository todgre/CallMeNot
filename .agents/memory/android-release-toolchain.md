---
name: Android release toolchain upgrades
description: Compatibility rules for modernizing this native Android release pipeline.
---

When raising the Android API level requires a newer Android Gradle Plugin, upgrade Kotlin-aware build processors as a compatible set rather than forcing metadata overrides. Keep full Gradle stack traces enabled in CI release builds.

**Why:** Native release builds run in GitHub Actions, and old billing, dependency-injection, and database processors can fail sequentially as newer Kotlin metadata and bytecode instrumentation are introduced. Full stack traces distinguish the responsible processor from the top-level Gradle task.

**How to apply:** For future target-SDK or Kotlin upgrades, check Billing, Hilt, Room, Compose compiler, AGP, and Gradle compatibility together. Treat a successful signed-bundle build separately from Play upload authorization.