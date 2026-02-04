package com.example.roadmedic

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pothole_reports")
data class PotholeReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imagePath: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)
