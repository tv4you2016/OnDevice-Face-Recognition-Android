package com.ioline.ithink.ai.layout



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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.ioline.ithink.ai.ProximityService
import com.ioline.ithink.ai.presentation.components.FaceDetectionService
import com.ioline.aicamera.utils.AppUtils.openTargetApp
import com.ioline.ithink.ai.R
import com.ioline.ithink.ai.presentation.components.AppLoading
import com.ioline.ithink.ai.presentation.screens.add_face.AddFaceScreen
import com.ioline.ithink.ai.presentation.screens.face_list.FaceListScreen
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
                    title = { Text("MordomusTABMNG",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF000000), // ✅ fundo preto
                    )
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
                    SettingItem(
                        title = "Proximity",
                        description = null,
                        enabled = proximityEnabled,
                        onToggleChange = { isChecked ->
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
                        },
                        toggleVisible = true,
                        toggleEnabled = true,
                        option = Option.ProximityDetection,
                        iconImage = Icons.Default.Sensors,
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
    expandedOption: Option?,                // estado global vindo do MainLayout
    onExpandChange: (Option?) -> Unit       // função para alterar o expandido
) {
    val isExpanded = expandedOption == option
    var showFaceList by remember { mutableStateOf(false) }

    // 👇 Cores dinâmicas
    val dynamicBorderColor by animateColorAsState(
        targetValue = when {
            isExpanded -> Color(0xFFFF9800) // laranja quando expandido
           // enabled -> Color(0xFFFF9800) // azul quando ativo
            else ->  Color.Gray // azul quando ativo
        },
        label = "borderColorAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(cornerRadius)
            )
            .background(Color(0xFF000000) , RoundedCornerShape(cornerRadius))
            .border(borderWidth, dynamicBorderColor, RoundedCornerShape(cornerRadius))
            .clickable {
                // 👇 Aqui está a correção principal:
                if (isExpanded) {
                    onExpandChange(null) // fecha se já estava aberto
                } else {
                    onExpandChange(option) // abre este e fecha os outros
                    if (option == Option.FacialDetection) {
                        showFaceList = true
                    }
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = iconImage,
                    contentDescription = "Open Face List",
                    modifier = Modifier.padding(end = 16.dp),
                    tint  = Color.White
                )
                Text(
                    text = title,
                    //fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    color = Color.White
                )
            }

            if (toggleVisible && onToggleChange != null) {
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggleChange,
                    enabled = toggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor  = Color(0xFFff931e),      // Cor do "botão" quando ativado
                        checkedThumbColor =Color.White,      // Cor da faixa quando ativado
                        uncheckedThumbColor= Color.White,           // Cor do botão quando desativado
                        uncheckedTrackColor= Color(0xFF808080),      // Cor da faixa quando desativado

                        //disabledCheckedThumbColor = Color.DarkGray, // Cor quando desativado + ativo
                        //disabledUncheckedThumbColor = Color.Gray
                    )


                )
            } else if (toggleVisible) {
                Switch(
                    checked = enabled,
                    onCheckedChange = null,
                    enabled = toggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFFF9800),      // Cor do "botão" quando ativado
                        checkedTrackColor = Color(0xFFFFC107),      // Cor da faixa quando ativado
                        uncheckedThumbColor = Color.Gray,           // Cor do botão quando desativado
                        uncheckedTrackColor = Color.LightGray,      // Cor da faixa quando desativado
                        disabledCheckedThumbColor = Color.DarkGray, // Cor quando desativado + ativo
                        disabledUncheckedThumbColor = Color.Gray
                    )
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            val screenHeight = LocalConfiguration.current.screenHeightDp.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .heightIn(max = screenHeight * 0.5f)
            ) {
                if (showFaceList) {
                    FaceListScreen(
                        onAddFaceClick = {
                            showFaceList = true
                            onAddFaceClick()
                        }
                    )
                } else {
                    Text(
                        text = description ?: "Tap to view details",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun FullscreenOverlay(onDismiss: () -> Unit) {
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
                AddFaceScreen(onNavigateBack = onDismiss)
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
