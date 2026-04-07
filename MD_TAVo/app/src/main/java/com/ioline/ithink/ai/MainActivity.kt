package com.ioline.ithink.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
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
import com.ioline.ithink.ai.UpdateChecker.UpdateChecker
import com.ioline.ithink.ai.UpdateChecker.UpdateResult
import com.ioline.ithink.ai.UpdateChecker.UpdaterActivity
import com.ioline.ithink.ai.UpdateChecker.scheduleDailyUpdateCheck
import com.ioline.ithink.ai.presentation.components.CameraService
import com.ioline.ithink.ai.presentation.components.FaceDetectionService
import com.ioline.ithink.ai.presentation.components.ProximityService
import com.ioline.ithink.ai.settingsdatastore.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.os.Build
import android.view.WindowManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay


//C:\Users\IOLine\Documents\GitHub\OnDevice-Face-Recognition-Android\app\build\outputs\apk\debug
class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager
    private var inactivityJob: Job? = null


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

        val openiThinkExtra = intent?.getBooleanExtra("openiThink", false) ?: false
        val settingsStore = SettingsDataStore(this)

        // Atualiza OpeniThink no DataStore conforme o extra
        lifecycleScope.launch {
            if (!openiThinkExtra) {
                val currentSettings = settingsStore.settingsFlow.first()
                settingsStore.saveSettings(
                    currentSettings.copy(
                        OpeniThink = currentSettings.OpeniThink.copy(openApk = false)
                    )
                )
            }
        }

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
    }




    @OptIn(ExperimentalGetImage::class)
    override fun onResume() {
        super.onResume()
        requestDeviceAdmin()
        // Código aqui


        if (!AppUtils.isAddUserFlowActive) {

            FaceDetectionService.stop(this@MainActivity)
            this@MainActivity.stopService(Intent(this@MainActivity, CameraService::class.java))
            this@MainActivity.stopService(Intent(this@MainActivity, ProximityService::class.java))
        }
        startInactivityTimer()
    }

    override fun onPause() {
        super.onPause()
        //inactivityJob?.cancel()

    }



    private fun requestDeviceAdmin() {
        val component = ComponentName(this, MyDeviceAdminReceiver::class.java)
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        if (!dpm.isAdminActive(component)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Necessário para desligar o ecrã automaticamente")
            }
            startActivity(intent)
        }
    }
}



