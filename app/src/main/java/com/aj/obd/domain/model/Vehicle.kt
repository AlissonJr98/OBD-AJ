package com.aj.obd.domain.model

enum class VehicleEra(val label: String, val description: String) {
    ALL("Todas as Épocas", "Exibe todos os veículos catalogados"),
    CLASSIC_KLINE("Clássicos K-Line (1996 - 2007)", "Comunicação K-Line / ISO 9141 / KWP2000 (Ex: Palio 2001, Uno, Marea, Stilo)"),
    INTERMEDIATE_CAN("Intermediários CAN (2008 - 2015)", "Primeira geração CAN Bus 500k/250k (Ex: Punto, Linea, Bravo, Doblò)"),
    MODERN_UDS("Modernos UDS & SGW (2016+)", "Rede CAN estendida UDS com proteção SGW (Ex: Toro, Renegade, Compass, Pulse)")
}

data class Vehicle(
    val id: String,
    val brand: String,
    val name: String,
    val years: String,
    val era: VehicleEra = VehicleEra.CLASSIC_KLINE,
    val engineOptions: String = "",
    val iconRes: String = "car",
    val defaultEcuId: String? = null,
    val supportedEcus: List<EcuModule> = emptyList()
)

enum class AdapterCable(val code: Int, val label: String, val pinoutDesc: String, val colorHex: Long) {
    DIRECT(0, "Direto (Sem Adaptador)", "Pinos padrão OBD 16 pinos: 7 (K-Line) ou 6/14 (CAN)", 0xFF757575),
    GREEN(1, "Cabo Verde (A1)", "Desvia K-Line pino 7 para pino 1 (ABS Bosch 5.3 / Direção)", 0xFF00E676),
    RED(2, "Cabo Vermelho (A2)", "Desvia K-Line pino 7 para pino 3 (Airbag TRW / MY97 / MY99)", 0xFFFF1744),
    YELLOW(3, "Cabo Amarelo (A3)", "Desvia CAN para pinos 1 e 9 (Rede BCM / Painel / Clima)", 0xFFFFD600),
    BLUE(5, "Cabo Azul (A5)", "Desvia CAN para pinos 3 e 11 (Multimídia / Uconnect)", 0xFF2979FF)
}

enum class EcuCategory(val label: String) {
    ENGINE("Motor / Injeção Eletrônica"),
    BRAKES("Freios / ABS"),
    AIRBAG("Airbag"),
    IMMOBILIZER("Imobilizador / Chave Code"),
    BODY("Body Computer (BCM)"),
    DASHBOARD("Painel de Instrumentos"),
    TRANSMISSION("Câmbio Automático / Dualogic"),
    CLIMATE("Climatização / Ar Condicionado"),
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
    val baudRate: Int = 10400,
    val isoCode: String? = null,
    val isDefault: Boolean = false
)

enum class ProtocolType {
    OBD2_AUTO,
    CAN_11BIT_500K,
    CAN_29BIT_500K,
    CAN_11BIT_250K,
    KLINE_KWP2000_FAST,
    KLINE_ISO9141
}
