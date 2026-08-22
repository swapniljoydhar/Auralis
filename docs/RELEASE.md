# Auralis Release Verification

The current verified release artifact is `artifacts/auralis-4.1.12-release-signed.apk`. It was built from the repository’s minified `release` variant with the pinned Android SDK, NDK `28.2.13676358`, CMake `3.22.1`, taglib, and utfcpp inputs initialized.

Version **4.1.12** completes the local single-MP3 ID3 `CHAP` sleep path. When a listener selects “sleep at end of chapter,” Auralis reads the embedded chapter boundaries, schedules the nearest future boundary, recalculates after a seek or pause/resume transition, and pauses only if that same Audiobooks-domain MP3 is still active at the boundary. The release adds focused policy tests for Audiobooks-to-Music timer isolation and out-of-order embedded chapter offsets.

The APK passed `apksigner verify` using APK Signature Schemes v2 and v3. Its certificate SHA-256 fingerprint is:

```text
ec4610ee86906a5b71a8b842627b64383670e59a132210a6f77367da7aa4e7ce
```

The SHA-256 checksum is recorded in [`../artifacts/SHA256SUMS.txt`](../artifacts/SHA256SUMS.txt) and the release-specific sidecar file. The final verified checksum is `2aefaa5748de4da1bc434c56995dc1e5f4bbe5a4d5ec0bf4b33e083eba49b2a8`.

The signing key was generated outside the repository only for this verification artifact and was deleted after signing. It is **not** a production update key. Because previous Auralis repair releases were also signed with deleted ephemeral keys, this APK must be installed after uninstalling an older ephemeral-key build. Future seamless installable updates require a securely managed, persistent release key whose fingerprint is published before distribution.

The 4.1.12 release gate completed successfully with the app and `musikr` unit tests, debug lint, `spotlessCheck`, and both debug and minified release builds.
