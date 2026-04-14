package ma.project.stickyfloat.data

import androidx.room.*
import ma.project.stickyfloat.model.Note
import ma.project.stickyfloat.model.NoteStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("UPDATE notes SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: NoteStatus)
}