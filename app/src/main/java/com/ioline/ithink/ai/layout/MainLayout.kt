package com.ioline.ithink.ai.layout



import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.ioline.ithink.ai.ProximityService
import com.ioline.ithink.ai.presentation.components.AppProgressDialog
import com.ioline.ithink.ai.presentation.components.FaceDetectionService
import com.ioline.ithink.ai.presentation.components.setProgressDialogText
import com.ioline.ithink.ai.presentation.components.showProgressDialog
import androidx.compose.ui.res.painterResource
import com.ioline.aicamera.utils.AppUtils.openTargetApp
import com.ioline.aicamera.utils.AppUtils.openlockNowApp
import com.ioline.ithink.ai.R
import com.ioline.ithink.ai.presentation.screens.face_list.FaceListScreen


// Definindo as opções disponíveis
enum class Option {
    FacialDetection,
    ProximityDetection,
    CameraDetection
}


@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(navController: NavController) {

    var selectedOption by remember { mutableStateOf<Option?>(null) }
    var expandedOption by remember { mutableStateOf<Option?>(null) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MordomusTABMNG") }

            )
        },
        floatingActionButton = {  // ✅ mover para o Scaffold
            val context = LocalContext.current  // ✅ obtém o contexto Android real

            FloatingActionButton(
                onClick = {
                   // openlockNowApp(context)
                    openTargetApp(context,false)

                }, // exemplo de ação
                containerColor = Color.White, // Azul, por exemplo

            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mordomus_ithink),
                    contentDescription = "Add a new face",
                    tint = Color.Unspecified, // mantém cor original
                    modifier = Modifier.size(50.dp), // ajusta o tamanho



                )
            }
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

                        FaceDetectionService.stop(context)


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
                toggleEnabled = true,
                option = Option.ProximityDetection,  // <- passa a opção
                expandedOption = expandedOption,
                onExpand = { option ->
                    expandedOption = if (expandedOption == option) null else option
                }
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

                        val intent = Intent(context, FaceDetectionService::class.java)
                        ContextCompat.startForegroundService(context, intent)

                        //navController.navigate("face-list") // ou outra tela
                    } else {
                        println("Facial Detect Disabled")


                    }
                },
                toggleVisible = true,
                toggleEnabled = true,
                option = Option.FacialDetection,  // <- passa a opção
                expandedOption = expandedOption,
                onExpand = { option ->
                    expandedOption = if (expandedOption == option) null else option
                }
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

                        FaceDetectionService.stop(context)
                        context.stopService(Intent(context, ProximityService::class.java))

                    } else {
                        println("Camera Detect Disabled")


                    }
                },
                toggleVisible = true,
                toggleEnabled = true,
                option = Option.CameraDetection,  // <- passa a opção
                expandedOption = expandedOption,
                onExpand = { option ->
                    expandedOption = if (expandedOption == option) null else option
                }
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
    option: Option, // Adiciona a opção que esse card representa
    toggleVisible: Boolean,
    toggleEnabled: Boolean = true,
    borderColor: Color = Color(0xFF1E88E5), // azul
    borderWidth: Dp = 1.dp,
    cornerRadius: Dp = 12.dp,
    shadowElevation: Dp = 4.dp,
    expandedOption: Option?, // 👈 controla se está expandido
    onExpand: (Option) -> Unit // 👈 callback de clique

) {
    val isExpanded = expandedOption == option


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(cornerRadius)
            )
            .background(Color.White, RoundedCornerShape(cornerRadius))
            .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))
            .clickable { onExpand(option) } // 👈 alterna expand/colapsar
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

        // 🔹 Conteúdo expandido
        if (isExpanded) {
            when (option) {
                Option.FacialDetection -> {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        // 👇 Aqui “injeta” o conteúdo
                        FaceListScreen(
                            onNavigateBack = { onExpand(option) }, // fecha ao voltar
                            onAddFaceClick = { /* abrir add-face */ }
                        )
                    }
                }

                Option.ProximityDetection -> {
                    Text(
                        text = "Proximity detection settings here...",
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Option.CameraDetection -> {
                    Text(
                        text = "Camera detection configuration...",
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

