package com.ioline.ithink.ai.presentation.screens.camera_sensor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioline.ithink.ai.R
import com.ioline.ithink.ai.presentation.theme.FaceNetAndroidTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSensor() {
    FaceNetAndroidTheme {
        Scaffold(
            containerColor = Color.Black,
            modifier = Modifier.fillMaxSize(),
        ) { innerPadding ->

            var sliderValue by remember { mutableFloatStateOf(85f) }

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

                // Slider com bola redonda personalizada 🎯
                Slider(

                    value = sliderValue,
                    onValueChange = { newValue -> sliderValue = newValue },
                    valueRange = 0f..100f,
                    steps = 0, // Remove os passos, linha contínua
                    colors = SliderDefaults.colors(
                        activeTrackColor = colorResource(id = R.color.md_orange),
                        inactiveTrackColor = Color.Gray,
                        thumbColor = colorResource(id = R.color.md_orange),  // Colorindo a bolinha de laranja
                        activeTickColor = colorResource(id = R.color.md_orange),
                        inactiveTickColor = Color.Gray,

                        disabledThumbColor= colorResource(id = R.color.md_orange),
                        disabledActiveTrackColor= colorResource(id = R.color.md_orange),
                        disabledActiveTickColor= colorResource(id = R.color.md_orange),
                        disabledInactiveTrackColor= colorResource(id = R.color.md_orange),
                        disabledInactiveTickColor= colorResource(id = R.color.md_orange),
                    ),


                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp) // aqui você aumenta a altura do slider
                )




                Text(
                    text = "Current value: ${sliderValue.toInt()}%", // 🔹 mostra como % inteiro
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
