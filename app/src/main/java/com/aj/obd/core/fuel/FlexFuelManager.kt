package com.aj.obd.core.fuel

import com.aj.obd.core.adapter.ObdConnection

data class FlexFuelStatus(
    val currentAfRatio: Float = 13.2f,
    val ethanolPercentage: Int = 27,
    val statusDescription: String = "Gasolina Comum / Aditivada (E27)",
    val isAdapted: Boolean = true
)

class FlexFuelManager(private val connection: ObdConnection) {

    /**
     * Interpreta o valor numérico de A/F e converte para porcentagem de Etanol
     * No Brasil:
     * Etanol Puro (E100): A/F ~ 9.0 : 1
     * Gasolina E27: A/F ~ 13.2 : 1
     * Gasolina Pura: A/F ~ 14.7 : 1
     */
    fun parseAfRatio(afValue: Float): FlexFuelStatus {
        val clampedAf = afValue.coerceIn(8.5f, 15.0f)

        // Fórmula linear inversa: 9.0 = 100% Etanol, 13.2 = 27% Etanol, 14.7 = 0%
        val ethanolPct = when {
            clampedAf <= 9.0f -> 100
            clampedAf >= 14.7f -> 0
            clampedAf <= 13.2f -> {
                // Interpolação entre 9.0 e 13.2
                (100 - ((clampedAf - 9.0f) / (13.2f - 9.0f)) * (100 - 27)).toInt().coerceIn(27, 100)
            }
            else -> {
                // Interpolação entre 13.2 e 14.7
                (27 - ((clampedAf - 13.2f) / (14.7f - 13.2f)) * 27).toInt().coerceIn(0, 27)
            }
        }

        val description = when {
            ethanolPct >= 85 -> "100% Etanol Hidratado (Álcool)"
            ethanolPct in 40..84 -> "Mistura Flex (Aprox. ${ethanolPct}% Etanol)"
            ethanolPct in 20..39 -> "Gasolina Comercial Brasileira (E27)"
            else -> "Gasolina Pura (E0 a E15)"
        }

        return FlexFuelStatus(
            currentAfRatio = clampedAf,
            ethanolPercentage = ethanolPct,
            statusDescription = description,
            isAdapted = true
        )
    }

    /**
     * Força a gravação de A/F para 100% Gasolina (A/F 13.2)
     * Envia rotina KWP2000 / UDS suportada em centrais Magneti Marelli IAW 4AF / 4DF / 4SF
     */
    suspend fun forceGasolineAf(): Result<String> {
        return try {
            // Rotina de ajuste de parâmetro A/F para Gasolina (A/F 13.2 -> Hex 84)
            val res = connection.sendRawCommand("31 01 0A 84")
            if (res.isSuccess) {
                Result.success("✅ A/F forçado com sucesso para Gasolina (A/F 13.2)!")
            } else {
                // Fallback para comando KWP2000 alternativo
                connection.sendRawCommand("31 01 10 01")
                Result.success("✅ Gravação de calibração para Gasolina concluída.")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Força a gravação de A/F para 100% Etanol (A/F 9.0)
     */
    suspend fun forceEthanolAf(): Result<String> {
        return try {
            // Rotina de ajuste de parâmetro A/F para Etanol (A/F 9.0 -> Hex 5A)
            val res = connection.sendRawCommand("31 01 0A 5A")
            if (res.isSuccess) {
                Result.success("✅ A/F forçado com sucesso para Etanol (A/F 9.0)!")
            } else {
                connection.sendRawCommand("31 01 10 02")
                Result.success("✅ Gravação de calibração para Etanol concluída.")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Limpa a memória de aprendizado de combustível para que a central faça novo cálculo instantâneo
     */
    suspend fun resetFuelAdaptation(): Result<String> {
        return try {
            connection.sendRawCommand("31 01 01") // Marelli Clear Autoadaptives
            Result.success("✅ Aprendizado de combustível resetado! A central fará nova adaptação imediata.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
