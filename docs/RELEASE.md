# Auralis Release Verification

The current verified release artifact is `artifacts/auralis-4.1.15-release-signed.apk`. It was built from the repository’s minified `release` variant with the pinned Android SDK, NDK `28.2.13676358`, CMake `3.22.1`, taglib, and utfcpp inputs initialized.

Version **4.1.15** adds a focused Audiobooks settings surface without changing Music settings or Music playback. It separates local-library, long-form playback, and appearance controls; moves audiobook skip duration, default speed, auto-rewind, and silence reduction out of generic Audio; lets listeners limit the Audiobooks projection to explicitly selected indexed local folders; and provides compact rows or a true cover-focused two-column grid with full-width Current, Not started, and Finished headers. The existing explicit PlaybackDomain remains authoritative, so the shared local index and player infrastructure do not permit these preferences to alter Music behavior.

The APK passed `apksigner verify` using APK Signature Schemes v2 and v3. Its certificate SHA-256 fingerprint is:

```text
9b99e732cf444fc59f0c1c5217b11fce0245e6865173014977ff19e08d2c9844
```

The SHA-256 checksum is recorded in [`../artifacts/SHA256SUMS.txt`](../artifacts/SHA256SUMS.txt) and the release-specific sidecar file. The final verified checksum is `0b57cd05b4453f5b3a9da574653d5dba462cfd064f1c554e93d3bd4dd819d16f`.

The signing key was generated outside the repository only for this verification artifact and was deleted after signing. It is **not** a production update key. Because previous Auralis repair releases were also signed with deleted ephemeral keys, this APK must be installed after uninstalling an older ephemeral-key build. Future seamless installable updates require a securely managed, persistent release key whose fingerprint is published before distribution.

The 4.1.15 release gate completed successfully with the app and `musikr` unit tests, debug lint, `spotlessCheck`, and both debug and minified release builds.
