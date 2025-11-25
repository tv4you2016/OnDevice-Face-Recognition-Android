package com.ioline.ithink.ai.layout


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
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
import androidx.compose.foundation.verticalScroll
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
import com.ioline.ithink.ai.presentation.components.AppLoading
import com.ioline.ithink.ai.presentation.screens.add_face.AddFaceScreen
import com.ioline.ithink.ai.presentation.screens.camera_sensor.CameraSensor
import com.ioline.ithink.ai.presentation.screens.face_list.FaceListScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.registerReceiver
import com.ioline.ithink.ai.AppUtils
import com.ioline.ithink.ai.AppUtils.openTargetAppSafe
import com.ioline.ithink.ai.WakeLock


// IMPORTS DO DATASTORE
import com.ioline.ithink.ai.settingsdatastore.SettingsDataStore
import com.ioline.ithink.ai.settingsdatastore.AppSettings

// IMPORTS DOS SERVIÇOS
import com.ioline.ithink.ai.presentation.components.ProximityService
import com.ioline.ithink.ai.presentation.components.CameraService
import com.ioline.ithink.ai.presentation.components.FaceDetectionService
import kotlinx.coroutines.flow.first


// APP UTILS
// Definindo as opções disponíveis
enum class Option {
    //ProximityDetection,
    FacialDetection,
    CameraDetection,
    None
}



// Lista de opções de detecção
val settingsOptions = listOf(
    //Option.ProximityDetection,
    Option.FacialDetection,
    Option.CameraDetection,
    Option.None
)


@androidx.annotation.OptIn(ExperimentalGetImage::class)
fun startServiceIfNeeded(context: Context, option: Option)
{

    when (option) {
/*
        Option.ProximityDetection -> {
            FaceDetectionService.stop(context)
            context.stopService(Intent(context, CameraService::class.java))

            val intent = Intent(context, ProximityService::class.java)
            context.startForegroundService(intent)
        }
*/
        Option.CameraDetection -> {
            FaceDetectionService.stop(context)
            context.stopService(Intent(context, ProximityService::class.java))

            val intent = Intent(context, CameraService::class.java)
            context.startForegroundService(intent)
        }

        Option.FacialDetection -> {
            context.stopService(Intent(context, ProximityService::class.java))
            context.stopService(Intent(context, CameraService::class.java))

            val intent = Intent(context, FaceDetectionService::class.java)
            context.startForegroundService(intent)
        }

        Option.None -> {
            FaceDetectionService.stop(context)
            context.stopService(Intent(context, CameraService::class.java))
            context.stopService(Intent(context, ProximityService::class.java))

            AppUtils.stopLoading(context, "Option.None")
            //AppUtils.startLoading(context, "Option.None")
        }
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
    onExpandChange: (Option?) -> Unit
) {


    val isExpandable = option == Option.FacialDetection || option == Option.CameraDetection
    val isExpanded = isExpandable && expandedOption == option

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
            .animateContentSize()
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
                        onToggleChange?.invoke(isChecked)

                        if (isExpandable && isChecked) {
                            onExpandChange(option)
/*
                            // 🔥 Mostra loading global
                            context.sendBroadcast(
                                Intent("GLOBAL_LOADING_UPDATE").apply { putExtra("loading", true) }
                            )
*/
                        } else if (!isChecked) {
                            onExpandChange(null)
                        }

                    },
                    enabled = toggleEnabled
                )

            }
        }

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

    AppUtils.startLoading(LocalContext.current , "SettingItemContent -> $option\"")


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(top = 12.dp, bottom = 12.dp)
            .heightIn(min = 120.dp, max = 260.dp)
    ) {


        when (option) {


            Option.CameraDetection -> CameraSensor()



            Option.FacialDetection -> FaceListScreen(
                onAddFaceClick = onAddFaceClick,
            )

            Option.None -> {

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



@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout() {
    val context = LocalContext.current

    // === DataStore ===
    val settingsStore = remember { SettingsDataStore(context) }
    val settings by settingsStore.settingsFlow.collectAsState(initial = AppSettings())
    var currentSettings by remember { mutableStateOf(settings) }
    LaunchedEffect(settings) { currentSettings = settings }

    // === UI States ===
    var overlayVisible by remember { mutableStateOf(false) }
    var expandedOption by remember { mutableStateOf<Option?>(currentSettings.detectionType) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()



    //Log.d("Loading","MainLayout")
    //AppUtils.startLoading(context)

    // 🔥 Ativa loading global imediatamente e inicia serviço
    LaunchedEffect(currentSettings.detectionType) {
        startServiceIfNeeded(context, currentSettings.detectionType)
        expandedOption = currentSettings.detectionType
    }

/*
    // Sempre que o detectionType mudar, garante que o serviço esteja ativo
    DisposableEffect(currentSettings.detectionType) {
        startServiceIfNeeded(context, currentSettings.detectionType)
        expandedOption = currentSettings.detectionType
        onDispose {
            // opcional: parar serviços ao sair do layout se desejar
        }
    }

    // Garante que o serviço facial reinicie após fechar o overlay
    LaunchedEffect(overlayVisible) {
        if (!overlayVisible) {
            startServiceIfNeeded(context, currentSettings.detectionType)
            expandedOption = currentSettings.detectionType
        }
    }
*/
   // globalLoading = False;
    // --- Se loading global estiver ativo, mostra só o loading ---


        // --- Layout principal ---
        Scaffold(
            containerColor = Color.Black,
            topBar = {
                TopAppBar(
                    title = { Text("Mordomus Tavo", color = Color.White, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
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
                                // Atualiza o AppSettings
                                val current = settingsStore.settingsFlow.first()
                                val updated = current.copy(
                                    OpeniThink = current.OpeniThink.copy(openApk = true)
                                )
                                settingsStore.saveSettings(updated)

                            }
/*
                            AppUtils.startLoading(context, "openTargetApp")
                            try {
                                WakeLock().wakeUpScreen(context)
                                WakeLock().unlockScreen(context)

                                openTargetAppSafe(context, "app.ioline.ithink")
                            } finally {
                                AppUtils.stopLoading(context, "openTargetApp")
                            }

 */
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
            Box(modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item { SectionTitle("Detection Type:") }

                    itemsIndexed(settingsOptions) { index, option ->
                        val isEnabled = currentSettings.detectionType == option
                        val title = when (option) {

                            Option.FacialDetection -> "Facial Detect"
                            Option.CameraDetection -> "Camera movement"
                            Option.None -> "None (Power Button)"
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
                                    val newSettings = currentSettings.copy(detectionType = option)
                                    currentSettings = newSettings
                                    coroutineScope.launch { settingsStore.saveSettings(newSettings) }
                                    // Somente expandir se for expandível
                                    if (option == Option.None) {
                                        expandedOption = null

                                    } else {
                                        expandedOption = option
                                    }
                                }
                            },
                            toggleVisible = true,
                            toggleEnabled = true,
                            option = option,
                            iconImage = icon,
                            expandedOption = expandedOption,
                            onAddFaceClick = { if (option == Option.FacialDetection) overlayVisible = true },
                            onExpandChange = { /* manual expand opcional */ }
                        )

                        if (expandedOption == option) {
                            LaunchedEffect(option) { listState.animateScrollToItem(index) }
                        }
                    }
                }

                if (overlayVisible) FullscreenOverlay(onDismiss = { overlayVisible = false })
            }
        }



}
