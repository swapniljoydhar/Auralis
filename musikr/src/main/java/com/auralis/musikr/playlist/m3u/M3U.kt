/*
 * Copyright (c) 2023 Auralis Contributors
 * M3U.kt is part of Auralis.
 *
 * Auralis is a free-software audio player for music and audiobooks, distributed
 * under the GNU General Public License v3.0 or later. It incorporates prior
 * free-software work; the attribution required by that license is retained in
 * PROVENANCE.md at the root of this repository.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.auralis.musikr.playlist.m3u

import android.content.Context
import com.auralis.musikr.Playlist
import com.auralis.musikr.fs.Components
import com.auralis.musikr.fs.Path
import com.auralis.musikr.fs.Volume
import com.auralis.musikr.fs.path.VolumeManager
import com.auralis.musikr.playlist.ExportConfig
import com.auralis.musikr.playlist.ImportedPlaylist
import com.auralis.musikr.playlist.PossiblePaths
import com.auralis.musikr.tag.Name
import com.auralis.musikr.util.correctWhitespace
import com.auralis.musikr.util.unlikelyToBeNull
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream

/**
 * Minimal M3U file format implementation.
 *
 * @author Auralis Contributors
 */
abstract class M3U {
    /**
     * Reads an M3U file from the given [stream] and returns a [ImportedPlaylist] containing the
     * paths to the files listed in the M3U file.
     *
     * @param stream The stream to read the M3U file from.
     * @param workingDirectory The directory that the M3U file is contained in. This is used to
     *   resolve relative paths.
     * @return An [ImportedPlaylist] containing the paths to the files listed in the M3U file,
     */
    internal abstract fun read(stream: InputStream, workingDirectory: Path): ImportedPlaylist?

    /**
     * Writes the given [playlist] to the given [outputStream] in the M3U format,.
     *
     * @param playlist The playlist to write.
     * @param outputStream The stream to write the M3U file to.
     * @param workingDirectory The directory that the M3U file is contained in. This is used to
     *   create relative paths to where the M3U file is assumed to be stored.
     * @param config The configuration to use when exporting the playlist.
     */
    internal abstract fun write(
        playlist: Playlist,
        outputStream: OutputStream,
        workingDirectory: Path,
        config: ExportConfig,
    )

    companion object {
        /** The mime type used for M3U files by the android system. */
        const val MIME_TYPE = "audio/x-mpegurl"

        internal fun from(context: Context): M3U = M3UImpl(VolumeManager.from(context))
    }
}

private class M3UImpl(private val volumeManager: VolumeManager) : M3U() {
    override fun read(stream: InputStream, workingDirectory: Path): ImportedPlaylist? {
        val volumes = volumeManager.getVolumes()
        val reader = BufferedReader(InputStreamReader(stream))
        val paths = mutableListOf<PossiblePaths>()
        var name: String? = null

        consumeFile@ while (true) {
            if (paths.size >= MAX_ENTRIES) break
            var path: String?
            collectMetadata@ while (true) {
                // The M3U format consists of "entries" that begin with a bunch of metadata
                // prefixed with "#", and then a relative/absolute path or url to the file.
                // We don't really care about the metadata except for the playlist name, so
                // we discard everything but that.
                // Lines are length-bounded: a single hostile multi-megabyte line must not
                // be able to exhaust memory via readLine's unbounded allocation.
                val currentLine =
                    (readBoundedLine(reader) ?: break@consumeFile).correctWhitespace()
                        ?: continue@collectMetadata
                if (currentLine.startsWith("#")) {
                    // Metadata entries are roughly structured
                    val split = currentLine.split(":", limit = 2)
                    when (split[0]) {
                        // Playlist name
                        "#PLAYLIST" -> name = split.getOrNull(1)?.correctWhitespace()
                        // Add more metadata handling here if needed.
                        else -> {}
                    }
                } else {
                    // Something that isn't a metadata entry, assume it's a path. It could be
                    // a URL, but it'll just get mangled really badly and not match with anything,
                    // so it's okay.
                    path = currentLine
                    break@collectMetadata
                }
            }

            // There is basically no formal specification of file paths in M3U, and it differs
            // based on the programs that generated it. I more or less have to consider any possible
            // interpretation as valid.
            val interpretations = interpretPath(unlikelyToBeNull(path))
            val possibilities =
                interpretations.flatMap { expandInterpretation(it, workingDirectory, volumes) }

            paths.add(possibilities)
        }

        return if (paths.isNotEmpty()) {
            ImportedPlaylist(name, paths)
        } else {
            // Couldn't get anything useful out of this file.
            null
        }
    }

    private data class InterpretedPath(val components: Components, val likelyAbsolute: Boolean)

    private fun interpretPath(path: String): List<InterpretedPath> =
        when {
            path.startsWith('/') -> listOf(InterpretedPath(Components.parseUnix(path), true))
            path.startsWith("./") -> listOf(InterpretedPath(Components.parseUnix(path), false))
            path.matches(WINDOWS_VOLUME_PREFIX_REGEX) ->
                listOf(InterpretedPath(Components.parseWindows(path.substring(2)), true))
            path.startsWith("\\") -> listOf(InterpretedPath(Components.parseWindows(path), true))
            path.startsWith(".\\") -> listOf(InterpretedPath(Components.parseWindows(path), false))
            else ->
                listOf(
                    InterpretedPath(Components.parseUnix(path), false),
                    InterpretedPath(Components.parseWindows(path), true),
                )
        }

    private fun expandInterpretation(
        path: InterpretedPath,
        workingDirectory: Path,
        volumes: List<Volume>,
    ): List<Path> {
        val absoluteInterpretation = Path(workingDirectory.volume, path.components)
        val relativeInterpretation =
            Path(workingDirectory.volume, path.components.absoluteTo(workingDirectory.components))
        val volumeExactMatch = volumes.find { it.components?.contains(path.components) == true }
        val volumeInterpretation =
            volumeExactMatch?.let {
                val components =
                    unlikelyToBeNull(volumeExactMatch.components).containing(path.components)
                Path(volumeExactMatch, components)
            }
        return if (path.likelyAbsolute) {
            listOfNotNull(volumeInterpretation, absoluteInterpretation, relativeInterpretation)
        } else {
            listOfNotNull(relativeInterpretation, volumeInterpretation, absoluteInterpretation)
        }
    }

    override fun write(
        playlist: Playlist,
        outputStream: OutputStream,
        workingDirectory: Path,
        config: ExportConfig,
    ) {
        val writer = outputStream.bufferedWriter()
        // Try to be as compliant to the spec as possible while also cramming it full of extensions
        // I imagine other players will use.
        writer.writeLine("#EXTM3U")
        writer.writeLine("#EXTENC:UTF-8")
        writer.writeLine("#PLAYLIST:${sanitizeField(playlist.name.raw)}")
        for (song in playlist.songs) {
            // EXTINF durations are whole seconds; the internal model keeps milliseconds.
            val durationSeconds = (song.durationMs / 1000L).coerceAtLeast(0L)
            writer.writeLine("#EXTINF:$durationSeconds,${sanitizeField(song.name.raw)}")
            val albumName = song.album.name
            if (albumName is Name.Known) {
                writer.writeLine("#EXTALB:${sanitizeField(albumName.raw)}")
            }
            for (artist in song.artists) {
                val name = artist.name
                if (name is Name.Known) {
                    writer.writeLine("#EXTART:${sanitizeField(name.raw)}")
                }
            }
            for (genre in song.genres) {
                val name = genre.name
                if (name is Name.Known) {
                    writer.writeLine("#EXTGEN:${sanitizeField(name.raw)}")
                }
            }

            val formattedPath =
                if (config.absolute) {
                    // The path is already absolute in this case, but we need to prefix and separate
                    // it differently depending on the setting.
                    if (config.windowsPaths) {
                        // Assume the plain windows C volume, since that's probably where most music
                        // libraries are on a windows PC.
                        "C:\\\\${song.path.volume.components?.let { it.windowsString + "\\" } ?: ""}${song.path.components.windowsString}"
                    } else {
                        "/${song.path.volume.components?.let { it.unixString + "/" } ?: ""}${song.path.components.unixString}"
                    }
                } else {
                    // First need to make this path relative to the working directory of the M3U
                    // file, and then format it with the correct separators.
                    val relativePath = song.path.components.relativeTo(workingDirectory.components)
                    if (config.windowsPaths) {
                        relativePath.windowsString
                    } else {
                        relativePath.unixString
                    }
                }
            writer.writeLine(formattedPath)
        }
        writer.flush()
    }

    /**
     * Read one line, capping the retained prefix at [MAX_LINE_CHARS]. Any remainder of an over-long
     * line is consumed and discarded so framing stays intact.
     *
     * @return The line without its terminator, or null at end of stream.
     */
    private fun readBoundedLine(reader: BufferedReader): String? {
        val builder = StringBuilder()
        var retained = 0
        var sawContent = false
        while (true) {
            val char = reader.read()
            if (char < 0) return builder.toString().takeIf { sawContent }
            sawContent = true
            if (char == '\n'.code) break
            if (char == '\r'.code) {
                reader.mark(1)
                if (reader.read() != '\n'.code) reader.reset()
                break
            }
            if (retained < MAX_LINE_CHARS) {
                builder.append(char.toChar())
                retained++
            }
        }
        return builder.toString()
    }

    private fun BufferedWriter.writeLine(line: String) {
        write(line)
        newLine()
    }

    /** Strip control characters so written fields can never break M3U line framing. */
    private fun sanitizeField(field: String): String =
        field.replace(Regex("[\\p{Cntrl}]"), " ").correctWhitespace() ?: "Unknown"

    private fun Components.absoluteTo(workingDirectory: Components): Components {
        var absoluteComponents = workingDirectory
        for (component in components) {
            when (component) {
                // Parent specifier, go "back" one directory (in practice cleave off the last
                // component)
                ".." -> absoluteComponents = absoluteComponents.parent()
                // Current directory, the components are already there.
                "." -> {}
                // New directory, add it
                else -> absoluteComponents = absoluteComponents.child(component)
            }
        }
        return absoluteComponents
    }

    private fun Components.relativeTo(workingDirectory: Components): Components {
        // We want to find the common prefix of the working directory and path, and then
        // and them combine them with the correct relative elements to make sure they
        // resolve the same.
        var commonIndex = 0
        while (
            commonIndex < components.size &&
                commonIndex < workingDirectory.components.size &&
                components[commonIndex] == workingDirectory.components[commonIndex]
        ) {
            ++commonIndex
        }

        var relativeComponents = Components.parseUnix(".")

        // TODO: Simplify this logic
        when {
            commonIndex == components.size && commonIndex == workingDirectory.components.size -> {
                // The paths are the same. This shouldn't occur.
            }
            commonIndex == components.size -> {
                // The working directory is deeper in the path, backtrack.
                for (i in 0..<workingDirectory.components.size - commonIndex) {
                    relativeComponents = relativeComponents.child("..")
                }
            }
            commonIndex == workingDirectory.components.size -> {
                // Working directory is shallower than the path, can just append the
                // non-common remainder of the path
                relativeComponents = relativeComponents.child(depth(commonIndex))
            }
            else -> {
                // The paths are siblings. Backtrack and append as needed.
                for (i in 0..<workingDirectory.components.size - commonIndex) {
                    relativeComponents = relativeComponents.child("..")
                }
                relativeComponents = relativeComponents.child(depth(commonIndex))
            }
        }

        return relativeComponents
    }

    private companion object {
        /** Refuse to index absurd playlists; legitimate M3U files never approach this size. */
        const val MAX_ENTRIES = 100_000
        /** Retained prefix per input line; longer lines are truncated, never fully buffered. */
        const val MAX_LINE_CHARS = 8_192
        val WINDOWS_VOLUME_PREFIX_REGEX = Regex("^[A-Za-z]:\\\\.*")
    }
}
