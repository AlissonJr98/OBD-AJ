package com.aj.obd.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.aj.obd.domain.model.AdapterCable
import com.aj.obd.domain.model.EcuCategory
import com.aj.obd.domain.model.EcuModule
import com.aj.obd.domain.model.ProtocolType
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

    /**
     * Auto-detecção: identifica a central exata a partir da assinatura ISO Code (5 bytes)
     * retornada pela central durante o handshake K-Line / CAN.
     */
    fun findEcuByIsoCode(isoCodeHex: String): EcuModule? {
        val db = dbInstance ?: return null
        val clean = isoCodeHex.replace(" ", "").uppercase()

        // 1. Tenta correspondência direta em isocodes
        val cursor = db.rawQuery("""
            SELECT u.ID, u.unit_name, u.inittype, u.ecuaddress, u.canid, u.adapter, u.baudrate, u.canidresp
            FROM isocodes i
            JOIN ECUUnits u ON i.device_id = u.ID
            WHERE i.iso_code = ? LIMIT 1
        """, arrayOf(clean))

        return cursor.use {
            if (it.moveToFirst()) {
                val id = it.getInt(0)
                val name = it.getString(1)
                val inittype = it.getInt(2)
                val address = it.getInt(3)
                val canId = it.getString(4) ?: "7E0"
                val adapterCode = it.getString(5) ?: "0"
                val baud = it.getInt(6)
                val canResp = it.getString(7) ?: "7E8"

                val protocol = when (inittype) {
                    0 -> ProtocolType.KLINE_ISO9141
                    1, 2 -> ProtocolType.KLINE_KWP2000_FAST
                    6 -> ProtocolType.CAN_11BIT_500K
                    7 -> ProtocolType.CAN_11BIT_500K
                    else -> ProtocolType.CAN_11BIT_500K
                }

                val cable = when (adapterCode) {
                    "1", "1,5" -> AdapterCable.GREEN
                    "2", "2,5" -> AdapterCable.RED
                    "3" -> AdapterCable.YELLOW
                    "5" -> AdapterCable.BLUE
                    else -> AdapterCable.DIRECT
                }

                EcuModule(
                    id = "auto_$id",
                    name = name,
                    category = if (name.contains("ABS", true)) EcuCategory.BRAKES else if (name.contains("Airbag", true)) EcuCategory.AIRBAG else EcuCategory.ENGINE,
                    protocol = protocol,
                    requestHeader = if (protocol == ProtocolType.KLINE_KWP2000_FAST) "81%02XF1".format(address) else canId,
                    responseHeader = if (protocol == ProtocolType.KLINE_KWP2000_FAST) "81F1%02X".format(address) else canResp,
                    adapterCable = cable,
                    physicalAddress = address,
                    baudRate = if (baud > 0) baud else 10400,
                    isoCode = clean
                )
            } else null
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {}
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}
}
