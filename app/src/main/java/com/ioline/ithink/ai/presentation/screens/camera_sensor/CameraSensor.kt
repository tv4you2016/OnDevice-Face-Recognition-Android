package com.ioline.ithink.ai.presentation.screens.camera_sensor


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioline.ithink.ai.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.registerReceiver
import com.ioline.ithink.ai.AppUtils

import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSensor( viewModel: CameraSensorViewModel = koinViewModel()
) {
    val sensorStatus by viewModel.sensorCameraStatus.collectAsState()
    val sliderValue by viewModel.sensitivity.collectAsState()

    val min = 100f
    val max = 10000f
    val invertedValue = max - sliderValue

    val context = LocalContext.current

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "CAMERA_SENSOR_UPDATE") {
                    val status = intent.getStringExtra("proximityLevel") ?: "Desconhecido"
                    viewModel.updateSensorStatus(status)
                }
            }
        }

        val filter = IntentFilter("CAMERA_SENSOR_UPDATE")
        registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)

        onDispose { context.unregisterReceiver(receiver) }
    }



    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sensitivity",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column {
            //Text(text = "Distância: ${sliderValue.toInt()}", color = Color.White)
            Slider(
                value = invertedValue,
                onValueChange = { viewModel.updateSensitivity(max - it) }, // converte de volta
                valueRange = min..max,
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
        val context = LocalContext.current


        val icon: ImageVector = if (sensorStatus == context.getString(R.string.próximo)) {
            ImageVector.vectorResource(id = R.drawable.ic_satisfied)
        } else {
            ImageVector.vectorResource(id = R.drawable.ic_dissatisfied)
        }

        Icon(
            imageVector = icon,
            contentDescription = sensorStatus,
            tint = Color.Unspecified, // mantém as cores originais
            modifier = Modifier
                .size(54.dp)
                .padding(top = 10.dp)
        )

    }

    //AppUtils.stopLoading(LocalContext.current,"CameraSensor")
}
