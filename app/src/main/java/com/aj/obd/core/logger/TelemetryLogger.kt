package com.aj.obd.core.logger

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.aj.obd.domain.model.LiveSensor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TelemetryLogger(private val context: Context) {

    private var currentLogFile: File? = null
    private var fileWriter: FileWriter? = null
    private var isRecording = false

    fun isLogging(): Boolean = isRecording

    suspend fun startLogging(vehicleName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val logDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) 
                ?: File(context.filesDir, "datalog")
            if (!logDir.exists()) logDir.mkdirs()

            val cleanName = vehicleName.replace(" ", "_").replace("/", "-")
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(logDir, "DATALOG_${cleanName}_$timestamp.csv")

            fileWriter = FileWriter(file, true)
            // Cabeçalho CSV
            fileWriter?.write("timestamp_ms,time,rpm,speed_kmh,map_turbo_bar,coolant_temp_c,battery_voltage_v\n")
            fileWriter?.flush()

            currentLogFile = file
            isRecording = true
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logSnapshot(sensors: List<LiveSensor>) = withContext(Dispatchers.IO) {
        if (!isRecording) return@withContext
        val w = fileWriter ?: return@withContext

        val rpm = sensors.find { it.id == "rpm" }?.currentValue ?: 0f
        val speed = sensors.find { it.id == "speed" }?.currentValue ?: 0f
        val turbo = sensors.find { it.id == "turbo" }?.currentValue ?: 0f
        val coolant = sensors.find { it.id == "coolant" }?.currentValue ?: 0f
        val battery = sensors.find { it.id == "battery" }?.currentValue ?: 0f

        val now = System.currentTimeMillis()
        val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(now))

        try {
            w.write("$now,$timeStr,%.1f,%.1f,%.2f,%.1f,%.2f\n".format(rpm, speed, turbo, coolant, battery))
            w.flush()
        } catch (_: Exception) {}
    }

    suspend fun stopLogging(): File? = withContext(Dispatchers.IO) {
        if (!isRecording) return@withContext null
        try {
            fileWriter?.flush()
            fileWriter?.close()
        } catch (_: Exception) {}
        isRecording = false
        val file = currentLogFile
        fileWriter = null
        file
    }

    fun shareLogFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Datalog Automotivo OBD AJ: ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Compartilhar Datalog CSV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {}
    }
}
