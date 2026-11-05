package com.aj.obd.data

import com.aj.obd.domain.model.AdapterCable
import com.aj.obd.domain.model.EcuCategory
import com.aj.obd.domain.model.EcuModule
import com.aj.obd.domain.model.ProtocolType
import com.aj.obd.domain.model.Vehicle
import com.aj.obd.domain.model.VehicleEra

object VehicleCatalog {

    val vehicles: List<Vehicle> = listOf(
        // =========================================================================
        // UNIVERSAL / TODOS OS VEÍCULOS (PADRÃO GLOBAL OBD-II)
        // =========================================================================
        Vehicle(
            id = "universal_obd2",
            brand = "Universal",
            name = "OBD-II Padrão (Todos os Carros)",
            years = "1996 - 2026",
            era = VehicleEra.ALL,
            engineOptions = "Gasolina / Etanol / Flex / Diesel / GNV",
            defaultEcuId = "universal_engine",
            supportedEcus = listOf(
                EcuModule(
                    id = "universal_engine",
                    name = "Injeção Eletrônica (OBD-II / EOBD)",
                    category = EcuCategory.ENGINE,
                    protocol = ProtocolType.OBD2_AUTO,
                    requestHeader = "7DF",
                    responseHeader = "7E8",
                    adapterCable = AdapterCable.DIRECT,
                    isDefault = true
                )
            )
        ),

        // =========================================================================
        // FIAT CLÁSSICOS & K-LINE (1996 - 2007) - DESTAQUE ESPECIAL PALIO 2001
        // =========================================================================
        Vehicle(
            id = "fiat_palio_2001",
            brand = "Fiat",
            name = "Palio 2001 (RST1 Fire / 16V / Torque)",
            years = "2001 - 2004",
            era = VehicleEra.CLASSIC_KLINE,
            engineOptions = "Fire 1.0/1.3 8V • 1.0/1.3 16V • 1.6 16V Torque • 1.8 GM",
            defaultEcuId = "palio_2001_iaw_4af",
            supportedEcus = listOf(
                EcuModule(
                    id = "palio_2001_iaw_4af",
                    name = "Magneti Marelli IAW 4AF / 4EF / 59F / 5AF",
                    category = EcuCategory.ENGINE,
                    protocol = ProtocolType.KLINE_KWP2000_FAST,
                    requestHeader = "8110F1",
                    responseHeader = "81F110",
                    adapterCable = AdapterCable.DIRECT,
                    physicalAddress = 0x10,
                    baudRate = 10400,
                    isDefault = true
                ),
                EcuModule(
                    id = "palio_2001_me73h4",
                    name = "Bosch Motronic ME7.3H4 / ME7.3.1 (16V)",
                    category = EcuCategory.ENGINE,
                    protocol = ProtocolType.KLINE_KWP2000_FAST,
                    requestHeader = "8110F1",
                    responseHeader = "81F110",
                    adapterCable = AdapterCable.DIRECT,
                    physicalAddress = 0x10,
                    baudRate = 10400
                ),
                EcuModule(
                    id = "palio_2001_iaw_1af",
                    name = "Magneti Marelli IAW 1AB / 1AF (1.6 16V Torque)",
                    category = EcuCategory.ENGINE,
                    protocol = ProtocolType.KLINE_ISO9141,
                    requestHeader = "6810F1",
                    responseHeader = "68F110",
                    adapterCable = AdapterCable.DIRECT,
                    physicalAddress = 0x10,
                    baudRate = 4800
                ),
                EcuModule(
                    id = "palio_2001_abs_53",
                    name = "ABS Bosch 5.3 (K-Line Pino 1)",
                    category = EcuCategory.BRAKES,
                    protocol = ProtocolType.KLINE_ISO9141,
                    requestHeader = "6820F1",
                    responseHeader = "68F120",
                    adapterCable = AdapterCable.GREEN, // Pino 1 (Cabo Verde A1)
                    physicalAddress = 0x20,
                    baudRate = 4800
                ),
                EcuModule(
                    id = "palio_2001_airbag_my99",
                    name = "Airbag TRW / AlliedSignal MY97 / MY99 (Pino 3)",
                    category = EcuCategory.AIRBAG,
                    protocol = ProtocolType.KLINE_ISO9141,
                    requestHeader = "6800F1",
                    responseHeader = "68F100",
                    adapterCable = AdapterCable.RED, // Pino 3 (Cabo Vermelho A2)
                    physicalAddress = 0x00,
                    baudRate = 4800
                ),
                EcuModule(
                    id = "palio_2001_code",
                    name = "Imobilizador Fiat Code 2",
                    category = EcuCategory.IMMOBILIZER,
                    protocol = ProtocolType.KLINE_ISO9141,
                    requestHeader = "688CF1",
                    responseHeader = "68F18C",
                    adapterCable = AdapterCable.DIRECT,
                    physicalAddress = 0x8C,
                    baudRate = 10400
                )
            )
        ),
        Vehicle(
            id = "fiat_palio_g1",
            brand = "Fiat",
            name = "Palio G1 (Fiasa / MPI / Torque)",
            years = "1996 - 2000",
            era = VehicleEra.CLASSIC_KLINE,
            engineOptions = "Fiasa 1.0/1.5 • 1.6 8V/16V Sevel",
            supportedEcus = listOf(
                EcuModule("palio_g1_1g7", "Magneti Marelli IAW 1G7 (Fiasa 1.0/1.5)", EcuCategory.ENGINE, ProtocolType.KLINE_ISO9141, "6810F1", "68F110", AdapterCable.DIRECT, 0x10, 4800, isDefault = true),
                EcuModule("palio_g1_1ab", "Magneti Marelli IAW 1AB (1.6 16V)", EcuCategory.ENGINE, ProtocolType.KLINE_ISO9141, "6810F1", "68F110", AdapterCable.DIRECT, 0x10, 4800),
                EcuModule("palio_g1_abs", "ABS Bosch 5.3", EcuCategory.BRAKES, ProtocolType.KLINE_ISO9141, "6820F1", "68F120", AdapterCable.GREEN, 0x20, 4800),
                EcuModule("palio_g1_airbag", "Airbag MY97", EcuCategory.AIRBAG, ProtocolType.KLINE_ISO9141, "6800F1", "68F100", AdapterCable.RED, 0x00, 4800)
            )
        ),
        Vehicle(
            id = "fiat_palio_g3",
            brand = "Fiat",
            name = "Palio G3 (RST2 Fire Flex / 1.8 GM)",
            years = "2004 - 2007",
            era = VehicleEra.CLASSIC_KLINE,
            engineOptions = "Fire 1.0/1.3/1.4 Flex • 1.8 Powertrain Flex",
            supportedEcus = listOf(
                EcuModule("palio_g3_4af", "Magneti Marelli IAW 4AF / 4SF Flex", EcuCategory.ENGINE, ProtocolType.KLINE_KWP2000_FAST, "8110F1", "81F110", AdapterCable.DIRECT, 0x10, 10400, isDefault = true),
                EcuModule("palio_g3_delphi", "Delphi MT27E (1.8 Powertrain GM)", EcuCategory.ENGINE, ProtocolType.KLINE_KWP2000_FAST, "8110F1", "81F110", AdapterCable.DIRECT, 0x10, 10400),
                EcuModule("palio_g3_bcm", "Body Computer Delphi", EcuCategory.BODY, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.YELLOW),
                EcuModule("palio_g3_abs", "ABS Bosch 8.0", EcuCategory.BRAKES, ProtocolType.KLINE_KWP2000_FAST, "8120F1", "81F120", AdapterCable.GREEN, 0x20, 10400)
            )
        ),
        Vehicle(
            id = "fiat_uno_mille",
            brand = "Fiat",
            name = "Uno / Mille (Fiasa / Fire / EP)",
            years = "1996 - 2013",
            era = VehicleEra.CLASSIC_KLINE,
            engineOptions = "Mille Electronic • EP • EX • Fire 1.0",
            supportedEcus = listOf(
                EcuModule("uno_iaw_4af", "Magneti Marelli IAW 4AF (Fire)", EcuCategory.ENGINE, ProtocolType.KLINE_KWP2000_FAST, "8110F1", "81F110", AdapterCable.DIRECT, 0x10, 10400, isDefault = true),
                EcuModule("uno_iaw_1g7", "Magneti Marelli IAW 1G7 (Fiasa)", EcuCategory.ENGINE, ProtocolType.KLINE_ISO9141, "6810F1", "68F110", AdapterCable.DIRECT, 0x10, 4800),
                EcuModule("uno_code", "Fiat Code 2", EcuCategory.IMMOBILIZER, ProtocolType.KLINE_ISO9141, "688CF1", "68F18C", AdapterCable.DIRECT, 0x8C, 10400)
            )
        ),
        Vehicle(
            id = "fiat_siena_strada",
            brand = "Fiat",
            name = "Siena & Strada (G1, G2 2001, G3, G4)",
            years = "1998 - 2020",
            era = VehicleEra.CLASSIC_KLINE,
            engineOptions = "Fire 1.0/1.3/1.4 • 1.6 16V • 1.8 Adventure Locker",
            supportedEcus = listOf(
                EcuModule("strada_4af", "Magneti Marelli IAW 4AF / 4SF / 5SF", EcuCategory.ENGINE, ProtocolType.KLINE_KWP2000_FAST, "8110F1", "81F110", AdapterCable.DIRECT, 0x10, 10400, isDefault = true),
                EcuModule("strada_me7", "Bosch Motronic ME7.3H4 (16V)", EcuCategory.ENGINE, ProtocolType.KLINE_KWP2000_FAST, "8110F1", "81F110", AdapterCable.DIRECT, 0x10, 10400),
                EcuModule("strada_abs", "ABS Bosch 5.3 / 8.0", EcuCategory.BRAKES, ProtocolType.KLINE_ISO9141, "6820F1", "68F120", AdapterCable.GREEN, 0x20, 4800),
                EcuModule("strada_airbag", "Airbag TRW MY99", EcuCategory.AIRBAG, ProtocolType.KLINE_ISO9141, "6800F1", "68F100", AdapterCable.RED, 0x00, 4800)
            )
        ),
        Vehicle(
            id = "fiat_marea",
            brand = "Fiat",
            name = "Marea & Marea Weekend (5 Cil. / 16V)",
            years = "1998 - 2007",
            era = VehicleEra.CLASSIC_KLINE,
            engineOptions = "2.0 20V (5 cil) • 2.4 20V • 2.0 Turbo • 1.8 16V",
            supportedEcus = listOf(
                EcuModule("marea_bosch_m210", "Bosch Motronic M2.10.4 (2.0 20V 5 Cil.)", EcuCategory.ENGINE, ProtocolType.KLINE_ISO9141, "6810F1", "68F110", AdapterCable.DIRECT, 0x10, 4800, isDefault = true),
                EcuModule("marea_bosch_me31", "Bosch Motronic ME3.1 / ME7.3.1 (2.4 20V / Turbo)", EcuCategory.ENGINE, ProtocolType.KLINE_KWP2000_FAST, "8110F1", "81F110", AdapterCable.DIRECT, 0x10, 10400),
                EcuModule("marea_hitachi", "Hitachi MPI (1.8 16V)", EcuCategory.ENGINE, ProtocolType.KLINE_ISO9141, "6810F1", "68F110", AdapterCable.DIRECT, 0x10, 4800),
                EcuModule("marea_abs", "ABS Bosch 5.3", EcuCategory.BRAKES, ProtocolType.KLINE_ISO9141, "6820F1", "68F120", AdapterCable.GREEN, 0x20, 4800),
                EcuModule("marea_airbag", "Airbag AlliedSignal / TRW", EcuCategory.AIRBAG, ProtocolType.KLINE_ISO9141, "6800F1", "68F100", AdapterCable.RED, 0x00, 4800),
                EcuModule("marea_clima", "Climatizador Automático Harrison/Borletti", EcuCategory.CLIMATE, ProtocolType.KLINE_ISO9141, "6808F1", "68F108", AdapterCable.DIRECT, 0x08, 4800)
            )
        ),
        Vehicle(
            id = "fiat_brava_bravo_antigo",
            brand = "Fiat",
            name = "Brava & Bravo Antigo (182)",
            years = "1999 - 2003",
            era = VehicleEra.CLASSIC_KLINE,
            engineOptions = "1.6 16V Torque • 1.8 16V • HGT 2.0 20V",
            supportedEcus = listOf(
                EcuModule("brava_1ab", "Magneti Marelli IAW 1AB / 1AF (1.6 16V)", EcuCategory.ENGINE, ProtocolType.KLINE_ISO9141, "6810F1", "68F110", AdapterCable.DIRECT, 0x10, 4800, isDefault = true),
                EcuModule("brava_me7", "Bosch Motronic ME7.3.1", EcuCategory.ENGINE, ProtocolType.KLINE_KWP2000_FAST, "8110F1", "81F110", AdapterCable.DIRECT, 0x10, 10400),
                EcuModule("brava_abs", "ABS Bosch 5.3", EcuCategory.BRAKES, ProtocolType.KLINE_ISO9141, "6820F1", "68F120", AdapterCable.GREEN, 0x20, 4800)
            )
        ),
        Vehicle(
            id = "fiat_stilo",
            brand = "Fiat",
            name = "Stilo (1.8 8V / 1.8 16V / 2.4 Abarth)",
            years = "2002 - 2010",
            era = VehicleEra.CLASSIC_KLINE,
            engineOptions = "1.8 8V Powertrain • 1.8 16V • 2.4 20V Abarth • Dualogic",
            supportedEcus = listOf(
                EcuModule("stilo_delphi", "Delphi MT27E (1.8 8V Powertrain)", EcuCategory.ENGINE, ProtocolType.KLINE_KWP2000_FAST, "8110F1", "81F110", AdapterCable.DIRECT, 0x10, 10400, isDefault = true),
                EcuModule("stilo_bosch_me73", "Bosch Motronic ME7.3.1 (1.8 16V / 2.4 Abarth)", EcuCategory.ENGINE, ProtocolType.KLINE_KWP2000_FAST, "8110F1", "81F110", AdapterCable.DIRECT, 0x10, 10400),
                EcuModule("stilo_bcm", "Body Computer Marelli BCM (Pinos 1/9)", EcuCategory.BODY, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.YELLOW),
                EcuModule("stilo_dualogic", "Câmbio Dualogic Magneti Marelli", EcuCategory.TRANSMISSION, ProtocolType.KLINE_KWP2000_FAST, "8118F1", "81F118", AdapterCable.DIRECT, 0x18, 10400),
                EcuModule("stilo_abs", "ABS Bosch 5.7 / 8.0 ESP", EcuCategory.BRAKES, ProtocolType.KLINE_KWP2000_FAST, "8120F1", "81F120", AdapterCable.GREEN, 0x20, 10400)
            )
        ),
        Vehicle(
            id = "fiat_doblo_idea",
            brand = "Fiat",
            name = "Doblò & Idea (Fire / Powertrain / E.torQ)",
            years = "2001 - 2020",
            era = VehicleEra.CLASSIC_KLINE,
            engineOptions = "Fire 1.3/1.4 • 1.8 Powertrain • 1.6/1.8 E.torQ",
            supportedEcus = listOf(
                EcuModule("doblo_4af", "Magneti Marelli IAW 4AF / 4DF / 4SF", EcuCategory.ENGINE, ProtocolType.KLINE_KWP2000_FAST, "8110F1", "81F110", AdapterCable.DIRECT, 0x10, 10400, isDefault = true),
                EcuModule("doblo_delphi", "Delphi MT27E (1.8 8V)", EcuCategory.ENGINE, ProtocolType.KLINE_KWP2000_FAST, "8110F1", "81F110", AdapterCable.DIRECT, 0x10, 10400),
                EcuModule("doblo_bcm", "Body Computer Delphi (CAN)", EcuCategory.BODY, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.YELLOW),
                EcuModule("doblo_abs", "ABS Bosch 8.0", EcuCategory.BRAKES, ProtocolType.KLINE_KWP2000_FAST, "8120F1", "81F120", AdapterCable.GREEN, 0x20, 10400)
            )
        ),

        // =========================================================================
        // FIAT INTERMEDIÁRIOS & CAN GERAÇÃO 1 (2007 - 2015)
        // =========================================================================
        Vehicle(
            id = "fiat_punto_linea_bravo",
            brand = "Fiat",
            name = "Punto, Linea & Bravo (1.4 / T-Jet / E.torQ)",
            years = "2007 - 2017",
            era = VehicleEra.INTERMEDIATE_CAN,
            engineOptions = "1.4 Fire • 1.4 T-Jet • 1.6/1.8 16V E.torQ • 1.9 16V",
            supportedEcus = listOf(
                EcuModule("punto_eng_7gf", "Injeção Marelli IAW 7GF (E.torQ)", EcuCategory.ENGINE, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT, isDefault = true),
                EcuModule("punto_eng_me7910", "Bosch ME7.9.10 (1.4 T-Jet)", EcuCategory.ENGINE, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT),
                EcuModule("punto_bcm", "Body Computer Delphi / Marelli", EcuCategory.BODY, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.YELLOW),
                EcuModule("punto_dualogic", "Câmbio Dualogic / FreeChoice Marelli", EcuCategory.TRANSMISSION, ProtocolType.CAN_11BIT_500K, "7E1", "7E9", AdapterCable.DIRECT),
                EcuModule("punto_ipc", "Painel de Instrumentos Marelli", EcuCategory.DASHBOARD, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.YELLOW),
                EcuModule("punto_abs", "ABS Bosch 8.0 / 9.0 ESP", EcuCategory.BRAKES, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT)
            )
        ),
        Vehicle(
            id = "fiat_novo_palio_grand_siena",
            brand = "Fiat",
            name = "Novo Palio (326) & Grand Siena",
            years = "2012 - 2018",
            era = VehicleEra.INTERMEDIATE_CAN,
            engineOptions = "1.0/1.4 Fire EVO • 1.6 16V E.torQ",
            supportedEcus = listOf(
                EcuModule("palio326_7gf", "Injeção Magneti Marelli IAW 7GF", EcuCategory.ENGINE, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT, isDefault = true),
                EcuModule("palio326_bcm", "Body Computer Delphi BCM", EcuCategory.BODY, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.YELLOW),
                EcuModule("palio326_ipc", "Painel de Instrumentos Marelli", EcuCategory.DASHBOARD, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.YELLOW),
                EcuModule("palio326_abs", "ABS Bosch 9.0", EcuCategory.BRAKES, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT)
            )
        ),

        // =========================================================================
        // FIAT MODERNOS & UDS (2016+)
        // =========================================================================
        Vehicle(
            id = "fiat_toro",
            brand = "Fiat",
            name = "Toro (Flex / Diesel / T270)",
            years = "2016 - 2026",
            era = VehicleEra.MODERN_UDS,
            engineOptions = "1.8 16V Flex • 2.0 Multijet Diesel • 1.3 Turbo T270",
            supportedEcus = createFcaModernEcus("Toro")
        ),
        Vehicle(
            id = "fiat_pulse_fastback",
            brand = "Fiat",
            name = "Pulse & Fastback (Drive / Audace / Impetus / Abarth)",
            years = "2021 - 2026",
            era = VehicleEra.MODERN_UDS,
            engineOptions = "1.3 Firefly • 1.0 Turbo T200 • 1.3 Turbo T270",
            supportedEcus = createFcaModernEcus("Pulse/Fastback")
        ),
        Vehicle(
            id = "fiat_argo_cronos",
            brand = "Fiat",
            name = "Argo & Cronos (Drive / Trekking / Precision / HGT)",
            years = "2017 - 2026",
            era = VehicleEra.MODERN_UDS,
            engineOptions = "1.0 / 1.3 Firefly • 1.8 16V E.torQ",
            supportedEcus = listOf(
                EcuModule("argo_eng_firefly", "Injeção Magneti Marelli IAW 10GF (Firefly)", EcuCategory.ENGINE, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT, isDefault = true),
                EcuModule("argo_bcm", "Body Computer Delphi BCM", EcuCategory.BODY, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.YELLOW),
                EcuModule("argo_ipc", "Painel de Instrumentos (TFT / Analógico)", EcuCategory.DASHBOARD, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.YELLOW),
                EcuModule("argo_abs", "Freios ABS / ESC Continental MK100", EcuCategory.BRAKES, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT),
                EcuModule("argo_airbag", "Airbag TRW", EcuCategory.AIRBAG, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT)
            )
        ),
        Vehicle(
            id = "fiat_mobi_novo_uno",
            brand = "Fiat",
            name = "Mobi & Novo Uno (Fire EVO / Firefly)",
            years = "2010 - 2024",
            era = VehicleEra.MODERN_UDS,
            engineOptions = "1.0 Fire EVO • 1.0/1.3 Firefly 3 Cilindros",
            supportedEcus = listOf(
                EcuModule("uno_eng_evo", "Injeção Magneti Marelli IAW 7GF (Evo)", EcuCategory.ENGINE, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT, isDefault = true),
                EcuModule("uno_bcm", "Body Computer Delphi", EcuCategory.BODY, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.YELLOW),
                EcuModule("uno_ipc", "Painel de Instrumentos", EcuCategory.DASHBOARD, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.YELLOW),
                EcuModule("uno_abs", "Freios ABS Bosch 9.0", EcuCategory.BRAKES, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT)
            )
        ),

        // =========================================================================
        // JEEP & OUTRAS MARCAS STELLANTIS
        // =========================================================================
        Vehicle(
            id = "jeep_renegade",
            brand = "Jeep",
            name = "Renegade (Sport / Longitude / Trailhawk)",
            years = "2015 - 2026",
            era = VehicleEra.MODERN_UDS,
            engineOptions = "1.8 16V • 2.0 Multijet Diesel 4x4 • 1.3 Turbo T270",
            supportedEcus = createFcaModernEcus("Renegade")
        ),
        Vehicle(
            id = "jeep_compass_commander",
            brand = "Jeep",
            name = "Compass & Commander (4x2 / 4x4)",
            years = "2017 - 2026",
            era = VehicleEra.MODERN_UDS,
            engineOptions = "2.0 Flex • 2.0 Diesel 4x4 • T270 • Hurricane 2.0T",
            supportedEcus = createFcaModernEcus("Compass")
        ),
        Vehicle(
            id = "ram_rampage",
            brand = "RAM",
            name = "Rampage (Rebel / Laramie / R/T)",
            years = "2023 - 2026",
            era = VehicleEra.MODERN_UDS,
            engineOptions = "2.0 Multijet Diesel • 2.0 Turbo Gasolina Hurricane 4",
            supportedEcus = createFcaModernEcus("Rampage")
        ),
        Vehicle(
            id = "alfa_147_156_gt",
            brand = "Alfa Romeo",
            name = "147, 156 & GT (Twin Spark / JTS / V6)",
            years = "1997 - 2010",
            era = VehicleEra.CLASSIC_KLINE,
            engineOptions = "1.6/2.0 Twin Spark • 2.0 JTS • 2.5/3.2 Busso V6",
            supportedEcus = listOf(
                EcuModule("alfa_me731", "Bosch Motronic ME7.3.1 (Twin Spark)", EcuCategory.ENGINE, ProtocolType.KLINE_KWP2000_FAST, "8110F1", "81F110", AdapterCable.DIRECT, 0x10, 10400, isDefault = true),
                EcuModule("alfa_bcm", "Body Computer Marelli NISA", EcuCategory.BODY, ProtocolType.KLINE_KWP2000_FAST, "8100F1", "81F100", AdapterCable.YELLOW, 0x00, 10400),
                EcuModule("alfa_abs", "ABS Bosch 5.7 / Lucas", EcuCategory.BRAKES, ProtocolType.KLINE_KWP2000_FAST, "8120F1", "81F120", AdapterCable.GREEN, 0x20, 10400),
                EcuModule("alfa_airbag", "Airbag TRW", EcuCategory.AIRBAG, ProtocolType.KLINE_ISO9141, "6800F1", "68F100", AdapterCable.RED, 0x00, 4800)
            )
        )
    )

    private fun createFcaModernEcus(modelName: String): List<EcuModule> {
        return listOf(
            EcuModule("${modelName}_eng_flex", "Injeção Magneti Marelli IAW 10GF (Flex/T270)", EcuCategory.ENGINE, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT, isDefault = true),
            EcuModule("${modelName}_eng_diesel", "Injeção Bosch EDC17C69 (2.0 Diesel)", EcuCategory.ENGINE, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT),
            EcuModule("${modelName}_bcm", "Body Computer (BCM) Continental / Delphi", EcuCategory.BODY, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.YELLOW),
            EcuModule("${modelName}_ipc", "Painel de Instrumentos (Cluster 7\" TFT)", EcuCategory.DASHBOARD, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.YELLOW),
            EcuModule("${modelName}_abs", "Freios ABS / ESC Continental MK100", EcuCategory.BRAKES, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT),
            EcuModule("${modelName}_airbag", "Airbag Continental", EcuCategory.AIRBAG, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.DIRECT),
            EcuModule("${modelName}_trans_at9", "Câmbio ZF 9HP48 (9 Marchas)", EcuCategory.TRANSMISSION, ProtocolType.CAN_11BIT_500K, "7E1", "7E9", AdapterCable.DIRECT),
            EcuModule("${modelName}_trans_aisin6", "Câmbio Aisin TF72-SC (6 Marchas)", EcuCategory.TRANSMISSION, ProtocolType.CAN_11BIT_500K, "7E1", "7E9", AdapterCable.DIRECT),
            EcuModule("${modelName}_radio", "Central Multimídia Uconnect", EcuCategory.ENTERTAINMENT, ProtocolType.CAN_11BIT_500K, "7E0", "7E8", AdapterCable.BLUE)
        )
    }
}
