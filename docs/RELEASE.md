# Auralis Release Verification

The current verified release artifact is `artifacts/auralis-4.1.8-release-signed.apk`. It was built from the repository’s minified `release` variant with the pinned Android SDK, NDK `28.2.13676358`, CMake `3.22.1`, taglib, and utfcpp inputs initialized.

Version **4.1.8** restores explicit upstream derivative-work provenance, aligns source headers with the formatter, safely publishes playback listener snapshots, and makes bottom-sheet settings initialization visible and retried when the dependency arrives.

The APK passed `apksigner verify` using APK Signature Schemes v2 and v3. Its certificate SHA-256 fingerprint is:

```text
6386e0eb238b00ba22ce0145257b9167a89de4ee4a9e2449a268c43e53f89f0f
```

The SHA-256 checksum is recorded in [`../artifacts/SHA256SUMS.txt`](../artifacts/SHA256SUMS.txt) and the release-specific sidecar file. The final verified checksum is `7a80f1e96d069dfa78455ff1f1030dc2ff2c12232fb0ae4f6172c81ff05329db`.

The signing key was generated outside the repository only for this verification artifact and was deleted after signing. It is **not** a production update key. Because previous Auralis repair releases were also signed with deleted ephemeral keys, this APK must be installed after uninstalling an older ephemeral-key build. Future seamless installable updates require a securely managed, persistent release key whose fingerprint is published before distribution.

The 4.1.8 release gate completed successfully with the app and `musikr` unit tests, debug lint, `spotlessCheck`, and both debug and minified release builds.
