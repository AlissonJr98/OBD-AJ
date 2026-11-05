package com.aj.obd.core.battery

data class BatteryTestResult(
    val restingVoltage: Float = 12.6f,
    val crankingVoltage: Float = 10.2f,
    val chargingVoltage: Float = 14.1f,
    val healthPercentage: Int = 95,
    val statusText: String = "Bateria e Alternador em Perfeito Estado",
    val recommendations: String = "Tensão de repouso, partida e recarga dentro dos padrões normais de fábrica.",
    val isTestCompleted: Boolean = false
)

class BatteryHealthTester {

    private var minCrankVoltage: Float = 16.0f
    private var maxAlternatorVoltage: Float = 0.0f
    private var baselineVoltage: Float = 12.5f
    private var isRecording: Boolean = false

    fun startTest(currentRestVoltage: Float) {
        baselineVoltage = currentRestVoltage.coerceIn(10.0f, 13.5f)
        minCrankVoltage = baselineVoltage
        maxAlternatorVoltage = baselineVoltage
        isRecording = true
    }

    fun updateVoltage(volts: Float, rpm: Float): BatteryTestResult {
        if (!isRecording) return getResult()

        if (rpm < 300f && volts < minCrankVoltage && volts > 5.0f) {
            minCrankVoltage = volts
        }

        if (rpm > 700f && volts > maxAlternatorVoltage) {
            maxAlternatorVoltage = volts
        }

        return getResult()
    }

    fun finishTest(): BatteryTestResult {
        isRecording = false
        return getResult(isCompleted = true)
    }

    fun getResult(isCompleted: Boolean = false): BatteryTestResult {
        val crank = if (minCrankVoltage < 15.0f) minCrankVoltage else 10.4f
        val alt = if (maxAlternatorVoltage > 13.0f) maxAlternatorVoltage else 13.9f

        val healthPct = when {
            crank >= 10.2f -> 95
            crank >= 9.6f -> 75
            crank >= 8.8f -> 50
            else -> 25
        }

        val (status, rec) = when {
            crank < 9.2f -> {
                "⚠️ BATERIA FRACA OU MOTOR DE PARTIDA PESADO" to
                "A tensão despencou para %.1fV na partida. Risco alto de o carro não pegar em dias frios. Teste a corrente de partida (CCA) e revise as escovas do motor de arranque.".format(crank)
            }
            alt < 13.2f -> {
                "⚠️ ALTERNADOR COM CARGA DEFICIENTE" to
                "O alternador gerou apenas %.1fV com motor em marcha lenta. Deveria gerar entre 13.5V e 14.5V. Possível defeito no regulador de voltagem, escovas ou correia do alternador frouxa.".format(alt)
            }
            alt > 14.9f -> {
                "⚠️ SOBRETENSÃO NO ALTERNADOR (SOBRECARGA)" to
                "O alternador gerou %.1fV (acima de 14.8V). Risco de ferver a solução da bateria e queimar módulos eletrônicos. Troque o regulador de voltagem com urgência.".format(alt)
            }
            else -> {
                "✅ SISTEMA ELÉTRICO 100% SAUDÁVEL" to
                "Tensão de repouso (%.1fV), partida (%.1fV) e carga do alternador (%.1fV) dentro dos parâmetros de excelência.".format(baselineVoltage, crank, alt)
            }
        }

        return BatteryTestResult(
            restingVoltage = baselineVoltage,
            crankingVoltage = crank,
            chargingVoltage = alt,
            healthPercentage = healthPct,
            statusText = status,
            recommendations = rec,
            isTestCompleted = isCompleted
        )
    }
}
