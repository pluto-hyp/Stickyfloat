package ma.project.stickyfloat.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class NoteStatus {
    IN_PROGRESS, DONE, CANCELLED
}

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val status: NoteStatus = NoteStatus.IN_PROGRESS,
    val createdAt: Long = System.currentTimeMillis()
)