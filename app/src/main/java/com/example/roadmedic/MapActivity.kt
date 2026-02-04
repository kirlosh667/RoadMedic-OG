package com.example.roadmedic

import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import java.io.File
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

        // ⭐ OSMDroid setup
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osm", MODE_PRIVATE)
        )

        // ⭐ REQUIRED — prevents crash
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_map)

        map = findViewById(R.id.map)
        map.setMultiTouchControls(true)

        etFrom = findViewById(R.id.etFrom)
        etTo = findViewById(R.id.etTo)
        btnRoute = findViewById(R.id.btnRoute)

        // ⭐ My Location
        myLocationOverlay =
            MyLocationNewOverlay(GpsMyLocationProvider(this), map)

        myLocationOverlay.enableMyLocation()
        map.overlays.add(myLocationOverlay)

        // Wait for GPS fix
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
                    val imagePath = doc.getString("imagePath")

                    val marker = Marker(map)
                    marker.position = GeoPoint(lat, lon)

                    // 🎨 Severity icons
                    marker.icon = when (severity) {
                        1 -> ContextCompat.getDrawable(this, R.drawable.ic_pothole_low)
                        2 -> ContextCompat.getDrawable(this, R.drawable.ic_pothole_medium)
                        else -> ContextCompat.getDrawable(this, R.drawable.ic_pothole_high)
                    }

                    // ⭐ Custom popup with photo
                    marker.infoWindow =
                        object : InfoWindow(R.layout.marker_info_window, map) {

                            override fun onOpen(item: Any?) {

                                val img =
                                    mView.findViewById<ImageView>(R.id.infoImage)
                                val title =
                                    mView.findViewById<TextView>(R.id.infoTitle)
                                val address =
                                    mView.findViewById<TextView>(R.id.infoAddress)

                                title.text = when (severity) {
                                    1 -> "🟡 LOW pothole"
                                    2 -> "🟠 MEDIUM pothole"
                                    else -> "🔴 HIGH pothole"
                                }

                                address.text =
                                    doc.getString("address") ?: "Unknown"

                                // 📸 Load photo
                                if (!imagePath.isNullOrEmpty()) {
                                    val file = File(imagePath)
                                    if (file.exists()) {
                                        img.setImageBitmap(
                                            BitmapFactory.decodeFile(file.absolutePath)
                                        )
                                    } else {
                                        img.setImageResource(
                                            android.R.drawable.ic_menu_report_image
                                        )
                                    }
                                } else {
                                    img.setImageResource(
                                        android.R.drawable.ic_menu_report_image
                                    )
                                }
                            }

                            override fun onClose() {}
                        }

                    marker.setOnMarkerClickListener { m, _ ->
                        m.showInfoWindow()
                        true
                    }

                    map.overlays.add(marker)

                    // 📏 Distance calculation
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

                // ⭐ Show nearest pothole
                if (nearest != Float.MAX_VALUE) {
                    Toast.makeText(
                        this,
                        "🚧 Nearest pothole: ${nearest.toInt()} m",
                        Toast.LENGTH_LONG
                    ).show()
                }

                // ⭐ Zoom to user
                if (myLoc != null) {
                    map.controller.animateTo(
                        GeoPoint(myLoc.latitude, myLoc.longitude)
                    )
                    map.controller.setZoom(16.0)
                }

                map.invalidate()
            }
    }

    // ---------------- ROUTING ----------------
    private fun drawRoute() {

        val start = getGeoPoint(etFrom.text.toString())
        val end = getGeoPoint(etTo.text.toString())

        if (start == null || end == null) {
            Toast.makeText(this, "Invalid location", Toast.LENGTH_SHORT).show()
            return
        }

        // Remove old routes
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

        } catch (e: Exception) { null }
    }
}
