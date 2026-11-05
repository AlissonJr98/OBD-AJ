package com.aj.obd.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.aj.obd.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
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

    private val releasesApiUrl = "https://api.github.com/repos/AlissonJr98/OBD-AJ/releases/latest"
    private val repoReleasesPage = "https://github.com/AlissonJr98/OBD-AJ/releases"

    suspend fun checkForUpdates(): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        val currentVersion = BuildConfig.VERSION_NAME

        try {
            val url = URL(releasesApiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "OBD-AJ-Android-App")
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 7000
            connection.readTimeout = 7000

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
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
                        latestVersion = tagName.ifEmpty { "1.1.0" },
                        currentVersion = currentVersion,
                        releaseTitle = releaseTitle,
                        releaseNotes = releaseNotes,
                        downloadUrl = apkDownloadUrl
                    )
                )
            } else {
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
     * Baixa o APK diretamente dentro do aplicativo com notificação de progresso
     * sem sair da tela nem abrir o navegador (Igual CineList e LivennIA)
     */
    suspend fun downloadApkInApp(
        apkUrl: String,
        onProgress: (progress: Float, downloadedMb: Float, totalMb: Float) -> Unit,
        onSuccess: (file: File) -> Unit,
        onError: (error: String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val destinationDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir

            val destinationFile = File(destinationDir, "OBD-AJ-Update.apk")
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            var currentUrl = apkUrl
            var connection: HttpURLConnection

            // Segue redirects (GitHub costuma redirecionar para objetos S3 do GitHub Releases)
            var redirects = 0
            while (true) {
                val urlObj = URL(currentUrl)
                connection = urlObj.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 10000
                connection.readTimeout = 15000
                connection.setRequestProperty("User-Agent", "OBD-AJ-InApp-Updater")

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                    val newUrl = connection.getHeaderField("Location")
                    currentUrl = newUrl
                    redirects++
                    if (redirects > 5) throw IllegalStateException("Muitos redirecionamentos ao baixar atualização.")
                    continue
                }
                break
            }

            val fileLength = connection.contentLengthLong
            val input: InputStream = connection.inputStream
            val output = FileOutputStream(destinationFile)

            val data = ByteArray(8192)
            var total: Long = 0
            var count: Int

            val totalMb = if (fileLength > 0) fileLength / (1024f * 1024f) else 36f

            while (input.read(data).also { count = it } != -1) {
                total += count
                output.write(data, 0, count)

                val progress = if (fileLength > 0) total.toFloat() / fileLength.toFloat() else 0.5f
                val downloadedMb = total / (1024f * 1024f)

                withContext(Dispatchers.Main) {
                    onProgress(progress.coerceIn(0f, 1f), downloadedMb, totalMb)
                }
            }

            output.flush()
            output.close()
            input.close()

            withContext(Dispatchers.Main) {
                onSuccess(destinationFile)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Erro ao baixar atualização: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Dispara o instalador nativo do Android diretamente na tela do app
     */
    fun installApk(file: File) {
        if (!file.exists() || file.length() == 0L) return

        // Verifica permissão no Android 8.0+ para instalar pacotes desconhecidos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            }
        }

        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            // Garante permissão explícita para os gerenciadores de instalação do Android
            val resolveList = context.packageManager.queryIntentActivities(
                installIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolveInfo in resolveList) {
                context.grantUriPermission(
                    resolveInfo.activityInfo.packageName,
                    apkUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
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
