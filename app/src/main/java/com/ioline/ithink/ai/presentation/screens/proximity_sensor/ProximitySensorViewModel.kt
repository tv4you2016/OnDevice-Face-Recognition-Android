package com.ioline.ithink.ai.presentation.screens.proximity_sensor

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

    private val _sensorReading = MutableStateFlow(0f)
    val sensorReading: StateFlow<Float> = _sensorReading

    private val _sensitivity = MutableStateFlow(50f)
    val sensitivity: StateFlow<Float> = _sensitivity

    fun updateSensitivity(value: Float) {
        _sensitivity.value = value
    }

    private val proximityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val distance = intent?.getFloatExtra("distance", 0f) ?: 0f
            viewModelScope.launch {
                _sensorReading.value = distance
            }
        }
    }

    init {
        // Registra o receiver
        val filter = IntentFilter("PROXIMITY_SENSOR_UPDATE")
        ContextCompat.registerReceiver(
            application,
            proximityReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(proximityReceiver)
    }
}
