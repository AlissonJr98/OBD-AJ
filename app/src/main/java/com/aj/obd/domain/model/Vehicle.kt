package com.aj.obd.domain.model

data class Vehicle(
    val id: String,
    val brand: String,
    val name: String,
    val years: String,
    val iconRes: String = "car",
    val supportedEcus: List<EcuModule> = emptyList()
)

enum class AdapterCable(val code: Int, val label: String, val pinoutDesc: String, val colorHex: Long) {
    DIRECT(0, "Direto (Sem Adaptador)", "Pinos padrão OBD 6 (CAN-H) e 14 (CAN-L)", 0xFF757575),
    YELLOW(3, "Cabo Amarelo (A3)", "Desvia CAN para pinos 1 e 9 (Rede BCM / Painel / Clima)", 0xFFFFD600),
    BLUE(5, "Cabo Azul (A5)", "Desvia CAN para pinos 3 e 11 (Multimídia / Uconnect)", 0xFF2979FF),
    RED(2, "Cabo Vermelho (A2)", "K-Line pino 3 (Airbag antigos)", 0xFFFF1744),
    GREEN(1, "Cabo Verde (A1)", "K-Line pinos 1, 7, 9, 12 (ABS / Direção assistida)", 0xFF00E676)
}

enum class EcuCategory(val label: String) {
    ENGINE("Motor / Injeção"),
    BODY("Body Computer (BCM)"),
    DASHBOARD("Painel de Instrumentos"),
    BRAKES("Freios / ABS / ESC"),
    AIRBAG("Airbag"),
    TRANSMISSION("Câmbio Automático"),
    CLIMATE("Climatização"),
    ENTERTAINMENT("Multimídia / Rádio")
}

data class EcuModule(
    val id: String,
    val name: String,
    val category: EcuCategory,
    val protocol: ProtocolType,
    val requestHeader: String,
    val responseHeader: String,
    val adapterCable: AdapterCable = AdapterCable.DIRECT,
    val physicalAddress: Int = 0x10,
    val isoCode: String? = null
)

enum class ProtocolType {
    CAN_11BIT_500K,
    CAN_29BIT_500K,
    CAN_11BIT_250K,
    KLINE_KWP2000_FAST,
    KLINE_ISO9141
}
