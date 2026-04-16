package ma.project.stickyfloat.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import ma.project.stickyfloat.R
import ma.project.stickyfloat.model.Note
import ma.project.stickyfloat.model.NoteStatus
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(
    private var notes: List<Note> = emptyList(),
    private val onDelete: (Note) -> Unit,
    private val onUpdateStatus: (Note, NoteStatus) -> Unit,
    private val onEdit: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    fun updateNotes(newNotes: List<Note>) {
        notes = newNotes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(notes[position])
    }

    override fun getItemCount(): Int = notes.size

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val chipStatus: Chip = itemView.findViewById(R.id.chipStatus)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnInProgress: MaterialButton = itemView.findViewById(R.id.btnInProgress)
        private val btnDone: MaterialButton = itemView.findViewById(R.id.btnDone)
        private val btnCancelled: MaterialButton = itemView.findViewById(R.id.btnCancelled)

        private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

        fun bind(note: Note) {
            tvContent.text = note.content
            tvDate.text = dateFormat.format(Date(note.createdAt))
            
            // Status Chip
            chipStatus.text = note.status.name.replace("_", " ")
            val textColor = when (note.status) {
                NoteStatus.IN_PROGRESS -> "#6699FF"
                NoteStatus.DONE -> "#44BB44"
                NoteStatus.CANCELLED -> "#BB4444"
            }
            
            chipStatus.setTextColor(android.graphics.Color.parseColor(textColor))
            chipStatus.chipStrokeColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(textColor))
            chipStatus.chipBackgroundColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(textColor).let { 
                android.graphics.Color.argb(30, android.graphics.Color.red(it), android.graphics.Color.green(it), android.graphics.Color.blue(it))
            })

            // Click listeners
            btnDelete.setOnClickListener { onDelete(note) }
            btnEdit.setOnClickListener { onEdit(note) }
            
            btnInProgress.setOnClickListener { onUpdateStatus(note, NoteStatus.IN_PROGRESS) }
            btnDone.setOnClickListener { onUpdateStatus(note, NoteStatus.DONE) }
            btnCancelled.setOnClickListener { onUpdateStatus(note, NoteStatus.CANCELLED) }
        }
    }
}