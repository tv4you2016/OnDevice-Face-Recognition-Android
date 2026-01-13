package com.ioline.ithink.ai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ioline.ithink.ai.layout.MainLayout
import com.ioline.ithink.ai.settingsdatastore.AppSettings
import com.ioline.ithink.ai.settingsdatastore.SettingsDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@Composable
fun MordomusRoot() {
    val context = LocalContext.current
    val settingsStore = remember { SettingsDataStore(context) }

    // 1) Carregar AppSettings UMA vez
    val initialSettings by produceState<AppSettings?>(initialValue = null) {
        value = settingsStore.settingsFlow.first()
    }

    // 2) Garantir tempo mínimo de splash (5 segundos)
    var minTimePassed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2_000)          // 5000 ms = 5 segundos
        minTimePassed = true
    }

    val shouldShowSplash = !minTimePassed || initialSettings == null

    if (shouldShowSplash) {
        SplashScreen()
    } else {
        MainLayout(
            settingsStore = settingsStore,
            initialSettings = initialSettings!!
        )
    }
}
