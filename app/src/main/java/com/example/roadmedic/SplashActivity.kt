package com.example.roadmedic

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Use the splash theme for nice instant color
        setTheme(R.style.Theme_RoadMedic_Splash)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Simple delay, then go to main screen
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1500) // 1.5 seconds
    }
}
