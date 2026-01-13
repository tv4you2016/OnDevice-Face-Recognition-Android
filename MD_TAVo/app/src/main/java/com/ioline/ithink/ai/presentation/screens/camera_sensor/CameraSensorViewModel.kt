package com.ioline.ithink.ai.presentation.screens.camera_sensor

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ioline.ithink.ai.settingsdatastore.SettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class CameraSensorViewModel(
    application: Application,
    private val settingsDataStore: SettingsDataStore
) : AndroidViewModel(application) {

    private val _sensorCameraStatus = MutableStateFlow("Distante")
    val sensorCameraStatus: StateFlow<String> = _sensorCameraStatus.asStateFlow()

    private val _sensitivity = MutableStateFlow(1000f) // valor inicial do slider
    val sensitivity: StateFlow<Float> = _sensitivity.asStateFlow()

    init {
        // Inicializa o slider com o valor salvo no DataStore
        viewModelScope.launch {
            val currentSettings = settingsDataStore.settingsFlow.first()
            _sensitivity.value = currentSettings.camera.sensitivity * 10000f
            Log.d("CameraSensorVM", "Slider initialized with ${_sensitivity.value}")
        }
    }



    fun updateSensitivity(value: Float) {
        _sensitivity.value = value

        // Atualiza o CameraService via broadcast
        val intent = Intent("UPDATE_SENSOR_REFERENCE")
        intent.putExtra("sensorReference", value)
        getApplication<Application>().sendBroadcast(intent)

        // Atualiza apenas o DataStore
        viewModelScope.launch {
            val currentSettings = settingsDataStore.settingsFlow.first()
            val newSettings = currentSettings.copy(
                camera = currentSettings.camera.copy(
                    sensitivity = value / 10000f // desfaz a escala
                )
            )
            settingsDataStore.saveSettings(newSettings)
            Log.d("CameraSensorVM", "Saved detectionThreshold: ${newSettings.camera.sensitivity}")
        }
    }

    fun updateSensorStatus(value: String) {
        _sensorCameraStatus.value = value
    }
}
