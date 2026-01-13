package com.ioline.ithink.ai.presentation.screens.proximity_sensor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioline.ithink.ai.AppUtils
import com.ioline.ithink.ai.R

import com.ioline.ithink.ai.presentation.theme.FaceNetAndroidTheme
import org.koin.androidx.compose.koinViewModel





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProximitySensor( onLoaded: () -> Unit,
    viewModel: ProximitySensorViewModel = koinViewModel()
) {
    val context = LocalContext.current

    val sensorStatus by viewModel.sensorReadingStatus.collectAsState()

    var hasLoaded by remember { mutableStateOf(false) }


    // RECEBE O BROADCAST CORRETAMENTE
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val distance = intent?.getFloatExtra("distance", 0f) ?: 0f
                val isNear = intent?.getBooleanExtra("Near", false) ?: false
                viewModel.updateSensor(distance)
                viewModel.updateSensorStatus(isNear)
                Log.d("ProximityService", "EVENT RECEIVED in Compose! $distance")
            }
        }

        val filter = IntentFilter("PROXIMITY_SENSOR_UPDATE")

        // ANDROID 14 FIX 🚨
        context.registerReceiver(
            receiver,
            filter,
            Context.RECEIVER_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }



    FaceNetAndroidTheme {
        Scaffold(
            containerColor = Color.Black,
            modifier = Modifier.fillMaxSize(),
        ) { innerPadding ->

            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sensitivity",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val icon = if (sensorStatus) {
                    Icons.Default.Visibility
                } else {
                    Icons.Default.VisibilityOff
                }

                Icon(
                    imageVector = icon,
                    contentDescription = "Sensor Status",
                    tint = colorResource(id = R.color.white),
                    modifier = Modifier
                        .size(48.dp)
                        .padding(top = 20.dp)
                )
            }
        }
    }

    if (!hasLoaded) {
        hasLoaded = true
        onLoaded()   // <-- só agora diz ao layout para remover o loading!
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProximitySensor_old(
    viewModel: ProximitySensorViewModel = koinViewModel()
) {
    val context = LocalContext.current

    val sensorValue by viewModel.sensorReading.collectAsState()
    val sliderValue by viewModel.sensitivity.collectAsState()



    val (proximityName, proximityVendor, maxRange) = AppUtils.getProximitySensorInfo(context)
    var safeMaxRange = maxRange ?: 5f

    if (proximityName == "Proximity sensor" && proximityVendor == "The Android Open Source Project") {
        safeMaxRange = 60000f
    }
    // RECEBE O BROADCAST CORRETAMENTE
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val distance = intent?.getFloatExtra("distance", 0f) ?: 0f
                viewModel.updateSensor(distance)
                Log.d("ProximityService", "EVENT RECEIVED in Compose! $distance")
            }
        }

        val filter = IntentFilter("PROXIMITY_SENSOR_UPDATE")

        // ANDROID 14 FIX 🚨
        context.registerReceiver(
            receiver,
            filter,
            Context.RECEIVER_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }



    FaceNetAndroidTheme {
        Scaffold(
            containerColor = Color.Black,
            modifier = Modifier.fillMaxSize(),
        ) { innerPadding ->

            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Sensor Sensitivity",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column {
                    Text(text = "Distância: ${sliderValue}")
                    Slider(
                        value = sliderValue,
                        onValueChange = { viewModel.updateSensitivity(it) },
                        valueRange = 0f..safeMaxRange,
                        colors = SliderDefaults.colors(
                            activeTrackColor = colorResource(id = R.color.md_orange),
                            inactiveTrackColor = Color.Gray,
                            thumbColor = colorResource(id = R.color.md_orange),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                    )
                }



                Text(
                    text = "Sensitivity: ${sliderValue.toInt()}",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = "Sensor Reading: ${sensorValue.toInt()}",
                    color = colorResource(id = R.color.md_orange),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
        }
    }
}






