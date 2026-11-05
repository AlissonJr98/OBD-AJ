package com.aj.obd.core.dyno

data class DynoPoint(
    val rpm: Float,
    val powerCv: Float,
    val torqueKgfm: Float,
    val speedKmh: Float
)

data class DynoResult(
    val peakPowerCv: Float = 0f,
    val peakPowerRpm: Float = 0f,
    val peakTorqueKgfm: Float = 0f,
    val peakTorqueRpm: Float = 0f,
    val points: List<DynoPoint> = emptyList(),
    val isRecording: Boolean = false
)

class VirtualDyno {

    var vehicleMassKg: Float = 1050f // Padrão Palio / Uno com piloto
    var isMeasuring: Boolean = false
        private set

    private val recordedPoints = mutableListOf<DynoPoint>()
    private var lastSpeedMs: Double = 0.0
    private var lastTimeMs: Long = 0

    fun startMeasurement() {
        isMeasuring = true
        recordedPoints.clear()
        lastSpeedMs = 0.0
        lastTimeMs = System.currentTimeMillis()
    }

    fun stopMeasurement(): DynoResult {
        isMeasuring = false
        val peakCv = recordedPoints.maxByOrNull { it.powerCv }?.powerCv ?: 0f
        val peakCvRpm = recordedPoints.maxByOrNull { it.powerCv }?.rpm ?: 0f
        val peakKgfm = recordedPoints.maxByOrNull { it.torqueKgfm }?.torqueKgfm ?: 0f
        val peakKgfmRpm = recordedPoints.maxByOrNull { it.torqueKgfm }?.rpm ?: 0f

        return DynoResult(
            peakPowerCv = peakCv,
            peakPowerRpm = peakCvRpm,
            peakTorqueKgfm = peakKgfm,
            peakTorqueRpm = peakKgfmRpm,
            points = recordedPoints.toList(),
            isRecording = false
        )
    }

    /**
     * Atualiza a física de potência e torque durante a aceleração
     * P (Watts) = F * v = (m * a + R_aero + R_atrito) * v
     * CV = Watts / 735.5
     * Torque (Nm) = (P_watts * 60) / (2 * pi * RPM)
     * Torque (kgfm) = Torque_Nm / 9.80665
     */
    fun update(speedKmh: Float, rpm: Float): DynoResult {
        if (!isMeasuring) return getResult()

        val now = System.currentTimeMillis()
        val dtSec = (now - lastTimeMs) / 1000.0
        val speedMs = speedKmh / 3.6

        if (dtSec > 0.04 && lastSpeedMs > 0.1) {
            val accel = (speedMs - lastSpeedMs) / dtSec // m/s²

            if (accel > 0.2 && rpm > 1500f) {
                // Força inercial (m * a)
                val fInertial = vehicleMassKg * accel

                // Resistência aerodinâmica aproximada (0.5 * rho * Cd * A * v²)
                val fAero = 0.5 * 1.225 * 0.35 * 1.9 * (speedMs * speedMs)

                // Resistência de rolagem (C_rr * m * g)
                val fRolling = 0.015 * vehicleMassKg * 9.81

                val fTotal = fInertial + fAero + fRolling
                val powerWatts = fTotal * speedMs
                val powerCv = (powerWatts / 735.49875).toFloat().coerceIn(10f, 450f)

                val torqueNm = if (rpm > 500f) (powerWatts * 60.0) / (2.0 * Math.PI * rpm) else 0.0
                val torqueKgfm = (torqueNm / 9.80665).toFloat().coerceIn(2f, 65f)

                recordedPoints.add(DynoPoint(rpm, powerCv, torqueKgfm, speedKmh))
            }
        }

        lastSpeedMs = speedMs
        lastTimeMs = now

        return getResult()
    }

    fun getResult(): DynoResult {
        val peakCv = recordedPoints.maxByOrNull { it.powerCv }?.powerCv ?: 0f
        val peakCvRpm = recordedPoints.maxByOrNull { it.powerCv }?.rpm ?: 0f
        val peakKgfm = recordedPoints.maxByOrNull { it.torqueKgfm }?.torqueKgfm ?: 0f
        val peakKgfmRpm = recordedPoints.maxByOrNull { it.torqueKgfm }?.rpm ?: 0f

        return DynoResult(
            peakPowerCv = peakCv,
            peakPowerRpm = peakCvRpm,
            peakTorqueKgfm = peakKgfm,
            peakTorqueRpm = peakKgfmRpm,
            points = recordedPoints.toList(),
            isRecording = isMeasuring
        )
    }
}
