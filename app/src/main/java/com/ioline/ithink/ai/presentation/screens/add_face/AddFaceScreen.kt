package com.ioline.ithink.ai.presentation.screens.add_face

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Button
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ioline.ithink.ai.R
import com.ioline.ithink.ai.presentation.components.AppProgressDialog
import com.ioline.ithink.ai.presentation.components.DelayedVisibility
import com.ioline.ithink.ai.presentation.components.hideProgressDialog
import com.ioline.ithink.ai.presentation.components.showProgressDialog
import com.ioline.ithink.ai.presentation.theme.FaceNetAndroidTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFaceScreen(onNavigateBack: (() -> Unit)) {
    val viewModel: AddFaceScreenViewModel = koinViewModel()


    FaceNetAndroidTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "Add Faces", style = MaterialTheme.typography.headlineSmall)
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Navigate Back",
                            )

                            viewModel.clearState() // 👈 Adicione isto

                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                ScreenUI(viewModel)
                ImageReadProgressDialog(viewModel, onNavigateBack)
            }
        }
    }
}

@Composable
private fun ScreenUI(viewModel: AddFaceScreenViewModel) {
    val pickVisualMediaLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia(),
        ) {
            viewModel.selectedImageURIs.value = it
        }

    var personName by remember { viewModel.personNameState }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

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
                modifier = Modifier.fillMaxWidth(),
                value = personName,
                onValueChange = { personName = it },
                label = { Text(text = "Enter the person's name") },
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
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
                ) {
                    Icon(imageVector = Icons.Default.Photo, contentDescription = "Choose photos")
                    Text(text = "Choose photos")
                }

                DelayedVisibility(viewModel.selectedImageURIs.value.isNotEmpty()) {
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.addImages()
                        }
                    ) {
                        Text(text = stringResource(id = R.string.add_faces))
                    }
                }
            }

            DelayedVisibility(viewModel.selectedImageURIs.value.isNotEmpty()) {
                Text(
                    text = "${viewModel.selectedImageURIs.value.size} image(s) selected",
                    style = MaterialTheme.typography.labelSmall,
                )
            }

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
        userScrollEnabled = false // pode deixar true (scroll só no grid se houver overflow)
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
