package com.ioline.ithink.ai.layout



import android.app.Activity
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioline.aicamera.utils.AppUtils
import com.ioline.ithink.ai.presentation.components.ProximityService
import com.ioline.ithink.ai.presentation.components.FaceDetectionService
import com.ioline.aicamera.utils.AppUtils.openTargetApp
import com.ioline.ithink.ai.R
import com.ioline.ithink.ai.presentation.components.AppLoading
import com.ioline.ithink.ai.presentation.components.CameraService
import com.ioline.ithink.ai.presentation.screens.add_face.AddFaceScreen
import com.ioline.ithink.ai.presentation.screens.camera_sensor.CameraSensor
import com.ioline.ithink.ai.presentation.screens.face_list.FaceListScreen
import com.ioline.ithink.ai.presentation.screens.proximity_sensor.ProximitySensor
import kotlinx.coroutines.launch


// Definindo as opções disponíveis
enum class Option {
    FacialDetection,
    ProximityDetection,
    CameraDetection
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {

    HOME("Home", R.drawable.ic_mordomus_ithink)
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalGetImage::class)
@Composable
fun MainLayout(navController: NavController) {
    var overlayVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var expandedOption by remember { mutableStateOf<Option?>(null) }
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // 👇 Box raiz que envolve tudo
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)) // define o fundo preto
    ) {


        // ----------- Scaffold (conteúdo da app) -----------
        Scaffold(
            containerColor = Color(0xFF000000), // 👈 fundo preto

            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "MordomusTABMNG",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF000000)), // Fundo preto
                  //  navigationIcon = null, // Remova o ícone de navegação à esquerda
                    actions = {
                        if (overlayVisible) {
                            IconButton(onClick = {
                                // Lógica para fechar o overlay
                                overlayVisible = false
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.ArrowBack, // Ícone da seta
                                    contentDescription = "Navigate Back",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                )
            },

            bottomBar = {
                NavigationBar(
                    modifier = Modifier.height(75.dp),
                    containerColor = Color(0xFF000000)
                ) {
                    AppDestinations.entries.forEach { destination ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    painter = painterResource(id = destination.icon),
                                    contentDescription = destination.label,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(44.dp)
                                )
                            },
                            selected = destination == currentDestination,
                            onClick = {
                                currentDestination = destination
                                if (destination == AppDestinations.HOME) {
                                    isLoading = true
                                    coroutineScope.launch {
                                        openTargetApp(context, true)
                                        kotlinx.coroutines.delay(2000)
                                        isLoading = false
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent,
                                selectedIconColor = Color.Unspecified,
                                unselectedIconColor = Color.Unspecified
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                        .fillMaxSize()
                ) {
                    var proximityEnabled by remember { mutableStateOf(true) }
                    var facialEnabled by remember { mutableStateOf(false) }
                    var cameraEnabled by remember { mutableStateOf(false) }

                    SectionTitle("Detection:" )

                    // --- seus SettingItems ---

                    val hasSensor = AppUtils.hasProximitySensor(context)





                    SettingItem(
                        title = "Proximity",
                        description = if (!hasSensor) "Sensor de proximidade não disponível neste dispositivo." else null,
                        enabled = proximityEnabled && hasSensor,
                        onToggleChange = if (hasSensor) { isChecked ->
                            proximityEnabled = isChecked
                            facialEnabled = false
                            cameraEnabled = false

                            if (isChecked) {
                                FaceDetectionService.stop(context)
                                val intent = Intent(context, ProximityService::class.java)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                            } else {
                                context.stopService(Intent(context, ProximityService::class.java))
                            }
                        } else null, // 👈 se não tiver sensor, desativa o toggle
                        toggleVisible = true,
                        toggleEnabled = hasSensor, // 👈 desativa visualmente o switch
                        option = Option.ProximityDetection,
                        iconImage = Icons.Default.Sensors,
                        onAddFaceClick = { overlayVisible = true },
                        expandedOption = expandedOption,
                        onExpandChange = { selected ->
                            expandedOption = if (expandedOption == selected) null else selected
                        }
                    )


                    SettingItem(
                        title = "Facial Detect",
                        description = null,
                        enabled = facialEnabled,
                        onToggleChange = { isChecked ->
                            facialEnabled = isChecked
                            proximityEnabled = false
                            cameraEnabled = false

                            if (isChecked) {
                                context.stopService(Intent(context, ProximityService::class.java))
                                val intent = Intent(context, FaceDetectionService::class.java)
                                ContextCompat.startForegroundService(context, intent)
                            } else {
                                FaceDetectionService.stop(context)
                            }
                        },
                        toggleVisible = true,
                        toggleEnabled = true,
                        option = Option.FacialDetection,
                        iconImage = Icons.Default.FaceRetouchingNatural,
                        onAddFaceClick = {
                            // Alterando o estado do overlay dentro do contexto composable
                            overlayVisible = true  // Alterando o estado no MainLayout
                        },
                        expandedOption = expandedOption,
                        onExpandChange = { selected ->
                            expandedOption = if (expandedOption == selected) null else selected
                        }
                    )

                    SettingItem(
                        title = "Camera movement",
                        description = null,
                        enabled = cameraEnabled,
                        onToggleChange = { isChecked ->
                            cameraEnabled = isChecked
                            proximityEnabled = false
                            facialEnabled = false


                            if (isChecked) {
                                FaceDetectionService.stop(context)
                                context.stopService(Intent(context, ProximityService::class.java))

                                val intent = Intent(context, CameraService::class.java)
                                ContextCompat.startForegroundService(context, intent)
                            }
                        },
                        toggleVisible = true,
                        toggleEnabled = true,
                        option = Option.CameraDetection,
                        iconImage = Icons.Default.Face,
                        onAddFaceClick = {
                            // Alterando o estado do overlay dentro do contexto composable
                            overlayVisible = true  // Alterando o estado no MainLayout
                        },
                        expandedOption = expandedOption,
                        onExpandChange = { selected ->
                            expandedOption = if (expandedOption == selected) null else selected
                        }
                    )

                }

                if (overlayVisible) {
                    FullscreenOverlay(onDismiss = { overlayVisible = false })
                }
            }
        }

        // ----------- OVERLAY GLOBAL (fora do Scaffold) -----------
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .zIndex(10f),
                contentAlignment = Alignment.Center
            ) {
                AppLoading(size = 80.dp)
            }
        }
    }
}


// Componente SettingItem modificado para receber 'overlayVisible' e 'onAddFaceClick'
@Composable
fun SettingItem(
    title: String,
    description: String?,
    enabled: Boolean,
    onToggleChange: ((Boolean) -> Unit)?,
    option: Option,
    toggleVisible: Boolean,
    toggleEnabled: Boolean = true,
    borderWidth: Dp = 4.dp,
    cornerRadius: Dp = 12.dp,
    shadowElevation: Dp = 4.dp,
    iconImage: ImageVector,
    onAddFaceClick: () -> Unit,
    expandedOption: Option?,
    onExpandChange: (Option?) -> Unit
) {
    val isExpanded = expandedOption == option

    // Cor da borda animada
    val borderColor by animateColorAsState(
        targetValue = if (isExpanded) colorResource(id = R.color.md_orange) else Color.Gray,
        label = "borderColor"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(elevation = shadowElevation, shape = RoundedCornerShape(cornerRadius))
            .background(Color.Black, RoundedCornerShape(cornerRadius))
            .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))
            .clickable {
                onExpandChange(if (isExpanded) null else option)
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ===== Cabeçalho da opção =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = iconImage,
                    contentDescription = "$title icon",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }

            if (toggleVisible) {
                DetectionSwitch(
                    checked = enabled,
                    onCheckedChange = onToggleChange,
                    enabled = toggleEnabled
                )
            }
        }

        // ===== Conteúdo expandido =====
        AnimatedVisibility(visible = isExpanded) {
            SettingItemContent(
                option = option,
                description = description,
                onAddFaceClick = onAddFaceClick
            )
        }
    }
}



@Composable
fun FullscreenOverlay( onDismiss: () -> Unit) {



        AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(),
                color = Color.White
            ) {
                AddFaceScreen(onNavigateBack = {
                    onDismiss() // Chama o onDismiss quando a sobreposição for fechada
                })
            }
        }
    }
}


@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color(0xFFff931e), // azul parecido
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}



@Composable
private fun SettingItemContent(
    option: Option,
    description: String?,
    onAddFaceClick: () -> Unit
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = screenHeight * 0.5f)
            .padding(top = 8.dp)
    ) {
        if (option == Option.FacialDetection) {
            FaceListScreen(onAddFaceClick = onAddFaceClick)
        }
        else if (option == Option.ProximityDetection) {
            ProximitySensor( onAddFaceClick = onAddFaceClick)
        }
        else  if (option == Option.CameraDetection) {
            CameraSensor( onAddFaceClick = onAddFaceClick)
        }
        else {
            Text(
                text = description ?: "Tap to view details",
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}


@Composable
private fun DetectionSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    enabled: Boolean
) {
    Switch(
        checked = checked,
        onCheckedChange = { if (enabled && onCheckedChange != null) onCheckedChange(it) },
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Color(0xFFff931e),
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color(0xFF808080),

            // 👇 Novos parâmetros corretos:
            disabledCheckedThumbColor = Color.Gray,
            disabledCheckedTrackColor = Color.DarkGray,
            disabledUncheckedThumbColor = Color.Gray,
            disabledUncheckedTrackColor = Color.DarkGray
        )
    )
}
