package com.aj.obd.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.aj.obd.domain.model.DtcFault
import com.aj.obd.domain.model.DtcSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AutomotiveDatabaseHelper(private val context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "automotive_knowledge.db"
        private const val DATABASE_VERSION = 1
    }

    private var dbInstance: SQLiteDatabase? = null

    suspend fun initializeDatabase(): Unit = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        if (!dbFile.exists()) {
            dbFile.parentFile?.mkdirs()
            context.assets.open("database/$DATABASE_NAME").use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        dbInstance = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
    }

    fun getDtcDescriptionPt(code: String): String? {
        val db = dbInstance ?: return null
        val cursor = db.rawQuery(
            "SELECT code_po, code_en FROM Faults WHERE hexcode = ? LIMIT 1",
            arrayOf(code)
        )
        return cursor.use {
            if (it.moveToFirst()) {
                val pt = it.getString(0)
                val en = it.getString(1)
                if (!pt.isNullOrBlank()) pt else en
            } else null
        }
    }

    fun findEcuByIsoCode(isoCodeHex: String): String? {
        val db = dbInstance ?: return null
        val clean = isoCodeHex.replace(" ", "").uppercase()
        val cursor = db.rawQuery(
            "SELECT device_id FROM isocodes WHERE iso_code = ? LIMIT 1",
            arrayOf(clean)
        )
        return cursor.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {}
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}
}
