package com.aj.obd.core.kwp

import com.aj.obd.core.adapter.ObdConnection
import com.aj.obd.domain.model.DtcFault
import com.aj.obd.domain.model.DtcSeverity

/**
 * Cliente KWP2000 (ISO 14230-4) para veículos Fiat da era K-Line (ex: Palio 2001, Uno, Siena, Marea, Stilo).
 * Comunica através do pino 7 da porta OBD (ou pinos 1 e 3 com adaptadores A1/A2).
 */
class Kwp2000Client(private val connection: ObdConnection) {

    /**
     * Serviço 0x81 - Start Communication (Handshake KWP2000)
     */
    suspend fun startCommunication(ecuAddress: Int = 0x10): Result<Boolean> {
        val hexCmd = "81%02XF181".format(ecuAddress)
        val res = connection.sendRawCommand(hexCmd).getOrElse { return Result.failure(it) }
        val clean = res.replace(" ", "").uppercase()
        // Resposta positiva KWP2000 começa com 0xC1 (0x81 + 0x40)
        return if (clean.contains("C1") || clean.contains("50") || clean.contains("OK")) {
            Result.success(true)
        } else {
            Result.failure(Exception("Falha ao iniciar comunicação KWP2000: $clean"))
        }
    }

    /**
     * Serviço 0x1A - Read ECU Identification
     * Usado para obter o ISO Code e a versão da calibração Marelli / Bosch (ex: IAW 4AFB.P1)
     */
    suspend fun readEcuIdentification(subFunction: Int = 0x90): Result<String> {
        val hexCmd = "1A%02X".format(subFunction)
        val res = connection.sendRawCommand(hexCmd).getOrElse { return Result.failure(it) }
        val clean = res.replace(" ", "").replace("\n", "").uppercase()
        val expected = "5A%02X".format(subFunction)

        val idx = clean.indexOf(expected)
        if (idx == -1) {
            return Result.failure(Exception("Resposta negativa da ECU KWP2000: $clean"))
        }

        val payload = clean.substring(idx + expected.length)
        return Result.success(payload)
    }

    /**
     * Serviço 0x21 - Read Data By Local Identifier (Sensores ao vivo KWP2000)
     */
    suspend fun readDataByLocalId(localId: Int): Result<ByteArray> {
        val hexCmd = "21%02X".format(localId)
        val res = connection.sendRawCommand(hexCmd).getOrElse { return Result.failure(it) }
        val clean = res.replace(" ", "").replace("\n", "").uppercase()
        val expected = "61%02X".format(localId)

        val idx = clean.indexOf(expected)
        if (idx == -1) {
            return Result.failure(Exception("LocalID 0x%02X não retornado: %s".format(localId, clean)))
        }

        val payloadHex = clean.substring(idx + expected.length)
        return try {
            val bytes = payloadHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Serviço 0x18 - Read Diagnostic Trouble Codes (Falhas KWP2000)
     */
    suspend fun readDtcKwp(ecuName: String): Result<List<DtcFault>> {
        val res = connection.sendRawCommand("1800FF00").getOrElse { return Result.failure(it) }
        val clean = res.replace(" ", "").replace("\n", "").uppercase()

        if (!clean.contains("58")) {
            return Result.success(emptyList()) // Nenhuma falha gravada
        }

        val faults = mutableListOf<DtcFault>()
        val data = clean.substringAfter("58")
        
        // Em KWP2000, os códigos vêm em pares de 2 ou 3 bytes
        for (i in 0 until (data.length / 4)) {
            val chunk = data.substring(i * 4, (i + 1) * 4)
            if (chunk == "0000" || chunk.length < 4) continue
            val formatted = "P" + chunk
            faults.add(
                DtcFault(
                    code = formatted,
                    description = getKwpDtcDescription(formatted),
                    ecuSource = ecuName,
                    severity = DtcSeverity.HIGH
                )
            )
        }

        return Result.success(faults)
    }

    /**
     * Serviço 0x14 - Clear Diagnostic Information KWP2000
     */
    suspend fun clearDtcKwp(): Result<Boolean> {
        val res = connection.sendRawCommand("14FF00").getOrElse { return Result.failure(it) }
        val clean = res.replace(" ", "").uppercase()
        return if (clean.contains("54") || clean.contains("OK")) {
            Result.success(true)
        } else {
            Result.failure(Exception("Falha ao apagar falhas KWP: $clean"))
        }
    }

    /**
     * Serviço 0x31 - Start Routine By Local Identifier
     * Especialmente útil para o Palio 2001: Reset dos Parâmetros Autoadaptativos da Injeção Marelli 4AF!
     */
    suspend fun resetAutoadaptiveParameters(): Result<Boolean> {
        // Rotina 0x01 no serviço 0x31 (Reset de autoadaptação de combustível/mistura)
        val res = connection.sendRawCommand("3101").getOrElse { return Result.failure(it) }
        val clean = res.replace(" ", "").uppercase()
        return if (clean.contains("7101") || clean.contains("50")) {
            Result.success(true)
        } else {
            Result.failure(Exception("Central recusou reset autoadaptativo: $clean"))
        }
    }

    /**
     * Serviço 0x3E - Tester Present (Keep-Alive)
     * Mantém a central K-Line acordada para não expirar o timeout de 2 segundos.
     */
    suspend fun sendTesterPresent(): Boolean {
        val res = connection.sendRawCommand("3E01").getOrNull() ?: ""
        return res.contains("7E") || res.contains("OK")
    }

    private fun getKwpDtcDescription(code: String): String = when (code) {
        "P0105" -> "Sensor de Pressão Absoluta no Coletor (MAP) - Sinal fora da faixa"
        "P0110" -> "Sensor de Temperatura do Ar de Admissão (ACT) - Falha no circuito"
        "P0115" -> "Sensor de Temperatura da Água (ECT) - Circuito aberto ou em curto"
        "P0120" -> "Sensor de Posição da Borboleta (TPS) - Pista 1 com defeito"
        "P0130" -> "Sonda Lambda (Sensor de Oxigênio) - Sem sinal / Mistura incorreta"
        "P0201", "P0202", "P0203", "P0204" -> "Injetor de Combustível - Falha elétrica no circuito"
        "P0300" -> "Falha múltipla de ignição / Cilindro com combustão falhando"
        "P0335" -> "Sensor de Rotação (CKP) / Roda Fônica 60-2 - Falha de sinal"
        "P0351", "P0352" -> "Bobina de Ignição (Cilindros 1-4 ou 2-3) - Falha no primário/secundário"
        "P0500" -> "Sensor de Velocidade do Veículo (VSS) - Sem sinal no câmbio"
        "P0505" -> "Atuador de Marcha Lenta (Motor de Passo) - Controle travado"
        "P0601" -> "Falha na memória interna (ROM/EEPROM) da central Marelli 4AF"
        "P1600" -> "Imobilizador Fiat Code - Falha na linha de autorização de partida"
        else -> "Código KWP2000 específico / Consulte manual técnico Fiat"
    }
}
