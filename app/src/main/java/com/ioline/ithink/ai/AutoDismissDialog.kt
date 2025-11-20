package com.ioline.ithink.ai

import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun AutoDismissDialog(
    message: String,
    onDismiss: () -> Unit
) {
    var showDialog by remember { mutableStateOf(true) }

    // Auto-dismiss after 10 seconds
    LaunchedEffect(Unit) {
        delay(10_000L)
        showDialog = false
        onDismiss()
    }

    if (showDialog) {
        Dialog(onDismissRequest = {
            showDialog = false
            onDismiss()
        }) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .widthIn(min = 200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            showDialog = false
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.md_orange),
                            contentColor = androidx.compose.ui.graphics.Color.White // Texto branco
                        )
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}
