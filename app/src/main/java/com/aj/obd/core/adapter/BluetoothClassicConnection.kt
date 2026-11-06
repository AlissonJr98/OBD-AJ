package com.aj.obd.core.adapter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.TimeoutException

class BluetoothClassicConnection(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) : ObdConnection {

    // UUID SPP padrão (Serial Port Profile) para dispositivos RFCOMM
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    @SuppressLint("MissingPermission")
    override suspend fun connect(address: String): Boolean = withContext(Dispatchers.IO) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _state.value = ConnectionState.Error("Bluetooth desativado no aparelho")
            return@withContext false
        }

        disconnect() // Limpa conexões e sockets anteriores

        val device: BluetoothDevice = try {
            bluetoothAdapter.getRemoteDevice(address)
        } catch (e: Exception) {
            _state.value = ConnectionState.Error("Endereço Bluetooth inválido: $address")
            return@withContext false
        }

        val devName = device.name ?: address
        _state.value = ConnectionState.Connecting(devName, "Preparando canal Bluetooth...")

        // Cancela descoberta ativa para liberar toda a largura de banda e rádio Bluetooth
        try {
            bluetoothAdapter.cancelDiscovery()
        } catch (_: Exception) {}

        var lastError: Exception? = null

        // 4 Estratégias de Conexão idênticas ao AlfaOBD e Car Scanner
        val strategies = listOf(
            "Canal SPP Padrão" to {
                device.createRfcommSocketToServiceRecord(sppUuid)
            },
            "Canal SPP Inseguro" to {
                device.createInsecureRfcommSocketToServiceRecord(sppUuid)
            },
            "Porta RFCOMM 1 Direta" to {
                val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                m.invoke(device, 1) as BluetoothSocket
            },
            "Porta RFCOMM 1 Insegura" to {
                val m = device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
                m.invoke(device, 1) as BluetoothSocket
            }
        )

        for ((strategyName, socketFactory) in strategies) {
            try {
                _state.value = ConnectionState.Connecting(devName, "Conectando via $strategyName...")
                val sock = socketFactory()
                sock.connect()

                if (sock.isConnected) {
                    socket = sock
                    inputStream = sock.inputStream
                    outputStream = sock.outputStream

                    _state.value = ConnectionState.Connecting(devName, "Identificando chip ELM327...")

                    // Drena e limpa a linha UART
                    try {
                        outputStream?.write("\r\r\r".toByteArray(Charsets.US_ASCII))
                        outputStream?.flush()
                    } catch (_: Exception) {}
                    delay(150)

                    val driver = Elm327Driver(this@BluetoothClassicConnection)
                    val handshakeRes = driver.handshake()

                    if (handshakeRes.isSuccess) {
                        val info = handshakeRes.getOrThrow()
                        _state.value = ConnectionState.Connected(
                            deviceName = devName,
                            adapterType = info.type,
                            firmwareVersion = info.version,
                            batteryVoltage = info.voltage,
                            isHighSpeedCapable = info.isHighSpeedCapable,
                            isEcuConnected = info.isEcuConnected,
                            detectedProtocol = info.detectedProtocol,
                            ignitionStatus = info.ignitionStatus
                        )
                        return@withContext true
                    } else {
                        // O socket conectou no rádio, mas o chip ELM não respondeu
                        lastError = handshakeRes.exceptionOrNull() as? Exception
                            ?: IllegalStateException("Chip ELM327 não respondeu")
                        try { sock.close() } catch (_: Exception) {}
                        socket = null
                        inputStream = null
                        outputStream = null
                    }
                }
            } catch (e: Exception) {
                lastError = e
                try { socket?.close() } catch (_: Exception) {}
                socket = null
                inputStream = null
                outputStream = null
            }
        }

        val msg = lastError?.localizedMessage ?: "Não foi possível comunicar com o chip ELM327."
        _state.value = ConnectionState.Error("Falha de conexão: $msg. Certifique-se de que o scanner está bem plugado no carro e com o LED aceso.")
        disconnect()
        false
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            inputStream?.close()
        } catch (_: Exception) {}
        try {
            outputStream?.close()
        } catch (_: Exception) {}
        try {
            socket?.close()
        } catch (_: Exception) {}
        inputStream = null
        outputStream = null
        socket = null
        _state.value = ConnectionState.Disconnected
    }

    override suspend fun sendRawCommand(cmd: String, timeoutMs: Long): Result<String> = withContext(Dispatchers.IO) {
        val s = socket
        if (s == null || !s.isConnected) {
            _state.value = ConnectionState.Error("Conexão Bluetooth perdida com o scanner.")
            return@withContext Result.failure(IOException("Socket Bluetooth desconectado"))
        }

        val inStream = inputStream ?: return@withContext Result.failure(IOException("Não conectado"))
        val outStream = outputStream ?: return@withContext Result.failure(IOException("Não conectado"))

        try {
            // Drena bytes residuais do buffer de entrada para evitar respostas antigas
            while (inStream.available() > 0) {
                inStream.read()
            }

            // Envia o comando ASCII terminado em Carriage Return (\r)
            val bytes = (cmd.trim() + "\r").toByteArray(Charsets.US_ASCII)
            outStream.write(bytes)
            outStream.flush()

            val responseBuffer = ByteArrayOutputStream()
            val startTime = System.currentTimeMillis()
            var charRead: Int

            // Lê byte a byte até o caractere de prompt '>' do ELM ou estourar o timeout
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (inStream.available() > 0) {
                    charRead = inStream.read()
                    if (charRead == -1) {
                        // EOF -> Dispositivo Bluetooth desconectou fisicamente
                        disconnect()
                        _state.value = ConnectionState.Error("Conexão Bluetooth caiu (Dispositivo desconectado)")
                        return@withContext Result.failure(IOException("Conexão Bluetooth perdida (EOF)"))
                    }
                    val ch = charRead.toChar()
                    if (ch == '>') {
                        break
                    }
                    responseBuffer.write(charRead)
                } else {
                    delay(4) // Cede processamento de coroutine enquanto aguarda o rádio BT
                }
            }

            val raw = responseBuffer.toString(Charsets.US_ASCII.name())
            val clean = raw
                .replace("\r", "\n")
                .lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.equals(cmd.trim(), ignoreCase = true) }
                .joinToString("\n")

            if (clean.isBlank() && System.currentTimeMillis() - startTime >= timeoutMs) {
                return@withContext Result.failure(TimeoutException("Timeout aguardando resposta de '$cmd'"))
            }

            Result.success(clean)
        } catch (e: IOException) {
            // Se houve falha de E/S no Bluetooth, o link caiu!
            disconnect()
            _state.value = ConnectionState.Error("Conexão Bluetooth interrompida: ${e.localizedMessage}")
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun readBatteryVoltage(): Float {
        val res = sendRawCommand("ATRV")
        val text = res.getOrNull()?.trim()?.replace("V", "")?.trim() ?: "0"
        return text.toFloatOrNull() ?: 0f
    }
}
