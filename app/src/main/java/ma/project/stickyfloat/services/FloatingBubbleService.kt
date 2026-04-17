package ma.project.stickyfloat.services

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.Handler
import android.provider.Settings
import android.view.*
import androidx.core.app.NotificationCompat
import ma.project.stickyfloat.R
import ma.project.stickyfloat.ui.NotesActivity
import ma.project.stickyfloat.data.NoteDatabase
import ma.project.stickyfloat.data.NoteRepository
import ma.project.stickyfloat.model.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import android.widget.EditText
import android.widget.Button
import android.widget.Toast
import android.view.ContextThemeWrapper

class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: View
    private var popupView: View? = null

    private lateinit var repository: NoteRepository
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var lastClickTime: Long = 0
    private val DOUBLE_CLICK_TIME_DELTA: Long = 500

    companion object {
        const val CHANNEL_ID = "sticky_float_channel"
        const val NOTIF_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val database = NoteDatabase.getDatabase(this)
        repository = NoteRepository(database.noteDao())
        if (Settings.canDrawOverlays(this)) {
            showBubble()
        }
    }

    private fun showBubble() {
        bubbleView = LayoutInflater.from(this).inflate(R.layout.layout_bubble, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 200
        }

        // Drag logic
        var initialX = 0; var initialY = 0
        var initialTouchX = 0f; var initialTouchY = 0f
        var isDragging = false

        bubbleView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) {
                        isDragging = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(bubbleView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        val clickTime = System.currentTimeMillis()
                        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                            openNotesActivity()
                        } else {
                            showNotePopup()
                        }
                        lastClickTime = clickTime
                    }
                    v.performClick()
                    true
                }
                else -> true  // was "false" — this was dropping events on some devices
            }
        }

        windowManager.addView(bubbleView, params)
    }

    private fun openNotesActivity() {
        val intent = Intent(this, NotesActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        dismissPopup()
    }

    private fun showNotePopup() {
        if (popupView != null) return

        val contextWrapper = ContextThemeWrapper(this, R.style.Theme_StickyFloat)
        popupView = LayoutInflater.from(contextWrapper).inflate(R.layout.dialog_sticky_note, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            dimAmount = 0.6f
        }

        val etNote = popupView?.findViewById<EditText>(R.id.et_note)
        val btnSave = popupView?.findViewById<Button>(R.id.btn_save)
        val btnCancel = popupView?.findViewById<Button>(R.id.btn_cancel)

        btnSave?.setOnClickListener {
            val content = etNote?.text?.toString()?.trim() ?: ""
            if (content.isNotEmpty()) {
                saveNote(content)
            } else {
                Toast.makeText(this, "Please write something first", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel?.setOnClickListener {
            dismissPopup()
        }

        popupView?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                dismissPopup()
            }
            true
        }

        windowManager.addView(popupView, params)
    }

    private fun saveNote(content: String) {
        serviceScope.launch {
            repository.insert(Note(content = content))
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this@FloatingBubbleService, "Note saved! 📌", Toast.LENGTH_SHORT).show()
                dismissPopup()
            }
        }
    }

    private fun dismissPopup() {
        if (popupView != null && popupView?.isAttachedToWindow == true) {
            windowManager.removeView(popupView)
            popupView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissPopup()
        if (::bubbleView.isInitialized && bubbleView.isAttachedToWindow) {
            windowManager.removeView(bubbleView)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "StickyFloat Bubble",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Keeps the floating note bubble active" }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("StickyFloat active")
            .setContentText("Tap the bubble to manage your notes")
            .setSmallIcon(R.drawable.ic_note)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
}