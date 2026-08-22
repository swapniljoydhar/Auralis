# Auralis Release Verification

The current verified release artifact is `artifacts/auralis-4.1.17-release-signed.apk`. It was built from the repository’s minified `release` variant with the pinned Android SDK, NDK `28.2.13676358`, CMake `3.22.1`, taglib, and utfcpp inputs initialized.

Version **4.1.17** adds targeted regression coverage for the isolation boundary between Music and Audiobooks. Audiobooks folder scoping now operates on a copied read-only projection, with tests proving a selected-folder projection cannot mutate or replace the shared Music snapshot. Conservative audiobook classification now has explicit coverage for locally indexed M4B, MP3, M4A, OGG, OGA, and OPUS files: M4B remains an intrinsic audiobook signal, while the other formats require audiobook metadata, folder markers, manual assignment, or long-form structure so ordinary music remains in Music.

The APK passed `apksigner verify` using APK Signature Schemes v2 and v3. Its certificate SHA-256 fingerprint is:

```text
93e2c2a529cf09a4982df42346de406215bef5de02909badc919b18269c615bd
```

The SHA-256 checksum is recorded in [`../artifacts/SHA256SUMS.txt`](../artifacts/SHA256SUMS.txt) and the release-specific sidecar file. The final verified checksum is `2accb82932b6d9fc2035026c148c80cce782796144ef7ad1632248b5c52b6919`.

The signing key was generated outside the repository only for this verification artifact and was deleted after signing. It is **not** a production update key. Because previous Auralis repair releases were also signed with deleted ephemeral keys, this APK must be installed after uninstalling an older ephemeral-key build. Future seamless installable updates require a securely managed, persistent release key whose fingerprint is published before distribution.

The 4.1.17 release gate completed successfully with the app and `musikr` unit tests, debug lint, `spotlessCheck`, and both debug and minified release builds.
