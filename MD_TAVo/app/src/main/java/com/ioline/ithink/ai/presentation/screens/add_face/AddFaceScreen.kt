package com.ioline.ithink.ai.presentation.screens.add_face

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.ioline.ithink.ai.R
import com.ioline.ithink.ai.presentation.components.AppProgressDialog
import com.ioline.ithink.ai.presentation.components.hideProgressDialog
import com.ioline.ithink.ai.presentation.components.showProgressDialog
import org.koin.androidx.compose.koinViewModel
import androidx.activity.compose.BackHandler
import androidx.camera.core.ExperimentalGetImage
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ioline.ithink.ai.AppUtils
import com.ioline.ithink.ai.presentation.components.FaceDetectionService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFaceScreen(
    onNavigateBack: () -> Unit,
    cameraOpenState: MutableState<Boolean>
) {
    val viewModel: AddFaceScreenViewModel = koinViewModel()

    LaunchedEffect(Unit) {
        AppUtils.isAddUserFlowActive = true
    }

    DisposableEffect(Unit) {
        onDispose {
            AppUtils.isAddUserFlowActive = false
        }
    }

    // Limpa o estado apenas UMA vez ao abrir a tela
    LaunchedEffect(Unit) {
        viewModel.clearState()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Black,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.add_faces),
                            color = Color.White
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black
                    )
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                ScreenUI(viewModel,cameraOpenState)
                ImageReadProgressDialog(viewModel, onNavigateBack)
            }
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onPhotoCaptured: (Uri) -> Unit
) {
    val name = "photo_${System.currentTimeMillis()}.jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera")
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                output.savedUri?.let { uri -> onPhotoCaptured(uri) }
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(context, "Erro ao capturar a foto", Toast.LENGTH_SHORT).show()
            }
        }
    )
}
@Composable
fun CameraScreen(
    onPhotoCaptured: (uri: Uri) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
            .build()
    }

    LaunchedEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            // Liberta o que estiver a usar CameraX neste processo (incl. service)
            cameraProvider.unbindAll()

            // Binda só o que esta screen precisa
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        IconButton(
            onClick = { takePhoto(context, imageCapture, onPhotoCaptured) },
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.CenterEnd)
                .padding(end = 32.dp)
        ) {
            Icon(
                Icons.Default.Camera,
                contentDescription = "Take photo",
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenUI(viewModel: AddFaceScreenViewModel, cameraOpenState: MutableState<Boolean>) {


    val context = LocalContext.current
    var personName by viewModel.personNameState
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Launcher da galeria
    val pickVisualMediaLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia()
        ) {
            viewModel.selectedImageURIs.value = it
        }

    // Controle para exibir a tela da câmera frontal
    var showCamera by cameraOpenState

    LaunchedEffect(showCamera) {
        if (showCamera) FaceDetectionService.pauseCamera(context)
        else FaceDetectionService.resumeCamera(context)
    }

    // ✅ Quando a câmara está aberta, o BACK fecha a câmara (não navega para trás)
    BackHandler(enabled = showCamera) {
        showCamera = false
        FaceDetectionService.resumeCamera(context)

    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        val totalHeight = maxHeight

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
        ) {
            if (showCamera) {
                // CÂMARA ocupa o ecrã todo
                CameraScreen(
                    onPhotoCaptured = { uri ->
                        viewModel.selectedImageURIs.value =
                            viewModel.selectedImageURIs.value + uri
                        showCamera = false
                        FaceDetectionService.resumeCamera(context)

                    }
                )
            } else {
                val hasImages = viewModel.selectedImageURIs.value.isNotEmpty()
                val scope = rememberCoroutineScope()

                if (!hasImages) {
                    // SEM FOTOS: formulário ocupa o ecrã todo
                    AddFaceForm(
                        viewModel = viewModel,
                        personName = personName,
                        onPersonNameChange = { personName = it },
                        onPickPhotos = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            pickVisualMediaLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onOpenCamera = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            scope.launch {
                                FaceDetectionService.pauseCamera(context)
                                delay(350) // dá tempo para libertar a camera
                                showCamera = true
                            }
                        },
                        onAddUser = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.addImages()
                        }
                    )

                    // (não mostra grelha)
                } else {
                    // COM FOTOS: duas colunas lado a lado  | formulário | fotos |
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ESQUERDA: formulário
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            AddFaceForm(
                                viewModel = viewModel,
                                personName = personName,
                                onPersonNameChange = { personName = it },
                                onPickPhotos = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    pickVisualMediaLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onOpenCamera = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    scope.launch {
                                        FaceDetectionService.pauseCamera(context)
                                        delay(350) // dá tempo para libertar a camera
                                        showCamera = true
                                    }
                                },
                                onAddUser = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    viewModel.addImages()
                                }
                            )
                        }

                        // DIREITA: fotos
                        ImagesGrid(
                            viewModel = viewModel,
                            columns = 3, // 👈 menos colunas porque está estreito
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddFaceForm(
    viewModel: AddFaceScreenViewModel,
    personName: String,
    onPersonNameChange: (String) -> Unit,
    onPickPhotos: () -> Unit,
    onOpenCamera: () -> Unit,
    onAddUser: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Gray, RoundedCornerShape(16.dp))
            .background(Color(0xFF151515), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = personName,
            onValueChange = onPersonNameChange,
            label = {
                Text(
                    text = stringResource(R.string.enter_the_person_s_name)
                )
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Black,
                unfocusedContainerColor = Color.Black,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = colorResource(id = R.color.md_orange),
                unfocusedIndicatorColor = Color.Gray,
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.LightGray,
                cursorColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        val canInteract = personName.isNotEmpty()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                enabled = canInteract,
                onClick = onPickPhotos,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.md_orange),
                    contentColor = Color.Black,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.LightGray
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Photo,
                    contentDescription = stringResource(R.string.choose_photos)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.choose_photos))
            }

            Button(
                enabled = canInteract,
                onClick = onOpenCamera,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.md_orange),
                    contentColor = Color.Black,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.LightGray
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = stringResource(R.string.take_photo)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.take_photo))
            }
        }

        val hasImages = viewModel.selectedImageURIs.value.isNotEmpty()
        if (hasImages) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddUser,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.md_orange),
                    contentColor = Color.Black,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.LightGray
                )
            ) {
                Text(text = stringResource(id = R.string.UserAdded))
            }
        }
    }
}


@Composable
private fun ImagesGrid(
    viewModel: AddFaceScreenViewModel,
    columns: Int,
    modifier: Modifier = Modifier
) {
    val uris by viewModel.selectedImageURIs

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = true
    ) {
        items(uris) { uri ->
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(2.dp)
            )
        }
    }
}

@Composable
private fun ImageReadProgressDialog(
    viewModel: AddFaceScreenViewModel,
    onNavigateBack: () -> Unit,
) {
    val isProcessing by viewModel.isProcessingImages
    val numImagesProcessed by viewModel.numImagesProcessed
    val context = LocalContext.current

    AppProgressDialog()

    if (isProcessing) {
        showProgressDialog()
    } else {
        if (numImagesProcessed > 0) {
            viewModel.clearState()
            Toast.makeText(
                context,
                stringResource(id = R.string.save_user),
                Toast.LENGTH_SHORT
            ).show()
            onNavigateBack()
        }
        hideProgressDialog()
    }
}
