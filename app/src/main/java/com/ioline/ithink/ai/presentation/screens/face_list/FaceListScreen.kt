package com.ioline.ithink.ai.presentation.screens.face_list


import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioline.ithink.ai.R
import com.ioline.ithink.ai.data.PersonRecord
import com.ioline.ithink.ai.presentation.components.AppAlertDialog
import com.ioline.ithink.ai.presentation.components.DelayedVisibility
import com.ioline.ithink.ai.presentation.components.createAlertDialog
import com.ioline.ithink.ai.presentation.screens.detect_screen.DetectScreenViewModel
import com.ioline.ithink.ai.presentation.theme.FaceNetAndroidTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceListScreen(onAddFaceClick: (() -> Unit),
) {
    FaceNetAndroidTheme {
        Scaffold(
            containerColor = Color(0xFF000000), // 👈 fundo preto
            modifier = Modifier.fillMaxSize(),
            floatingActionButton = {
                FloatingActionButton(onClick = onAddFaceClick) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add a new face")
                }
            },
        ) { innerPadding ->
            val viewModel: FaceListScreenViewModel = koinViewModel()
            Column(modifier = Modifier.padding(innerPadding)) {
                ScreenUI(viewModel)
                //AppAlertDialog()
            }
        }
    }
}

@Composable
private fun ScreenUI(viewModel: FaceListScreenViewModel) {
    val faces by viewModel.personFlow.collectAsState(emptyList())

    if (faces.isEmpty()) {
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

    LazyColumn { items(faces) {FaceListItem(it) { viewModel.removeFace(it.personID)}}}


}

@Composable
private fun FaceListItem(
    personRecord: PersonRecord,
    onRemoveFaceClick: (() -> Unit),
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF000000))
            .border(4.dp, Color.Gray, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f) // ocupa o espaço disponível
        ) {
            Text(
                text = personRecord.personName,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp,
                maxLines = 1,
                color = Color.White
            )
        }

        Icon(
            modifier = Modifier.clickable {
                createAlertDialog(
                    dialogTitle = "Remove person",
                    dialogText = "Are you sure to remove this person from the database? " +
                            "The face for this person will not be detected in realtime.",
                    dialogPositiveButtonText = "Remove",
                    onPositiveButtonClick = onRemoveFaceClick,
                    dialogNegativeButtonText = "Cancel",
                    onNegativeButtonClick = {}
                )
            },
            imageVector = Icons.Default.Clear,
            contentDescription = "Remove face",
            tint = Color.White
        )

        Spacer(modifier = Modifier.width(8.dp)) // espaço entre ícone e borda direita
    }

}
