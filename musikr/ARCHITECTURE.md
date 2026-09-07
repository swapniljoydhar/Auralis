# Musikr Architecture Overview

`musikr` is a multi-threaded, local music loading library for Android that works with Android's Storage Access Framework (SAF) and platform media extraction to provide robust, local-first music indexing.

## Core Design Principles

1. **Stateless API**: Side-effects are contained within the Storage layer
2. **Explicit Configuration**: All parameters must be explicitly configured
3. **Pipeline Architecture**: Three-step processing pipeline for music loading
4. **Platform Media Engine**: Concurrent metadata extraction using Android's `MediaMetadataRetriever`
5. **Coroutine-Based**: Uses Kotlin coroutines and Flows for efficient async operations

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                       Application Layer                     │
│                           (Auralis)                         │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                         Musikr API                          │
│                    (Musikr.kt interface)                    │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Pipeline Architecture                    │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐     │
│  │ ExploreStep  │ → │ ExtractStep  │ → │ EvaluateStep │     │
│  └──────────────┘   └──────────────┘   └──────────────┘     │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                       Storage Layer                         │
│  - Cache (Room database)                                    │
│  - Covers (Cover art storage and retrieval)                 │
│  - Playlists (User playlist management)                     │
└─────────────────────────────────────────────────────────────┘
```

## Pipeline Steps

### 1. ExploreStep
- **Purpose**: Discover music files on the device
- **Process**:
  - Uses FS module to explore file system via SAF
  - Filters for audio MIME types and playlists
  - Checks cache for previously indexed files
  - Manages cover art retrieval
- **Output**: Stream of `Explored` items (songs/playlists to process)

### 2. ExtractStep
- **Purpose**: Extract metadata from discovered files
- **Process**:
  - Uses `MetadataExtractor` with Android's `MediaMetadataRetriever`
  - Handles multiple tag formats (ID3v1/v2, MP4, Vorbis, FLAC)
  - Extracts audio properties (bitrate, duration, track numbers)
  - Manages cover art extraction
- **Output**: Stream of `Extracted` items with full metadata

### 3. EvaluateStep
- **Purpose**: Build the music graph from extracted metadata
- **Process**:
  - Creates relationships between songs, albums, artists, genres
  - Resolves naming conflicts and duplicates
  - Applies user interpretation preferences
  - Builds final library structure
- **Output**: Complete `MutableLibrary` with all relationships

## Key Components

### Config & Storage
- **Config**: Main configuration container
  - `fs`: File system access configuration
  - `storage`: Persistent storage components
  - `interpretation`: Tag interpretation rules
- **Storage**: Side-effect laden components
  - `cache`: Metadata caching (Room database)
  - `covers`: Cover art storage and retrieval
  - `storedPlaylists`: User playlist management

### Data Models
- **Music**: Base interface for all music items with UID system
- **Song**: Individual track with full metadata
- **Album**: Collection of songs (includes EPs, singles, etc.)
- **Artist**: Explicit (album artist) and implicit (track artist) albums
- **Genre**: Grouping by musical genre
- **Playlist**: User-created or imported playlists

### File System (FS)
- Abstraction over Android's Storage Access Framework
- Supports multiple storage locations
- Handles permissions and URI management

## Threading Model
- **Coroutine-based**: All operations use Kotlin coroutines
- **Multi-threaded extraction**: Parallel metadata extraction
- **Buffered channels**: For efficient pipeline communication
- **Dispatcher control**: Explicit IO dispatcher usage

## Performance Optimizations
- **Caching**: Persistent Room metadata caching
- **Parallel processing**: Multi-threaded file processing
- **Lazy evaluation**: On-demand cover loading
- **Memory efficiency**: Streaming pipeline architecture
