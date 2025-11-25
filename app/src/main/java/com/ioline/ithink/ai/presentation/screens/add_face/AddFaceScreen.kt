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
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ioline.ithink.ai.R
import com.ioline.ithink.ai.presentation.components.AppProgressDialog
import com.ioline.ithink.ai.presentation.components.DelayedVisibility
import com.ioline.ithink.ai.presentation.components.hideProgressDialog
import com.ioline.ithink.ai.presentation.components.showProgressDialog
import com.ioline.ithink.ai.presentation.theme.FaceNetAndroidTheme
import org.koin.androidx.compose.koinViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFaceScreen(
    onNavigateBack: () -> Unit
) {
    var overlayVisible by remember { mutableStateOf(true) }


    val viewModel: AddFaceScreenViewModel = koinViewModel()
    // 👇 Box raiz que envolve tudo
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // define o fundo preto
    ) {

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Black, // 👈 fundo preto

        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                ScreenUI(viewModel)
                ImageReadProgressDialog(viewModel, onNavigateBack)
            }
        }
    }

    // Se o overlay estiver visível, chama clearState
    if (overlayVisible) {
        viewModel.clearState() // Limpa o estado quando a sobreposição é visível
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
                .align(Alignment.CenterEnd)  // Centralizado verticalmente e à direita
                .padding(bottom = 32.dp)
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
    var personName by remember { viewModel.personNameState }
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

    // Conteúdo principal
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
            // Se showCamera estiver ativo, mostra CameraScreen
            if (showCamera) {
                CameraScreen(
                    onPhotoCaptured = { uri ->
                        viewModel.selectedImageURIs.value =
                            viewModel.selectedImageURIs.value + uri
                        showCamera = false
                    }
                )
            }  else {
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

                    //textStyle = LocalTextStyle.current.copy(color = Color.White),
                    colors = TextFieldDefaults.colors(
                        //setting the text field background when it is focused
                        unfocusedLabelColor =  Color.White,
                        focusedLabelColor =  Color.White,
                        focusedContainerColor = Color.Black,

                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,

                        //setting the text field background when it is unfocused or initial state
                        unfocusedContainerColor = Color.Black,
                        unfocusedIndicatorColor = Color.White,
                        focusedIndicatorColor = Color.White,
                        cursorColor = Color.White
                        //setting the text field background when it is disabled
                       // disabledContainerColor = Color.Green,
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
                    // Botão galeria
                    Button(
                        enabled = personName.isNotEmpty(),
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
                        Icon(imageVector = Icons.Default.Photo, contentDescription = "Choose photos")
                        Text(text = "Choose photos")
                    }

                    // Botão câmera frontal
                    Button(
                        enabled = personName.isNotEmpty(),
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
                        Icon(imageVector = Icons.Default.Photo, contentDescription = "Take Photo")
                        Text(text = "Take Photo")
                    }

                    // Botão adicionar imagens
                    DelayedVisibility(viewModel.selectedImageURIs.value.isNotEmpty()) {
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
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add photos")
                            Text(text = stringResource(id = R.string.add_faces))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Grid de imagens
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
    val uris by remember { viewModel.selectedImageURIs }

    // ✅ O grid agora é limitado ao espaço restante
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = true // pode deixar true (scroll só no grid se houver overflow)
    ) {
        items(uris) { uri ->
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier
                    .aspectRatio(1f) // mantém as imagens quadradas
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
    val isProcessing by remember { viewModel.isProcessingImages }
    val numImagesProcessed by remember { viewModel.numImagesProcessed }
    val context = LocalContext.current
    AppProgressDialog()
    if (isProcessing) {
        showProgressDialog()
    } else {

        if (numImagesProcessed > 0) {
            viewModel.clearState() // 👈 Adicione isto
            Toast.makeText(context, 	stringResource(id = R.string.add_facesOK), Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
        hideProgressDialog()
    }
}
