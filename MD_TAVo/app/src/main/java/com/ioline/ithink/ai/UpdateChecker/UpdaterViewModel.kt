package com.ioline.ithink.ai.UpdateChecker

import android.app.Application
import android.app.DownloadManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ioline.ithink.ai.MyDeviceAdminReceiver
import com.ioline.ithink.ai.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

data class UpdaterUiState(
    val latestVersionName: String = "",
    val isDownloading: Boolean = false,
    val progress: Int = 0,
    val statusText: String = "",
    val buttonEnabled: Boolean = true
)

class UpdaterViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    private var downloadId: Long = -1L
    private var downloadManager: DownloadManager? = null

    private val _uiState = MutableStateFlow(UpdaterUiState())
    val uiState = _uiState.asStateFlow()

    fun setLatestVersionName(name: String) {
        _uiState.update { it.copy(latestVersionName = name) }
    }

    // ----------------------------------------------------------------------
    // 1. APAGAR TODOS OS APKs VIA DOWNLOADMANAGER
    // ----------------------------------------------------------------------
    fun deleteAllRelatedDownloads() {
        val dm = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val query = DownloadManager.Query()
        val cursor = dm.query(query)

        cursor?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_ID))
                val uri = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))

                if (uri != null && uri.contains("mordomus_tavo", ignoreCase = true)) {
                    dm.remove(id)  // APAGA COMPLETAMENTE
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // 2. APAGAR ARQUIVOS NA PASTA DOWNLOAD (BACKUP)
    // ----------------------------------------------------------------------
    fun deleteOldApksInPublicFolder() {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        dir?.listFiles()?.forEach { file ->
            if (
                file.isFile &&
                file.name.contains("mordomus_tavo", ignoreCase = true) &&
                file.name.endsWith(".apk")
            ) {
                file.delete()
            }
        }
    }


    fun deleteAnyApkInDownloadFolder() {
        val dir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )

        dir?.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".apk")) {
                file.delete()
            }
        }
    }

    // ----------------------------------------------------------------------
    // INICIAR DOWNLOAD + INSTALAÇÃO
    // ----------------------------------------------------------------------
    fun startUpdate(apkUrl: String) {
        if (apkUrl.isBlank()) {
            _uiState.update {
                it.copy(statusText = "URL inválida", buttonEnabled = true, isDownloading = false)
            }
            return
        }
        // LIMPAR ARQUIVOS ANTIGOS ANTES DE BAIXAR
        deleteAllRelatedDownloads()
        deleteOldApksInPublicFolder()
        deleteAnyApkInDownloadFolder()

        val dm = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager = dm

        val apkName = apkUrl.substringAfterLast("/")

        val request = DownloadManager.Request(apkUrl.toUri())
            .setTitle(appContext.getString(R.string.download_update))
            .setDescription(appContext.getString(R.string.reload_page))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, apkName)

        downloadId = dm.enqueue(request)

        _uiState.update {
            it.copy(isDownloading = true, buttonEnabled = false, progress = 0, statusText = "0%")
        }

        // Monitoramento de progresso
        viewModelScope.launch(Dispatchers.IO) {
            var downloading = true

            while (downloading) {
                val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId))

                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val downloaded = c.getLong(
                            c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        )
                        val total = c.getLong(
                            c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        )

                        if (total > 0) {
                            val progress = ((downloaded * 100) / total).toInt().coerceIn(0, 100)

                            _uiState.update {
                                it.copy(progress = progress, statusText = "$progress%")
                            }
                        }

                        when (c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {

                            DownloadManager.STATUS_SUCCESSFUL -> {
                                downloading = false

                                _uiState.update {
                                    it.copy(progress = 100, statusText = "Install", isDownloading = false)
                                }

                                val apkFile = File(
                                    Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_DOWNLOADS
                                    ),
                                    apkName
                                )

                                installApkSmart(apkFile)
                            }

                            DownloadManager.STATUS_FAILED -> {
                                downloading = false
                                _uiState.update {
                                    it.copy(statusText = "Download Fail", isDownloading = false, buttonEnabled = true)
                                }
                            }
                        }
                    }
                }

                delay(500)
            }
        }
    }

    // ----------------------------------------------------------------------
    // INSTALAÇÃO INTELIGENTE
    // ----------------------------------------------------------------------
    private fun installApkSmart(apkFile: File) {
        when {
            isDeviceOwner() -> installApkDeviceOwner(apkFile)
            isRootAvailable() -> silentInstallRoot(apkFile)
            else -> showInstallPrompt(apkFile)
        }
    }

    private fun isDeviceOwner(): Boolean {
        val dpm = appContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isDeviceOwnerApp(appContext.packageName)
    }

    // INSTALAÇÃO COMO DEVICE OWNER
    private fun installApkDeviceOwner(apkFile: File) {
        if (!apkFile.exists()) return

        val pi = appContext.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = pi.createSession(params)
        val session = pi.openSession(sessionId)

        FileInputStream(apkFile).use { input ->
            session.openWrite("apk", 0, apkFile.length()).use { out ->
                input.copyTo(out)
                session.fsync(out)
            }
        }

        val intent = Intent()
        val sender = PendingIntent.getBroadcast(
            appContext,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        ).intentSender

        session.commit(sender)
        session.close()

        // 🔥 REMOVE APK APÓS INSTALAÇÃO SILENCIOSA
        apkFile.delete()
    }

    private fun isRootAvailable(): Boolean {
        val paths = listOf("/system/xbin/su", "/system/bin/su", "/sbin/su")
        return paths.any { File(it).exists() }
    }

    // INSTALAÇÃO COMO ROOT
    private fun silentInstallRoot(apkFile: File): Boolean {
        if (!apkFile.exists()) return false

        return try {
            val process = Runtime.getRuntime()
                .exec(arrayOf("su", "0", "pm", "install", "-r", apkFile.absolutePath))

            val exitCode = process.waitFor()

            if (exitCode == 0) {
                apkFile.delete() // 🔥 REMOVE APÓS INSTALAÇÃO ROOT
                true
            } else {
                showInstallPrompt(apkFile)
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showInstallPrompt(apkFile)
            false
        }
    }

    // INSTALAÇÃO NORMAL (com prompt)
    private fun showInstallPrompt(apkFile: File) {
        if (!apkFile.exists()) return

        // 🔥 APAGA TODOS OS APKS DA PASTA DOWNLOAD ANTES DE ABRIR O INSTALADOR
       // deleteAnyApkInDownloadFolder()

        viewModelScope.launch(Dispatchers.Main) {

            if (!appContext.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData("package:${appContext.packageName}".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(intent)
                return@launch
            }

            val uri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.provider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            appContext.startActivity(intent)
        }
    }
}
