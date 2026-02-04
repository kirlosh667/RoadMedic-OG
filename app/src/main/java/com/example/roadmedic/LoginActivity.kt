package com.example.roadmedic

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.roadmedic.admin.AdminDashboardActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // MUST be first
        setContentView(R.layout.activity_login)

        val prefs = getSharedPreferences("session", MODE_PRIVATE)

        val rgRole = findViewById<RadioGroup>(R.id.rgRole)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val rbUser = findViewById<RadioButton>(R.id.rbUser)
        val rbAdmin = findViewById<RadioButton>(R.id.rbAdmin)

        // ✅ Clear fields when switching role
        rgRole.setOnCheckedChangeListener { _, checkedId ->

            etUsername.text.clear()
            etPassword.text.clear()

            if (checkedId == R.id.rbAdmin) {
                etUsername.hint = "Admin username"
                etPassword.hint = "Admin password"
            } else {
                etUsername.hint = "Username (user1,user2...)"
                etPassword.hint = "Password (1234)"
            }
        }

        btnLogin.setOnClickListener {

            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // 🔥 ADMIN LOGIN
            if (rbAdmin.isChecked) {

                if (username == "admin" && password == "admin123") {
                    Toast.makeText(this,"Admin login",Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    return@setOnClickListener
                } else {
                    Toast.makeText(this,"Wrong admin login",Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // 🔥 USER LOGIN
            if (rbUser.isChecked) {

                val validUser = Regex("^user\\d+$").matches(username)

                if (validUser && password == "1234") {
                    prefs.edit().putString("username", username).apply()

                    Toast.makeText(this,"Login success",Toast.LENGTH_SHORT).show()

                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this,"Use user1,user2... and 1234",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
