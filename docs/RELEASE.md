# Auralis Release Verification

The current verified release artifact is `artifacts/auralis-4.1.7-release-signed.apk`. It was built from the repository’s minified `release` variant with the pinned Android SDK, NDK `28.2.13676358`, CMake `3.22.1`, taglib, and utfcpp inputs initialized.

Version **4.1.7** repairs Music queue isolation, Music/Audiobooks catalog separation, local MP3 embedded chapter selection, the crashing chapter dialog, audiobook control clarity, the launcher icon, and the About/settings presentation.

The APK passed `apksigner verify` using APK Signature Schemes v2 and v3. Its certificate SHA-256 fingerprint is:

```text
5dc2d1127a8e5b38d48669999e0d79c1ec563194508115a19dc1c8f64c488779
```

The SHA-256 checksum is recorded in [`../artifacts/SHA256SUMS.txt`](../artifacts/SHA256SUMS.txt) and the release-specific sidecar file. The final verified checksum is `2c82dd32b63e89dc04605cb6fba25e2f8f985efd7783e1db0aabb7ba635ccde3`.

The signing key was generated outside the repository only for this verification artifact and was deleted after signing. It is **not** a production update key. Because the previous 4.1.6 release was also signed with a deleted ephemeral key, this APK must be installed after uninstalling the prior Auralis package. Future installable updates must use a securely managed, persistent release key whose fingerprint is published before distribution.

The 4.1.7 release gate completed successfully with the app and `musikr` unit tests, debug lint, `spotlessCheck`, and both debug and minified release builds.
