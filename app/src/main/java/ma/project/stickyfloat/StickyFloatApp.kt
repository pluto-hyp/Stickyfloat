package ma.project.stickyfloat

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class StickyFloatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}