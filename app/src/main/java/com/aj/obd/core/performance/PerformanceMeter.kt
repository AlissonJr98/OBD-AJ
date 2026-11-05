package com.aj.obd.core.performance

data class PerformanceRun(
    val time0to60Ms: Long? = null,
    val time0to100Ms: Long? = null,
    val time100mMs: Long? = null,
    val time201mMs: Long? = null,
    val time402mMs: Long? = null,
    val topSpeedKmh: Float = 0f,
    val maxAccelG: Float = 0f,
    val runDate: Long = System.currentTimeMillis()
)

enum class DragMeterState {
    READY_WAITING_LAUNCH, // Parado (0 km/h), aguardando arrancada
    MEASURING,            // Em aceleração plena
    COMPLETED             // Atingiu 100 km/h ou 402m
}

class PerformanceMeter {

    var state: DragMeterState = DragMeterState.READY_WAITING_LAUNCH
        private set

    private var launchTimestamp: Long = 0
    private var lastTimestamp: Long = 0
    private var accumulatedDistanceMeters: Double = 0.0

    private var t0to60: Long? = null
    private var t0to100: Long? = null
    private var t100m: Long? = null
    private var t201m: Long? = null
    private var t402m: Long? = null
    private var peakSpeed: Float = 0f
    private var maxG: Float = 0f

    private var previousSpeedMs: Double = 0.0

    fun reset() {
        state = DragMeterState.READY_WAITING_LAUNCH
        launchTimestamp = 0
        lastTimestamp = 0
        accumulatedDistanceMeters = 0.0
        t0to60 = null
        t0to100 = null
        t100m = null
        t201m = null
        t402m = null
        peakSpeed = 0f
        maxG = 0f
        previousSpeedMs = 0.0
    }

    /**
     * Atualiza a velocidade recebida da telemetria OBD2 (km/h)
     */
    fun updateSpeed(speedKmh: Float): PerformanceRun {
        val now = System.currentTimeMillis()
        val speedMs = speedKmh / 3.6 // km/h -> m/s

        if (speedKmh > peakSpeed) peakSpeed = speedKmh

        when (state) {
            DragMeterState.READY_WAITING_LAUNCH -> {
                // Se o carro estava parado e começou a se mover (> 1.5 km/h)
                if (speedKmh > 1.5f) {
                    state = DragMeterState.MEASURING
                    launchTimestamp = now
                    lastTimestamp = now
                    accumulatedDistanceMeters = 0.0
                    previousSpeedMs = speedMs
                }
            }
            DragMeterState.MEASURING -> {
                val dtSec = (now - lastTimestamp) / 1000.0
                if (dtSec > 0.001) {
                    // Integração trapezoidal da distância percorrida
                    val avgSpeedMs = (speedMs + previousSpeedMs) / 2.0
                    accumulatedDistanceMeters += avgSpeedMs * dtSec

                    // Cálculo instantâneo de aceleração em G
                    val accelMs2 = (speedMs - previousSpeedMs) / dtSec
                    val currentG = (accelMs2 / 9.80665).toFloat()
                    if (currentG > maxG) maxG = currentG
                }

                val elapsedMs = now - launchTimestamp

                // Marca 0 a 60 km/h
                if (t0to60 == null && speedKmh >= 60f) {
                    t0to60 = elapsedMs
                }

                // Marca 0 a 100 km/h
                if (t0to100 == null && speedKmh >= 100f) {
                    t0to100 = elapsedMs
                }

                // Marca 100 metros
                if (t100m == null && accumulatedDistanceMeters >= 100.0) {
                    t100m = elapsedMs
                }

                // Marca 201 metros (1/8 de milha)
                if (t201m == null && accumulatedDistanceMeters >= 201.0) {
                    t201m = elapsedMs
                }

                // Marca 402 metros (1/4 de milha)
                if (t402m == null && accumulatedDistanceMeters >= 402.0) {
                    t402m = elapsedMs
                    state = DragMeterState.COMPLETED
                }

                lastTimestamp = now
                previousSpeedMs = speedMs

                // Se completou 0-100 e passou de 400m ou freou
                if (t0to100 != null && speedKmh < 10f && elapsedMs > 5000) {
                    state = DragMeterState.COMPLETED
                }
            }
            DragMeterState.COMPLETED -> {
                // Medição finalizada
            }
        }

        return PerformanceRun(
            time0to60Ms = t0to60,
            time0to100Ms = t0to100,
            time100mMs = t100m,
            time201mMs = t201m,
            time402mMs = t402m,
            topSpeedKmh = peakSpeed,
            maxAccelG = maxG
        )
    }

    fun getDistanceMeters(): Double = accumulatedDistanceMeters
    fun getElapsedTimeMs(): Long = if (launchTimestamp > 0) System.currentTimeMillis() - launchTimestamp else 0
}
