package com.example.roadmedic.admin

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.roadmedic.R
import com.google.firebase.database.*

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var dbRef: DatabaseReference
    private val reportsList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        listView = findViewById(R.id.listReports)

        dbRef = FirebaseDatabase.getInstance().getReference("reports")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            reportsList
        )

        listView.adapter = adapter

        // 🔥 REAL-TIME UPDATES
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                reportsList.clear()

                for (child in snapshot.children) {
                    val user = child.child("user").value.toString()
                    val lat = child.child("latitude").value.toString()
                    val lon = child.child("longitude").value.toString()
                    val sev = child.child("severity").value.toString()

                    reportsList.add(
                        "User: $user\nLat: $lat\nLon: $lon\nSeverity: $sev"
                    )
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
