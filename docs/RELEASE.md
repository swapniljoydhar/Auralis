# Auralis release artifact

The current verified release artifact is `artifacts/auralis-4.1.6-release-signed.apk`. It was built from the repository’s minified `release` variant with the pinned Android SDK, NDK `28.2.13676358`, CMake `3.22.1`, taglib, and utfcpp inputs initialized.

The APK passed `apksigner verify` using APK Signature Scheme v2 and v3. Its certificate SHA-256 fingerprint is:

```text
fed72d7711a79519b1cde48e894da7df633a50733ef5adc54afa7711529d7d87
```

The SHA-256 checksum is recorded in [`../artifacts/SHA256SUMS.txt`](../artifacts/SHA256SUMS.txt) and the release-specific sidecar file. The final verified checksum is `1e7c852a4721daa0ffd7bbe7a1375106c0ac427a78339dc28ea26920048e39ff`. The signing key was generated outside the repository only for this verification artifact and was deleted after signing. It is **not** a production update key; future installable updates must use a securely managed, persistent release key whose fingerprint is published before distribution.

The 4.1.6 release gate completed successfully with the app and `musikr` unit tests, debug lint, `spotlessCheck`, and both debug and minified release builds.
