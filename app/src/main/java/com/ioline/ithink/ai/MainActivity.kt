package com.ioline.ithink.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ioline.ithink.ai.presentation.screens.add_face.AddFaceScreen
import com.ioline.ithink.ai.presentation.screens.detect_screen.DetectScreen
import com.ioline.ithink.ai.presentation.screens.face_list.FaceListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navHostController = rememberNavController()
            NavHost(
                navController = navHostController,
                startDestination = "detect",
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
            ) {
                composable("add-face") { AddFaceScreen { navHostController.navigateUp() } }
                composable("detect") { DetectScreen { navHostController.navigate("face-list") } }
                composable("face-list") {
                    FaceListScreen(
                        onNavigateBack = { navHostController.navigateUp() },
                        onAddFaceClick = { navHostController.navigate("add-face") },
                    )
                }
            }
        }
    }
}
