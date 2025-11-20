package com.ioline.ithink.ai.layout


import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat.startForegroundService
import com.ioline.ithink.ai.presentation.components.ProximityService
import com.ioline.ithink.ai.AppUtils.openTargetApp
import com.ioline.ithink.ai.AutoDismissDialog
import com.ioline.ithink.ai.PeriodicAppLauncherService
import com.ioline.ithink.ai.R
import com.ioline.ithink.ai.TouchOverlay
import com.ioline.ithink.ai.settingsdatastore.AppSettings
import com.ioline.ithink.ai.settingsdatastore.SettingsDataStore
import com.ioline.ithink.ai.presentation.components.AppLoading
import com.ioline.ithink.ai.presentation.screens.camera_sensor.CameraSensor
import com.ioline.ithink.ai.presentation.screens.face_list.FaceListScreen
import com.ioline.ithink.ai.presentation.screens.proximity_sensor.ProximitySensor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable


// Definindo as opções disponíveis
@Serializable
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
    HOME("Home", R.drawable.ic_mordomus_ithink),
    SAVE("Save",R.drawable.ic_save),
}

// Lista de opções de detecção
val settingsOptions = listOf(
    Option.ProximityDetection,
    Option.FacialDetection,
    Option.CameraDetection,
    Option.None
)


@Composable
fun MainLayout() {

    var appReady by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = appReady,
        transitionSpec = {
            fadeIn(tween(450)) + scaleIn(initialScale = 0.9f) togetherWith
                    fadeOut(tween(300)) + scaleOut(targetScale = 0.9f)
        }
    ) { ready ->
        if (!ready) {
            LoadingScreen()
            // Preload corre apenas quando authenticated = true e ready = false
            LaunchedEffect(Unit) {
                withFrameNanos { }
                delay(250)  // <-- 1 frame extra para Compose respirar
                appReady = true
            }
        } else {
            AppContent()
        }
    }
}

// Função auxiliar para iniciar os serviços
fun startServiceIfNeeded(context:Context, option: Option) {
    // Mostra overlay
    val overlay = TouchOverlay(context) {
        Log.d("MainActivity", "Overlay callback acionado")
    }
    overlay.show()

    val intent = Intent(context, PeriodicAppLauncherService::class.java)
    startForegroundService(context,intent)

    if (option == Option.ProximityDetection)
    {
        val intent = Intent(context, ProximityService::class.java)
        context.startForegroundService(intent)
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AppLoading(size = 80.dp)
    }
}


fun getProximitySensorInfo(context: Context): Triple<String?, String?, Float?> {
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    return if (proximitySensor != null) {
        Log.d("ProximityInfo", "Nome do sensor: ${proximitySensor.name}")
        Log.d("ProximityInfo", "Fabricante: ${proximitySensor.vendor}")
        Log.d("ProximityInfo", "Alcance máximo: ${proximitySensor.maximumRange}")

        Triple(
            proximitySensor.name,
            proximitySensor.vendor,
            proximitySensor.maximumRange
        )
    } else {
        Triple(null, null, null)
    }
}


@Composable
fun rememberProximitySensorInfo(context: Context): Triple<String?, String?, Float?> {
    return remember {
        getProximitySensorInfo(context)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent() {
    var showDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Dados de configuração
    val settingsStore = remember { SettingsDataStore(context) }
    val settings by settingsStore.settingsFlow.collectAsState(initial = AppSettings())

    var currentSettings by remember { mutableStateOf(AppSettings()) }

    LaunchedEffect(settings) {
        currentSettings = settings
    }

    // Estados de UI
    var isLoading by remember { mutableStateOf(false) }

    var expandedOption by remember { mutableStateOf<Option?>(currentSettings.detectionType) }
    LaunchedEffect(currentSettings) {
        expandedOption = currentSettings.detectionType
    }
    startServiceIfNeeded(context,currentSettings.detectionType)


    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()



    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("Mordomus Tavo", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.height(75.dp),containerColor = Color(0xFF000000)
            ) {
                AppDestinations.entries.forEach { destination ->
                    NavigationBarItem(
                        icon = { Icon(painterResource(id = destination.icon), tint = Color.Unspecified,contentDescription = destination.label, modifier = Modifier.size(44.dp)) },
                        selected = destination == AppDestinations.HOME,
                        onClick = {
                            if (destination == AppDestinations.HOME) {
                                isLoading = true
                                coroutineScope.launch {
                                    delay(800)
                                    openTargetApp(context, true)
                                    isLoading = false
                                }
                            } else if (destination == AppDestinations.SAVE) {

                                coroutineScope.launch {
                                    settingsStore.saveSettings(currentSettings)
                                    showDialog = true        // <- APENAS ISTO!!!
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

        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 10.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {

            // Seção de seleção de tipo de detecção
            item {
                DetectionTypeSection(
                    settingsOptions = settingsOptions,
                    expandedOption = expandedOption,
                    onOptionSelected = { selectedOption ->
                        expandedOption = selectedOption
                        currentSettings = currentSettings.copy(detectionType = selectedOption)
                        startServiceIfNeeded(context,selectedOption)
                    }

                )
            }

            // Conteúdo dinâmico do item expandido
            item {
                AnimatedVisibility(expandedOption != null) {
                    ExpandedOptionContent(
                        expandedOption = expandedOption,
                        onOverlayChange = { },
                        context
                    )
                }
            }
        }

        // Overlay de loading
        AnimatedVisibility(isLoading) {
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

    if (showDialog) {
        AutoDismissDialog(
            message = "Configurações gravadas!",
            onDismiss = { showDialog = false }
        )
    }
}


@Composable
fun DetectionTypeSection(
    settingsOptions: List<Option>,
    expandedOption: Option?,
    onOptionSelected: (Option) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text("Detection Type:", color = Color(0xFFff931e), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        DetectionTypeComboBox(
            options = settingsOptions,
            selected = expandedOption,
            onSelect = onOptionSelected
        )
    }
}

@Composable
fun ExpandedOptionContent(
    expandedOption: Option?,
    onOverlayChange: (Boolean) -> Unit,
    context: Context
) {
    val (proximityName, proximityVendor, proximityMaxRange) = rememberProximitySensorInfo(context)


    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 380.dp).animateContentSize()
    ) {
        when (expandedOption) {
            Option.FacialDetection -> FaceListScreen(onAddFaceClick = { onOverlayChange(true) })
            Option.ProximityDetection ->
                if (proximityName != "prox_stk3311" &&
                    proximityVendor != "sensortek"
                ) {
                    ProximitySensor()
                }
            Option.CameraDetection -> CameraSensor()
            else -> {}
        }
    }

}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionTypeComboBox(
    options: List<Option>,
    selected: Option?,
    onSelect: (Option) -> Unit,
    onExpandChange: ((Option?, Boolean) -> Unit)? = null // <- callback com item e expand

) {
    var expanded by remember { mutableStateOf(false) }

    val optionIcons = mapOf(
        Option.ProximityDetection to Icons.Default.Sensors,
        Option.FacialDetection to Icons.Default.FaceRetouchingNatural,
        Option.CameraDetection to Icons.Default.Face,
        Option.None to Icons.Default.Close
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { newExpanded ->
            expanded = newExpanded
            onExpandChange?.invoke(selected, newExpanded) // <-- envia item atual + estado
        }
    ) {
        val defaultOption = options.first { it.name == "None" }

        OutlinedTextField(
            value = (selected ?: defaultOption).name, // pega o None se nada selecionado
            onValueChange = {},
            readOnly = true,
            leadingIcon = {
                val current = selected ?: defaultOption
                Icon(
                    imageVector = optionIcons[current] ?: Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.White // aqui força a cor branca
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFff931e),
                focusedLabelColor = Color(0xFFff931e),
                unfocusedBorderColor = Color(0xFFff931e),
                unfocusedLabelColor = Color(0xFFff931e),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )


        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.Black)
        ) {
            options.forEach { opt ->   // <- usa 'opt' aqui
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = optionIcons[opt] ?: Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(opt.name, color = Color.White)
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(opt)   // <- seleciona
                    }
                )
            }
        }
    }
}
