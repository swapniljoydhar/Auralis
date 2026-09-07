/*
 * Copyright (c) 2023 Auralis Contributors
 * PersistenceModule.kt is part of Auralis.
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

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface PersistenceModule {
    @Binds fun repository(persistenceRepository: PersistenceRepositoryImpl): PersistenceRepository
}

@Module
@InstallIn(SingletonComponent::class)
class PersistenceRoomModule {
    @Singleton
    @Provides
    fun database(@ApplicationContext context: Context) =
        Room.databaseBuilder(
                context.applicationContext,
                PersistenceDatabase::class.java,
                "playback_persistence.db",
            )
            .addMigrations(
                PersistenceDatabase.MIGRATION_27_32,
                PersistenceDatabase.MIGRATION_38_39,
                PersistenceDatabase.MIGRATION_39_40,
                PersistenceDatabase.MIGRATION_40_41,
            )
            .fallbackToDestructiveMigration(true)
            .build()

    // Note: the legacy single-session PlaybackState/Queue tables are intentionally left
    // without providers. The tables still exist so the 39->40 migration can copy them into
    // the domain-keyed snapshots; nothing else may read or write them.
    @Provides
    fun audiobookProgressDao(database: PersistenceDatabase) = database.audiobookProgressDao()

    @Provides
    fun domainPlaybackStateDao(database: PersistenceDatabase) = database.domainPlaybackStateDao()
}
