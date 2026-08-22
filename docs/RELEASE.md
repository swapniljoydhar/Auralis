# Auralis Release Verification

The current verified release artifact is `artifacts/auralis-4.1.11-release-signed.apk`. It was built from the repository’s minified `release` variant with the pinned Android SDK, NDK `28.2.13676358`, CMake `3.22.1`, taglib, and utfcpp inputs initialized.

Version **4.1.11** corrects the explicit PlaybackDomain path for auto-discovered long-form local MP3 audiobooks. The service now carries the queue domain through progress persistence, auto-rewind, silence-skipping, and restored speed rather than falling back to filename/tag classification. Sleep timers now belong to their audiobook domain and session, cancel on replacement playback, pause on real automatic chapter-file transitions for the chapter-end mode, and show an unobtrusive active state in the Audiobooks controls.

The APK passed `apksigner verify` using APK Signature Schemes v2 and v3. Its certificate SHA-256 fingerprint is:

```text
f251307cfb267e8b91436f4e245a91e064ba4d2e80e62f34c347b793998852ad
```

The SHA-256 checksum is recorded in [`../artifacts/SHA256SUMS.txt`](../artifacts/SHA256SUMS.txt) and the release-specific sidecar file. The final verified checksum is `18c1df150c7452a1eb8d2bf4c741cb4ad4c1cb9f38a872a494919ee878a8f267`.

The signing key was generated outside the repository only for this verification artifact and was deleted after signing. It is **not** a production update key. Because previous Auralis repair releases were also signed with deleted ephemeral keys, this APK must be installed after uninstalling an older ephemeral-key build. Future seamless installable updates require a securely managed, persistent release key whose fingerprint is published before distribution.

The 4.1.11 release gate completed successfully with the app and `musikr` unit tests, debug lint, `spotlessCheck`, and both debug and minified release builds.
