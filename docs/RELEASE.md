# Auralis release artifact

The verified release artifact is `artifacts/auralis-release-signed.apk`. It was built from the repository’s minified `release` variant with the pinned Android SDK, NDK `28.2.13676358`, CMake `3.22.1`, taglib, and utfcpp inputs initialized.

The APK passed `apksigner verify` using APK Signature Scheme v2 and v3. Its certificate SHA-256 fingerprint is:

```text
9abd9691f890cf422eb242afcd005742c85bda9051f854bfb28bcc45e89c7588
```

The SHA-256 checksum is recorded in [`../artifacts/SHA256SUMS.txt`](../artifacts/SHA256SUMS.txt). The final verified checksum is `7e560faa077ea41a77a0af9cdc83ca38784f57450fa28188aff5edcfce918d64`. The signing key was generated outside the repository only for this verification artifact and was deleted after signing. It is **not** a production update key; future installable updates must use a securely managed, persistent release key whose fingerprint is published before distribution.

The release gate completed successfully with the app and `musikr` unit tests, debug lint, `spotlessCheck`, and both debug and minified release builds.
