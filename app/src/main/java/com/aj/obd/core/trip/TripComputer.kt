package com.aj.obd.core.trip

data class TripStats(
    val fuelPricePerLiter: Float = 5.89f,
    val instantConsumptionKmL: Float = 0f,
    val averageConsumptionKmL: Float = 12.4f,
    val totalCostReais: Float = 0f,
    val totalDistanceKm: Float = 0f,
    val totalFuelUsedLiters: Float = 0f,
    val tripDurationSec: Long = 0L
)

class TripComputer {

    var fuelPricePerLiter: Float = 5.89f
    private var totalDistanceMeters: Double = 0.0
    private var totalFuelUsedLiters: Double = 0.0
    private var startTimeMs: Long = System.currentTimeMillis()
    private var lastUpdateMs: Long = System.currentTimeMillis()

    fun reset() {
        totalDistanceMeters = 0.0
        totalFuelUsedLiters = 0.0
        startTimeMs = System.currentTimeMillis()
        lastUpdateMs = System.currentTimeMillis()
    }

    /**
     * Atualiza os cálculos da viagem com base na velocidade (km/h) e rotação (RPM)
     */
    fun update(speedKmh: Float, rpm: Float): TripStats {
        val now = System.currentTimeMillis()
        val dtSec = (now - lastUpdateMs) / 1000.0
        lastUpdateMs = now

        if (dtSec > 0.001 && dtSec < 10.0) {
            val speedMs = speedKmh / 3.6
            val distMeters = speedMs * dtSec
            totalDistanceMeters += distMeters

            // Estimativa de consumo de combustível baseada em RPM e carga
            val fuelFlowLiterPerHour = if (speedKmh > 1f || rpm > 500f) {
                // Modelo de consumo estimado para motor 1.0 a 1.8 8V/16V
                (rpm * 0.0013f + (speedKmh * 0.025f)).coerceIn(0.6f, 18.0f)
            } else {
                0.0f
            }

            val fuelLitersThisStep = (fuelFlowLiterPerHour / 3600.0) * dtSec
            totalFuelUsedLiters += fuelLitersThisStep
        }

        val totalKm = (totalDistanceMeters / 1000.0).toFloat()
        val avgKmL = if (totalFuelUsedLiters > 0.05) (totalKm / totalFuelUsedLiters.toFloat()).coerceIn(3.0f, 35.0f) else 12.5f

        val instantKmL = if (speedKmh > 5f && rpm > 600f) {
            (speedKmh / (rpm * 0.0013f + speedKmh * 0.025f)).coerceIn(4.0f, 40.0f)
        } else if (speedKmh <= 1f) {
            0.0f
        } else {
            12.0f
        }

        val totalCost = (totalFuelUsedLiters * fuelPricePerLiter).toFloat()
        val elapsedSec = (now - startTimeMs) / 1000L

        return TripStats(
            fuelPricePerLiter = fuelPricePerLiter,
            instantConsumptionKmL = instantKmL,
            averageConsumptionKmL = avgKmL,
            totalCostReais = totalCost,
            totalDistanceKm = totalKm,
            totalFuelUsedLiters = totalFuelUsedLiters.toFloat(),
            tripDurationSec = elapsedSec
        )
    }
}
