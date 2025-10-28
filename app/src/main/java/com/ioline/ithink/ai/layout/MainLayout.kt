package com.ioline.ithink.ai.layout



import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioline.ithink.ai.ProximityService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                //.padding(16.dp)
        ) {
            val context = LocalContext.current  // ✅ obtém o contexto Android real

            var proximityEnabled by remember { mutableStateOf(true) }
            var facialEnabled by remember { mutableStateOf(false) }
            var cameraEnabled by remember { mutableStateOf(false) }


            // General Section
            SectionTitle("General")

            var backgroundEnabled by remember { mutableStateOf(true) }
            SettingItem(
                title = "Background Access",
                description = "Excluded from battery optimizations and will always run in the background",
                enabled = backgroundEnabled,
                onToggleChange = { backgroundEnabled = it },
                toggleVisible = true,
                toggleEnabled = false
            )

            SettingItem(
                title = "Start on Boot",
                description = "Automatically start at device boot",
                enabled = true,
                onToggleChange = {},
                toggleVisible = true,
                toggleEnabled = false
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Motion Detection Section
            SectionTitle("Detection:")


            SettingItem(
                title = "Proximity",
                description = null,
                enabled = proximityEnabled,
                onToggleChange = { isChecked ->
                    // Atualiza o estado
                    proximityEnabled = isChecked
                    facialEnabled = false
                    cameraEnabled =  false

                    // Código que queres executar quando o switch mudar
                    if (isChecked) {
                        println("Proximity Detect Enabled")
                        // ou chama uma função, inicia serviço, etc.
                        val intent = Intent(context, ProximityService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }

                    } else {
                        println("Proximity Detect Disabled")
                        context.stopService(Intent(context, ProximityService::class.java))

                    }
                },
                toggleVisible = true,
                toggleEnabled = true
            )


            SettingItem(
                title = "Facial Detect",
                description = null,
                enabled = facialEnabled,
                onToggleChange = { isChecked ->
                    // Atualiza o estado
                    facialEnabled = isChecked
                    proximityEnabled = false
                    cameraEnabled =  false

                    // Código que queres executar quando o switch mudar
                    if (isChecked) {
                        println("Facial Detect Enabled")

                        context.stopService(Intent(context, ProximityService::class.java))

                    } else {
                        println("Facial Detect Disabled")


                    }
                },
                toggleVisible = true,
                toggleEnabled = true
            )

            SettingItem(
                title = "Camera movement",
                description = null,
                enabled = cameraEnabled,
                onToggleChange = { isChecked ->
                    // Atualiza o estado
                    cameraEnabled = isChecked
                    proximityEnabled = false
                    facialEnabled =  false

                    // Código que queres executar quando o switch mudar
                    if (isChecked) {
                        println("Camera Detect Enabled")

                        context.stopService(Intent(context, ProximityService::class.java))

                    } else {
                        println("Camera Detect Disabled")


                    }
                },
                toggleVisible = true,
                toggleEnabled = true
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color(0xFF1E88E5), // azul parecido
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SettingItem(
    title: String,
    description: String?,
    enabled: Boolean,
    onToggleChange: ((Boolean) -> Unit)?,
    toggleVisible: Boolean,
    toggleEnabled: Boolean = true,
    borderColor: Color = Color(0xFF1E88E5), // azul
    borderWidth: Dp = 1.dp,
    cornerRadius: Dp = 12.dp,
    shadowElevation: Dp = 4.dp


) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(cornerRadius)
            )
            .background(Color.White, RoundedCornerShape(cornerRadius))
            .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))
            .clickable {
                // opcional: se quiser que ao clicar em qualquer parte do item altere o toggle
                onToggleChange?.invoke(!enabled)
            }
            .padding(horizontal = 16.dp, vertical = 12.dp) // padding interno
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically

        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                 maxLines = 1

            )

            if (toggleVisible && onToggleChange != null) {
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggleChange,
                    enabled = toggleEnabled
                )
            } else if (toggleVisible) {
                // switch disabled but visible (greyed out)
                Switch(
                    checked = enabled,
                    onCheckedChange = null,
                    enabled = toggleEnabled
                )
            }

        }

        /*
        if (!description.isNullOrEmpty()) {
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

         */
    }
}

