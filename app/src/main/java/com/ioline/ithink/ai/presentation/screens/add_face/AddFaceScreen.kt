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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFaceScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: AddFaceScreenViewModel = koinViewModel()

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
                    /*
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    */
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black
                    )
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                ScreenUI(viewModel)
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
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
            .build()
    }

    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenUI(viewModel: AddFaceScreenViewModel) {

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
    var showCamera by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

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
                CameraScreen(
                    onPhotoCaptured = { uri ->
                        viewModel.selectedImageURIs.value =
                            viewModel.selectedImageURIs.value + uri
                        showCamera = false
                    }
                )
            } else {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            color = if (isFocused) colorResource(id = R.color.md_orange) else Color.Gray,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(Color.Transparent, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text(text = "Enter the person's name") },
                    singleLine = true,
                    interactionSource = interactionSource,
                    colors = TextFieldDefaults.colors(
                        unfocusedLabelColor = Color.White,
                        focusedLabelColor = Color.White,
                        focusedContainerColor = Color.Black,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        unfocusedContainerColor = Color.Black,
                        unfocusedIndicatorColor = Color.White,
                        focusedIndicatorColor = Color.White,
                        cursorColor = Color.White
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, Color.Gray, RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    val canInteract = personName.isNotEmpty()

                    // Botão galeria
                    Button(
                        enabled = canInteract,
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            pickVisualMediaLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.md_orange),
                            contentColor = Color.White,
                            disabledContainerColor = Color.Gray,
                            disabledContentColor = Color.LightGray
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = "Choose photos"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Choose photos")
                    }

                    // Botão câmera frontal
                    Button(
                        enabled = canInteract,
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            showCamera = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.md_orange),
                            contentColor = Color.White,
                            disabledContainerColor = Color.Gray,
                            disabledContentColor = Color.LightGray
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Take Photo"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Take Photo")
                    }

                    // Botão adicionar imagens (sem animação extra)
                    val hasImages = viewModel.selectedImageURIs.value.isNotEmpty()
                    if (hasImages) {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                viewModel.addImages()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(id = R.color.md_orange),
                                contentColor = Color.White,
                                disabledContainerColor = Color.Gray,
                                disabledContentColor = Color.LightGray
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add photos"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(id = R.string.add_faces))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                ImagesGrid(
                    viewModel = viewModel,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ImagesGrid(viewModel: AddFaceScreenViewModel, modifier: Modifier = Modifier) {
    val uris by viewModel.selectedImageURIs

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
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
                stringResource(id = R.string.add_facesOK),
                Toast.LENGTH_SHORT
            ).show()
            onNavigateBack()
        }
        hideProgressDialog()
    }
}
