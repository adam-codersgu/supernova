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

@Database(entities = [Song::class, Playlist::class, SongPlays::class], version = 4, exportSchema = false)
abstract class MusicDatabase : RoomDatabase() {

    abstract fun musicDao(): MusicDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun songPlaysDao(): SongPlaysDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val epochDay = LocalDate.now().minusWeeks(2).toEpochDay()
                db.execSQL("CREATE TABLE IF NOT EXISTS `SongPlays` " +
                        "(`songPlaysId` INTEGER NOT NULL, `songId` INTEGER NOT NULL, " +
                        "`epochDays` INTEGER NOT NULL, `qtyOfPlays` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`songPlaysId`))")
                db.execSQL("INSERT INTO SongPlays (songId, epochDays, qtyOfPlays) " +
                        "SELECT songId, $epochDay, song_plays FROM music_library")
                db.execSQL("CREATE TABLE IF NOT EXISTS `music_library_backup` " +
                        "(`songId` INTEGER NOT NULL, `song_track` INTEGER NOT NULL, " +
                        "`song_title` TEXT, `song_artist` TEXT, `song_album_name` TEXT, " +
                        "`song_album_id` TEXT NOT NULL, `song_year` TEXT NOT NULL, " +
                        "`song_favourite` INTEGER NOT NULL, PRIMARY KEY(`songId`))")
                db.execSQL("INSERT INTO music_library_backup SELECT " +
                        "songId, song_track, song_title, song_artist, song_album_name, " +
                        "song_album_id, song_year, song_favourite FROM music_library")
                db.execSQL("DROP TABLE music_library")
                db.execSQL("ALTER TABLE music_library_backup RENAME TO music_library")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `SongPlays_backup` " +
                        "(`songPlaysId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`songId` INTEGER NOT NULL, `epochDays` INTEGER NOT NULL, " +
                        "`qtyOfPlays` INTEGER NOT NULL)")
                db.execSQL("INSERT INTO SongPlays_backup SELECT " +
                        "songPlaysId, songId, epochDays, qtyOfPlays " +
                        "FROM SongPlays")
                db.execSQL("DROP TABLE SongPlays")
                db.execSQL("ALTER TABLE SongPlays_backup RENAME TO SongPlays")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE music_library ADD COLUMN remember_progress " +
                        "INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE music_library ADD COLUMN playback_progress " +
                        "INTEGER NOT NULL DEFAULT 0")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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