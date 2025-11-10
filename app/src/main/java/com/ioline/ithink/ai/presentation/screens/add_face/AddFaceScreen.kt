package com.ioline.ithink.ai.presentation.screens.add_face

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
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

@Composable
private fun ScreenUI(viewModel: AddFaceScreenViewModel) {

    val context = LocalContext.current

    val pickVisualMediaLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia(),
        ) {
            viewModel.selectedImageURIs.value = it
        }

    var personName by remember { viewModel.personNameState }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current



    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Atualiza o estado das imagens
            // Aqui você pode adicionar a URI da última foto tirada (veja observação abaixo)
            Toast.makeText(context, "Foto salva em DCIM/Camera 📸", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "A foto não foi tirada", Toast.LENGTH_SHORT).show()
        }
    }


    // ✅ Usa BoxWithConstraints para medir a altura disponível
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
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color.Gray, RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                value = personName,
                onValueChange = { personName = it },
                label = { Text(text = "Enter the person's name") },
                singleLine = true,
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
                Button(
                    enabled = viewModel.personNameState.value.isNotEmpty(),
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        pickVisualMediaLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.md_orange), // Cor de fundo do botão
                        contentColor = Color.White, // Cor do texto e ícones
                        disabledContainerColor = Color.Gray, // Cor de fundo quando desabilitado
                        disabledContentColor = Color.LightGray // Cor do texto e ícones quando desabilitado
                    )
                ) {
                    Icon(imageVector = Icons.Default.Photo, contentDescription = "Choose photos")
                    Text(text = "Choose photos")
                }

                Button(
                    enabled = viewModel.personNameState.value.isNotEmpty(),
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()

                        // 👉 Cria o arquivo em DCIM/Camera na hora do clique
                        val contentValues = android.content.ContentValues().apply {
                            put(
                                android.provider.MediaStore.Images.Media.DISPLAY_NAME,
                                "photo_${System.currentTimeMillis()}.jpg"
                            )
                            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera")
                        }

                        val photoUri = context.contentResolver.insert(
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                        )

                        // Cria o intent para abrir a câmera
                        val intent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri)

                        // 👇 Tenta forçar câmera frontal
                        intent.putExtra("android.intent.extras.CAMERA_FACING", 1)
                        intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
                        intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1)

                        // Lança a câmera
                        cameraLauncher.launch(intent)

                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.md_orange), // Cor de fundo do botão
                        contentColor = Color.White, // Cor do texto e ícones
                        disabledContainerColor = Color.Gray, // Cor de fundo quando desabilitado
                        disabledContentColor = Color.LightGray // Cor do texto e ícones quando desabilitado
                    )
                ) {
                    Icon(imageVector = Icons.Default.Photo, contentDescription = "Take Photo")
                    Text(text = "Take photos")
                }


                DelayedVisibility(viewModel.selectedImageURIs.value.isNotEmpty()) {
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.addImages()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.md_orange), // Cor de fundo do botão
                            contentColor = Color.White, // Cor do texto e ícones
                            disabledContainerColor = Color.Gray, // Cor de fundo quando desabilitado
                            disabledContentColor = Color.LightGray // Cor do texto e ícones quando desabilitado
                        )

                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add photos")
                        Text(text = stringResource(id = R.string.add_faces))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
/*
            DelayedVisibility(viewModel.selectedImageURIs.value.isNotEmpty()) {
                Text(
                    text = "${viewModel.selectedImageURIs.value.size} image(s) selected",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
*/
            // ✅ O grid ocupa o restante espaço
            ImagesGrid(
                viewModel = viewModel,
                modifier = Modifier
                    .weight(1f) // ocupa tudo o que sobra
                    .fillMaxWidth()
            )
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
