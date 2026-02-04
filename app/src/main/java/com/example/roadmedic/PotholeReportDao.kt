package com.example.roadmedic

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PotholeReportDao {

    @Insert
    fun insertReport(report: PotholeReport)

    @Query("SELECT * FROM pothole_reports")
    fun getAllReports(): List<PotholeReport>
}
