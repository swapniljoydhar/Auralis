# Auralis Release Verification

The current verified release artifact is `artifacts/auralis-4.1.18-release-signed.apk`. It was built from the repository’s minified `release` variant with the pinned Android SDK, NDK `28.2.13676358`, CMake `3.22.1`, taglib, and utfcpp inputs initialized.

Version **4.1.18** closes the confirmed chapter-parity gap for local MP4 audiobook containers. The embedded chapter reader now recognizes bounded Nero-style MP4 `chpl` metadata in M4A and M4B files alongside existing MP3 ID3v2 `CHAP` frames, so validated embedded chapters can drive the same navigation, bookmark, progress-context, and end-of-chapter sleep behavior. The format audit confirms M4B, MP3, M4A, OGG, OGA, and OPUS use the established local indexing, metadata, playback, and projection paths. OGG/OGA/OPUS continue to support stable file-chapter books; they have no common embedded chapter metadata convention to infer safely. The update also adds a concurrent selected-folder projection regression and a focused `docs/AUDIOBOOKS.md` flow guide.

The APK passed `apksigner verify` using APK Signature Schemes v2 and v3. Its certificate SHA-256 fingerprint is:

```text
d3183db8e81539e81fcbb408205a5d182c1915d84925ec1c50156dfd51b095fa
```

The SHA-256 checksum is recorded in [`../artifacts/SHA256SUMS.txt`](../artifacts/SHA256SUMS.txt) and the release-specific sidecar file. The final verified checksum is `6b09f2b3953aabd73dbd211050c7a4680c66b2a4b198c22533c81f25026e3b83`.

The signing key was generated outside the repository only for this verification artifact and was deleted after signing. It is **not** a production update key. Because previous Auralis repair releases were also signed with deleted ephemeral keys, this APK must be installed after uninstalling an older ephemeral-key build. Future seamless installable updates require a securely managed, persistent release key whose fingerprint is published before distribution.

The 4.1.18 release gate completed successfully with the app and `musikr` unit tests, debug lint, `spotlessCheck`, and both debug and minified release builds.
