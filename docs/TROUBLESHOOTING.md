# Auralis troubleshooting

## Build setup

Initialize every nested dependency before building:

```bash
git submodule update --init --recursive
```

The Android build requires the SDK platform and build tools declared by the root Gradle configuration, NDK `28.2.13676358`, and a CMake installation on `PATH`. Do not commit `local.properties`; it is machine-specific.

## Empty library

Confirm that Auralis has permission to read local audio and that at least one Music location has been selected. Storage Access Framework locations can be revoked by Android or the document provider; remove and re-add a location if it no longer opens. Audiobook grouping changes the projection only and does not move files.

## Playback and resume

If playback resumes from an unexpected point, stop playback, reopen the book or song, and verify that the current item is still present in the local library. Auralis persists a coherent snapshot of the current queue and position; a file moved or deleted outside the app cannot be restored by the player.

## Crash reports

Include the Auralis version, Android version, device model, exact reproduction steps, and a sanitized logcat or Android bug-report archive. Remove personal file paths, account identifiers, and private media before uploading. For metadata or playback failures, provide a short non-copyrighted sample that reproduces the issue when possible.

## Reporting a defect

Search existing issues first, then open a report in the repository. A good report distinguishes the observed behavior from the expected behavior and identifies whether the failure is in Music, Audiobooks, shared playback, indexing, or persistence.
