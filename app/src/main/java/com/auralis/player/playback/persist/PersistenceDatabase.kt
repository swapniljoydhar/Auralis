/*
 * Copyright (c) 2023 Auralis Contributors
 * PersistenceDatabase.kt is part of Auralis.
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
 
package com.auralis.player.playback.persist

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import com.auralis.musikr.Music
import com.auralis.player.playback.state.RepeatMode

/**
 * Provides raw access to the database storing the persisted playback state.
 *
 * @author Auralis Contributors
 */
@Database(
    entities =
        [
            PlaybackState::class,
            QueueHeapItem::class,
            QueueShuffledMappingItem::class,
            AudiobookProgressEntity::class,
            DomainPlaybackState::class,
            DomainQueueHeapItem::class,
            DomainQueueMappingItem::class,
        ],
    version = 41,
    exportSchema = false,
)
@TypeConverters(Music.UID.TypeConverters::class)
abstract class PersistenceDatabase : RoomDatabase() {
    /**
     * Get the current [PlaybackStateDao].
     *
     * @return A [PlaybackStateDao] providing control of the database's playback state tables.
     */
    abstract fun playbackStateDao(): PlaybackStateDao

    /**
     * Get the current [QueueDao].
     *
     * @return A [QueueDao] providing control of the database's queue tables.
     */
    abstract fun queueDao(): QueueDao

    /** Get the DAO for durable, per-book audiobook progress. */
    abstract fun audiobookProgressDao(): AudiobookProgressDao

    abstract fun domainPlaybackStateDao(): DomainPlaybackStateDao

    companion object {
        val MIGRATION_27_32 =
            Migration(27, 32) {
                // Switched from custom names to just letting room pick the names
                it.execSQL("ALTER TABLE playback_state RENAME TO PlaybackState")
                it.execSQL("ALTER TABLE queue_heap RENAME TO QueueHeapItem")
                it.execSQL("ALTER TABLE queue_mapping RENAME TO QueueMappingItem")
            }

        /** Adds audiobook progress without touching the existing Music playback tables. */
        val MIGRATION_38_39 =
            Migration(38, 39) {
                it.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS AudiobookProgressEntity (
                        chapterUid TEXT NOT NULL PRIMARY KEY,
                        bookKey TEXT NOT NULL,
                        positionMs INTEGER NOT NULL,
                        completed INTEGER NOT NULL,
                        updatedMs INTEGER NOT NULL
                    )
                    """
                        .trimIndent()
                )
            }

        /** Splits the legacy single playback session into domain-keyed snapshot tables. */
        val MIGRATION_39_40 =
            Migration(39, 40) {
                it.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS DomainPlaybackState (
                        domain TEXT NOT NULL PRIMARY KEY,
                        `index` INTEGER NOT NULL,
                        positionMs INTEGER NOT NULL,
                        repeatMode TEXT NOT NULL,
                        songUid TEXT NOT NULL,
                        parentUid TEXT
                    )
                    """
                        .trimIndent()
                )
                it.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS DomainQueueHeapItem (
                        domain TEXT NOT NULL,
                        id INTEGER NOT NULL,
                        uid TEXT NOT NULL,
                        PRIMARY KEY(domain, id)
                    )
                    """
                        .trimIndent()
                )
                it.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS DomainQueueMappingItem (
                        domain TEXT NOT NULL,
                        id INTEGER NOT NULL,
                        `index` INTEGER NOT NULL,
                        PRIMARY KEY(domain, id)
                    )
                    """
                        .trimIndent()
                )
                it.execSQL(
                    """
                    INSERT OR IGNORE INTO DomainPlaybackState(domain, `index`, positionMs, repeatMode, songUid, parentUid)
                    SELECT 'MUSIC', `index`, positionMs, repeatMode, songUid, parentUid FROM PlaybackState WHERE id = 0
                    """
                        .trimIndent()
                )
                it.execSQL(
                    """
                    INSERT OR IGNORE INTO DomainQueueHeapItem(domain, id, uid)
                    SELECT 'MUSIC', id, uid FROM QueueHeapItem
                    """
                        .trimIndent()
                )
                it.execSQL(
                    """
                    INSERT OR IGNORE INTO DomainQueueMappingItem(domain, id, `index`)
                    SELECT 'MUSIC', id, `index` FROM QueueShuffledMappingItem
                    """
                        .trimIndent()
                )
            }

        /** Adds the optional embedded-ID3 chapter start for one-file audiobook location context. */
        val MIGRATION_40_41 =
            Migration(40, 41) {
                it.execSQL(
                    "ALTER TABLE AudiobookProgressEntity ADD COLUMN embeddedChapterStartMs INTEGER"
                )
            }
    }
}

/**
 * Provides control of the persisted playback state table.
 *
 * @author Auralis Contributors
 */
@Dao
interface PlaybackStateDao {
    /**
     * Get the previously persisted [PlaybackState].
     *
     * @return The previously persisted [PlaybackState], or null if one was not present.
     */
    @Query("SELECT * FROM PlaybackState WHERE id = 0") suspend fun getState(): PlaybackState?

    /** Delete any previously persisted [PlaybackState]s. */
    @Query("DELETE FROM PlaybackState") suspend fun nukeState()

    /**
     * Insert a new [PlaybackState] into the database.
     *
     * @param state The [PlaybackState] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertState(state: PlaybackState)
}

/**
 * Provides control of the persisted queue state tables.
 *
 * @author Auralis Contributors
 */
@Dao
interface QueueDao {
    /**
     * Get the previously persisted queue heap.
     *
     * @return A list of persisted [QueueHeapItem]s wrapping each heap item.
     */
    @Query("SELECT * FROM QueueHeapItem") suspend fun getHeap(): List<QueueHeapItem>

    /**
     * Get the previously persisted queue mapping.
     *
     * @return A list of persisted [QueueShuffledMappingItem]s wrapping each heap item.
     */
    @Query("SELECT * FROM QueueShuffledMappingItem")
    suspend fun getShuffledMapping(): List<QueueShuffledMappingItem>

    /** Delete any previously persisted queue heap entries. */
    @Query("DELETE FROM QueueHeapItem") suspend fun nukeHeap()

    /** Delete any previously persisted queue mapping entries. */
    @Query("DELETE FROM QueueShuffledMappingItem") suspend fun nukeShuffledMapping()

    /**
     * Insert new heap entries into the database.
     *
     * @param heap The list of wrapped [QueueHeapItem]s to insert.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertHeap(heap: List<QueueHeapItem>)

    /**
     * Insert new mapping entries into the database.
     *
     * @param mapping The list of wrapped [QueueShuffledMappingItem] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertShuffledMapping(mapping: List<QueueShuffledMappingItem>)
}

// TODO: Figure out how to get RepeatMode to map to an int instead of a string
@Entity
data class PlaybackState(
    @PrimaryKey val id: Int,
    val index: Int,
    val positionMs: Long,
    val repeatMode: RepeatMode,
    val songUid: Music.UID,
    val parentUid: Music.UID?,
)

@Entity data class QueueHeapItem(@PrimaryKey val id: Int, val uid: Music.UID)

@Entity data class QueueShuffledMappingItem(@PrimaryKey val id: Int, val index: Int)

/** One durable resume record per audiobook chapter/file. */
@Entity
data class AudiobookProgressEntity(
    @PrimaryKey val chapterUid: String,
    val bookKey: String,
    val positionMs: Long,
    val completed: Boolean,
    val updatedMs: Long,
    val embeddedChapterStartMs: Long?,
)

@Dao
interface AudiobookProgressDao {
    @Query("SELECT * FROM AudiobookProgressEntity WHERE bookKey = :bookKey ORDER BY updatedMs DESC")
    suspend fun getForBook(bookKey: String): List<AudiobookProgressEntity>

    @Query("SELECT * FROM AudiobookProgressEntity WHERE chapterUid = :chapterUid LIMIT 1")
    suspend fun getForChapter(chapterUid: String): AudiobookProgressEntity?

    @Query("SELECT * FROM AudiobookProgressEntity WHERE chapterUid IN (:chapterUids)")
    suspend fun getForChapters(chapterUids: List<String>): List<AudiobookProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: AudiobookProgressEntity)

    @Query("DELETE FROM AudiobookProgressEntity WHERE bookKey = :bookKey")
    suspend fun deleteForBook(bookKey: String)

    @Query("DELETE FROM AudiobookProgressEntity WHERE chapterUid IN (:chapterUids)")
    suspend fun deleteForChapters(chapterUids: List<String>)
}

@Entity
data class DomainPlaybackState(
    @PrimaryKey val domain: String,
    val index: Int,
    val positionMs: Long,
    val repeatMode: RepeatMode,
    val songUid: Music.UID,
    val parentUid: Music.UID?,
)

@Entity(primaryKeys = ["domain", "id"])
data class DomainQueueHeapItem(val domain: String, val id: Int, val uid: Music.UID)

@Entity(primaryKeys = ["domain", "id"])
data class DomainQueueMappingItem(val domain: String, val id: Int, val index: Int)

@Dao
interface DomainPlaybackStateDao {
    @Query("SELECT * FROM DomainPlaybackState WHERE domain = :domain LIMIT 1")
    suspend fun getState(domain: String): DomainPlaybackState?

    @Query("SELECT * FROM DomainQueueHeapItem WHERE domain = :domain ORDER BY id")
    suspend fun getHeap(domain: String): List<DomainQueueHeapItem>

    @Query("SELECT * FROM DomainQueueMappingItem WHERE domain = :domain ORDER BY id")
    suspend fun getMapping(domain: String): List<DomainQueueMappingItem>

    @Query("DELETE FROM DomainPlaybackState WHERE domain = :domain")
    suspend fun clearState(domain: String)

    @Query("DELETE FROM DomainQueueHeapItem WHERE domain = :domain")
    suspend fun clearHeap(domain: String)

    @Query("DELETE FROM DomainQueueMappingItem WHERE domain = :domain")
    suspend fun clearMapping(domain: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertState(state: DomainPlaybackState)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHeap(heap: List<DomainQueueHeapItem>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMapping(mapping: List<DomainQueueMappingItem>)
}
