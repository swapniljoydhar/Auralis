# Auralis Release Verification

The current verified release artifact is `artifacts/auralis-4.1.14-release-signed.apk`. It was built from the repository’s minified `release` variant with the pinned Android SDK, NDK `28.2.13676358`, CMake `3.22.1`, taglib, and utfcpp inputs initialized.

Version **4.1.14** turns Audiobooks into a durable local listening library. It adds Current, Not started, and Finished lifecycle sections; percentage, listened-time, and remaining-time progress; an explicit one-tap Resume action; a dedicated bookmark destination with direct resume, removal, and unavailable-chapter visibility; and a Room 40-to-41 migration that persists the active embedded ID3 chapter start beside local chapter-file progress. The playback service and direct bookmark action now write the embedded chapter context without altering the independent Music domain.

The APK passed `apksigner verify` using APK Signature Schemes v2 and v3. Its certificate SHA-256 fingerprint is:

```text
04b0fd4915e20694b5448f07a782cce75487123159794b107e1170e631a19620
```

The SHA-256 checksum is recorded in [`../artifacts/SHA256SUMS.txt`](../artifacts/SHA256SUMS.txt) and the release-specific sidecar file. The final verified checksum is `52d722ad237aa877bf289e7d230600880369252b29b520c7ff85b15aa7aa335e`.

The signing key was generated outside the repository only for this verification artifact and was deleted after signing. It is **not** a production update key. Because previous Auralis repair releases were also signed with deleted ephemeral keys, this APK must be installed after uninstalling an older ephemeral-key build. Future seamless installable updates require a securely managed, persistent release key whose fingerprint is published before distribution.

The 4.1.14 release gate completed successfully with the app and `musikr` unit tests, debug lint, `spotlessCheck`, and both debug and minified release builds.
