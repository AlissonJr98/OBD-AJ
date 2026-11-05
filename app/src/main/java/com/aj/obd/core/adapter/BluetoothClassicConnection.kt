package com.aj.obd.core.adapter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID

class BluetoothClassicConnection(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) : ObdConnection {

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var socket: BluetoothSocket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    @SuppressLint("MissingPermission")
    override suspend fun connect(address: String): Boolean = withContext(Dispatchers.IO) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _state.value = ConnectionState.Error("Bluetooth desativado no aparelho")
            return@withContext false
        }

        try {
            val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(address)
            _state.value = ConnectionState.Connecting(device.name ?: address, "Criando Socket SPP...")

            bluetoothAdapter.cancelDiscovery()
            var isConnected = false
            var lastSocketException: Exception? = null

            // 1ª Tentativa: Standard RFCOMM SPP UUID
            try {
                socket = device.createRfcommSocketToServiceRecord(sppUuid)
                socket?.connect()
                isConnected = true
            } catch (e1: Exception) {
                lastSocketException = e1
                try { socket?.close() } catch (_: Exception) {}
                socket = null
            }

            // 2ª Tentativa: Reflection RFCOMM no canal 1 (essencial para clones chineses ELM327)
            if (!isConnected) {
                _state.value = ConnectionState.Connecting(device.name ?: address, "Tentando canal alternativo RFCOMM...")
                try {
                    val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    socket = m.invoke(device, 1) as BluetoothSocket
                    socket?.connect()
                    isConnected = true
                } catch (e2: Exception) {
                    lastSocketException = e2
                    try { socket?.close() } catch (_: Exception) {}
                    socket = null
                }
            }

            // 3ª Tentativa: Insecure RFCOMM Socket
            if (!isConnected) {
                try {
                    socket = device.createInsecureRfcommSocketToServiceRecord(sppUuid)
                    socket?.connect()
                    isConnected = true
                } catch (e3: Exception) {
                    lastSocketException = e3
                    try { socket?.close() } catch (_: Exception) {}
                    socket = null
                }
            }

            if (!isConnected || socket == null) {
                throw lastSocketException ?: IllegalStateException("Não foi possível estabelecer conexão RFCOMM com o scanner.")
            }

            reader = BufferedReader(InputStreamReader(socket!!.inputStream))
            writer = BufferedWriter(OutputStreamWriter(socket!!.outputStream))

            _state.value = ConnectionState.Connecting(device.name ?: address, "Inicializando Chipset ELM327...")
            val driver = Elm327Driver(this@BluetoothClassicConnection)
            val initResult = driver.handshake()

            if (initResult.isSuccess) {
                val info = initResult.getOrThrow()
                _state.value = ConnectionState.Connected(
                    deviceName = device.name ?: address,
                    adapterType = info.type,
                    firmwareVersion = info.version,
                    batteryVoltage = info.voltage,
                    isHighSpeedCapable = info.isHighSpeedCapable
                )
                true
            } else {
                _state.value = ConnectionState.Error("Falha na inicialização do adaptador: ${initResult.exceptionOrNull()?.message}")
                disconnect()
                false
            }
        } catch (e: Exception) {
            _state.value = ConnectionState.Error("Erro ao conectar: ${e.localizedMessage}")
            disconnect()
            false
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            reader?.close()
            writer?.close()
            socket?.close()
        } catch (_: Exception) {}
        reader = null
        writer = null
        socket = null
        _state.value = ConnectionState.Disconnected
    }

    override suspend fun sendRawCommand(cmd: String, timeoutMs: Long): Result<String> = withContext(Dispatchers.IO) {
        val w = writer ?: return@withContext Result.failure(IllegalStateException("Não conectado"))
        val r = reader ?: return@withContext Result.failure(IllegalStateException("Não conectado"))

        try {
            withTimeout(timeoutMs) {
                // Envia comando terminado em Carriage Return (\r)
                w.write(cmd + "\r")
                w.flush()

                val buffer = StringBuilder()
                var c: Int
                while (true) {
                    c = r.read()
                    if (c == -1) break
                    val ch = c.toChar()
                    if (ch == '>') { // Prompt pronto do ELM
                        break
                    }
                    buffer.append(ch)
                }

                val clean = buffer.toString()
                    .replace("\r", "\n")
                    .lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.equals(cmd, ignoreCase = true) }
                    .joinToString("\n")

                Result.success(clean)
            }
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
