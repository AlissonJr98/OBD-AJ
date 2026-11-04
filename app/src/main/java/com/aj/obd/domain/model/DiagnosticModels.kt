package com.aj.obd.domain.model

data class DtcFault(
    val code: String,
    val description: String,
    val ecuSource: String,
    val severity: DtcSeverity = DtcSeverity.MEDIUM,
    val possibleCauses: List<String> = emptyList(),
    val isPending: Boolean = false
)

enum class DtcSeverity(val label: String, val colorHex: Long) {
    CRITICAL("Crítico", 0xFFFF1744),
    HIGH("Grave", 0xFFFF9100),
    MEDIUM("Moderado", 0xFFFFEA00),
    INFO("Informativo", 0xFF00E5FF)
}

data class LiveSensor(
    val id: String,
    val name: String,
    val shortName: String,
    val didHex: String,
    val unit: String,
    val currentValue: Float = 0f,
    val minValue: Float = 0f,
    val maxValue: Float = 100f,
    val slope: Float = 1f,
    val offset: Float = 0f,
    val isSelectedForDashboard: Boolean = false,
    val history: List<Float> = emptyList()
)

data class ProxiFeature(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val byteOffset: Int,
    val bitMask: Int,
    val isEnabled: Boolean,
    val requiresReboot: Boolean = true
)
