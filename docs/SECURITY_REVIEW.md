# Auralis security review

The review covered Android manifest exposure, exported service and provider boundaries, content-URI handling, settings migration, sorting dispatch, external media-browser callbacks, native submodule provenance, local build configuration, and release signing inputs.

The exported cover provider now accepts only read-mode requests matching the constrained cover URI pattern, rejects path traversal segments, returns `null` for unknown MIME types, and returns an empty cursor for metadata queries instead of throwing. The exported media-browser service validates that the claimed client package belongs to the calling UID before returning the browsable root; spoofed package claims receive an empty hierarchy. The service remains exported because Android media controllers and Auto require a declared media-browser service, and the service’s `onGetRoot()` remains the access-control boundary.

The review also removed crash-prone defaults: the normal sort-mode adapter bind path now binds data, settings migration defaults to a no-op, unsupported sort operations preserve the current order, and audiobook sorting cannot fall into a music-only exception. Debug-only identity resources no longer create release resource gaps or duplicate base resources.

The build uses a repository-pinned taglib and nested utfcpp checkout. No private key, password, `local.properties`, generated native build directory, or cloud credential is part of the intended commit. Lint still reports non-blocking warnings such as unused resources, legacy API deprecations, custom-view naming conventions, and third-party/private-resource usage; these are recorded rather than hidden. A device/emulator behavioral test of every playback path was not possible in the sandbox, so release review should still include install, permission, library-scan, Music, Audiobooks, background playback, Android Auto, widget, and upgrade tests.

The Android media-browser access model follows the [official Android guidance](https://developer.android.com/media/legacy/audio/mediabrowserservice), which requires the intent-filter and identifies `onGetRoot()` as the client-access boundary.
