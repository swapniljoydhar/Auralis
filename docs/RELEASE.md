# Auralis Release Verification

The current verified release artifact is `artifacts/auralis-4.1.9-release-signed.apk`. It was built from the repository’s minified `release` variant with the pinned Android SDK, NDK `28.2.13676358`, CMake `3.22.1`, taglib, and utfcpp inputs initialized.

Version **4.1.9** hardens local-library crash paths, streams provider artwork through Android’s background pipe helper, makes the audiobook chapter picker view-lifecycle-bound, removes unsafe metadata/index assumptions, and refreshes the Music and Audiobooks now-playing hierarchy with separate transport emphasis.

The APK passed `apksigner verify` using APK Signature Schemes v2 and v3. Its certificate SHA-256 fingerprint is:

```text
6c07b277c87a00d8399b560890222a4e15f384a044f0d8be9c4a3a6b36463cec
```

The SHA-256 checksum is recorded in [`../artifacts/SHA256SUMS.txt`](../artifacts/SHA256SUMS.txt) and the release-specific sidecar file. The final verified checksum is `2c4c065083c42b2894cc1f6f6a9b7cb56787aef5cbaec410f8dadba9be5c5dde`.

The signing key was generated outside the repository only for this verification artifact and was deleted after signing. It is **not** a production update key. Because previous Auralis repair releases were also signed with deleted ephemeral keys, this APK must be installed after uninstalling an older ephemeral-key build. Future seamless installable updates require a securely managed, persistent release key whose fingerprint is published before distribution.

The 4.1.9 release gate completed successfully with the app and `musikr` unit tests, debug lint, `spotlessCheck`, and both debug and minified release builds.
