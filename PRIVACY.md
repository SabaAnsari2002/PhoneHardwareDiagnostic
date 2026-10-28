# Privacy and Data Safety

The implementation stage is offline-first and does not request `INTERNET`, IMEI, IMSI, subscriber identifiers, contact list, SMS or call logs. It does not use analytics or send reports to a server.

Storage is Room in the app's private database, containing device model name, Android version, test outcome, measurements, evidence/source metadata and timestamps. Files created by CameraX capture and app-cache storage verification are deleted after test callbacks/cleanup; microphone PCM is analyzed only in memory, and GPS latitude/longitude are not persisted. On abnormal process termination, Android's app cache may briefly retain an incomplete temporary image; use the application's storage controls to remove cache if needed.

Camera, microphone and precise location permissions are requested for their respective user-started tests. Bluetooth CONNECT is declared for supported capability queries, but the passive scan doesn't request it; restricted state is reported instead. The app's user-triggered export writes a chosen PDF, JSON or CSV to a destination controlled by the user through Android's Storage Access Framework. JSON reports should be treated as potentially personal device metadata if the user chooses to share them.

History can be deleted inside the app. `allowBackup=false` prevents standard Android backup of private diagnostic history. This is source code requiring a full security and platform compliance review before publication.
