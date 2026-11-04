package com.aj.obd.core.uds

import com.aj.obd.core.adapter.ObdConnection
import com.aj.obd.domain.model.DtcFault
import com.aj.obd.domain.model.DtcSeverity

class UdsClient(private val connection: ObdConnection) {

    /**
     * UDS Service 0x10 - Diagnostic Session Control
     * 0x01 = Default Session
     * 0x02 = Programming Session
     * 0x03 = Extended Diagnostic Session
     */
    suspend fun startSession(sessionType: Int = 0x03): Result<Boolean> {
        val hexCmd = "10%02X".format(sessionType)
        val res = connection.sendRawCommand(hexCmd).getOrElse { return Result.failure(it) }
        val clean = res.replace(" ", "")
        val expected = "50%02X".format(sessionType)
        return if (clean.contains(expected)) {
            Result.success(true)
        } else {
            Result.failure(Exception("Resposta negativa da central para sessão $sessionType: $clean"))
        }
    }

    /**
     * UDS Service 0x22 - Read Data By Identifier (DID)
     */
    suspend fun readDid(did: Int): Result<ByteArray> {
        val hexCmd = "22%04X".format(did)
        val res = connection.sendRawCommand(hexCmd).getOrElse { return Result.failure(it) }
        val clean = res.replace(" ", "").replace("\n", "")
        val expectedPrefix = "62%04X".format(did)

        val idx = clean.indexOf(expectedPrefix)
        if (idx == -1) {
            return Result.failure(Exception("DID 0x%04X não retornado ou resposta negativa: %s".format(did, clean)))
        }

        val payloadHex = clean.substring(idx + expectedPrefix.length)
        return try {
            val bytes = payloadHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * UDS Service 0x2E - Write Data By Identifier (DID)
     */
    suspend fun writeDid(did: Int, data: ByteArray): Result<Boolean> {
        val hexData = data.joinToString("") { "%02X".format(it) }
        val hexCmd = "2E%04X%s".format(did, hexData)
        val res = connection.sendRawCommand(hexCmd).getOrElse { return Result.failure(it) }
        val clean = res.replace(" ", "")
        val expected = "6E%04X".format(did)
        return if (clean.contains(expected)) {
            Result.success(true)
        } else {
            Result.failure(Exception("Falha na gravação do DID 0x%04X: $clean".format(did)))
        }
    }

    /**
     * UDS Service 0x19 0x02 - Read DTC Information By Status Mask
     */
    suspend fun readDtcs(ecuName: String): Result<List<DtcFault>> {
        val res = connection.sendRawCommand("19020D").getOrElse { return Result.failure(it) }
        val clean = res.replace(" ", "").replace("\n", "")
        
        if (!clean.contains("5902")) {
            return Result.success(emptyList()) // Sem falhas gravadas
        }

        val faults = mutableListOf<DtcFault>()
        val data = clean.substringAfter("59020D", missingDelimiterValue = clean.substringAfter("5902"))
        
        // Cada DTC em UDS possui 4 bytes: 3 bytes de código DTC + 1 byte de status
        for (i in 0 until (data.length / 8)) {
            val chunk = data.substring(i * 8, (i + 1) * 8)
            val dtcHex = chunk.take(6)
            val formattedCode = formatDtcCode(dtcHex)
            faults.add(
                DtcFault(
                    code = formattedCode,
                    description = getDtcDescription(formattedCode),
                    ecuSource = ecuName,
                    severity = getDtcSeverity(formattedCode)
                )
            )
        }

        return Result.success(faults)
    }

    /**
     * UDS Service 0x14 - Clear Diagnostic Information (Apagar erros)
     */
    suspend fun clearDtcs(): Result<Boolean> {
        val res = connection.sendRawCommand("14FFFFFF").getOrElse { return Result.failure(it) }
        val clean = res.replace(" ", "")
        return if (clean.contains("54")) {
            Result.success(true)
        } else {
            Result.failure(Exception("Falha ao apagar DTCs: $clean"))
        }
    }

    private fun formatDtcCode(rawHex: String): String {
        if (rawHex.length < 4) return rawHex
        val firstChar = when (rawHex[0]) {
            '0', '1', '2', '3' -> 'P'
            '4', '5', '6', '7' -> 'C'
            '8', '9', 'A', 'B' -> 'B'
            'C', 'D', 'E', 'F' -> 'U'
            else -> 'P'
        }
        val secondChar = ((rawHex[0].digitToInt(16) and 0x03)).toString()
        val rest = rawHex.substring(1, 4)
        return "$firstChar$secondChar$rest"
    }

    private fun getDtcDescription(code: String): String = when (code) {
        "P0300" -> "Detecção de falhas aleatórias / múltiplos cilindros falhando"
        "P0301" -> "Falha de combustão detectada no Cilindro 1"
        "P0302" -> "Falha de combustão detectada no Cilindro 2"
        "P0303" -> "Falha de combustão detectada no Cilindro 3"
        "P0304" -> "Falha de combustão detectada no Cilindro 4"
        "P0100" -> "Mau funcionamento no circuito do sensor MAF / Fluxo de ar"
        "P0115" -> "Falha no circuito do sensor de temperatura do líquido de arrefecimento (ECT)"
        "P0130" -> "Mau funcionamento no circuito do sensor de oxigênio (Sonda Lambda 1)"
        "P0420" -> "Eficiência do catalisador abaixo do limite (Banco 1)"
        "U1701" -> "Comunicação perdida com o Módulo de Controle do Motor (ECM) na CAN"
        "U1706" -> "Comunicação perdida com o Módulo de Freios ABS / ESC na CAN"
        "U1711" -> "Comunicação perdida com o Módulo de Câmbio Automático na CAN"
        "B1001" -> "Configuração do Body Computer inconsistente / Necessita Alinhamento de Proxi"
        "B1004" -> "Falha no transponder / Antena da chave Code"
        "C1200" -> "Sensor de velocidade de roda com sinal incorreto"
        else -> "Código de falha específico do fabricante / Consulte manual técnico"
    }

    private fun getDtcSeverity(code: String): DtcSeverity = when {
        code.startsWith("U") -> DtcSeverity.CRITICAL // Erros de barramento CAN
        code.startsWith("P03") || code.startsWith("P02") -> DtcSeverity.CRITICAL // Falhas de motor/cilindros
        code.startsWith("B1") -> DtcSeverity.MEDIUM
        else -> DtcSeverity.HIGH
    }
}
