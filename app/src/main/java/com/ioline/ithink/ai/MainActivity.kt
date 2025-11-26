package com.ioline.ithink.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ioline.ai.PermissionManager

import com.ioline.ithink.ai.settingsdatastore.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
                        // Agora a navegação inicial entra sempre pelo root
                        MordomusRoot()
                    }
                }
            }
        }

        permissionManager.requestAll()
    }
}
