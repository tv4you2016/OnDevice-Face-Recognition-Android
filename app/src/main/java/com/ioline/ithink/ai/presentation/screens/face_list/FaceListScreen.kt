package com.ioline.ithink.ai.presentation.screens.face_list

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioline.ithink.ai.AppUtils
import com.ioline.ithink.ai.R
import com.ioline.ithink.ai.data.PersonRecord
import com.ioline.ithink.ai.presentation.components.createAlertDialog
import com.ioline.ithink.ai.presentation.screens.add_face.AddFaceScreen
import com.ioline.ithink.ai.presentation.theme.FaceNetAndroidTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.annotation.Single



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceListScreen(onAddFaceClick: () -> Unit) {

    val viewModel: FaceListScreenViewModel = koinViewModel()

    FaceNetAndroidTheme {
        Scaffold(
            containerColor = Color.Black,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(Modifier.padding(top = 16.dp)) {

                AddFaceHeader(onAddFaceClick)

                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, Color.Gray, RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                ) {
                    Text(
                        text = "User List:",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                    )

                    ScreenUI(viewModel)   // PASSA O VIEWMODEL AQUI ✔
                }
            }
        }
    }
}


@Composable
private fun AddFaceHeader(onAddFaceClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.Gray, RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable { onAddFaceClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.add_faces),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(id = R.string.add_faces),
            tint = Color.White,
            modifier = Modifier
                .size(28.dp)
                .clickable { onAddFaceClick() }
        )
    }
}

@Composable
private fun EmptyFacesUI() {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))

    ) {
        Text(
            text = stringResource(id = R.string.no_faces),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                //.padding(horizontal = 16.dp, vertical = 8.dp)
                //.background(Color(0xFF1E88E5), RoundedCornerShape(16.dp))
                .padding(vertical = 12.dp, horizontal = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }



}

@Composable
private fun ScreenUI(viewModel: FaceListScreenViewModel) {
    val context = LocalContext.current

    val faces by viewModel.personFlow.collectAsState(emptyList())
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var scrollEnabled by remember { mutableStateOf(false) }


    // 👉 PARA O LOADING AQUI
    LaunchedEffect(faces) {
        AppUtils.stopLoading(context, "FacesLoaded")

    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 300.dp)
            .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { scrollEnabled = true },
                    onDragEnd = { scrollEnabled = false },
                    onDragCancel = { scrollEnabled = false },
                    onVerticalDrag = { change, dragAmount ->
                        if (scrollEnabled) {
                            coroutineScope.launch {
                                val i = listState.firstVisibleItemIndex
                                val o = listState.firstVisibleItemScrollOffset
                                listState.scrollToItem(i, o - dragAmount.toInt())
                            }
                        }
                        change.consume()
                    }
                )
            }
    ) {
        LazyColumn(
            state = listState,
            userScrollEnabled = false,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (faces.isEmpty()) {
                item { EmptyFacesUI() }
            } else {
                items(faces) { face ->
                    FaceListItem(
                        personRecord = face,
                        onRemoveFaceClick = { viewModel.removeFace(face.personID) }
                    )
                }
            }
        }
    }
}




@Composable
private fun FaceListItem(
    personRecord: PersonRecord,
    onRemoveFaceClick: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = personRecord.personName,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = "Remove ${personRecord.personName}",
            tint = Color.Red,
            modifier = Modifier
                .size(24.dp)
                .clickable { showDialog = true }

        )


    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Remove person") },
            text = { Text("Are you sure you want to remove ${personRecord.personName}?") },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveFaceClick()
                    showDialog = false
                }) {
                    Text("Remove", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            textContentColor = Color.White
        )
    }





}
