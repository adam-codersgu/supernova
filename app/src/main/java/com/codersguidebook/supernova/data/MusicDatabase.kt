package com.codersguidebook.supernova.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.entities.SongPlays
import com.codersguidebook.supernova.utils.DefaultPlaylistHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate

@Database(entities = [Song::class, Playlist::class, SongPlays::class], version = 2, exportSchema = false)
abstract class MusicDatabase : RoomDatabase() {

    abstract fun musicDao(): MusicDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun songPlaysDao(): SongPlaysDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val epochDay = LocalDate.now().minusWeeks(2).toEpochDay()
                db.execSQL("INSERT INTO SongPlays (songId, epochDays, qtyOfPlays) " +
                        "SELECT songId, $epochDay, song_plays FROM music_table")
                db.execSQL("ALTER TABLE music_table DROP COLUMN song_plays")
            }
        }

        @Volatile
        private var database: MusicDatabase? = null

        fun getDatabase(
            context: Context,
            scope: CoroutineScope
        ): MusicDatabase {

            database ?: kotlin.run {
                // the builder needs a context, the Database class and a name for your database
                database = Room.databaseBuilder(context, MusicDatabase::class.java, "music_database")
                    // destroy the earlier database if the version is incremented
                    .fallbackToDestructiveMigration()
                    .addCallback(MusicDatabaseCallback(context, scope))
                    .addMigrations(MIGRATION_1_2)
                    .build()
            }

            return database!!
        }
    }

    private class MusicDatabaseCallback(
        private val context: Context,
        private val scope: CoroutineScope
    ) : Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            database?.let { database ->
                scope.launch {
                    populatePlaylistTable(database.playlistDao())
                }
            }
        }

        suspend fun populatePlaylistTable(playlistDao: PlaylistDao) {
            val defaultPlaylistHelper = DefaultPlaylistHelper(context)
            for (pair in defaultPlaylistHelper.playlistPairs) {
                playlistDao.insert(Playlist(pair.first, pair.second, null, true))
            }
        }
    }
}