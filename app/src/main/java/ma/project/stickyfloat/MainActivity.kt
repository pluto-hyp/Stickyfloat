package ma.project.stickyfloat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ma.project.stickyfloat.databinding.ActivityMainBinding
import ma.project.stickyfloat.services.FloatingBubbleService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val OVERLAY_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStart.setOnClickListener { requestOverlayPermission() }
        binding.btnStop.setOnClickListener { stopService() }
        binding.btnStoredNotes.setOnClickListener { openNotesActivity() }
    }

    private fun openNotesActivity() {
        val intent = Intent(this, ma.project.stickyfloat.ui.NotesActivity::class.java)
        startActivity(intent)
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_CODE)
        } else {
            startBubbleService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_CODE) {
            if (Settings.canDrawOverlays(this)) {
                startBubbleService()
            } else {
                Toast.makeText(this, "Permission required to show floating bubble", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startBubbleService() {
        val intent = Intent(this, FloatingBubbleService::class.java)
        startForegroundService(intent)
        Toast.makeText(this, "Bubble is now floating! 🟡", Toast.LENGTH_SHORT).show()
        moveTaskToBack(true)
    }

    private fun stopService() {
        stopService(Intent(this, FloatingBubbleService::class.java))
        Toast.makeText(this, "Bubble stopped", Toast.LENGTH_SHORT).show()
    }
}