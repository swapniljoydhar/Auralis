# musikr

`musikr` is Auralis’s high-performance local music-indexing and metadata engine. It scans user-selected directories and SAF trees, extracts metadata tags (ID3v2, MP4/M4A, Vorbis, FLAC), resolves multi-artist and multi-genre hierarchies, maintains a cached memory graph, and provides immutable models consumed by the player layer.

`musikr` intentionally does not own UI or playback transport. The app layer decides how Music and Audiobooks are projected, while `musikr` provides robust metadata and filesystem primitives for local media.

## Architecture

* **Pure Kotlin & Media Engine**: Fast, concurrent media parsing powered by standard platform APIs (`MediaMetadataRetriever`) with zero native NDK overhead.
* **Pipeline Processing**: Structured into asynchronous `ExtractStep`, `TagParser`, and `EvaluateStep` stages for optimal multi-threaded scanning.
* **In-Memory Graph**: Produces normalized, immutable representations of `Song`, `Album`, `Artist`, and `Genre` with deduplicated entity uids.

## Testing & Verification

```bash
# Run unit tests
gradle :musikr:test
```

## License

`musikr` is distributed under the GNU General Public License, version 3 or later. See [`LICENSE`](../LICENSE) for complete licensing terms.
