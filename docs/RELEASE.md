# Auralis Release Process

This document describes how to cut a verified Auralis release. The build is pure
Kotlin/Java — no NDK, CMake, submodules, or native checkouts are required. Any machine
with JDK 21 and the Android SDK (platform 36, build-tools) can reproduce the artifacts.

## Preconditions

- Working tree clean on the release commit.
- `local.properties` (if present) points at a valid SDK; it is never committed.
- A persistent release signing key stored **outside** the repository. Ephemeral keys
  break seamless updates: every past ephemeral-key build had to be uninstalled before
  the next could be installed. Publish the release certificate SHA-256 fingerprint
  before distributing the APK.

## Release gate

Run the full gate from the repository root:

```bash
./gradlew spotlessCheck
./gradlew musikr:testDebug app:testDebug
./gradlew app:assembleRelease
```

All three steps must pass: formatting, the `musikr` and app unit-test suites
(playback-domain isolation, snapshot isolation, audiobook organization, chapters,
bookmarks, lifecycle, ReplayGain parsing), and the minified `release` build with
R8 and resource shrinking enabled.

## Signing and verification

1. Sign the unsigned release APK with `apksigner` and the persistent release key.
2. Verify with `apksigner verify --print-certs`.
3. Record the APK SHA-256 checksum in the release notes.

Static verification does not replace a physical-device review: launcher masks,
themed icons, large-font layouts, TalkBack traversal, permission flows, library
scan, Music and Audiobooks playback, background playback, the widget, explicit
domain switching, and upgrade installs over the previous release.
