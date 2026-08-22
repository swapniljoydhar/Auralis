# Auralis Release Verification

The current verified release artifact is `artifacts/auralis-4.1.20-release-signed.apk`. It was built from the repository’s minified `release` variant with the pinned Android SDK, NDK `28.2.13676358`, CMake `3.22.1`, taglib, and utfcpp inputs initialized.

Version **4.1.20** adds three local-folder organization strategies exclusively to the Audiobooks library. **Each book folder** keeps every physical book directory separate, even if unrelated books contain matching album tags. **Selected folder is one book** treats all supported local audio nested below the longest matching chosen root as one audiobook. **Author and book folders** recognizes the local structure `<selected root>/<author>/<book>/…` and derives the book and author context from those directory levels; malformed or shallower paths safely fall back to their immediate book directory. Selected roots remain available as grouping context even when the existing selected-folder filter is disabled, while the filter itself continues to control which local audio is admitted to Audiobooks. Music settings, Music library projections, and persisted Music snapshots remain unchanged.

The APK passed `apksigner verify` using APK Signature Schemes v2 and v3. Its certificate SHA-256 fingerprint is:

```text
673c6e77630e358da43a0852e3a4b06654e6911b4c49adf8ad503b06f52faee2
```

The SHA-256 checksum is recorded in [`../artifacts/SHA256SUMS.txt`](../artifacts/SHA256SUMS.txt) and the release-specific sidecar file. The final verified checksum is `9b9c84fedcda981104ee65dd874ed1d8782d538c6a1a2f064bcd6ed4a018ca15`.

The signing key was generated outside the repository only for this verification artifact and was deleted after signing. It is **not** a production update key. Because previous Auralis repair releases were also signed with deleted ephemeral keys, this APK must be installed after uninstalling an older ephemeral-key build. Future seamless installable updates require a securely managed, persistent release key whose fingerprint is published before distribution.

The 4.1.20 release gate completed successfully with the app and `musikr` unit tests, debug lint, `spotlessCheck`, and both debug and minified release builds. Focused organization tests additionally cover unknown preference fallback; separate-directory keys; selected-root grouping; author/book hierarchy resolution; safe malformed-path fallback; and Music snapshot isolation during concurrent Audiobooks refreshes.
