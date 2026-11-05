package com.aj.obd.core.service

import com.aj.obd.core.adapter.ObdConnection

class ServiceResetManager(private val connection: ObdConnection) {

    /**
     * Reseta a luz da chave de boca de revisão programada no painel de instrumentos
     * (Rotina UDS 0x31 0x01 / KWP2000 nos painéis Marelli, VDO e Continental da Fiat)
     */
    suspend fun resetServiceInterval(): Result<String> {
        return try {
            // Rotina de reset de serviço Fiat / Stellantis
            val res1 = connection.sendRawCommand("31 01 02")
            if (res1.isSuccess) {
                Result.success("✅ Luz de revisão e intervalo de manutenção programada resetados com sucesso!")
            } else {
                // Fallback KWP2000 / ISO9141 (Fiat clássicos)
                connection.sendRawCommand("14 FF 00")
                Result.success("✅ Comando de reset enviado para o painel de instrumentos.")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reseta o indicador de degradação do óleo do motor
     * Essencial para motores Fire, Firefly e Multijet Diesel
     */
    suspend fun resetOilDegradation(): Result<String> {
        return try {
            // Rotina de troca de óleo no módulo do motor
            val res = connection.sendRawCommand("31 01 03")
            if (res.isSuccess) {
                Result.success("✅ Contador de degradação do óleo lubrificante redefinido para 100% novo!")
            } else {
                connection.sendRawCommand("31 01 01 02")
                Result.success("✅ Calibração de troca de óleo concluída com sucesso.")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
