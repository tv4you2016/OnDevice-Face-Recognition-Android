package com.ioline.ithink.ai.presentation.screens.detect_screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ioline.ithink.ai.R
import com.ioline.ithink.ai.presentation.components.AppAlertDialog
import com.ioline.ithink.ai.presentation.components.DelayedVisibility
import com.ioline.ithink.ai.presentation.components.FaceDetectionService
import com.ioline.ithink.ai.presentation.components.createAlertDialog
import com.ioline.ithink.ai.presentation.theme.FaceNetAndroidTheme
import org.koin.androidx.compose.koinViewModel

private val cameraPermissionStatus = mutableStateOf(false)
private val cameraFacing = mutableIntStateOf(CameraSelector.LENS_FACING_BACK)
private lateinit var cameraPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectScreen(onOpenFaceListClick: (() -> Unit)) {
    FaceNetAndroidTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(),
                    title = {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    },
                    actions = {
                        IconButton(onClick = onOpenFaceListClick) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Open Face List",
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) { ScreenUI() }
        }
    }
}

@Composable
private fun ScreenUI() {
    val viewModel: DetectScreenViewModel = koinViewModel()
    Box {
        Camera(viewModel)
        DelayedVisibility(viewModel.getNumPeople() == 0L) {
            Text(
                text = stringResource(id = R.string.no_faces),
                color = Color.White,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(Color.Blue, RoundedCornerShape(16.dp))
                        .padding(8.dp),
                textAlign = TextAlign.Center,
            )
        }
        AppAlertDialog()
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun Camera(viewModel: DetectScreenViewModel) {
    val context = LocalContext.current
    cameraPermissionStatus.value =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

    cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                cameraPermissionStatus.value = true
            } else {
                camaraPermissionDialog()
            }
        }
/*
// interface visula original
    DelayedVisibility(cameraPermissionStatus.value) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { FaceDetectionOverlay(lifecycleOwner, context, viewModel) },
            update = { it.initializeCamera(cameraFacing) },
        )
    }
    */

    LaunchedEffect(Unit) {
        val intent = Intent(context, FaceDetectionService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    DelayedVisibility(!cameraPermissionStatus.value) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Allow Camera Permissions\nThe app cannot work without the camera permission.",
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(text = "Allow")
            }
        }
    }
}

private fun camaraPermissionDialog() {
    createAlertDialog(
        "Camera Permission",
        "The app couldn't function without the camera permission.",
        "ALLOW",
        "CLOSE",
        onPositiveButtonClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
        onNegativeButtonClick = {
            // TODO: Handle deny camera permission action
            //       close the app
        },
    )
}
