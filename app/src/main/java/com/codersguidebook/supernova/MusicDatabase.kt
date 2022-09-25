package com.codersguidebook.supernova

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(entities = [Song::class, Playlist::class], version = 1, exportSchema = false)
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
                    .addCallback(MusicDatabaseCallback(scope))
                    .build()
            }

            return database!!
        }
    }

    private class MusicDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            database?.let { database ->
                scope.launch {
                    populatePlaylistTable(database.playlistDao())
                }
            }
        }

        suspend fun populatePlaylistTable(playlistDao: PlaylistDao) {
            val defaultPlaylistNames = listOf(
                "Favourites",
                "Recently played",
                "Song of the day",
                "Most played"
            )
            for (p in defaultPlaylistNames) {
                playlistDao.insert(Playlist(0, p, null, true))
            }
        }
    }
}