# Auralis Release Verification

The current verified release artifact is `artifacts/auralis-4.1.19-release-signed.apk`. It was built from the repository’s minified `release` variant with the pinned Android SDK, NDK `28.2.13676358`, CMake `3.22.1`, taglib, and utfcpp inputs initialized.

Version **4.1.19** begins the cohesive Auralis design-flow pass without merging the two listening products. Fresh installs now land directly in Music rather than encountering a blocking Music-versus-Audiobooks chooser; the current or last selected library remains available through a persistent, accessible toolbar action. Music and Audiobooks retain their independent domain state and purpose-specific controls. The settings root now groups shared Appearance controls, Music library and playback controls, Audiobooks controls, and local-library maintenance so listeners can understand what belongs to each listening context.

The APK passed `apksigner verify` using APK Signature Schemes v2 and v3. Its certificate SHA-256 fingerprint is:

```text
a0cc70aca0a0791107b45b942e1c921ab00832a9694285e64304a8393edbc6d3
```

The SHA-256 checksum is recorded in [`../artifacts/SHA256SUMS.txt`](../artifacts/SHA256SUMS.txt) and the release-specific sidecar file. The final verified checksum is `91d7c32af96b46c8fc77e82af8b2d34e14d2ec79548cd9c9ba72ee8f2a0a986d`.

The signing key was generated outside the repository only for this verification artifact and was deleted after signing. It is **not** a production update key. Because previous Auralis repair releases were also signed with deleted ephemeral keys, this APK must be installed after uninstalling an older ephemeral-key build. Future seamless installable updates require a securely managed, persistent release key whose fingerprint is published before distribution.

The 4.1.19 release gate completed successfully with the app and `musikr` unit tests, debug lint, `spotlessCheck`, and both debug and minified release builds.
