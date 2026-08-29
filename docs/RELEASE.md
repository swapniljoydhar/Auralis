# Auralis Release Verification

The current verified release artifact is `artifacts/auralis-4.1.21-material3-release-signed.apk`. It was built from the repository’s minified `release` variant with the pinned Android SDK, NDK `28.2.13676358`, CMake `3.22.1`, taglib, and utfcpp inputs initialized.

Version **4.1.21** is the first Auralis Material 3 experience release. It retains the existing native Material 3 dependency and theme contract, then makes the shared hierarchy more intentional across Music and Audiobooks: the home app bar now states the active library context; indexing, empty states, Music detail, search, mini-player, and expanded-player surfaces use clearer tonal roles; and the long-form Audiobook detail and bookmarks screens use Material 3 type and action hierarchy. The release preserves local-only media, explicit `PlaybackDomain`, independent Music/Audiobooks snapshots, and domain-pure queues.

The Auralis identity is now vector-first and Material 3-ready. An original open-listening-page mark replaces the prior detailed raster foreground in the adaptive launcher icon, legacy launcher fallback, Android themed monochrome layer, Android 12+ splash, and compact widget glyph. The mark represents Music listening and Audiobook storytelling without reproducing another product's branding.

The APK passed `apksigner verify` using APK Signature Schemes v2 and v3. Its certificate SHA-256 fingerprint is:

```text
174a8b0c7ba4f7a86293350a1b4d7d84388cb268ded7d6ca541acd6f75708ddb
```

The SHA-256 checksum is recorded in [`../artifacts/SHA256SUMS.txt`](../artifacts/SHA256SUMS.txt) and the release-specific sidecar file. The final verified checksum is `12c3a9e23c5e71653c7c8e1857deffde11f8a39e09c4ea086a431799f7ad8a89`.

The signing key was generated outside the repository only for this verification artifact and was deleted after signing. It is **not** a production update key. Because previous Auralis repair releases were also signed with deleted ephemeral keys, this APK must be installed after uninstalling an older ephemeral-key build. Future seamless installable updates require a securely managed, persistent release key whose fingerprint is published before distribution.

The 4.1.21 release gate completed successfully with the app and `musikr` unit tests, debug lint, `spotlessCheck`, and both debug and minified release builds. Focused existing playback-domain, Music snapshot-isolation, Audiobooks organization, chapter, bookmark, and lifecycle tests remain part of the passing suite. Static verification does not replace a physical-device review of launcher masks, Android themed icons, large-font layouts, TalkBack traversal, or the user’s own local media library.
