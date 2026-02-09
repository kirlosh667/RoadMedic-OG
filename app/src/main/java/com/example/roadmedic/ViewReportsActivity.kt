package com.example.roadmedic

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore

class ViewReportsActivity : AppCompatActivity() {

    private lateinit var listReports: ListView
    private lateinit var tvTitle: TextView
    private lateinit var btnClearMy: Button

    private val db = FirebaseFirestore.getInstance()

    private var rawData = mutableListOf<HashMap<String, Any>>()
    private var docIds = mutableListOf<String>()

    private var isMyReports = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_reports)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(topAppBar)
        topAppBar.setNavigationOnClickListener { finish() }

        tvTitle = findViewById(R.id.tvTitle)
        listReports = findViewById(R.id.listReports)
        btnClearMy = findViewById(R.id.btnClearMy)

        findViewById<Button>(R.id.btnMyReports)
            .setOnClickListener { loadReports(true) }

        findViewById<Button>(R.id.btnOtherReports)
            .setOnClickListener { loadReports(false) }

        btnClearMy.setOnClickListener { clearMyReports() }

        loadReports(true)
    }

    private fun loadReports(myReports: Boolean) {

        isMyReports = myReports
        btnClearMy.visibility = if (myReports) View.VISIBLE else View.GONE

        val currentUser =
            getSharedPreferences("session", MODE_PRIVATE)
                .getString("username", "") ?: ""

        val query =
            if (myReports)
                db.collection("potholes").whereEqualTo("userId", currentUser)
            else
                db.collection("potholes").whereNotEqualTo("userId", currentUser)

        query.get().addOnSuccessListener { result ->

            rawData.clear()
            docIds.clear()

            val displayList = mutableListOf<String>()

            for (doc in result) {

                val data = HashMap(doc.data)

                rawData.add(data)
                docIds.add(doc.id)

                val time =
                    (data["timestamp"] as? Long)
                        ?: data["timestamp"]?.toString()?.toLongOrNull()
                        ?: 0L

                val timeString =
                    android.text.format.DateFormat
                        .format("yyyy-MM-dd HH:mm", time)

                // ⭐ SHOW USERNAME HERE
                displayList.add(
                    "👤 Reported by: ${data["userId"]}\n" +
                            "Time: $timeString\n" +
                            "Severity: ${data["severity"]}\n" +
                            "Lat: ${data["latitude"]}, Lon: ${data["longitude"]}\n" +
                            "${data["address"] ?: ""}"
                )
            }

            tvTitle.text =
                if (myReports)
                    "My Reports (${displayList.size})"
                else
                    "Other Reports (${displayList.size})"

            listReports.adapter = object : ArrayAdapter<String>(
                this,
                R.layout.list_item_report,
                R.id.txtItem,
                displayList
            ) {
                override fun getView(
                    pos: Int,
                    convertView: View?,
                    parent: ViewGroup
                ): View {

                    val view =
                        super.getView(pos, convertView, parent)

                    val btnDelete =
                        view.findViewById<Button>(R.id.btnDelete)

                    if (isMyReports) {
                        btnDelete.visibility = View.VISIBLE
                        btnDelete.setOnClickListener {
                            deleteReport(pos)
                        }
                    } else {
                        btnDelete.visibility = View.GONE
                    }

                    return view
                }
            }

            listReports.setOnItemClickListener { _, _, pos, _ ->
                openDetail(rawData[pos])
            }
        }
    }

    private fun deleteReport(pos: Int) {

        AlertDialog.Builder(this)
            .setTitle("Delete Report")
            .setMessage("Delete this report?")
            .setPositiveButton("Yes") { _, _ ->

                db.collection("potholes")
                    .document(docIds[pos])
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Deleted",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadReports(true)
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun clearMyReports() {

        val currentUser =
            getSharedPreferences("session", MODE_PRIVATE)
                .getString("username", "") ?: ""

        db.collection("potholes")
            .whereEqualTo("userId", currentUser)
            .get()
            .addOnSuccessListener { result ->

                val batch = db.batch()
                for (doc in result)
                    batch.delete(doc.reference)

                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Cleared",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadReports(true)
                    }
            }
    }

    private fun openDetail(data: HashMap<String, Any>) {

        val intent = Intent(this, ReportDetailActivity::class.java)

        intent.putExtra(
            "timestamp",
            data["timestamp"].toString()
        )

        intent.putExtra(
            "lat",
            data["latitude"].toString()
        )

        intent.putExtra(
            "lon",
            data["longitude"].toString()
        )

        // ⭐ PASS USERNAME TO DETAIL PAGE
        intent.putExtra(
            "userId",
            data["userId"]?.toString() ?: "Unknown"
        )

        intent.putExtra(
            "imageUrl",
            data["imageUrl"]?.toString() ?: ""
        )

        intent.putExtra(
            "imagePath",
            data["imagePath"]?.toString() ?: ""
        )

        startActivity(intent)
    }
}
