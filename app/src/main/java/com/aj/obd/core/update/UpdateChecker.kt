package com.aj.obd.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
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
        } catch (_: Exception) {
            // Continua para o fallback via GitHub Releases API
        }

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
                        latestVersion = tagName.ifEmpty { "1.2.2" },
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
     * Baixa o APK diretamente dentro do aplicativo com barra de progresso suave
     * sem sair da tela e instala automaticamente ao concluir (Igual CineList e LivennIA)
     */
    suspend fun downloadApkInApp(
        apkUrl: String,
        onProgress: (progress: Float, downloadedMb: Float, totalMb: Float) -> Unit,
        onSuccess: (file: File) -> Unit,
        onError: (error: String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val destinationDir = File(baseDir, "updates")
            if (!destinationDir.exists()) {
                destinationDir.mkdirs()
            }

            val destinationFile = File(destinationDir, "OBD-AJ-Update.apk")
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            var currentUrl = apkUrl
            var connection: HttpURLConnection

            // Segue redirects com segurança (GitHub Releases redireciona para storage S3/Azure da Microsoft)
            var redirects = 0
            while (true) {
                val urlObj = URL(currentUrl)
                connection = urlObj.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) OBD-AJ-Android-Updater")
                connection.setRequestProperty("Accept", "*/*")

                val status = connection.responseCode
                if (status in listOf(301, 302, 303, 307, 308)) {
                    val newUrl = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (newUrl.isNullOrBlank()) {
                        throw IllegalStateException("Redirecionamento sem cabeçalho Location.")
                    }
                    currentUrl = newUrl
                    redirects++
                    if (redirects > 8) throw IllegalStateException("Muitos redirecionamentos ao baixar atualização.")
                    continue
                }
                if (status != HttpURLConnection.HTTP_OK) {
                    connection.disconnect()
                    throw IllegalStateException("Servidor retornou status HTTP $status ao baixar atualização.")
                }
                break
            }

            val fileLength = connection.contentLengthLong
            val input: InputStream = connection.inputStream
            val output = FileOutputStream(destinationFile)

            val data = ByteArray(32 * 1024) // 32 KB buffer para download veloz
            var total: Long = 0
            var count: Int
            var lastUiUpdateTime = 0L

            val estimatedTotalBytes = if (fileLength > 1_000_000L) fileLength else 37_500_000L
            val totalMb = estimatedTotalBytes / (1024f * 1024f)

            while (input.read(data).also { count = it } != -1) {
                total += count
                output.write(data, 0, count)

                val now = System.currentTimeMillis()
                // Limita dispatch para a UI a cada 50ms para manter a tela a 60fps
                if (now - lastUiUpdateTime > 50L || total >= estimatedTotalBytes) {
                    lastUiUpdateTime = now
                    val progress = (total.toFloat() / estimatedTotalBytes.toFloat()).coerceIn(0f, 0.99f)
                    val downloadedMb = total / (1024f * 1024f)

                    withContext(Dispatchers.Main) {
                        onProgress(progress, downloadedMb, totalMb)
                    }
                }
            }

            output.flush()
            output.close()
            input.close()
            connection.disconnect()

            // Validação de integridade do arquivo APK baixado
            if (destinationFile.length() < 100_000L) {
                destinationFile.delete()
                throw IllegalStateException("Arquivo da atualização incompleto ou corrompido.")
            }

            // Garante permissões de leitura no filesystem Linux
            destinationFile.setReadable(true, false)
            destinationFile.setWritable(true, false)

            // Verifica cabeçalho ZIP ('P', 'K')
            val isZip = destinationFile.inputStream().use { stream ->
                val header = ByteArray(2)
                stream.read(header)
                header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte()
            }
            if (!isZip) {
                destinationFile.delete()
                throw IllegalStateException("O arquivo baixado não é um pacote APK válido.")
            }

            withContext(Dispatchers.Main) {
                onProgress(1f, total / (1024f * 1024f), totalMb)
                onSuccess(destinationFile)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Erro ao baixar atualização: ${e.localizedMessage}")
            }
        }
    }

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Ative a permissão 'Permitir desta fonte' para atualizar o OBD AJ.", Toast.LENGTH_LONG).show()
                }
            } catch (_: Exception) {
                try {
                    val genericIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(genericIntent)
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Dispara o instalador nativo do Android diretamente na tela do app
     */
    fun installApk(file: File): Boolean {
        if (!file.exists() || file.length() < 100_000L) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "APK não encontrado ou corrompido.", Toast.LENGTH_SHORT).show()
            }
            return false
        }

        // Garante permissões de leitura do arquivo no disco
        file.setReadable(true, false)
        file.setWritable(true, false)

        // 1. CHECAGEM CRÍTICA DE PERMISSÃO ANTES DE DISPARAR O INTENT
        // Se não tiver permissão, o Android ignora o Intent silenciosamente. Abrimos as configurações diretamente:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !canRequestPackageInstalls()) {
            openInstallPermissionSettings()
            return false
        }

        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }

            // Garante permissão explícita para os gerenciadores de instalação do Android
            val resolveList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    installIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(installIntent, PackageManager.MATCH_DEFAULT_ONLY)
            }

            for (resolveInfo in resolveList) {
                context.grantUriPermission(
                    resolveInfo.activityInfo.packageName,
                    apkUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            context.startActivity(installIntent)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Erro ao abrir o instalador: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
            return false
        }
    }

    fun openInBrowser(downloadUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
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
