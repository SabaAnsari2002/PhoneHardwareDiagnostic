# Test Plan and Manual Device Checklist

**As of 2026-10-28:** No physical Android handset or Android SDK/Gradle installation was available to the generation environment. **No APK compilation or functional device test has been run.** Checks below describe what must be verified, not what has already passed.

## Build and setup

- [ ] Gradle sync and `:app:assembleDebug` on JDK 17/21 with SDK 37.
- [ ] `:app:testDebugUnitTest` and `:app:connectedDebugAndroidTest`.
- [ ] Install on an API 23 phone and one API 31+/37 phone.
- [ ] Rotation, background, resume, process termination and repeated test execution.
- [ ] TalkBack readable test labels, landscape, split-window, Persian RTL and dark/light themes.

## Device diversity

- [ ] Samsung Galaxy (One UI), Google Pixel, Xiaomi / Redmi / POCO, OnePlus, Motorola.
- [ ] Phone missing NFC, IR, barometer, gyroscope and fingerprint hardware.
- [ ] Phone with many physical lenses not individually exposed through Camera2.
- [ ] Phone with Android biometrics enrolled for fingerprint and face; never claim which modality succeeded.

## Passive scan

- [ ] Compare Build and system feature list to Android-reported facts.
- [ ] Inspect every SensorManager.TYPE_ALL entry and vendor sensor metadata.
- [ ] Confirm missing optional features are never failed tests.
- [ ] Confirm restricted camera and Bluetooth access is reported as restricted/permission needed.
- [ ] Verify no camera, mic, GNSS acquisition or biometric prompt occurs during Quick Scan.
- [ ] Verify battery current sentinel values are not shown as valid samples.

## Functional tests

- [ ] Storage: 1 MiB checksum match, no temp file remains; insufficient space handled.
- [ ] RAM: limited pattern round-trip, no aggressive allocations.
- [ ] CPU: single 1.5 s bounded workload, stop and severe thermal behavior.
- [ ] Sensors: event stream, no default accelerometer, denied body sensor, one-shot sensors, lifecycle unregister.
- [ ] Display: inspect every solid color, checkerboard, gradient and confirm visually.
- [ ] Touch: activate edge/corner cells; incomplete coverage stays inconclusive.
- [ ] Microphone: grant/deny/revoke RECORD_AUDIO; silent room and voiced sample; no permanent PCM file.
- [ ] Speaker: check bounded playback, routing, quiet-volume warning and user confirmation.
- [ ] Camera: grant/deny CAMERA; front/rear preview, photo capture, camera busy and screen leave.
- [ ] GPS: outdoors/indoors, provider disabled, denied fine location, 24-second timeout; no stored coordinates.
- [ ] Vibration: present/absent motor and actual tactile confirmation.
- [ ] Torch: with/without flash, camera resource conflict and teardown.
- [ ] Volume keys: both KeyEvents observed; power key remains manually assessable only.
- [ ] Biometrics: successful strong biometric, no enrollment, cancellation, lockout and repeated prompt.
- [ ] Battery trend: monitor on battery and charger; stop screen and verify lifecycle sampling.

## Persistence / exports

- [ ] Close/reopen app and verify Room session history.
- [ ] Delete one session and all sessions; cascade dependent rows.
- [ ] SAF export to user-selected PDF, JSON, CSV destination.
- [ ] Validate JSON schema and CSV quoted values, including commas/newlines.
- [ ] Read long multipage PDFs, check text clipping and Persian locale limitations.
- [ ] Compare same-device and different-device session data; missing tests remain missing.
- [ ] Inspect manifest and confirm no INTERNET permission, analytics, background service, contact/SMS, or sensitive IDs.

## Interpretation scenarios

- [ ] No GPS fix indoors should be INCONCLUSIVE, not FAILED.
- [ ] No sensor detected should be NOT_DETECTED, not FAILED.
- [ ] Biometric enrollment absent should not mean fingerprint hardware failure.
- [ ] Speaker "not heard" is user-confirmed with routing/volume warning, not an SPL result.
- [ ] Battery Android health code must not be called an exact capacity-retention percentage.
- [ ] Storage integrity success must not be described as NAND health proof.
