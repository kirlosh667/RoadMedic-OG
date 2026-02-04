package com.example.roadmedic

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.util.*

class ReportPotholeActivity : AppCompatActivity() {

    private lateinit var imgPreview: ImageView
    private lateinit var txtLocationOutput: TextView

    private var latestBitmap: Bitmap? = null
    private var lastLat: Double? = null
    private var lastLon: Double? = null
    private var lastAddress: String? = null

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    // CAMERA
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val bitmap =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        result.data?.extras?.getParcelable("data", Bitmap::class.java)
                    else
                        result.data?.extras?.get("data") as? Bitmap

                bitmap?.let {
                    latestBitmap = it
                    imgPreview.setImageBitmap(it)
                }
            }
        }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) openCamera()
        }

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) getLocation()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_pothole)

        findViewById<MaterialToolbar>(R.id.topAppBar)
            .setNavigationOnClickListener { finish() }

        imgPreview = findViewById(R.id.imgPreview)
        txtLocationOutput = findViewById(R.id.txtLocationOutput)

        findViewById<Button>(R.id.btnCapturePhoto).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) openCamera()
            else requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

        findViewById<Button>(R.id.btnGetLocation).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) getLocation()
            else requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        findViewById<Button>(R.id.btnSaveReport).setOnClickListener {
            saveReport()
        }
    }

    private fun openCamera() {
        cameraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
    }

    private fun getLocation() {
        fusedLocationClient.getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { loc ->
            if (loc != null) {
                lastLat = loc.latitude
                lastLon = loc.longitude
                txtLocationOutput.text = "Location: $lastLat, $lastLon"
                fetchAddress(lastLat!!, lastLon!!)
            } else {
                Toast.makeText(this, "Turn ON GPS", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun fetchAddress(lat: Double, lon: Double) {
        Thread {
            try {
                val geo = Geocoder(this, Locale.getDefault())
                val addr = geo.getFromLocation(lat, lon, 1)?.firstOrNull()
                val line = addr?.getAddressLine(0)

                runOnUiThread {
                    lastAddress = line
                    txtLocationOutput.text =
                        "Location: $lat, $lon\n${line ?: ""}"
                }
            } catch (_: Exception) {}
        }.start()
    }

    // SAVE REPORT
    private fun saveReport() {

        val bitmap = latestBitmap ?: run {
            Toast.makeText(this, "Take photo first", Toast.LENGTH_LONG).show()
            return
        }

        val lat = lastLat ?: run {
            Toast.makeText(this, "Get location first", Toast.LENGTH_LONG).show()
            return
        }

        val lon = lastLon!!

        val timestamp = System.currentTimeMillis()

        // ⭐ SAVE IMAGE FILE
        val file = File(filesDir, "pothole_$timestamp.jpg")
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
        }

        val severity = when (findViewById<RadioGroup>(R.id.rgSeverity).checkedRadioButtonId) {
            R.id.rbMedium -> 2
            R.id.rbHigh -> 3
            else -> 1
        }

        val username =
            getSharedPreferences("session", MODE_PRIVATE)
                .getString("username", "guest") ?: "guest"

        // ⭐ FIX HERE — SAVE imagePath
        val data = hashMapOf(
            "timestamp" to timestamp,
            "latitude" to lat,
            "longitude" to lon,
            "severity" to severity,
            "address" to lastAddress,
            "userId" to username,
            "imagePath" to file.absolutePath   // ⭐ IMPORTANT LINE
        )

        FirebaseFirestore.getInstance()
            .collection("potholes")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Uploaded!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "❌ Upload failed", Toast.LENGTH_LONG).show()
            }
    }
}
