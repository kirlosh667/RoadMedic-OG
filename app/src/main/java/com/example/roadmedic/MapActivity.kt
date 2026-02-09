package com.example.roadmedic

import android.app.AlertDialog
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*

class MapActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var myLocationOverlay: MyLocationNewOverlay

    private lateinit var etFrom: EditText
    private lateinit var etTo: EditText
    private lateinit var btnRoute: Button

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osm", MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_map)

        map = findViewById(R.id.map)
        map.setMultiTouchControls(true)

        // Close popup on map tap
        map.setOnTouchListener { _, _ ->
            InfoWindow.closeAllInfoWindowsOn(map)
            false
        }

        etFrom = findViewById(R.id.etFrom)
        etTo = findViewById(R.id.etTo)
        btnRoute = findViewById(R.id.btnRoute)

        myLocationOverlay =
            MyLocationNewOverlay(GpsMyLocationProvider(this), map)

        myLocationOverlay.enableMyLocation()
        map.overlays.add(myLocationOverlay)

        myLocationOverlay.runOnFirstFix {
            runOnUiThread { loadPotholes() }
        }

        btnRoute.setOnClickListener { drawRoute() }
    }

    // ---------------- LOAD POTHOLES ----------------
    private fun loadPotholes() {

        db.collection("potholes").get()
            .addOnSuccessListener { result ->

                var nearest = Float.MAX_VALUE
                val myLoc = myLocationOverlay.lastFix

                for (doc in result) {

                    val lat = doc.getDouble("latitude") ?: continue
                    val lon = doc.getDouble("longitude") ?: continue
                    val severity = (doc.getLong("severity") ?: 1).toInt()
                    val imageUrl = doc.getString("imageUrl")

                    val marker = Marker(map)
                    marker.position = GeoPoint(lat, lon)

                    marker.icon = when (severity) {
                        1 -> ContextCompat.getDrawable(this, R.drawable.ic_pothole_low)
                        2 -> ContextCompat.getDrawable(this, R.drawable.ic_pothole_medium)
                        else -> ContextCompat.getDrawable(this, R.drawable.ic_pothole_high)
                    }

                    // INFO WINDOW
                    marker.infoWindow =
                        object : InfoWindow(R.layout.marker_info_window, map) {

                            override fun onOpen(item: Any?) {

                                val img = mView.findViewById<ImageView>(R.id.infoImage)
                                val title = mView.findViewById<TextView>(R.id.infoTitle)
                                val badge = mView.findViewById<TextView>(R.id.infoSeverityBadge)
                                val reporter = mView.findViewById<TextView>(R.id.infoReporter)
                                val address = mView.findViewById<TextView>(R.id.infoAddress)

                                // Severity UI
                                when (severity) {
                                    1 -> {
                                        title.text = "Low pothole"
                                        badge.text = "LOW"
                                        badge.setBackgroundColor(0xFFFFC107.toInt())
                                    }
                                    2 -> {
                                        title.text = "Medium pothole"
                                        badge.text = "MEDIUM"
                                        badge.setBackgroundColor(0xFFFF9800.toInt())
                                    }
                                    else -> {
                                        title.text = "High pothole"
                                        badge.text = "HIGH"
                                        badge.setBackgroundColor(0xFFE53935.toInt())
                                    }
                                }

                                reporter.text =
                                    "Reported by: ${doc.getString("userId") ?: "Unknown"}"

                                address.text =
                                    doc.getString("address") ?: "Unknown location"

                                // Image load
                                if (!imageUrl.isNullOrEmpty()) {
                                    Glide.with(img)
                                        .asBitmap()
                                        .load(imageUrl)
                                        .into(object :
                                            CustomTarget<android.graphics.Bitmap>() {

                                            override fun onResourceReady(
                                                resource: android.graphics.Bitmap,
                                                transition: Transition<in android.graphics.Bitmap>?
                                            ) {
                                                img.setImageBitmap(resource)
                                            }

                                            override fun onLoadCleared(
                                                placeholder: android.graphics.drawable.Drawable?
                                            ) {}
                                        })
                                } else {
                                    img.setImageResource(
                                        android.R.drawable.ic_menu_report_image
                                    )
                                }

                                // ⭐ CLICK ON POPUP -> ACTIONS
                                mView.setOnClickListener {
                                    showActionDialog(
                                        lat,
                                        lon,
                                        address.text.toString()
                                    )
                                }
                            }

                            override fun onClose() {}
                        }

                    // Click behavior
                    marker.setOnMarkerClickListener { m, _ ->
                        InfoWindow.closeAllInfoWindowsOn(map)
                        m.showInfoWindow()
                        true
                    }

                    map.overlays.add(marker)

                    // Nearest pothole calc
                    if (myLoc != null) {
                        val res = FloatArray(1)
                        Location.distanceBetween(
                            myLoc.latitude,
                            myLoc.longitude,
                            lat,
                            lon,
                            res
                        )

                        if (res[0] < nearest)
                            nearest = res[0]
                    }
                }

                if (nearest != Float.MAX_VALUE) {
                    Toast.makeText(
                        this,
                        "🚧 Nearest pothole: ${nearest.toInt()} m",
                        Toast.LENGTH_LONG
                    ).show()
                }

                myLoc?.let {
                    map.controller.animateTo(
                        GeoPoint(it.latitude, it.longitude)
                    )
                    map.controller.setZoom(16.0)
                }

                map.invalidate()
            }
    }

    // ⭐ ACTION DIALOG
// ⭐ ACTION DIALOG (PRO UI)
    private fun showActionDialog(lat: Double, lon: Double, address: String) {

        val options = arrayOf(
            "Open in Google Maps",
            "Share location"
        )

        val dialog = AlertDialog.Builder(this, R.style.PremiumDialog)
            .setTitle("Choose action")
            .setItems(options) { _, which ->

                when (which) {

                    // Open in Maps
                    0 -> {
                        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(Pothole)")
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }

                    // Share
                    1 -> {
                        val shareText =
                            "Pothole reported at:\n$address\nhttps://maps.google.com/?q=$lat,$lon"

                        val intent = Intent(Intent.ACTION_SEND)
                        intent.type = "text/plain"
                        intent.putExtra(Intent.EXTRA_TEXT, shareText)
                        startActivity(Intent.createChooser(intent, "Share via"))
                    }
                }
            }
            .create()

        dialog.show()

        // ⭐ Rounded background + border
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_bg)
    }

    // ---------------- ROUTING ----------------
    private fun drawRoute() {

        val start = getGeoPoint(etFrom.text.toString())
        val end = getGeoPoint(etTo.text.toString())

        if (start == null || end == null) {
            Toast.makeText(this, "Invalid location", Toast.LENGTH_SHORT).show()
            return
        }

        map.overlays.removeAll { it is Polyline }

        val roadManager = OSRMRoadManager(this, "RoadMedic")
        val waypoints = arrayListOf(start, end)

        Thread {
            val road = roadManager.getRoad(waypoints)

            runOnUiThread {

                val polyline =
                    RoadManager.buildRoadOverlay(road)

                map.overlays.add(polyline)

                map.controller.setZoom(15.0)
                map.controller.setCenter(start)

                Toast.makeText(
                    this,
                    "🛣️ Route distance: ${(road.mLength * 1000).toInt()} m",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.start()
    }

    // ---------------- GEOCODER ----------------
    private fun getGeoPoint(text: String): GeoPoint? {

        if (text.lowercase() == "my")
            return myLocationOverlay.myLocation

        return try {
            val geo = Geocoder(this, Locale.getDefault())
            val addr = geo.getFromLocationName(text, 1)

            if (!addr.isNullOrEmpty())
                GeoPoint(addr[0].latitude, addr[0].longitude)
            else null

        } catch (e: Exception) {
            null
        }
    }
}
