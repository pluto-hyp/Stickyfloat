package ma.project.stickyfloat.data

import ma.project.stickyfloat.model.Note
import ma.project.stickyfloat.model.NoteStatus
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val dao: NoteDao) {
    val allNotes: Flow<List<Note>> = dao.getAllNotes()

    suspend fun insert(note: Note) = dao.insert(note)
    suspend fun update(note: Note) = dao.update(note)
    suspend fun delete(note: Note) = dao.delete(note)
    suspend fun updateStatus(id: Long, status: NoteStatus) = dao.updateStatus(id, status)
}