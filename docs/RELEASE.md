# Auralis Release Verification

The current verified release artifact is `artifacts/auralis-4.1.16-release-signed.apk`. It was built from the repository’s minified `release` variant with the pinned Android SDK, NDK `28.2.13676358`, CMake `3.22.1`, taglib, and utfcpp inputs initialized.

Version **4.1.16** refines the Auralis Audiobooks experience without changing Music. While the local index is working, an Audiobooks-only loading message replaces the completed-empty copy; a completed empty shelf now gives concise local-library guidance; and lifecycle headings and summaries distinguish In progress, Not started, and Finished without raw classifier wording. Compact and cover-grid rows now separate author/local metadata from listening state, and both library and book-detail titles use bounded ellipsis-safe treatments while retaining their complete accessibility labels. The explicit PlaybackDomain and Music library behavior remain unchanged.

The APK passed `apksigner verify` using APK Signature Schemes v2 and v3. Its certificate SHA-256 fingerprint is:

```text
9095b1e5803554738cd1ccb0f6eebe808357cb23f06150c96c3958b59abd1ad5
```

The SHA-256 checksum is recorded in [`../artifacts/SHA256SUMS.txt`](../artifacts/SHA256SUMS.txt) and the release-specific sidecar file. The final verified checksum is `1c7628bb1f70eb757b944a707f832d92e5eacf64691d9eb3dd0afcff24e5fde5`.

The signing key was generated outside the repository only for this verification artifact and was deleted after signing. It is **not** a production update key. Because previous Auralis repair releases were also signed with deleted ephemeral keys, this APK must be installed after uninstalling an older ephemeral-key build. Future seamless installable updates require a securely managed, persistent release key whose fingerprint is published before distribution.

The 4.1.16 release gate completed successfully with the app and `musikr` unit tests, debug lint, `spotlessCheck`, and both debug and minified release builds.
