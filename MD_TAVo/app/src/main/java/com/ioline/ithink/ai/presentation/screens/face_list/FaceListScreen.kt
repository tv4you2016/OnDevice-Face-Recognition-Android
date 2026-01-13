package com.ioline.ithink.ai.presentation.screens.face_list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioline.ithink.ai.R
import com.ioline.ithink.ai.data.PersonRecord
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceListScreen(
    onAddFaceClick: () -> Unit
) {
    val viewModel: FaceListScreenViewModel = koinViewModel()

    val faces by viewModel.personFlow.collectAsState(emptyList())
    val userCount = faces.size

    val userListTitle = pluralStringResource(
        id = R.plurals.user_list_title,
        count = userCount,
        userCount
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        // Botão principal: Adicionar utilizador
        AddFaceHeader(onAddFaceClick)

        Spacer(Modifier.height(12.dp))

        // Container da lista
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF666666), RoundedCornerShape(16.dp))
                .background(Color(0xFF151515), RoundedCornerShape(16.dp))
                .padding(vertical = 12.dp, horizontal = 12.dp),
        ) {
            Text(
                text = userListTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            ScreenUI(
                faces = faces,
                onRemoveFace = { id -> viewModel.removeFace(id) }
            )
        }
    }
}

@Composable
private fun AddFaceHeader(
    onAddFaceClick: () -> Unit
) {
    Button(
        onClick = onAddFaceClick,
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFff931e),
            contentColor = Color.Black,
            disabledContainerColor = Color.Gray,
            disabledContentColor = Color.LightGray
        )
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(id = R.string.add_faces)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(id = R.string.add_faces),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun EmptyFacesUI() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
            .padding(vertical = 16.dp, horizontal = 12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.no_faces),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ScreenUI(
    faces: List<PersonRecord>,
    onRemoveFace: (Long) -> Unit
) {
    val listState = rememberLazyListState()

    if (faces.isEmpty()) {
        EmptyFacesUI()
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 260.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(faces) { face ->
            FaceListItem(
                personRecord = face,
                onRemoveFaceClick = { onRemoveFace(face.personID) }
            )
        }
    }
}

@Composable
private fun FaceListItem(
    personRecord: PersonRecord,
    onRemoveFaceClick: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF222222),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                tint = Color(0xFFFF6B6B),
                modifier = Modifier
                    .size(24.dp)
                    .clickable { showDialog = true }
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Remover utilizador") },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.confirm_remove,
                        personRecord.personName
                    )
                )
            },


            //text = { Text("Tens a certeza que queres remover ${personRecord.personName}?") },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveFaceClick()
                    showDialog = false
                }) {
                    Text(stringResource(id= R.string.remove) , color = Color(0xFFFF6B6B))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(id= R.string.cancel), color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            textContentColor = Color.White
        )
    }
}
