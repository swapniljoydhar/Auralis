# Auralis Release Verification

The current verified release artifact is `artifacts/auralis-4.1.10-release-signed.apk`. It was built from the repository’s minified `release` variant with the pinned Android SDK, NDK `28.2.13676358`, CMake `3.22.1`, taglib, and utfcpp inputs initialized.

Version **4.1.10** turns the recorded Voice reference into Auralis-specific local-player behavior without importing Voice product identity. Audiobooks now group into clear Current and Not started sections with compact cover rows; long-form local MP3 chapter sets are discoverable without the former cross-library assignment flow; and now-playing exposes chapter, bookmark, sleep, and speed actions only in the explicit Audiobooks domain. Music retains its own queue filtering and transport hierarchy.

The APK passed `apksigner verify` using APK Signature Schemes v2 and v3. Its certificate SHA-256 fingerprint is:

```text
21a1a957787881c4d47ff871ab6f3b6e0b5474581789267053a80665edf322b5
```

The SHA-256 checksum is recorded in [`../artifacts/SHA256SUMS.txt`](../artifacts/SHA256SUMS.txt) and the release-specific sidecar file. The final verified checksum is `fdd63792a7cbd935f4bcbe7b912841062660792f0d95591fce3d2732d4af9e09`.

The signing key was generated outside the repository only for this verification artifact and was deleted after signing. It is **not** a production update key. Because previous Auralis repair releases were also signed with deleted ephemeral keys, this APK must be installed after uninstalling an older ephemeral-key build. Future seamless installable updates require a securely managed, persistent release key whose fingerprint is published before distribution.

The 4.1.10 release gate completed successfully with the app and `musikr` unit tests, debug lint, `spotlessCheck`, and both debug and minified release builds.
