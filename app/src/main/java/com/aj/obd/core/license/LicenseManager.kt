package com.aj.obd.core.license

import android.content.Context
import android.content.SharedPreferences

data class LicenseState(
    val isPro: Boolean = true, // Padrão PRO habilitado para uso do desenvolvedor e oficinas
    val licenseKey: String = "OBDAJ-PRO-2026-VIP",
    val customerName: String = "Oficina / Usuário VIP",
    val expiryDate: String = "Vitalícia (Sem expiração)",
    val isTrial: Boolean = false
)

class LicenseManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("obd_aj_license", Context.MODE_PRIVATE)

    fun getLicense(): LicenseState {
        val isPro = prefs.getBoolean("is_pro", true)
        val key = prefs.getString("license_key", "OBDAJ-PRO-2026-VIP") ?: "OBDAJ-PRO-2026-VIP"
        val name = prefs.getString("customer_name", "Oficina / Usuário VIP") ?: "Oficina / Usuário VIP"
        return LicenseState(
            isPro = isPro,
            licenseKey = key,
            customerName = name
        )
    }

    fun activateKey(key: String, name: String = "Cliente OBD AJ"): Boolean {
        val sanitized = key.trim().uppercase()
        // Algoritmo de validação simples para comercialização offline
        if (sanitized.startsWith("OBDAJ-") && sanitized.length >= 12) {
            prefs.edit()
                .putBoolean("is_pro", true)
                .putString("license_key", sanitized)
                .putString("customer_name", name)
                .apply()
            return true
        }
        return false
    }

    fun setProMode(enabled: Boolean) {
        prefs.edit().putBoolean("is_pro", enabled).apply()
    }
}
