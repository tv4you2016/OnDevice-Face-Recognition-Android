package com.ioline.ai

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.ioline.ithink.ai.MyDeviceAdminReceiver

class PermissionManager(
    private val activity: ComponentActivity,
    private val onAllPermissionsGranted: () -> Unit // callback para quando tudo estiver pronto
) {

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    // Launcher para permissões normais
    private val permissionsLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val allGranted = results.values.all { it }
            results.forEach { (perm, granted) ->
                Log.d("PermissionManager", "Permissão: $perm -> $granted")
            }

            if (!allGranted) {
                val toAsk = results.filterValues { !it }.keys.toTypedArray()
                if (toAsk.isNotEmpty()) {
                    Log.d("PermissionManager", "❌ Faltam permissões: ${toAsk.joinToString()} — pedindo novamente...")
                    Handler(Looper.getMainLooper()).postDelayed({
                        permissionsLauncher.launch(toAsk)
                    }, 1500)
                }
            } else {
                checkOverlayPermission()
            }
        }

    // Launcher para overlay
    private val overlayLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (!Settings.canDrawOverlays(activity)) {
                Toast.makeText(activity, "Permissão de sobreposição necessária!", Toast.LENGTH_SHORT).show()
                checkOverlayPermission()
            } else {
                requestIgnoreBatteryOptimizations()
            }
        }

    // Launcher para ignorar otimização de bateria
    private val ignoreBatteryLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(activity.packageName)) {
                Toast.makeText(activity, "Permissão de ignorar bateria necessária!", Toast.LENGTH_SHORT).show()
                requestIgnoreBatteryOptimizations()
            } else {
                requestDeviceAdmin()
            }
        }

    // Launcher para Device Admin
    private val deviceAdminLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            logPermissionStates()
            // Tudo pronto, chama callback para carregar o layout
            onAllPermissionsGranted()
        }

    // Chama todo o fluxo de permissões
    fun requestAll() {
        if (!hasAllPermissions()) {
            permissionsLauncher.launch(requiredPermissions)
        } else {
            checkOverlayPermission()
        }
    }

    private fun hasAllPermissions(): Boolean =
        requiredPermissions.all {
            ContextCompat.checkSelfPermission(activity, it) ==
                    PackageManager.PERMISSION_GRANTED
        }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(activity)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            overlayLauncher.launch(intent)
        } else {
            requestIgnoreBatteryOptimizations()
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(activity.packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = "package:${activity.packageName}".toUri()
            }
            ignoreBatteryLauncher.launch(intent)
        } else {
            requestDeviceAdmin()
        }
    }

    private fun requestDeviceAdmin() {
        val componentName = ComponentName(activity, MyDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Permita para desligar o ecrã")
        }
        deviceAdminLauncher.launch(intent)
    }

    fun logPermissionStates() {
        val hasCamera = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        val overlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(activity)
        val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
        val battery = pm.isIgnoringBatteryOptimizations(activity.packageName)

        Log.d("PermissionManager", "📷 CAMERA: $hasCamera")
        Log.d("PermissionManager", "🪟 Overlay: $overlay")
        Log.d("PermissionManager", "🔋 Ignora bateria: $battery")
    }
}
