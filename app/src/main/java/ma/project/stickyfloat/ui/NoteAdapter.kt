package ma.project.stickyfloat.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ma.project.stickyfloat.R
import ma.project.stickyfloat.services.AnthropicService
import ma.project.stickyfloat.databinding.DialogAiExpandBinding
import ma.project.stickyfloat.databinding.ItemNoteBinding
import ma.project.stickyfloat.model.Note
import ma.project.stickyfloat.model.NoteStatus
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(
    private val lifecycleScope: LifecycleCoroutineScope,
    private val onStatusChange: (Note, NoteStatus) -> Unit,
    private val onDelete: (Note) -> Unit,
    private val onEdit: (Note) -> Unit,
    private val onSaveExpanded: (String) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) {
            binding.tvContent.text = note.content

            val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            binding.tvDate.text = sdf.format(Date(note.createdAt))

            binding.tvContent.paintFlags = when (note.status) {
                NoteStatus.DONE, NoteStatus.CANCELLED ->
                    binding.tvContent.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                else ->
                    binding.tvContent.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            val (chipIcon, chipText) = when (note.status) {
                NoteStatus.IN_PROGRESS -> "🔄" to "In Progress"
                NoteStatus.DONE -> "✅" to "Done"
                NoteStatus.CANCELLED -> "❌" to "Cancelled"
            }
            binding.chipStatus.text = "$chipIcon $chipText"

            binding.btnInProgress.setOnClickListener { onStatusChange(note, NoteStatus.IN_PROGRESS) }
            binding.btnDone.setOnClickListener { onStatusChange(note, NoteStatus.DONE) }
            binding.btnCancelled.setOnClickListener { onStatusChange(note, NoteStatus.CANCELLED) }
            binding.btnDelete.setOnClickListener { onDelete(note) }
            binding.btnEdit.setOnClickListener { onEdit(note) }

            binding.btnAiExpand.setOnClickListener {
                showAiExpandDialog(note)
            }
        }

        private fun showAiExpandDialog(note: Note) {
            val ctx = binding.root.context
            val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_ai_expand, null)
            val dialogBinding = DialogAiExpandBinding.bind(dialogView)

            dialogBinding.tvOriginalNote.text = note.content
            dialogBinding.progressAi.visibility = View.VISIBLE
            dialogBinding.tvAiResult.visibility = View.GONE
            dialogBinding.tvError.visibility = View.GONE

            val dialog = MaterialAlertDialogBuilder(ctx, R.style.NoteDialog)
                .setTitle("✨ Expand Note")
                .setView(dialogView)
                .setNegativeButton("Close", null)
                .setPositiveButton("Save as new note", null) // set below after result
                .show()

            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.isEnabled = false

            lifecycleScope.launch {
                try {
                    val result = AnthropicService.expandNote(note.content)

                    dialogBinding.progressAi.visibility = View.GONE
                    dialogBinding.tvAiResult.visibility = View.VISIBLE
                    dialogBinding.tvAiResult.text = result

                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.let { btn ->
                        btn.isEnabled = true
                        btn.setOnClickListener {
                            onSaveExpanded("${note.content}\n\n$result")
                            dialog.dismiss()
                        }
                    }

                } catch (e: Exception) {
                    dialogBinding.progressAi.visibility = View.GONE
                    dialogBinding.tvError.visibility = View.VISIBLE
                    dialogBinding.tvError.text = "⚠️ ${e.message ?: "Something went wrong. Check your API key and internet connection."}"
                }
            }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Note, newItem: Note) = oldItem == newItem
    }
}