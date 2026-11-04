package com.aj.obd.data

import com.aj.obd.domain.model.AdapterCable
import com.aj.obd.domain.model.EcuCategory
import com.aj.obd.domain.model.EcuModule
import com.aj.obd.domain.model.ProtocolType
import com.aj.obd.domain.model.Vehicle

object VehicleCatalog {

    val vehicles: List<Vehicle> = listOf(
        Vehicle(
            id = "fiat_toro",
            brand = "Fiat",
            name = "Toro (Flex / Diesel / T270)",
            years = "2016 - 2026",
            supportedEcus = createFcaModernEcus("Toro")
        ),
        Vehicle(
            id = "jeep_renegade",
            brand = "Jeep",
            name = "Renegade (1.8 / 2.0 Diesel / T270)",
            years = "2015 - 2026",
            supportedEcus = createFcaModernEcus("Renegade")
        ),
        Vehicle(
            id = "jeep_compass",
            brand = "Jeep",
            name = "Compass / Commander",
            years = "2017 - 2026",
            supportedEcus = createFcaModernEcus("Compass")
        ),
        Vehicle(
            id = "fiat_fastback_pulse",
            brand = "Fiat",
            name = "Pulse / Fastback (T200 / T270)",
            years = "2021 - 2026",
            supportedEcus = createFcaModernEcus("Pulse/Fastback")
        ),
        Vehicle(
            id = "fiat_argo_cronos",
            brand = "Fiat",
            name = "Argo / Cronos (1.0 / 1.3 / 1.8)",
            years = "2017 - 2026",
            supportedEcus = listOf(
                EcuModule("argo_eng_firefly", "Injeção Magneti Marelli IAW 10GF (Firefly)", EcuCategory.ENGINE, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT),
                EcuModule("argo_bcm", "Body Computer Delphi BCM", EcuCategory.BODY, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.YELLOW),
                EcuModule("argo_ipc", "Painel de Instrumentos (TFT / Analógico)", EcuCategory.DASHBOARD, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.YELLOW),
                EcuModule("argo_abs", "Freios ABS / ESC Continental MK100", EcuCategory.BRAKES, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT),
                EcuModule("argo_airbag", "Airbag TRW", EcuCategory.AIRBAG, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT)
            )
        ),
        Vehicle(
            id = "fiat_mobi_uno",
            brand = "Fiat",
            name = "Mobi / Novo Uno (EVO / Firefly)",
            years = "2010 - 2024",
            supportedEcus = listOf(
                EcuModule("uno_eng_evo", "Injeção Magneti Marelli IAW 7GF (Evo)", EcuCategory.ENGINE, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT),
                EcuModule("uno_bcm", "Body Computer Delphi", EcuCategory.BODY, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.YELLOW),
                EcuModule("uno_ipc", "Painel de Instrumentos", EcuCategory.DASHBOARD, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.YELLOW),
                EcuModule("uno_abs", "Freios ABS Bosch 9.0", EcuCategory.BRAKES, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT)
            )
        ),
        Vehicle(
            id = "fiat_punto_linea_bravo",
            brand = "Fiat",
            name = "Punto / Linea / Bravo",
            years = "2008 - 2017",
            supportedEcus = listOf(
                EcuModule("punto_eng_7gf", "Injeção Marelli IAW 7GF / Bosch ME7.9.10", EcuCategory.ENGINE, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT),
                EcuModule("punto_bcm", "Body Computer Delphi / Marelli", EcuCategory.BODY, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.YELLOW),
                EcuModule("punto_dualogic", "Câmbio Dualogic / FreeChoice Magneti Marelli", EcuCategory.TRANSMISSION, ProtocolType.CAN_11BIT_500K, "7E1", "7E9", AdapterCable.DIRECT),
                EcuModule("punto_ipc", "Painel de Instrumentos", EcuCategory.DASHBOARD, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.YELLOW)
            )
        ),
        Vehicle(
            id = "fiat_palio_siena_strada",
            brand = "Fiat",
            name = "Palio / Siena / Strada (Fire / 1.8)",
            years = "2001 - 2020",
            supportedEcus = listOf(
                EcuModule("palio_eng_4sf", "Injeção Magneti Marelli IAW 4AF / 4SF / 4DF", EcuCategory.ENGINE, ProtocolType.KLINE_KWP2000_FAST, "10", "F1", AdapterCable.DIRECT),
                EcuModule("palio_bcm", "Body Computer Marelli / Delphi", EcuCategory.BODY, ProtocolType.KLINE_KWP2000_FAST, "00", "F1", AdapterCable.DIRECT),
                EcuModule("palio_abs", "ABS Bosch 5.3 / 8.0", EcuCategory.BRAKES, ProtocolType.KLINE_ISO9141, "20", "F1", AdapterCable.GREEN),
                EcuModule("palio_airbag", "Airbag TRW", EcuCategory.AIRBAG, ProtocolType.KLINE_ISO9141, "40", "F1", AdapterCable.RED)
            )
        )
    )

    private fun createFcaModernEcus(modelName: String): List<EcuModule> {
        return listOf(
            EcuModule("${modelName}_eng_flex", "Injeção Magneti Marelli IAW 10GF (Flex/T270)", EcuCategory.ENGINE, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT),
            EcuModule("${modelName}_eng_diesel", "Injeção Bosch EDC17C69 (2.0 Multijet Diesel)", EcuCategory.ENGINE, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT),
            EcuModule("${modelName}_bcm", "Body Computer (BCM) Continental / Delphi", EcuCategory.BODY, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.YELLOW),
            EcuModule("${modelName}_ipc", "Painel de Instrumentos (Cluster 7\" TFT)", EcuCategory.DASHBOARD, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.YELLOW),
            EcuModule("${modelName}_abs", "Freios ABS / Controle de Estabilidade Continental MK100", EcuCategory.BRAKES, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT),
            EcuModule("${modelName}_airbag", "Airbag Continental", EcuCategory.AIRBAG, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT),
            EcuModule("${modelName}_trans_at9", "Câmbio Automático ZF 9HP48 (9 Marchas)", EcuCategory.TRANSMISSION, ProtocolType.CAN_11BIT_500K, "7E1", "7E9", AdapterCable.DIRECT),
            EcuModule("${modelName}_trans_aisin6", "Câmbio Automático Aisin TF72-SC (6 Marchas)", EcuCategory.TRANSMISSION, ProtocolType.CAN_11BIT_500K, "7E1", "7E9", AdapterCable.DIRECT),
            EcuModule("${modelName}_radio", "Central Multimídia Uconnect 7\" / 8.4\" / 10.1\"", EcuCategory.ENTERTAINMENT, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.BLUE)
        )
    }
}
