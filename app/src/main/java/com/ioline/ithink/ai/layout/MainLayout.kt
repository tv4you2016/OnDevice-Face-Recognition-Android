package com.ioline.ithink.ai.layout



import android.content.Intent
import android.os.Build
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FaceRetouchingNatural
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
import androidx.navigation.NavController
import com.ioline.ithink.ai.AppUtils
import com.ioline.ithink.ai.presentation.components.ProximityService
import com.ioline.ithink.ai.presentation.components.FaceDetectionService
import com.ioline.ithink.ai.AppUtils.openTargetApp
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
    CameraDetection,

    None
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
    var expandedOption by remember { mutableStateOf<Option?>(Option.FacialDetection) }
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val listState = rememberLazyListState()

    var proximityEnabled by remember { mutableStateOf(false) }
    var facialEnabled by remember { mutableStateOf(true) }
    var cameraEnabled by remember { mutableStateOf(false) }
    var noneEnabled by remember { mutableStateOf(false) }

    val settingsOptions = listOf(
        Option.ProximityDetection,
        Option.FacialDetection,
        Option.CameraDetection,
        Option.None
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        Scaffold(
            containerColor = Color(0xFF000000),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Mordomus Tavo",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF000000)),
                    actions = {
                        if (overlayVisible) {
                            IconButton(onClick = { overlayVisible = false }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
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
                /*
                Column(
                    modifier = Modifier.padding(10.dp)
                ) {
                    SectionTitle("Detection Type:")
                }
                */

                LazyColumn(
                    state = listState,

                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .fillMaxSize(),


                    contentPadding = PaddingValues(bottom = 80.dp) // espaço para a bottom nav

                ) {
                    item { // título
                        SectionTitle("Detection Type:")
                    }

                    itemsIndexed(settingsOptions) { index, option ->

                        val hasSensor = AppUtils.hasProximitySensor(context)

                        val title = when(option) {
                            Option.ProximityDetection -> "Proximity"
                            Option.FacialDetection -> "Facial Detect"
                            Option.CameraDetection -> "Camera movement"
                            Option.None -> "None (Power Button)"
                        }

                        val description = when(option) {
                            Option.ProximityDetection -> if (!hasSensor) "Sensor de proximidade não disponível neste dispositivo." else null
                            else -> null
                        }

                        val enabled = when(option) {
                            Option.ProximityDetection -> proximityEnabled && hasSensor
                            Option.FacialDetection -> facialEnabled
                            Option.CameraDetection -> cameraEnabled
                            Option.None -> noneEnabled
                        }

                        val toggleEnabled = when(option) {
                            Option.ProximityDetection -> hasSensor
                            else -> true
                        }

                        val iconImage = when(option) {
                            Option.ProximityDetection -> Icons.Default.Sensors
                            Option.FacialDetection -> Icons.Default.FaceRetouchingNatural
                            Option.CameraDetection -> Icons.Default.Face
                            Option.None -> Icons.Default.Close
                        }

                        SettingItem(
                            title = title,
                            description = description,
                            enabled = enabled,
                            onToggleChange = { isChecked ->
                                when(option) {
                                    Option.ProximityDetection -> {
                                        proximityEnabled = isChecked
                                        if (isChecked) {
                                            facialEnabled = false
                                            cameraEnabled = false
                                            noneEnabled = false

                                            FaceDetectionService.stop(context)
                                            context.stopService(Intent(context, CameraService::class.java))

                                            val intent = Intent(context, ProximityService::class.java)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                context.startForegroundService(intent)
                                            } else {
                                                context.startService(intent)
                                            }
                                        } else {
                                            context.stopService(Intent(context, ProximityService::class.java))
                                        }
                                    }
                                    Option.FacialDetection -> {
                                        facialEnabled = isChecked
                                        if (isChecked) {
                                            proximityEnabled = false
                                            cameraEnabled = false
                                            noneEnabled = false

                                            context.stopService(Intent(context, ProximityService::class.java))
                                            context.stopService(Intent(context, CameraService::class.java))

                                            val intent = Intent(context, FaceDetectionService::class.java)
                                            ContextCompat.startForegroundService(context, intent)
                                        } else {
                                            FaceDetectionService.stop(context)
                                        }
                                    }
                                    Option.CameraDetection -> {
                                        cameraEnabled = isChecked
                                        if (isChecked) {
                                            proximityEnabled = false
                                            facialEnabled = false
                                            noneEnabled = false

                                            FaceDetectionService.stop(context)
                                            context.stopService(Intent(context, ProximityService::class.java))

                                            val intent = Intent(context, CameraService::class.java)
                                            ContextCompat.startForegroundService(context, intent)
                                        } else {
                                            context.stopService(Intent(context, CameraService::class.java))
                                        }
                                    }
                                    Option.None -> {
                                        noneEnabled = isChecked

                                        if (isChecked) {
                                            cameraEnabled = false
                                            proximityEnabled = false
                                            facialEnabled = false

                                            FaceDetectionService.stop(context)
                                            context.stopService(Intent(context, ProximityService::class.java))
                                            context.stopService(Intent(context, CameraService::class.java))
                                        }
                                    }
                                }
                            },
                            toggleVisible = true,
                            toggleEnabled = toggleEnabled,
                            option = option,
                            iconImage = iconImage,
                            onAddFaceClick = {
                                overlayVisible = when(option) {
                                    Option.FacialDetection, Option.CameraDetection, Option.ProximityDetection -> true
                                    else -> false
                                }
                            },
                            expandedOption = expandedOption,

                            onExpandChange = { selected ->
                                expandedOption = if (selected == Option.None) {
                                    null
                                } else if (expandedOption == selected) {
                                    null
                                } else {
                                    selected
                                }
                            }
                        )

                        if (expandedOption == option) {
                            LaunchedEffect(option) {
                                listState.animateScrollToItem(index)
                            }
                        }
                    }
                }

                if (overlayVisible) {
                    FullscreenOverlay(onDismiss = { overlayVisible = false })
                }
            }
        }

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

           /* .clickable {
                onExpandChange(if (isExpanded) null else option)
            }

           */
            .animateContentSize() // 👈 AQUI — anima o card inteiro!
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
                    onCheckedChange = { isChecked ->
                        // ✅ Impede desligar o próprio item
                        if (!enabled && isChecked) {
                            // Estava desligado e foi ligado → ativa
                            onToggleChange?.invoke(true)
                            onExpandChange(option)
                        } else if (enabled && isChecked) {
                            // Já estava ligado → ignora clique
                            return@DetectionSwitch
                        }
                    },
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
            // Altura adaptável e limitada
            .animateContentSize() // 👈 adiciona animação de expansão suave
            .padding(top = 12.dp, bottom = 12.dp) // 👈 espaço acima/abaixo do conteúdo
            .heightIn(min = 120.dp, max = 240.dp) // 👈 não enche o ecrã
    ) {
        when (option) {
            Option.FacialDetection -> FaceListScreen(onAddFaceClick = onAddFaceClick)
            Option.ProximityDetection -> ProximitySensor(onAddFaceClick = onAddFaceClick)
            Option.CameraDetection -> CameraSensor(onAddFaceClick = onAddFaceClick)
            else -> Text(
                text = description ?: "Power Button",
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
