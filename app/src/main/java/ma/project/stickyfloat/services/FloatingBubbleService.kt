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

    private var removeView: View? = null
    private lateinit var removeParams: WindowManager.LayoutParams
    private var isOverRemove = false

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
            prepareRemoveView()
        }
    }

    private fun prepareRemoveView() {
        removeView = LayoutInflater.from(this).inflate(R.layout.layout_remove_bubble, null)
        removeParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 100 // Distance from bottom
        }
    }

    private fun showRemoveView() {
        if (removeView?.isAttachedToWindow == false) {
            windowManager.addView(removeView, removeParams)
        }
    }

    private fun hideRemoveView() {
        if (removeView?.isAttachedToWindow == true) {
            windowManager.removeView(removeView)
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
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true
                        showRemoveView()
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(bubbleView, params)
                        checkProximity(event.rawX, event.rawY)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    hideRemoveView()
                    if (isOverRemove) {
                        stopSelf()
                        return@setOnTouchListener true
                    }
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
                else -> true
            }
        }

        windowManager.addView(bubbleView, params)
    }

    private fun checkProximity(rawX: Float, rawY: Float) {
        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        // Target is bottom center
        val targetX = screenWidth / 2f
        val targetY = screenHeight - 200f // Approximate position of remove icon center

        val distance = Math.sqrt(
            Math.pow((rawX - targetX).toDouble(), 2.0) +
                    Math.pow((rawY - targetY).toDouble(), 2.0)
        )

        val wasOverRemove = isOverRemove
        isOverRemove = distance < 250 // threshold pixels

        if (wasOverRemove != isOverRemove) {
            // Visual feedback: scale up the remove icon
            val scale = if (isOverRemove) 1.5f else 1.0f
            removeView?.findViewById<View>(R.id.iv_remove)?.animate()
                ?.scaleX(scale)
                ?.scaleY(scale)
                ?.setDuration(200)
                ?.start()
        }
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
        hideRemoveView()
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