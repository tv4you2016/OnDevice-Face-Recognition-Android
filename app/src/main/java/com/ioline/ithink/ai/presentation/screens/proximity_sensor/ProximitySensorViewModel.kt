package com.ioline.ithink.ai.presentation.screens.proximity_sensor

import android.R
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Single


@KoinViewModel
class ProximitySensorViewModel(application: Application) : AndroidViewModel(application) {

    val _sensorReadingStatus = MutableStateFlow(false)

    private val _sensorReading = MutableStateFlow(0f)
    val sensorReading: StateFlow<Float> = _sensorReading

    private val _sensitivity = MutableStateFlow(3f)
    val sensitivity: StateFlow<Float> = _sensitivity

    val sensorReadingStatus: StateFlow<Boolean> = _sensorReadingStatus

    fun updateSensitivity(value: Float) {
        _sensitivity.value = value
    }

    // Chamado pelo receiver no Composable
    fun updateSensor(value: Float) {
        _sensorReading.value = value
    }

    fun updateSensorStatus(value: Boolean) {
        _sensorReadingStatus.value = value
    }
}
