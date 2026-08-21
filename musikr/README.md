# musikr

`musikr` is Auralis’s local music-library engine. It reads user-selected folders through Android’s Storage Access Framework, parses tags with the pinned TagLib source, interprets multi-value metadata, stores a compact cache, and exposes the domain model consumed by the Music space.

The module intentionally does not own Android UI or playback policy. The app layer decides how Music and Audiobooks are projected, while `musikr` provides stable metadata and filesystem primitives for local media.

## API boundary

This is an internal library with a deliberately small and evolving API. Callers should use the interfaces already exercised by the `app` module rather than depending on implementation details. Changes to tag interpretation, cache serialization, or native bindings require unit tests and a debug build.

## Build and tests

From the repository root, initialize the nested submodules and run:

```bash
git submodule update --init --recursive
./gradlew :musikr:test
./gradlew :app:assembleDebug
```

Native TagLib output is built for the Android ABIs configured by the module. The pinned `taglib` and `utfcpp` revisions are part of the repository’s reproducible build inputs and must not be replaced by unreviewed system libraries.

## License

`musikr` is distributed under the GNU General Public License, version 3 or later. See the repository `LICENSE`, `NOTICE`, and the module’s vendored third-party notices for complete attribution.
