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
fun CameraSensor(
    viewModel: CameraSensorViewModel = koinViewModel()
) {
    val context = LocalContext.current

    // State vindo do ViewModel (View Model – modelo de vista)
    val sensorStatus by viewModel.sensorCameraStatus.collectAsState()
    val sliderValue by viewModel.sensitivity.collectAsState()

    // Constantes (não mudam)
    val min = 100f
    val max = 10_000f
    val invertedValue = max - sliderValue

    // 🔄 BroadcastReceiver registado só enquanto este composable está na tela
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == "CAMERA_SENSOR_UPDATE") {
                    val status = intent.getStringExtra("proximityLevel") ?: "Desconhecido"
                    viewModel.updateSensorStatus(status)
                }
            }
        }

        val filter = IntentFilter("CAMERA_SENSOR_UPDATE")
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // Ícone derivado do estado (state – estado)
    val isNear = sensorStatus == context.getString(R.string.proximo)
    val icon: ImageVector = if (isNear) {
        ImageVector.vectorResource(id = R.drawable.ic_satisfied)
    } else {
        ImageVector.vectorResource(id = R.drawable.ic_dissatisfied)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sensitivity",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        Slider(
            value = invertedValue,
            onValueChange = { viewModel.updateSensitivity(max - it) },
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

        Icon(
            imageVector = icon,
            contentDescription = sensorStatus,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(100.dp)
                .padding(top = 2.dp)
        )
    }

    // Continua comentado – evita side-effects pesados na UI:
    // AppUtils.stopLoading(LocalContext.current,"CameraSensor")
}
