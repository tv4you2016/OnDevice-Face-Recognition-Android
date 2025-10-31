package com.ioline.ithink.ai

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.ioline.ai.PermissionManager
import com.ioline.ithink.ai.layout.MainLayout
import com.ioline.ithink.ai.presentation.screens.add_face.AddFaceScreen
import com.ioline.ithink.ai.presentation.screens.detect_screen.DetectScreen
import com.ioline.ithink.ai.presentation.screens.face_list.FaceListScreen

class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WakeLock().unlockScreen(this@MainActivity)

        // Inicializa PermissionManager com callback
        permissionManager = PermissionManager(this) {
            setContent {

                FaceListScreen(
                    onAddFaceClick = {
                    }
                )

                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "main_layout") {
                    composable("main_layout") {
                        MainLayout(navController)
                    }

                }

            }



        }
        // Come�a o fluxo de permiss�es
        permissionManager.requestAll()
    }
}
