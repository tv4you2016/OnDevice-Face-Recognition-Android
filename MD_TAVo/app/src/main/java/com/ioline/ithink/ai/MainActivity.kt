package com.ioline.ithink.ai

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ioline.ai.PermissionManager
import com.ioline.ithink.ai.UpdateChecker.scheduleDailyUpdateCheck
import com.ioline.ithink.ai.presentation.components.CameraService
import com.ioline.ithink.ai.presentation.components.FaceDetectionService
import com.ioline.ithink.ai.presentation.components.ProximityService
import com.ioline.ithink.ai.settingsdatastore.SettingsDataStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.core.content.edit

class MainActivity : ComponentActivity() {


    private lateinit var permissionManager: PermissionManager
    private var inactivityJob: Job? = null
    private var bootFlowHandled = false
    private var hasResetOpenApk = false

    private val settingsStore by lazy {
        SettingsDataStore(this)
    }

    private val prefs by lazy {
        getSharedPreferences("boot_flags", Context.MODE_PRIVATE)
    }
    private fun lockScreen() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val component = ComponentName(this, MyDeviceAdminReceiver::class.java)

        if (dpm.isAdminActive(component)) {
            Log.d("IOLine", "🔒 A desligar ecrã por inatividade")
            dpm.lockNow()
        } else {
            Log.e("IOLine", "Device Admin NÃO ativo")
        }
    }

    private fun startInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = lifecycleScope.launch {
            delay(30_000)
            lockScreen()
        }
    }

    private fun resetInactivityTimer() {
        startInactivityTimer()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        resetInactivityTimer()
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        enableEdgeToEdge()
        WakeLock().unlockScreen(this@MainActivity)

        val pendingBootOpen = prefs.getBoolean("pending_open_ithink_after_boot", false)

        Log.d("MainActivity", "onCreate | pendingBootOpen=$pendingBootOpen")

        permissionManager = PermissionManager(this) {
            setContent {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "main_layout"
                ) {
                    composable("main_layout") {
                        val context = LocalContext.current
                        scheduleDailyUpdateCheck(context)
                        MordomusRoot()
                    }
                }
            }
        }

        permissionManager.requestAll()

        if (pendingBootOpen) {
            handleBootFlow(settingsStore, prefs)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val pendingBootOpen = prefs.getBoolean("pending_open_ithink_after_boot", false)
        Log.d("MainActivity", "onNewIntent | pendingBootOpen=$pendingBootOpen")

        if (pendingBootOpen && !bootFlowHandled) {  // Só executa se não foi tratado
            handleBootFlow(settingsStore, prefs)
        }
    }

    private fun handleBootFlow(
        settingsStore: SettingsDataStore,
        prefs: android.content.SharedPreferences
    ) {
        if (bootFlowHandled) {
            Log.d("MainActivity", "Boot flow já tratado, ignorado")
            return
        }

        bootFlowHandled = true

        lifecycleScope.launch {
            try {
                val currentSettings = settingsStore.settingsFlow.first()
                val selectedService = currentSettings.detectionType.toString()

                if (selectedService.equals("none", ignoreCase = true)) {
                    Log.d("MainActivity", "Serviço = none, não abre app.ioline.ithink")
                    prefs.edit { putBoolean("pending_open_ithink_after_boot", false) }
                    bootFlowHandled = false  // Reset para permitir tentativas futuras
                    return@launch
                }

                // Salva openApk = true
                settingsStore.saveSettings(
                    currentSettings.copy(
                        OpeniThink = currentSettings.OpeniThink.copy(openApk = true)
                    )
                )
                Log.d("MainActivity", "OpeniThink.openApk salvo como true")

                delay(1500)

                val launchIntent = packageManager.getLaunchIntentForPackage("app.ioline.ithink")

                if (launchIntent != null) {
                    prefs.edit { putBoolean("pending_open_ithink_after_boot", false) }
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)  // Mudado de RESET_TASK_IF_NEEDED
                    startActivity(launchIntent)
                    Log.d("MainActivity", "app.ioline.ithink aberta após init da app")
                    // NÃO chame finish() aqui!
                } else {
                    Log.e("MainActivity", "app.ioline.ithink não encontrada")
                    prefs.edit { putBoolean("pending_open_ithink_after_boot", false) }
                    bootFlowHandled = false
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Erro no fluxo de boot", e)
                prefs.edit { putBoolean("pending_open_ithink_after_boot", false) }
                bootFlowHandled = false
            }
        }
    }

    @OptIn(ExperimentalGetImage::class)
    override fun onResume() {
        super.onResume()

        // Só reseta se NÃO estamos em fluxo de boot E o boot já foi concluído
        val pendingBootOpen = prefs.getBoolean("pending_open_ithink_after_boot", false)
        val isBootFlowComplete = bootFlowHandled && !pendingBootOpen

        if (isBootFlowComplete && !hasResetOpenApk) {
            hasResetOpenApk = true
            lifecycleScope.launch {
                val currentSettings = settingsStore.settingsFlow.first()
                if (currentSettings.OpeniThink.openApk) {
                    settingsStore.saveSettings(
                        currentSettings.copy(
                            OpeniThink = currentSettings.OpeniThink.copy(openApk = false)
                        )
                    )
                    Log.d("MainActivity", "OpeniThink.openApk resetado para false após boot completo")
                }
            }
        }

        requestDeviceAdmin()

        if (!AppUtils.isAddUserFlowActive) {
            FaceDetectionService.stop(this@MainActivity)
            stopService(Intent(this@MainActivity, CameraService::class.java))
            stopService(Intent(this@MainActivity, ProximityService::class.java))
        }

        startInactivityTimer()
    }

    override fun onPause() {
        super.onPause()
    }

    private fun requestDeviceAdmin() {
        val component = ComponentName(this, MyDeviceAdminReceiver::class.java)
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        if (!dpm.isAdminActive(component)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Necessário para desligar o ecrã automaticamente"
                )
            }
            startActivity(intent)
        }
    }
}