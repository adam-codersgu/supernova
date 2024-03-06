package com.codersguidebook.supernova.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.entities.SongPlays
import com.codersguidebook.supernova.utils.DefaultPlaylistHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(entities = [Song::class, Playlist::class, SongPlays::class], version = 2, exportSchema = false)
abstract class MusicDatabase : RoomDatabase() {

    abstract fun musicDao(): MusicDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
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