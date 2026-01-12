package com.ioline.ithink.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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

//C:\Users\IOLine\Documents\GitHub\OnDevice-Face-Recognition-Android\app\build\outputs\apk\debug
class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        // Código aqui

        if (!AppUtils.isAddUserFlowActive) {

            FaceDetectionService.stop(this@MainActivity)
            this@MainActivity.stopService(Intent(this@MainActivity, CameraService::class.java))
            this@MainActivity.stopService(Intent(this@MainActivity, ProximityService::class.java))
        }
    }
}


// colocar um texto que informa o user de que quanto mais photos melhor

