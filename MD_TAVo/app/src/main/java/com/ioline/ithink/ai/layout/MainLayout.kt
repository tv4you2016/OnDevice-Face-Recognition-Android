package com.ioline.ithink.ai.layout


import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


import com.ioline.ithink.ai.R
import com.ioline.ithink.ai.presentation.screens.add_face.AddFaceScreen
import com.ioline.ithink.ai.presentation.screens.camera_sensor.CameraSensor
import com.ioline.ithink.ai.presentation.screens.face_list.FaceListScreen
import kotlinx.coroutines.launch


import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.ioline.ithink.ai.AppUtils
import com.ioline.ithink.ai.AppUtils.openTargetAppSafe
import com.ioline.ithink.ai.SplashScreen
import com.ioline.ithink.ai.WakeLock


// IMPORTS DO DATASTORE
import com.ioline.ithink.ai.settingsdatastore.SettingsDataStore
import com.ioline.ithink.ai.settingsdatastore.AppSettings

// IMPORTS DOS SERVIÇOS
import com.ioline.ithink.ai.presentation.components.ProximityService
import com.ioline.ithink.ai.presentation.components.CameraService
import com.ioline.ithink.ai.presentation.components.FaceDetectionService

import android.provider.Settings


// APP UTILS
// Definindo as opções disponíveis
enum class Option {
    //ProximityDetection,
    None,
    FacialDetection,
    CameraDetection,

}



// Lista de opções de detecção
val settingsOptions = listOf(
    //Option.ProximityDetection,
    Option.FacialDetection,
    Option.CameraDetection,
    Option.None
)


@androidx.annotation.OptIn(ExperimentalGetImage::class)
fun startServiceIfNeeded(context: Context, option: Option) {
    Log.d("ServiceStart", "startServiceIfNeeded -> $option")

    try {
        when (option) {
            Option.CameraDetection -> {
                FaceDetectionService.stop(context)
                context.stopService(Intent(context, ProximityService::class.java))

                val intent = Intent(context, CameraService::class.java)
                context.startForegroundService(intent)
                Log.d("ServiceStart", "CameraService startForegroundService called")
            }

            Option.FacialDetection -> {
                context.stopService(Intent(context, ProximityService::class.java))
                context.stopService(Intent(context, CameraService::class.java))

                val intent = Intent(context, FaceDetectionService::class.java)
                context.startForegroundService(intent)
                Log.d("ServiceStart", "FaceDetectionService startForegroundService called")
            }

            Option.None -> {
                FaceDetectionService.stop(context)
                context.stopService(Intent(context, CameraService::class.java))
                context.stopService(Intent(context, ProximityService::class.java))
                Log.d("ServiceStart", "All services stopped")
            }
        }
    } catch (t: Throwable) {
        Log.e("ServiceStart", "FAILED starting service for option=$option", t)
    }
}





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
    onExpandChange: (Option?) -> Unit,
    isFaceLoading: Boolean,
    setFaceLoading: (Boolean) -> Unit

    ) {
    val isExpandable = option == Option.FacialDetection || option == Option.CameraDetection
    val isExpanded = isExpandable && expandedOption == option

    // ❌ sem animateColorAsState
    val borderColor =
        if (isExpanded) colorResource(id = R.color.md_orange) else Color.Gray

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(elevation = shadowElevation, shape = RoundedCornerShape(cornerRadius))
            .background(Color.Black, RoundedCornerShape(cornerRadius))
            .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))
            // ❌ sem animateContentSize
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
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
                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    if (description != null) {
                        Text(
                            text = description,
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }

            if (toggleVisible) {
                DetectionSwitch(
                    checked = enabled,
                    onCheckedChange = { isChecked ->
                        onToggleChange?.invoke(isChecked)

                        if (isExpandable && isChecked) {
                            onExpandChange(option)
                        } else if (!isChecked) {
                            onExpandChange(null)
                        }
                    },
                    enabled = toggleEnabled
                )
            }
        }

        // ❌ sem AnimatedVisibility
        if (isExpanded) {
            Spacer(modifier = Modifier.height(8.dp))
            SettingItemContent(
                option = option,
                description = description,
                onAddFaceClick = onAddFaceClick,
                isFaceLoading = isFaceLoading,
                setFaceLoading = setFaceLoading


            )
        }
    }
}



@Composable
fun FullscreenOverlay(onDismiss: () -> Unit,  cameraOpenState: MutableState<Boolean>) {
    // ❌ sem AnimatedVisibility
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
            AddFaceScreen(
                onNavigateBack = { onDismiss() },
                cameraOpenState = cameraOpenState
            )
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
        modifier = Modifier.padding(2.dp)
    )
}



@Composable
private fun SettingItemContent(
    option: Option,
    description: String?,
    onAddFaceClick: () -> Unit,
    isFaceLoading: Boolean,
    setFaceLoading: (Boolean) -> Unit

) {
    // ❌ Sem DataStore aqui
    // ❌ Sem AppUtils.startLoading aqui
    // ✅ Só UI

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 2.dp)
            .heightIn(min = 120.dp, max = 260.dp)   // já é suficiente
    ) {
        when (option) {
            Option.CameraDetection -> {
                CameraSensor()
            }

            Option.FacialDetection -> {
                var renderFaceList by remember { mutableStateOf(false) }

                // Quando entra no FacialDetection, liga o loading e só compõe o FaceList depois de 1 frame
                LaunchedEffect(Unit) {
                    setFaceLoading(true)
                    renderFaceList = false
                    kotlinx.coroutines.yield()   // dá 1 frame para desenhar o Splash
                    renderFaceList = true
                }

                if (renderFaceList && !isFaceLoading) {
                    FaceListScreen(onAddFaceClick = onAddFaceClick)
                }
            }

            Option.None -> {
                // nada
            }
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



@OptIn(ExperimentalMaterial3Api::class, ExperimentalGetImage::class)
@Composable
fun MainLayout(
    settingsStore: SettingsDataStore,
    initialSettings: AppSettings
) {
    val context = LocalContext.current

    // Começa com o que já veio do splash/root
    val settings by settingsStore.settingsFlow.collectAsState(initial = initialSettings)
    var currentSettings by remember { mutableStateOf(settings) }

    val versionName = context.packageManager
        .getPackageInfo(context.packageName, 0)
        .versionName


    LaunchedEffect(settings) {
        currentSettings = settings
    }

    var isFaceLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var overlayVisible by remember { mutableStateOf(false) }
    var expandedOption by remember { mutableStateOf<Option?>(currentSettings.detectionType) }
    val addFaceCameraOpen = remember { mutableStateOf(false) }


    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.ioline.ithink.ai.action.FACE_READY") {
                    isFaceLoading = false
                    Log.d("MainLayout", "Face READY received -> hide loading")
                }
            }
        }

        val filter = android.content.IntentFilter("com.ioline.ithink.ai.action.FACE_READY")
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        onDispose { context.unregisterReceiver(receiver) }
    }


    // Sempre que detectionType mudar -> garante serviço correto
    LaunchedEffect(currentSettings.detectionType) {
        startServiceIfNeeded(context, currentSettings.detectionType)
        expandedOption = currentSettings.detectionType
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Mordomus Tavo",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ver: $versionName",
                            color = Color.White,
                            fontWeight = FontWeight.Bold

                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
                actions = {
                    if (overlayVisible) {
                        IconButton(onClick = {
                            if (addFaceCameraOpen.value) {
                                addFaceCameraOpen.value = false   // ✅ fecha a câmara primeiro
                            } else {
                                overlayVisible = false            // ✅ fecha o overlay
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate Back",
                                tint = Color.White
                            )
                        }
                    }
                    if (!overlayVisible) {
                        IconButton(onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_SETTINGS)
                            )
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "System Settings",
                                tint = Color.White
                            )
                        }
                    }
                }


            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.height(75.dp), containerColor = Color.Black) {
                NavigationBarItem(
                    icon = {
                        Icon(
                            painterResource(id = R.drawable.ic_mordomus_ithink),
                            contentDescription = "Home",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(44.dp)
                        )
                    },
                    selected = true,
                    onClick = {
                        coroutineScope.launch {
                            startServiceIfNeeded(context, currentSettings.detectionType)


                            val updated = currentSettings.copy(
                                OpeniThink = currentSettings.OpeniThink.copy(openApk = true)
                            )
                            settingsStore.saveSettings(updated)

                            AppUtils.startLoading(context, "openTargetApp")
                            try {
                                WakeLock().wakeUpScreen(context)
                                WakeLock().unlockScreen(context)
                                openTargetAppSafe(context, "app.ioline.ithink")
                            } finally {
                                AppUtils.stopLoading(context, "openTargetApp")
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                item { SectionTitle(stringResource(id = R.string.DetectionType)) }

                itemsIndexed(settingsOptions) { index, option ->
                    val isEnabled = currentSettings.detectionType == option
                    val title = when (option) {
                        Option.FacialDetection -> context.getString(R.string.facial_detect)
                        Option.CameraDetection -> context.getString(R.string.camera_movement)
                        Option.None -> context.getString(R.string.none_power_button)
                    }
                    val icon = when (option) {
                        Option.FacialDetection -> Icons.Default.FaceRetouchingNatural
                        Option.CameraDetection -> Icons.Default.Face
                        Option.None -> Icons.Default.Close
                    }

                    SettingItem(
                        title = title,
                        description = null,
                        enabled = isEnabled,
                        onToggleChange = { checked ->
                            if (checked) {
                                coroutineScope.launch {
                                    if (option == Option.FacialDetection && currentSettings.detectionType != Option.FacialDetection) {
                                        isFaceLoading = true

                                        // safety timeout (não fica preso se houver erro)
                                        scope.launch {
                                            kotlinx.coroutines.delay(6000)
                                            if (isFaceLoading) isFaceLoading = false
                                        }
                                    }

                                    val updated = currentSettings.copy(
                                        detectionType = option,
                                        // aqui tratamos também do openApk = false
                                        OpeniThink = currentSettings.OpeniThink.copy(openApk = false)
                                    )
                                    scope.launch {
                                        kotlinx.coroutines.yield()
                                        settingsStore.saveSettings(updated)
                                    }
                                }

                                expandedOption = if (option == Option.None) null else option
                            }
                        },
                        toggleVisible = true,
                        toggleEnabled = true,
                        option = option,
                        iconImage = icon,
                        expandedOption = expandedOption,
                        onAddFaceClick = {
                            if (option == Option.FacialDetection) overlayVisible = true
                        },
                        onExpandChange = { /* podemos ignorar ou implementar depois */ },
                        isFaceLoading = isFaceLoading,
                        setFaceLoading = { isFaceLoading = it },

                    )
                }
            }

            if (overlayVisible) {
                FullscreenOverlay(
                    onDismiss = {
                        overlayVisible = false
                        addFaceCameraOpen.value = false
                        startServiceIfNeeded(context, currentSettings.detectionType) // ✅ força re-start

                    },
                    cameraOpenState = addFaceCameraOpen
                )
            }

            if (isFaceLoading) {
                SplashScreen()
            }
        }
    }
}
