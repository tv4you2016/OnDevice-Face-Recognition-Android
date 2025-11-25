package com.ioline.ithink.ai
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ioline.ai.PermissionManager
import com.ioline.ithink.ai.AppUtils.GlobalLoadingController
import com.ioline.ithink.ai.layout.MainLayout
import com.ioline.ithink.ai.presentation.components.AppLoading
import com.ioline.ithink.ai.settingsdatastore.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager


    @Composable
    fun LoadingExample() {
        val isLoading by AppUtils.GlobalLoadingController()

        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                AppUtils.LoadingScreen()
            } else {
                MainLayout()
            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WakeLock().unlockScreen(this@MainActivity)


        val openiThinkExtra = intent?.getBooleanExtra("openiThink", false) ?: false

        val settingsStore = SettingsDataStore(this)

        lifecycleScope.launch {
            if (openiThinkExtra) {
                // Se true, abre a app
                //AppUtils.openTargetAppSafe(this@MainActivity, "app.ioline.ithink")
            } else {
                // Se false, grava no DataStore para futura referência
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

                // Estado global controlado por broadcasts


                NavHost(
                    navController = navController,
                    startDestination = "main_layout"
                ) {
                    composable("main_layout") {
                        LoadingExample()
                        /*
                        if (globalLoading) {
                            AppUtils.LoadingScreen()

                        } else {
                            MainLayout()
                        }

                         */
                    }
                }
            }

        }

        permissionManager.requestAll()

    }
}



