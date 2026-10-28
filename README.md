# Phone Hardware Diagnostic — Android Studio Source (Implementation Stage 1)

**Status:** Actual Kotlin/Jetpack Compose application source. **Not a production release or verified APK.** The project is the first integrated implementation stage from the provided specification. No hardware results are simulated in production. A physical-device build and verification have **not** been performed in the generation environment.

## Build

- Install Android Studio with **Android SDK 37**, SDK Build Tools 36+, **JDK 17 or 21**, and network access for dependencies.
- Open this folder as an Android Studio project, synchronize Gradle, then use `:app:assembleDebug` (or `./gradlew :app:assembleDebug`).
- Versions: Android Gradle Plugin 9.4.1, Gradle 9.6.0, Kotlin 2.4.21, Compose BOM 2026.09.00, Room 2.8.5, Hilt 2.60.1, CameraX 1.6.2.
- Minimum API 23 (Android 6), compile and target API 37 (Android 17).
- **Gradle wrapper notice:** The distribution bootstrap bundled here is a small, auditable Java JAR **rather than the official Gradle wrapper JAR** because official distribution files could not be downloaded during project generation. It pins the SHA-256 hash of Gradle 9.6.0 before extraction. After successfully running Gradle, replace it with the official wrapper by running `./gradlew wrapper --gradle-version 9.6.0` twice. Android Studio's Gradle integration may also regenerate an official wrapper. See `gradle/wrapper/bootstrap-src`.

## Implemented stage

- Material 3 Compose navigation, dark/light themes, dashboard and test catalog; baseline English/Persian string resources.
- Real passive inventory for **19 categories**, including Build/PackageManager, battery, display, camera characteristics, audio routes, every app-visible SensorManager sensor, location provider, Wi-Fi, Bluetooth, cellular/SIM, NFC/IR/USB, CPU, RAM, storage, biometrics, physical interfaces, security, and thermal/network status.
- Real functional tests: app-cache write/read SHA-256 verification, bounded process-buffer integrity, short single-coroutine CPU work, accelerometer/light/proximity event arrival, 16-kHz microphone PCM acquisition with RMS and dBFS, GPS fix attempt, touch grid, screen color patterns, CameraX preview + JPEG capture, quiet playback with user audibility confirmation, vibration confirmation, torch control, physical volume event observation, and genuine BiometricPrompt authentication; charging status transitions, local Wi-Fi observation, BLE advertisement reception, NFC technology reading, and USB host attachment observation.
- Live battery sampling graphs and live SensorManager explorer for available sensors, including vendor sensors and on-shot trigger sensors.
- Hilt injection, Room local sessions/results, DataStore dark-mode setting.
- Local JSON schema v1, escaped CSV, and paginated PDF export via Android's Storage Access Framework; rudimentary local report comparison and deletion.
- Bounded operations, cancellation and cleanup on screen disposal for major sensor/camera/audio resources; runtime permissions when initiating microphone, camera or GPS tests.
- No network requests in the production app. No personal device identifiers are requested. GPS exact coordinates and captured microphone PCM are not persisted.

## Limitations — not yet implemented or not fully verified

This is an **integrated stage**, **not** the 100% complete 19-module professional product described in the source prompt. It should not be marketed or relied on as a certified device-health diagnostic application.

- Missing full workflows: guided multi-step *Full Diagnostic* with resume/skip/retry policy; all individual camera video/AF/zoom/flash quality algorithms; full audio stereo/earpiece/loopback tests; GNSS satellite C/N0; Wi-Fi scan and link-stability studies; BLE peripheral connection/connection-stability; NFC non-tag modes; controlled IR transmitter and USB host transaction; cellular service/signal details; battery charge/discharge workload measurements; multi-threaded/sustained CPU benchmarks; small-file throughput comparisons; calibrated quality assessment; detailed device comparison + JSON import; optional internet throughput tests.
- Not all API 23–37 permissions/target-37 behavior or OEM edge cases have been exercised on real devices. Protected and inaccessible information stays unavailable; it is not invented.
- English and Persian string resource files exist, but several Compose screen labels and technical explanations remain English-only and full RTL QA is outstanding.
- CameraX capture does not prove physical lens quality. A real sensor event proves delivery, not calibration. `PASSED` in functional tests is constrained to the method listed in each result. Device-wide health percentages are deliberately omitted.
- The report PDF renderer uses a compact single-column layout and needs additional typography/RTL refinement.
- Automated tests include pure policy tests and a smoke instrumentation test, but no full hardware farm validation.

## Structure

```
app/src/main/java/com/phonehardware/diagnostic/
  core/     Diagnostic models, result evidence and policy rules
  hardware/ On-device passive reads, bounded active tests, report exports
  data/     Room entities, DAOs, JSON/CSV serialization, preferences
  di/       Hilt database provision
  ui/       Compose app, navigation, ViewModel, history, comparison, theme
    screens/ Hardware interaction screens and real live charts
```

## Result semantics

1. **Presence** is discovered from official feature/sensor/camera APIs. This is not a PASS.
2. **Availability** may be blocked by permissions, other apps, disabled state, or policy.
3. **Functionality** is confirmed only when the declared test method provides evidence.
4. **Performance** includes units and limited test conditions.
5. **Condition** is not inferred from weak measurements. Battery capacity health, NAND wear, CPU temperature and fingerprint modality are not invented.

`NOT_TESTED`, `NOT_DETECTED`, `INCONCLUSIVE`, `PERMISSION_REQUIRED` and `UNAVAILABLE` are never silently converted into `FAILED`.

## Developers

Run `./gradlew testDebugUnitTest`, `./gradlew connectedDebugAndroidTest` (device attached), then manually follow `TESTING.md` before publishing. Dependencies may need update if APIs or AndroidX compatibility change. No debug signing key is shipped; Android Studio debug builds use their normal generated debug key.

For privacy and data retention see `PRIVACY.md`, and for staged roadmap and engineering notes see `DEVELOPMENT.md`.
