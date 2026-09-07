# Auralis Troubleshooting

## Build Setup

Auralis builds with standard Gradle and the Android SDK:

```bash
# Build the application
gradle :app:assembleDebug

# Run unit test suites
gradle :app:testDebugUnitTest :musikr:test
```

The Android build requires the standard Android SDK platform and build tools. NDK or CMake installations are not required as metadata extraction utilizes high-performance platform media APIs.

## Library Indexing

- **Grant Storage Permission**: Ensure Auralis is granted storage/audio permissions to read local media files.
- **Select Library Folders**: In Settings, verify that your music and/or audiobook directories are selected.
- **Scoped Storage**: Storage Access Framework (SAF) folder permissions can occasionally be revoked by Android when storage volumes are unmounted; if a folder stops loading, simply re-select it in Settings.
- **Audiobook Folders**: Verify whether folder organization is set to separate folders or unified directory in Audiobook Settings.

## Playback and Resume

- **Accurate Position Restore**: Auralis continuously saves audiobook positions in an ACID Room database. If a file is moved, renamed, or deleted outside of Auralis, the player will safely report the missing track without crashing.
- **Sleep Timer Shake-to-Reset**: Shake gesture sensitivity is calibrated for gentle bed/nightstand motion. Ensure the "Shake to extend sleep timer" toggle is enabled in Audiobook Settings.

## Defect Reports

When opening an issue or contributing:
1. Provide device manufacturer, model, and Android OS version.
2. Clearly distinguish whether the issue affects the Music domain, Audiobook domain, or the shared media service.
3. Include relevant sanitized logcat output.
