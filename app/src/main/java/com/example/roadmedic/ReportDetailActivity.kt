package com.example.roadmedic

import android.content.Intent
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ReportDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_detail)

        // Toolbar
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Views
        val imgDetail = findViewById<ImageView>(R.id.imgDetail)
        val tvDetailTime = findViewById<TextView>(R.id.tvDetailTime)
        val tvDetailLocation = findViewById<TextView>(R.id.tvDetailLocation)
        val btnOpenMap = findViewById<Button>(R.id.btnOpenInMaps)
        val btnShare = findViewById<Button>(R.id.btnShareReport)

        // Intent data
        val timestampStr = intent.getStringExtra("timestamp")
        val imagePath = intent.getStringExtra("imagePath")   // old local
        val imageUrl = intent.getStringExtra("imageUrl")     // new cloudinary
        val latStr = intent.getStringExtra("lat")
        val lonStr = intent.getStringExtra("lon")

        if (timestampStr == null || latStr == null || lonStr == null) {
            Toast.makeText(this, "Missing report data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // TIME
        val timestamp = timestampStr.toLong()
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val formattedTime = sdf.format(Date(timestamp))
        tvDetailTime.text = "🕒 $formattedTime"

        // LOCATION
        val lat = latStr.toDouble()
        val lon = lonStr.toDouble()

        // ⭐ IMAGE LOADING (Cloudinary + Local fallback)

        when {
            // ✅ Cloudinary URL
            !imageUrl.isNullOrEmpty() -> {
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(imgDetail)
            }

            // ✅ Old local file support
            !imagePath.isNullOrEmpty() -> {
                val file = File(imagePath)

                if (file.exists()) {
                    imgDetail.setImageBitmap(
                        BitmapFactory.decodeFile(file.absolutePath)
                    )
                } else {
                    imgDetail.setImageResource(
                        android.R.drawable.ic_menu_report_image
                    )
                }
            }

            // ❌ No image
            else -> {
                imgDetail.setImageResource(
                    android.R.drawable.ic_menu_report_image
                )
            }
        }

        // ADDRESS
        Thread {
            val address = getAddressFromLatLng(lat, lon)
            runOnUiThread {
                tvDetailLocation.text = "📍 $address"
            }
        }.start()

        // MAP
        btnOpenMap.setOnClickListener {
            val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

        // SHARE
        btnShare.setOnClickListener {

            val shareText = """
                🚧 Pothole Report
                
                📍 Location: ${tvDetailLocation.text}
                🕒 Time: $formattedTime
                
                https://maps.google.com/?q=$lat,$lon
            """.trimIndent()

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)

            startActivity(Intent.createChooser(shareIntent, "Share Report"))
        }
    }

    private fun getAddressFromLatLng(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)

            if (!addresses.isNullOrEmpty())
                addresses[0].getAddressLine(0) ?: "$lat, $lon"
            else "$lat, $lon"

        } catch (e: Exception) {
            "$lat, $lon"
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
