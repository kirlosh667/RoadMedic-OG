package com.example.roadmedic

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Toolbar
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(topAppBar)

        // Username from session
        val prefs = getSharedPreferences("session", MODE_PRIVATE)
        val username = prefs.getString("username", "Guest")

        // Show username in centered TextView
        val txtLoggedIn = findViewById<TextView>(R.id.txtLoggedIn)
        txtLoggedIn.text = "Logged in as: $username"

        // Buttons
        findViewById<Button>(R.id.btnReportPothole).setOnClickListener {
            startActivity(Intent(this, ReportPotholeActivity::class.java))
        }

        findViewById<Button>(R.id.btnViewReports).setOnClickListener {
            startActivity(Intent(this, ViewReportsActivity::class.java))
        }

        findViewById<Button>(R.id.btnViewMap).setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
    }

    // Menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.bottom_menu, menu)
        return true
    }

    // Logout
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_profile) {

            val prefs = getSharedPreferences("session", MODE_PRIVATE)
            val username = prefs.getString("username", "User")

            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Do you want to logout, $username?")
                .setPositiveButton("Yes") { _, _ ->
                    prefs.edit().clear().apply()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()

            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
