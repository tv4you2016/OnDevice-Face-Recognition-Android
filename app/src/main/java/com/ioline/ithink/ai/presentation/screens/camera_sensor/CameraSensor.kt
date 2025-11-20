package com.ioline.ithink.ai.presentation.screens.camera_sensor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.compose.foundation.layout.*
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
import com.ioline.ithink.ai.R
import com.ioline.ithink.ai.layout.getProximitySensorInfo
import com.ioline.ithink.ai.presentation.screens.proximity_sensor.ProximitySensorViewModel
import com.ioline.ithink.ai.presentation.theme.FaceNetAndroidTheme
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSensor(
    viewModel: CameraSensorViewModel = koinViewModel()
) {
    val sensorStatus by viewModel.sensorCameraStatus.collectAsState()
    val sliderValue by viewModel.sensitivity.collectAsState()

    val min = 100f
    val max = 2500f
    val invertedValue = max - sliderValue

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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

        Text(
            text = sensorStatus,
            color = colorResource(id = R.color.md_orange),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 20.dp)
        )
    }
}
