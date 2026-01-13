package com.ioline.ithink.ai.UpdateChecker


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ioline.ithink.ai.R
import com.ioline.ithink.ai.WakeLock

@Composable
fun UpdaterScreen(
    apkUrl: String,
    latestVersionName: String,
    autoUpdate: Boolean = false,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    // Acorda a tela se necessário
    WakeLock().wakeUpScreen(context)
    WakeLock().unlockScreen(context)


    val viewModel: UpdaterViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(latestVersionName) {
        viewModel.deleteAllRelatedDownloads()
        viewModel.deleteOldApksInPublicFolder()
        viewModel.deleteAnyApkInDownloadFolder()

        viewModel.setLatestVersionName(latestVersionName)

    }

    if (autoUpdate){
        LaunchedEffect(Unit) {
            viewModel.startUpdate(apkUrl)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF151515), shape = MaterialTheme.shapes.medium)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Mordomus Tavo",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${stringResource(id = R.string.update_available)}: ${uiState.latestVersionName}",
                color = Color.LightGray,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(24.dp))

            // Barra de progresso
            LinearProgressIndicator(
                progress = uiState.progress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = Color(0xFFff931e),
                trackColor = Color.DarkGray
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = uiState.statusText,
                color = Color.White,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.cancel))
                }

                Button(
                    onClick = { viewModel.startUpdate(apkUrl) },
                    enabled = uiState.buttonEnabled && !uiState.isDownloading,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFff931e),
                        contentColor = Color.Black,
                        disabledContainerColor = Color.Gray,
                        disabledContentColor = Color.LightGray
                    )
                ) {
                    Text(stringResource(R.string.update))
                }
            }
        }
    }
}
