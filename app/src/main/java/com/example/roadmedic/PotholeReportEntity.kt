package com.example.roadmedic

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pothole_reports")
data class PotholeReportEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val timestamp: Long,
    val imagePath: String,
    val latitude: Double,
    val longitude: Double,
    val severity: Int,          // 1 = Low, 2 = Medium, 3 = High
    val address: String? = null,

    val userId: String          // ⭐ ADD THIS
)
