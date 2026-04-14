package ma.project.stickyfloat.data

import android.content.Context
import androidx.room.*
import ma.project.stickyfloat.model.Note
import ma.project.stickyfloat.model.NoteStatus

class Converters {
    @TypeConverter
    fun fromStatus(value: NoteStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): NoteStatus = NoteStatus.valueOf(value)
}

@Database(entities = [Note::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}