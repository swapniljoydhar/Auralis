# Auralis Release Verification

The current verified release artifact is `artifacts/auralis-4.1.13-release-signed.apk`. It was built from the repository’s minified `release` variant with the pinned Android SDK, NDK `28.2.13676358`, CMake `3.22.1`, taglib, and utfcpp inputs initialized.

Version **4.1.13** introduces the dedicated Audiobooks chapter navigator. It validates and orders local embedded ID3 chapter timestamps, preserves resolved file-chapter queue order, shows a clear embedded/file source and timing range for every chapter, marks the active chapter, and uses accessible 56dp interactive rows. Selection is rejected when the open navigator no longer matches the active explicit Audiobooks queue or current song, avoiding stale navigation into a changed book or Music session.

The APK passed `apksigner verify` using APK Signature Schemes v2 and v3. Its certificate SHA-256 fingerprint is:

```text
aae423e5383fb8051fde004f61c46b015e8a89d42d6990d347a3c778fb3d2c6e
```

The SHA-256 checksum is recorded in [`../artifacts/SHA256SUMS.txt`](../artifacts/SHA256SUMS.txt) and the release-specific sidecar file. The final verified checksum is `d901aadec04622f0aeb72feebce5371b7ecc1fc9919e40353416cebade8a30b5`.

The signing key was generated outside the repository only for this verification artifact and was deleted after signing. It is **not** a production update key. Because previous Auralis repair releases were also signed with deleted ephemeral keys, this APK must be installed after uninstalling an older ephemeral-key build. Future seamless installable updates require a securely managed, persistent release key whose fingerprint is published before distribution.

The 4.1.13 release gate completed successfully with the app and `musikr` unit tests, debug lint, `spotlessCheck`, and both debug and minified release builds.
