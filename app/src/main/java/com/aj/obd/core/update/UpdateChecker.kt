package com.aj.obd.core.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.aj.obd.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val currentVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String
)

class UpdateChecker(private val context: Context) {

    private val rawVersionJsonUrl = "https://raw.githubusercontent.com/AlissonJr98/OBD-AJ/main/version.json"
    private val releasesApiUrl = "https://api.github.com/repos/AlissonJr98/OBD-AJ/releases/latest"
    private val repoReleasesPage = "https://github.com/AlissonJr98/OBD-AJ/releases"

    suspend fun checkForUpdates(): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        val currentVersion = BuildConfig.VERSION_NAME

        // 1. Tenta primeiro pelo version.json no raw.githubusercontent.com (SEM LIMITE DE TAXA DE API)
        try {
            val jsonUrl = URL("$rawVersionJsonUrl?t=${System.currentTimeMillis()}")
            val conn = jsonUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) OBD-AJ-Android-App")
            conn.connectTimeout = 6000
            conn.readTimeout = 6000

            if (conn.responseCode == 200) {
                val jsonString = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(jsonString)
                val remoteVersion = json.optString("versionName", "").removePrefix("v").trim()
                val title = json.optString("title", "Nova Atualização do OBD AJ")
                val notes = json.optString("notes", "Melhorias de desempenho, design 3D e novas funções.")
                val apkUrl = json.optString("apkUrl", "")

                if (remoteVersion.isNotBlank() && apkUrl.isNotBlank()) {
                    val hasUpdate = isVersionNewer(remoteVersion, currentVersion)
                    return@withContext Result.success(
                        AppUpdateInfo(
                            hasUpdate = hasUpdate,
                            latestVersion = remoteVersion,
                            currentVersion = currentVersion,
                            releaseTitle = title,
                            releaseNotes = notes,
                            downloadUrl = apkUrl
                        )
                    )
                }
            } else {
                conn.disconnect()
            }
        } catch (_: Exception) {}

        // 2. Fallback via GitHub Releases API
        try {
            val url = URL(releasesApiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "OBD-AJ-Android-App")
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                val json = JSONObject(jsonString)

                val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                val releaseTitle = json.optString("name", "Nova Atualização Disponível")
                val releaseNotes = json.optString("body", "Melhorias de desempenho, design 3D futurista e conexão rápida com o scanner.")
                val htmlUrl = json.optString("html_url", repoReleasesPage)

                var apkDownloadUrl = htmlUrl
                val assets = json.optJSONArray("assets")
                if (assets != null && assets.length() > 0) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = asset.optString("browser_download_url", htmlUrl)
                            break
                        }
                    }
                }

                val hasUpdate = isVersionNewer(tagName, currentVersion)

                Result.success(
                    AppUpdateInfo(
                        hasUpdate = hasUpdate,
                        latestVersion = tagName.ifEmpty { "1.2.6" },
                        currentVersion = currentVersion,
                        releaseTitle = releaseTitle,
                        releaseNotes = releaseNotes,
                        downloadUrl = apkDownloadUrl
                    )
                )
            } else {
                connection.disconnect()
                Result.success(
                    AppUpdateInfo(
                        hasUpdate = false,
                        latestVersion = currentVersion,
                        currentVersion = currentVersion,
                        releaseTitle = "Versão Atualizada",
                        releaseNotes = "Você já está na versão mais recente.",
                        downloadUrl = repoReleasesPage
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ABORDAGEM 1 (Recomendada): Abre o APK direto no navegador padrão (Chrome, Samsung Internet).
     * 100% à prova de falhas em qualquer versão do Android. O navegador faz o download veloz
     * e notifica o usuário com o botão 'Abrir' / 'Instalar' de imediato.
     */
    fun openInBrowser(downloadUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Abrindo download no navegador...", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Erro ao abrir navegador: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * ABORDAGEM 2: Dispara o download nativo via DownloadManager do Android
     * O download roda em segundo plano e notifica na barra de status do aparelho.
     */
    fun startDownloadManager(downloadUrl: String, versionName: String): Long {
        try {
            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("OBD AJ v$versionName - Atualização")
                setDescription("Baixando novo pacote APK...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "OBD-AJ-v$versionName.apk")
                setMimeType("application/vnd.android.package-archive")
            }
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = dm.enqueue(request)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Download iniciado! Acompanhe na barra de notificações do celular.", Toast.LENGTH_LONG).show()
            }
            return downloadId
        } catch (e: Exception) {
            openInBrowser(downloadUrl)
            return -1L
        }
    }

    private fun isVersionNewer(remote: String, current: String): Boolean {
        if (remote.isBlank()) return false
        val rParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(rParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
