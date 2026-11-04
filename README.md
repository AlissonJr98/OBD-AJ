# OBD AJ — Scanner & Diagnóstico Automotivo Avançado

**OBD AJ** é um aplicativo Android moderno desenvolvido em **Kotlin** e **Jetpack Compose**, projetado para substituir e modernizar softwares legados de diagnóstico como o AlfaOBD. Focado principalmente em veículos do Grupo Stellantis / FCA (Fiat, Jeep, RAM, Alfa Romeo), o aplicativo combina diagnósticos avançados com uma interface no estilo cockpit esportivo e ativações seguras em um toque.

---

## 🚀 Principais Recursos e Diferenciais

* **Painel Digital Cockpit (Tempo Real):**
  * Manômetros animados com renderização fluida (Gauges circulares).
  * Monitoramento de Rotação (RPM), Velocidade (km/h), Pressão de Turbo (bar), Temperatura do Líquido de Arrefecimento (°C) e Tensão da Bateria (V).
* **Diagnóstico Completo de Falhas (DTC):**
  * Leitura e interpretação de mais de 22.000 códigos de erro com descrições em Português.
  * Classificação visual de gravidade (Crítico, Grave, Moderado, Informativo).
  * Limpeza segura de códigos com 1 toque.
* **Proxi Fácil (One-Click Coding & Alinhamento de Proxi):**
  * Ativação e desativação de opcionais originais via chaveamento visual:
    * Faróis de Neblina / Milha
    * Cornering Lights (luz de curva)
    * Câmera de Ré na Central Multimídia Uconnect
    * Paddle Shifters (borboletas no volante)
    * Menu de DRL no computador de bordo
    * Bip do cinto de segurança
  * **Proteção Anti-Brick:** Criação de backup automático do arquivo `.hex` original antes de qualquer gravação na central de carroceria (BCM).
* **Assistente de Cabos & Adaptadores:**
  * Alertas visuais indicando qual cabo adaptador físico deve ser utilizado (Cabo Amarelo A3 para BCM/Painel, Cabo Azul A5 para Multimídia, Direto para Injeção/Câmbio).
* **Detecção de Adaptador Genuíno vs Clone:**
  * Análise de capacidade de barramento e identificação de chips STN (OBDLink), vLinker e ELM327 v1.4.
* **Modo Simulador Interno:**
  * Permite testar todo o ecossistema e interface mesmo sem um adaptador físico plugado no veículo.

---

## 🛠️ Tecnologias Utilizadas

* **Linguagem:** Kotlin 100%
* **Interface Gráfica:** Jetpack Compose + Material Design 3
* **Concorrência:** Kotlin Coroutines & StateFlow
* **Banco de Dados:** SQLite nativo com base automotiva pré-carregada (`automotive_knowledge.db` - 49.7 MB)
* **Protocolos Automotivos Suportados:**
  * UDS (Unified Diagnostic Services - ISO 14229)
  * ISO-TP (Transport Layer - ISO 15765-2)
  * CAN Bus 11-bit e 29-bit a 50k, 250k e 500k baud (ISO 15765-4)
  * KWP2000 Fast Init e Slow Init (ISO 14230-4)
  * ISO 9141-2 (K-Line tradicional)

---

## 📂 Estrutura de Diretórios

```
e:\Android\OBD AJ
├── app/
│   ├── src/main/
│   │   ├── assets/database/       # Base com 1600+ ECUs, 22.000+ DTCs e Proxi
│   │   ├── java/com/aj/obd/
│   │   │   ├── core/
│   │   │   │   ├── adapter/       # Drivers ELM327, Bluetooth SPP e Simulador
│   │   │   │   ├── uds/           # Cliente UDS (0x10, 0x14, 0x19, 0x22, 0x2E)
│   │   │   │   └── proxi/         # Gerenciador e gravador de Proxi com backup
│   │   │   ├── data/              # Catálogo de veículos e Helper SQLite
│   │   │   ├── domain/model/      # Modelos de dados
│   │   │   └── ui/                # Telas e Componentes Jetpack Compose
│   │   │       ├── components/    # Manômetros circulares e badges de cabos
│   │   │       ├── screens/       # Home, Cockpit, Falhas, Proxi, Conexão
│   │   │       └── theme/         # Design System Dark Carbon & Cockpit Glow
```
