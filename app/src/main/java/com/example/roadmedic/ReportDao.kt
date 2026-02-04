
package com.example.roadmedic

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ReportDao {

    @Insert
    fun insert(report: PotholeReportEntity)

    // ALL
    @Query("SELECT * FROM pothole_reports ORDER BY timestamp DESC")
    fun getAllReports(): List<PotholeReportEntity>

    // MY REPORTS
    @Query("SELECT * FROM pothole_reports WHERE userId = :userId ORDER BY timestamp DESC")
    fun getMyReports(userId: String): List<PotholeReportEntity>

    // OTHER REPORTS
    @Query("SELECT * FROM pothole_reports WHERE userId != :userId ORDER BY timestamp DESC")
    fun getOtherReports(userId: String): List<PotholeReportEntity>

    @Query("DELETE FROM pothole_reports")
    fun deleteAll()
}
