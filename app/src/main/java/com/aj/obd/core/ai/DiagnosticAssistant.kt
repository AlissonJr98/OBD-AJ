package com.aj.obd.core.ai

data class AiDiagnosticReport(
    val dtcCode: String,
    val dtcDescription: String,
    val summary: String,
    val probableCauses: List<String>,
    val testSteps: List<String>,
    val recommendedParts: List<String>,
    val severity: String = "MÉDIA"
)

object DiagnosticAssistant {

    fun generateReport(code: String, description: String, vehicleName: String = "Fiat Palio"): AiDiagnosticReport {
        val cleanCode = code.uppercase().trim()

        return when {
            cleanCode.startsWith("P0300") || cleanCode in listOf("P0301", "P0302", "P0303", "P0304") -> {
                val cyl = if (cleanCode == "P0300") "múltiplos cilindros" else "cilindro ${cleanCode.last()}"
                AiDiagnosticReport(
                    dtcCode = cleanCode,
                    dtcDescription = description.ifBlank { "Falha de combustão / ignição detectada em $cyl" },
                    summary = "A central detectou que a queima da mistura ar/combustível não ocorreu corretamente em $cyl. Isso causa trepidação, perda de potência e aumento no consumo.",
                    probableCauses = listOf(
                        "Velas de ignição desgastadas ou com folga de eletrodo incorreta (padrão 0.8mm)",
                        "Cabos de vela com fuga de corrente ou alta resistência (acima de 5k ohms)",
                        "Bobina de ignição dupla com falha no primário ou secundário (comum em motores Fire)",
                        "Bico injetor travado, com baixa vazão ou gotejando",
                        "Entrada falsa de ar na admissão ou baixa compressão no cilindro"
                    ),
                    testSteps = listOf(
                        "1. Meça a resistência dos cabos de vela com multímetro na escala de 20k Ohms.",
                        "2. Remova as velas e inspecione a coloração (eletrodo preto = excesso/óleo, esbranquiçado = falta).",
                        "3. Teste a centelha da bobina com centelhador em cada saída de cilindro.",
                        "4. Realize teste de estanqueidade e vazão dos bicos injetores na máquina de bicos."
                    ),
                    recommendedParts = listOf(
                        "Jogo de Velas de Ignição (NGK BKR6E / Resistivas)",
                        "Jogo de Cabos de Vela de Silicone",
                        "Bobina de Ignição Dupla (Marelli / Bosch)"
                    ),
                    severity = "ALTA (Evite rodar muito para não danificar o catalisador)"
                )
            }
            cleanCode in listOf("P0130", "P0131", "P0132", "P0133", "P0134", "P0135") -> {
                AiDiagnosticReport(
                    dtcCode = cleanCode,
                    dtcDescription = description.ifBlank { "Circuito da Sonda Lambda (Sensor de Oxigênio Pré-Catalisador)" },
                    summary = "A sonda lambda não está oscilando entre 0.1V e 0.9V como esperado ou o circuito do aquecedor interno está aberto, impedindo a central de regular a injeção em malha fechada.",
                    probableCauses = listOf(
                        "Sonda lambda contaminada por fuligem, combustível adulterado ou queima de óleo",
                        "Resistência do aquecedor da sonda queimada (pino 1 e 2 com circuito aberto)",
                        "Fissura ou vazamento de escape antes da sonda (ar entrando no coletor)",
                        "Fiação do chicote partida ou conector oxidado"
                    ),
                    testSteps = listOf(
                        "1. Meça a resistência do aquecedor da sonda (deve marcar entre 4 e 15 Ohms).",
                        "2. Monitore o sinal no osciloscópio do app: o sinal deve variar rapidamente entre 150mV e 850mV com motor aquecido.",
                        "3. Verifique se chegam 12V no aquecedor com a chave ligada."
                    ),
                    recommendedParts = listOf(
                        "Sonda Lambda Pré-Catalisador 4 fios (Planar / Finger)",
                        "Reparo do chicote ou conector fêmea"
                    ),
                    severity = "MÉDIA (Aumenta o consumo de combustível)"
                )
            }
            cleanCode in listOf("P0115", "P0116", "P0117", "P0118") -> {
                AiDiagnosticReport(
                    dtcCode = cleanCode,
                    dtcDescription = description.ifBlank { "Sensor de Temperatura do Líquido de Arrefecimento (ECT)" },
                    summary = "A injeção perdeu a leitura precisa da temperatura do motor. O carro pode apresentar dificuldade para pegar frio, afogamento, consumo excessivo e ventoinha ligada direto em modo de emergência.",
                    probableCauses = listOf(
                        "Sensor de temperatura da água (ECT) com circuito aberto ou em curto",
                        "Conector verde/azul com terminal quebrado ou chicote ressecado perto do cabeçote",
                        "Válvula termostática travada aberta (motor não atinge a temperatura ideal de 90°C)",
                        "Ar no sistema de arrefecimento impedindo o contato da água com o sensor"
                    ),
                    testSteps = listOf(
                        "1. Meça a resistência NTC do sensor com multímetro (aprox. 2.5k Ohms a 20°C e 250 Ohms a 90°C).",
                        "2. Cheque a alimentação de referência de 5.0V no conector do sensor com a chave ligada.",
                        "3. Verifique se o nível do líquido de arrefecimento está correto e faça sangria de ar."
                    ),
                    recommendedParts = listOf(
                        "Sensor de Temperatura da Água (MTE-Thomson / Iguaçu)",
                        "Válvula Termostática com carcaça",
                        "Líquido de arrefecimento orgânico concentrado"
                    ),
                    severity = "ALTA (Pode causar superaquecimento e queima da junta do cabeçote)"
                )
            }
            cleanCode in listOf("P0170", "P0171", "P0172") -> {
                val mix = if (cleanCode == "P0172") "Rica (Excesso de Combustível)" else "Pobre (Falta de Combustível / Entrada de Ar)"
                AiDiagnosticReport(
                    dtcCode = cleanCode,
                    dtcDescription = description.ifBlank { "Ajuste de Combustível Fora da Faixa - Mistura $mix" },
                    summary = "A central eletrônica atingiu o limite máximo de compensação de combustível (Long Term Fuel Trim). O motor está operando com mistura descalibrada.",
                    probableCauses = listOf(
                        "Entrada falsa de ar por mangueira do hidrovácuo, respiro do cárter ou juntas do coletor",
                        "Bomba de combustível com baixa pressão (abaixo de 3.0 a 3.8 bar na linha)",
                        "Filtro de combustível obstruído ou regulador de pressão furado",
                        "Corpo de borboleta (TBI) muito sujo ou descalibrado",
                        "Combustível de má qualidade ou descalibração do A/F Flex"
                    ),
                    testSteps = listOf(
                        "1. Instale o manômetro de pressão e vazão na linha de combustível (deve manter 3.5 a 4.2 bar).",
                        "2. Aplique limpa-contato nas mangueiras de vácuo para detectar entrada de ar falsa.",
                        "3. Use a função 'Ajuste Forçado de A/F' no app para alinhar a proporção de Etanol/Gasolina."
                    ),
                    recommendedParts = listOf(
                        "Filtro de Combustível e Pré-filtro da bomba",
                        "Refil da Bomba de Combustível (Bosch / Marelli)",
                        "Kit de anéis de vedação dos bicos injetores"
                    ),
                    severity = "MÉDIA"
                )
            }
            cleanCode == "P1300" || cleanCode == "P1301" -> {
                AiDiagnosticReport(
                    dtcCode = cleanCode,
                    dtcDescription = "Aprendizado de Roda Fônica Não Realizado (Específico Fiat Fire)",
                    summary = "Falha clássica da linha Fiat Fire (Palio, Uno, Siena, Strada). Ocorre após trocar a correia dentada, sensor de rotação ou reset da central. A luz da injeção fica piscando no painel.",
                    probableCauses = listOf(
                        "Substituição recente da correia dentada sem calibração da roda fônica",
                        "Substituição do sensor de rotação ou polia do virabrequim",
                        "Troca ou reprogramação da central de injeção IAW 4AF / 4EF / 59F",
                        "Dentes da roda fônica amassados ou com sujeira metálica"
                    ),
                    testSteps = listOf(
                        "1. Aqueça o motor até acionar a ventoinha (temperatura da água acima de 85°C).",
                        "2. Com o veículo parado em ponto morto, acelere fundo até 5.000 RPM por 3 segundos.",
                        "3. Solte o acelerador até a marcha lenta e repita mais duas vezes.",
                        "4. Desligue a ignição por 15 segundos. A luz de injeção irá parar de piscar!"
                    ),
                    recommendedParts = listOf(
                        "Não requer troca de peças! Apenas calibração de rotação."
                    ),
                    severity = "BAIXA (Procedimento de rotina de oficina)"
                )
            }
            else -> {
                AiDiagnosticReport(
                    dtcCode = cleanCode,
                    dtcDescription = description.ifBlank { "Código de Falha de Diagnóstico Automotivo" },
                    summary = "Código de anomalia registrado na memória não-volátil da central eletrônica. Indica leitura fora dos parâmetros nominais de calibração.",
                    probableCauses = listOf(
                        "Mau contato no chicote elétrico ou oxidação no conector do sensor/atuador",
                        "Desgaste natural do componente eletrônico ou mecânico",
                        "Flutuação na tensão de alimentação (bateria ou aterramento do bloco)",
                        "Sinal espúrio gerado durante partida com bateria descarregada"
                    ),
                    testSteps = listOf(
                        "1. Verifique a integridade visual do chicote e terminais elétricos.",
                        "2. Meça a tensão de alimentação e sinal com chave de ignição ligada.",
                        "3. Apague a memória de falhas pelo app e realize teste de rodagem de 5 km para verificar reincidência."
                    ),
                    recommendedParts = listOf(
                        "Limpa-contato elétrico para conectores",
                        "Componente sensor/atuador correspondente ao circuito"
                    ),
                    severity = "MÉDIA"
                )
            }
        }
    }
}
