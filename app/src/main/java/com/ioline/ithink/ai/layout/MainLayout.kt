package com.ioline.ithink.ai.layout


import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.Save
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


// Definindo as opções disponíveis
enum class Option {
    FacialDetection,
    ProximityDetection,
    CameraDetection,
    None
}

val optionIcons = mapOf(
    Option.ProximityDetection to Icons.Default.Sensors,
    Option.FacialDetection to Icons.Default.FaceRetouchingNatural,
    Option.CameraDetection to Icons.Default.Face,
    Option.None to Icons.Default.Close
)

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Home", R.drawable.ic_mordomus_ithink),
    APPLY("Save",R.drawable.ic_save),
}


@Composable
fun MainLayout(navController: NavController) {

    val context = LocalContext.current

    var isAuthenticated by remember { mutableStateOf(true) } ///FALSE -> porque já nao querem o a pagina de login
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

            AppContent(context)
           //Box(Modifier.fillMaxSize().background(Color.Red))

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


@Composable
fun getProximitySensorInfo(context: Context): Triple<String?, String?, Float?> {
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    return if (proximitySensor != null) {
        Log.d("ProximityInfo", "Nome do sensor: ${proximitySensor.name}")
        Log.d("ProximityInfo", "Fabricante: ${proximitySensor.vendor}")
        Log.d("ProximityInfo", "Alcance máximo: ${proximitySensor.maximumRange}")

        Triple(
            proximitySensor.name,    // sensorName
            proximitySensor.vendor,  // sensorVendor
            proximitySensor.maximumRange  // sensorMaxRange
        )
    } else {
        Triple(null, null, null) // Sensor não disponível
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(context: Context) {

    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(false) }
    var pendingProximityStart by remember { mutableStateOf(false) }

    var overlayVisible by remember { mutableStateOf(false) }

    //var expandedOption by remember { mutableStateOf<Option?>(Option.FacialDetection) }
    var expandedOption by remember { mutableStateOf<Option?>(null) }

    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    val coroutineScope = rememberCoroutineScope()

    val listState = rememberLazyListState()


    val settingsOptions = listOf(
        Option.ProximityDetection,
        Option.FacialDetection,
        Option.CameraDetection,
        Option.None
    )


    val (proximityName, proximityVendor, proximityMaxRange) = getProximitySensorInfo(context)




    LaunchedEffect(pendingProximityStart) {
        if (pendingProximityStart) {
            withFrameNanos {}
            delay(120)

            val intent = Intent(context, ProximityService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }



  // ⬇ lê o sensor 1 única vez quando o layout inicializa
 //   LaunchedEffect(Unit) {
 //
 //   }


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
                                    delay(800)   // <-- coloca 800 ms ANTES do openTargetApp tempo para aparece o loading
                                    openTargetApp(context, true)
                                    isLoading = false
                                }
                            }
                            if (destination == AppDestinations.APPLY) {

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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "Detection Type:",
                            color = Color(0xFFff931e),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp)) // espaço entre o texto e o combo

                        DetectionTypeComboBox(
                            options = settingsOptions,
                            selected = expandedOption,
                            onSelect = { selectedOption ->
                                expandedOption = selectedOption
                                println("Item selecionado: ${selectedOption.name}")
                                // Aqui você pode disparar qualquer ação com o item selecionado
                                when (selectedOption) {
                                    Option.FacialDetection -> {
                                        // faz algo


                                    }
                                    Option.ProximityDetection -> {

                                        if (proximityName != "prox_stk3311" && proximityVendor != "sensortek") {
                                            // faz outra coisa, só inicia o serviço se quiser
                                            val intent = Intent(context, ProximityService::class.java)
                                            context.startForegroundService(intent)
                                        }

                                    }
                                    Option.CameraDetection -> {
                                        // etc
                                    }
                                    Option.None -> {}
                                }
                            },
                            onExpandChange = { currentItem, isExpanded ->
                                if (isExpanded) {
                                   // println("Dropdown abriu! Item atual: ${currentItem?.name}")
                                    // aqui você pode disparar qualquer ação com o item atual
                                }
                            }
                        )
                    }
                }


                // 👉 BLOCO EXPANDIDO (conteúdo dinamico)
                item {
                    AnimatedVisibility(expandedOption != null) {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 380.dp)  // 👈 altura máxima controlada
                                .animateContentSize()
                        ) {
                            when (expandedOption) {
                                Option.FacialDetection -> FaceListScreen(onAddFaceClick = { overlayVisible = true })
                                Option.ProximityDetection -> {
                                    if (proximityName != "prox_stk3311" && proximityVendor != "sensortek") {
                                        ProximitySensor(onAddFaceClick = { overlayVisible = true })
                                    }
                                }
                                Option.CameraDetection -> CameraSensor(onAddFaceClick = { overlayVisible = true })
                                Option.None -> {}
                                null -> {}
                            }
                        }

                    }
                }
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
