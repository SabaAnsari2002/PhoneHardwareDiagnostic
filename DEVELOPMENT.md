# Engineering Notes and Next Integrated Stages

## Stage 1 — currently provided

Project bootstrapping, Hilt/Room/DataStore, a registry of 19 passive inventories plus 23 real functional/interactive tests, Compose user paths, SAF reports, local session compare and policy/smoke tests.

No fake production hardware repositories. Unit tests may use deterministic data without becoming a production results source.

## Stage 2 — unimplemented

Extend test registry with capability-aware execution for BLE peripheral connections, NFC non-tag functions, USB host transactions, cellular signal callbacks and extended Wi-Fi stability tests. Integrate per-API permission manager, expected stimulation guidance and hardware resource arbitration. Provide full-session guided workflow with interrupted-session restoration.

## Stage 3 — unimplemented

Camera2 advanced characteristics and reproducible image quality tests, video frame-rate, autofocus/flash/zoom; stereo/earpiece routing and audio loopback; GNSS status callback; sustained/multi-thread CPU sampling and thermal controls; better storage benchmarking. Protect devices with explicit workload caps and resource locks.

## Stage 4 — unimplemented

Versioned JSON import with schema validation and cross-device comparisons, localization of all Compose strings into proper resources, professional PDF layout/RTL support, comprehensive instrumentation/OEM testing, privacy audit and CI Android build matrix.

## Architectural rules

`DiagnosticResult` stores independent `outcome`, `source`, `method`, timestamp, Android API and `Metric.evidence`. `NOT_TESTED` inventory explicitly says operational status is not assessed. A real confirmed user inspection may pass a limited visible/audible check, but it does not imply calibrated quality.

Never infer CPU silicon temperature, device health percentage, battery capacity retention or fingerprint modality solely from an API flag. Thermal and GNSS values can be missing or stale. Result displays must retain warnings and precise origins.

For a new test, add a `TestSpec` to `core/TestCatalog`, implement its real hardware logic in `hardware` or a UI screen, maintain permission/lifecycle cleanup, persist via `ResultRepository`, and add tests. Do not add unimplemented test IDs to the production catalog.
