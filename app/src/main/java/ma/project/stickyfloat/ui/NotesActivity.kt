package ma.project.stickyfloat.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ma.project.stickyfloat.R
import ma.project.stickyfloat.data.NoteDatabase
import ma.project.stickyfloat.data.NoteRepository
import ma.project.stickyfloat.model.Note
import ma.project.stickyfloat.model.NoteStatus

class NotesActivity : AppCompatActivity() {

    private lateinit var repository: NoteRepository
    private lateinit var adapter: NoteAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var fabAdd: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notes)

        // Fix for the NullPointerException: root ID is "main"
        val rootView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initDatabase()
        initViews()
        setupRecyclerView()
        observeNotes()
    }

    private fun initDatabase() {
        val database = NoteDatabase.getDatabase(this)
        repository = NoteRepository(database.noteDao())
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerNotes)
        tvEmpty = findViewById(R.id.tvEmpty)
        fabAdd = findViewById(R.id.fabAddNote)
        val btnClose: View = findViewById(R.id.btnClose)

        btnClose.setOnClickListener { finish() }
        fabAdd.setOnClickListener { showAddNoteDialog() }
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter(
            onDelete = { note -> deleteNote(note) },
            onUpdateStatus = { note, status -> updateNoteStatus(note, status) },
            onEdit = { note -> showEditNoteDialog(note) }
        )
        recyclerView.adapter = adapter
    }

    private fun observeNotes() {
        lifecycleScope.launch {
            repository.allNotes.collectLatest { notes ->
                adapter.updateNotes(notes)
                tvEmpty.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showAddNoteDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_note, null)
        val etContent = dialogView.findViewById<EditText>(R.id.etNoteContent)

        AlertDialog.Builder(this, R.style.NoteDialog)
            .setTitle("Add New Note")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val content = etContent.text.toString().trim()
                if (content.isNotEmpty()) {
                    addNewNote(content)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditNoteDialog(note: Note) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_note, null)
        val etContent = dialogView.findViewById<EditText>(R.id.etNoteContent)
        etContent.setText(note.content)

        AlertDialog.Builder(this, R.style.NoteDialog)
            .setTitle("Edit Note")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val content = etContent.text.toString().trim()
                if (content.isNotEmpty()) {
                    updateNoteContent(note, content)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addNewNote(content: String) {
        lifecycleScope.launch {
            repository.insert(Note(content = content))
            Toast.makeText(this@NotesActivity, "Note added", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateNoteContent(note: Note, content: String) {
        lifecycleScope.launch {
            repository.update(note.copy(content = content))
        }
    }

    private fun updateNoteStatus(note: Note, status: NoteStatus) {
        lifecycleScope.launch {
            repository.updateStatus(note.id, status)
        }
    }

    private fun deleteNote(note: Note) {
        lifecycleScope.launch {
            repository.delete(note)
            Toast.makeText(this@NotesActivity, "Note deleted", Toast.LENGTH_SHORT).show()
        }
    }
}