package com.ioline.ithink.ai.presentation.screens.proximity_sensor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioline.ithink.ai.R
import com.ioline.ithink.ai.data.PersonRecord
import com.ioline.ithink.ai.presentation.theme.FaceNetAndroidTheme
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProximitySensor(
    onAddFaceClick: () -> Unit,
    viewModel: ProximitySensorViewModel = koinViewModel()
) {
    val sliderValue by viewModel.sensitivity.collectAsState()
    val sensorValue by viewModel.sensorReading.collectAsState()

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

                Slider(
                    value = sliderValue,
                    onValueChange = { viewModel.updateSensitivity(it) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = colorResource(id = R.color.md_orange),
                        inactiveTrackColor = Color.Gray,
                        thumbColor = colorResource(id = R.color.md_orange),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                )

                // Mostra o valor ajustado
                Text(
                    text = "Sensitivity: ${sliderValue.toInt()}%",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Mostra o valor real do sensor 🔥🔥🔥
                Text(
                    text = "Sensor Reading: ${sensorValue} cm",
                    color = colorResource(id = R.color.md_orange),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
        }
    }
}


