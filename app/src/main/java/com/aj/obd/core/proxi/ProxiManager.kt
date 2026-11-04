package com.aj.obd.core.proxi

import com.aj.obd.core.uds.UdsClient
import com.aj.obd.domain.model.ProxiFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ProxiBackup(
    val timestamp: Long,
    val dateFormatted: String,
    val vehicleId: String,
    val originalHex: String,
    val filePath: String
)

class ProxiManager(
    private val udsClient: UdsClient,
    private val backupDir: File
) {

    // Lista de recursos prontos para o Grupo Fiat / Jeep (Toro, Argo, Cronos, Renegade, Compass)
    fun getAvailableFeatures(currentHex: String): List<ProxiFeature> {
        val bytes = hexToByteArray(currentHex)
        return listOf(
            ProxiFeature(
                id = "fog_lights",
                title = "Faróis de Neblina / Milha",
                description = "Habilita a saída elétrica e o botão dos faróis de neblina no BCM",
                category = "Iluminação",
                byteOffset = 45,
                bitMask = 0x04, // Bit 2
                isEnabled = isBitSet(bytes, 45, 0x04)
            ),
            ProxiFeature(
                id = "cornering_lights",
                title = "Cornering Lights",
                description = "Acende automaticamente o farol de milha na direção da curva ao esterçar",
                category = "Iluminação",
                byteOffset = 45,
                bitMask = 0x08, // Bit 3
                isEnabled = isBitSet(bytes, 45, 0x08)
            ),
            ProxiFeature(
                id = "reverse_camera",
                title = "Câmera de Ré no Uconnect",
                description = "Ativa a entrada de vídeo e comutação automática na ré na central multimídia",
                category = "Conforto / Multimídia",
                byteOffset = 88,
                bitMask = 0x01, // Bit 0
                isEnabled = isBitSet(bytes, 88, 0x01)
            ),
            ProxiFeature(
                id = "paddle_shifters",
                title = "Paddle Shifters (Borboletas no Volante)",
                description = "Habilita as borboletas de troca de marcha no volante esportivo",
                category = "Performance",
                byteOffset = 112,
                bitMask = 0x10, // Bit 4
                isEnabled = isBitSet(bytes, 112, 0x10)
            ),
            ProxiFeature(
                id = "drl_menu",
                title = "Menu DRL no Painel",
                description = "Permite ligar/desligar a luz de rodagem diurna pelo computador de bordo",
                category = "Painel / Conforto",
                byteOffset = 62,
                bitMask = 0x02, // Bit 1
                isEnabled = isBitSet(bytes, 62, 0x02)
            ),
            ProxiFeature(
                id = "seatbelt_chime",
                title = "Aviso Sonoro de Cinto de Segurança",
                description = "Ativa ou silencia o bip sonoro contínuo de falta de cinto",
                category = "Segurança",
                byteOffset = 70,
                bitMask = 0x01, // Bit 0
                isEnabled = isBitSet(bytes, 70, 0x01)
            )
        )
    }

    suspend fun createSafetyBackup(vehicleId: String, hexPayload: String): ProxiBackup = withContext(Dispatchers.IO) {
        if (!backupDir.exists()) backupDir.mkdirs()
        val now = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(now))
        val file = File(backupDir, "PROXI_BACKUP_${vehicleId}_$dateStr.hex")
        file.writeText(hexPayload)

        ProxiBackup(
            timestamp = now,
            dateFormatted = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(now)),
            vehicleId = vehicleId,
            originalHex = hexPayload,
            filePath = file.absolutePath
        )
    }

    fun toggleFeature(currentHex: String, feature: ProxiFeature, enable: Boolean): String {
        val bytes = hexToByteArray(currentHex)
        if (feature.byteOffset >= bytes.size) return currentHex

        if (enable) {
            bytes[feature.byteOffset] = (bytes[feature.byteOffset].toInt() or feature.bitMask).toByte()
        } else {
            bytes[feature.byteOffset] = (bytes[feature.byteOffset].toInt() and feature.bitMask.inv()).toByte()
        }

        return byteArrayToHex(bytes)
    }

    suspend fun executeProxiAlignment(newHexPayload: String): Result<Boolean> {
        // 1. Inicia Sessão de Diagnóstico Estendida
        val sessionResult = udsClient.startSession(0x03)
        if (sessionResult.isFailure) return Result.failure(sessionResult.exceptionOrNull()!!)

        // 2. Grava a nova configuração no BCM (DID 0x0103)
        val writeResult = udsClient.writeDid(0x0103, hexToByteArray(newHexPayload))
        if (writeResult.isFailure) return Result.failure(writeResult.exceptionOrNull()!!)

        // 3. Opcional: dispara rotina de Proxi Alignment UDS RoutineControl 0x31 0x01
        // para os demais módulos sincronizarem
        return Result.success(true)
    }

    private fun isBitSet(bytes: ByteArray, offset: Int, mask: Int): Boolean {
        if (offset >= bytes.size) return false
        return (bytes[offset].toInt() and mask) != 0
    }

    private fun hexToByteArray(hex: String): ByteArray {
        val clean = hex.replace(" ", "").replace("\n", "")
        return clean.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private fun byteArrayToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }
}
