package com.ioline.ithink.ai.layout


import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.content.edit
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
import kotlinx.coroutines.delay
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


@Composable
fun MainLayout(navController: NavController) {

    val context = LocalContext.current

    var isAuthenticated by remember { mutableStateOf(false) }
    var appReady by remember { mutableStateOf(false) }

    val prefs = context.getSharedPreferences("app_prefs", MODE_PRIVATE)
    if (!prefs.contains("access_code")) {
        prefs.edit { putString("access_code", "1234") }
    }

    val savedCode = prefs.getString("access_code", "1234") ?: "1234"

    AnimatedContent(
        targetState = isAuthenticated to appReady,
        transitionSpec = {
            fadeIn(tween(450)) + scaleIn(initialScale = 0.9f) togetherWith
                    fadeOut(tween(300)) + scaleOut(targetScale = 0.9f)
        }
    ) { (authenticated, ready) ->

        if (!authenticated) {

            LoginScreen(
                savedCode = savedCode,
                onLoginSuccess = {
                    isAuthenticated = true
                }
            )

        } else if (!ready) {

            LoadingScreen()

            // Preload corre apenas quando authenticated = true e ready = false
            LaunchedEffect(Unit) {
                withFrameNanos { }
                delay(250)  // <-- 1 frame extra para Compose respirar
                appReady = true
            }

        } else {

           // AppContent(context)
            Box(Modifier.fillMaxSize().background(Color.Red))

        }

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

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(context: Context) {
    var isLoading by remember { mutableStateOf(false) }

    var overlayVisible by remember { mutableStateOf(false) }

    var expandedOption by remember { mutableStateOf<Option?>(Option.FacialDetection) }
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

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
                                   // kotlinx.coroutines.delay(2000)
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

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {

                item {
                    SectionTitle("Detection Type:")
                }

                itemsIndexed(settingsOptions) { index, option ->

                    val hasSensor = AppUtils.hasProximitySensor(context)

                    val title = when (option) {
                        Option.ProximityDetection -> "Proximity"
                        Option.FacialDetection -> "Facial Detect"
                        Option.CameraDetection -> "Camera movement"
                        Option.None -> "None (Power Button)"
                    }

                    val description = when (option) {
                        Option.ProximityDetection -> if (!hasSensor) "Sensor não disponível." else null
                        else -> null
                    }

                    val enabled = when (option) {
                        Option.ProximityDetection -> proximityEnabled && hasSensor
                        Option.FacialDetection -> facialEnabled
                        Option.CameraDetection -> cameraEnabled
                        Option.None -> noneEnabled
                    }

                    val toggleEnabled = when (option) {
                        Option.ProximityDetection -> hasSensor
                        else -> true
                    }

                    val iconImage = when (option) {
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
                            when (option) {
                                Option.ProximityDetection -> {
                                    proximityEnabled = isChecked
                                    if (isChecked) {
                                        facialEnabled = false
                                        cameraEnabled = false
                                        noneEnabled = false

                                        FaceDetectionService.stop(context)
                                        context.stopService(
                                            Intent(
                                                context,
                                                CameraService::class.java
                                            )
                                        )

                                        val intent =
                                            Intent(context, ProximityService::class.java)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            context.startForegroundService(intent)
                                        } else {
                                            context.startService(intent)
                                        }
                                    } else {
                                        context.stopService(
                                            Intent(
                                                context,
                                                ProximityService::class.java
                                            )
                                        )
                                    }
                                }

                                Option.FacialDetection -> {
                                    facialEnabled = isChecked
                                    if (isChecked) {
                                        proximityEnabled = false
                                        cameraEnabled = false
                                        noneEnabled = false

                                        context.stopService(
                                            Intent(
                                                context,
                                                ProximityService::class.java
                                            )
                                        )
                                        context.stopService(
                                            Intent(
                                                context,
                                                CameraService::class.java
                                            )
                                        )

                                        val intent =
                                            Intent(context, FaceDetectionService::class.java)
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
                                        context.stopService(
                                            Intent(
                                                context,
                                                ProximityService::class.java
                                            )
                                        )

                                        val intent =
                                            Intent(context, CameraService::class.java)
                                        ContextCompat.startForegroundService(context, intent)
                                    } else {
                                        context.stopService(
                                            Intent(
                                                context,
                                                CameraService::class.java
                                            )
                                        )
                                    }
                                }

                                Option.None -> {
                                    noneEnabled = isChecked

                                    if (isChecked) {
                                        cameraEnabled = false
                                        proximityEnabled = false
                                        facialEnabled = false

                                        FaceDetectionService.stop(context)
                                        context.stopService(
                                            Intent(
                                                context,
                                                ProximityService::class.java
                                            )
                                        )
                                        context.stopService(
                                            Intent(
                                                context,
                                                CameraService::class.java
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        toggleVisible = true,
                        toggleEnabled = toggleEnabled,
                        option = option,
                        iconImage = iconImage,
                        onAddFaceClick = {
                            overlayVisible = option != Option.None
                        },
                        expandedOption = expandedOption,
                        onExpandChange = { selected ->
                            expandedOption = when {
                                selected == Option.None -> null
                                expandedOption == selected -> null
                                else -> selected
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



@Composable
fun LoginScreen(savedCode: String, onLoginSuccess: () -> Unit) {

    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    // ⚡ Trigger para o LaunchedEffect
    var loginTriggered by remember { mutableStateOf(false) }

    // ⛔ ESTE É O ÚNICO LUGAR ONDE PODE FICAR UM LaunchedEffect
    LaunchedEffect(loginTriggered) {
        if (loginTriggered) {
            // Mostra loading suave por pelo menos 400ms
            delay(400)
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("Mordomus Tavo", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(24.dp))

            Text(
                "Insira o código de acesso",
                color = colorResource(id = R.color.md_orange),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = code,
                onValueChange = { if (it.all(Char::isDigit)) code = it },
                label = { Text("Código", color = Color.LightGray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(id = R.color.md_orange),
                    cursorColor = colorResource(id = R.color.md_orange),
                    focusedLabelColor = colorResource(id = R.color.md_orange),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = colorResource(id = R.color.md_orange)
                        )
                    }
                }
            )

            Spacer(Modifier.height(30.dp))

            Button(
                onClick = {
                    if (code == savedCode) {
                        isLoading = true
                        loginTriggered = true   // ⚡ Agora o LaunchedEffect é ativado
                    } else {
                        error = "Código incorreto. Tente novamente."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.md_orange))
            ) {
                Text("Entrar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            AnimatedVisibility(error != null) {
                Text(
                    text = error ?: "",
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        AnimatedVisibility(isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .zIndex(10f),
                contentAlignment = Alignment.Center
            ) {
                AppLoading(size = 80.dp)
            }
        }
    }
}
