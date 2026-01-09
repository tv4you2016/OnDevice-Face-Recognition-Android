package com.ioline.ithink.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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


                        //checkForUpdates(context,false)
                        scheduleDailyUpdateCheck(context)



                        MordomusRoot()




                    }
                }
            }
        }

        permissionManager.requestAll()
    }


    fun checkForUpdates(context: Context, force: Boolean): Boolean {
        val currentVersionCode = packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()

        var isUpdateAvailable = false  // Variável para armazenar o resultado

        lifecycleScope.launch {
            // Obtendo o resultado da verificação de atualização
            val result = UpdateChecker(this@MainActivity).checkForUpdate(currentVersionCode)

            // Usando 'onSuccess' e 'onFailure' para tratar os diferentes casos
            result.onSuccess { updateResult ->
                when (updateResult) {
                    is UpdateResult.UpdateAvailable -> {
                        isUpdateAvailable = true  // Definindo como true se houver uma atualização disponível

                        if (force) {
                            // Inicia a Activity de atualização forçada
                            val intent = Intent(context, UpdaterActivity::class.java).apply {
                                putExtra("apkUrl", updateResult.url)
                                putExtra("latestVersionName", updateResult.version)
                            }
                            context.startActivity(intent)
                        }
                    }
                    is UpdateResult.AlreadyUpToDate -> {
                        Log.d("UpdateChecker", "App já está atualizada.")
                    }
                    else -> {}
                }
            }.onFailure { exception ->
                Log.e("UpdateChecker", "Erro ao verificar atualização: ${exception.message}")
            }
        }

        return isUpdateAvailable  // Retorna o valor booleano
    }


    override fun onResume() {
        super.onResume()
        // Código aqui


        FaceDetectionService.stop(this@MainActivity)
        this@MainActivity.stopService(Intent(this@MainActivity, CameraService::class.java))
        this@MainActivity.stopService(Intent(this@MainActivity, ProximityService::class.java))

    }
}


// falta no restore da app quando esta minimizada

