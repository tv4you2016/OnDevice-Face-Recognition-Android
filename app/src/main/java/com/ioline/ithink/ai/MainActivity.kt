package com.ioline.ithink.ai
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ioline.ai.PermissionManager
import com.ioline.ithink.ai.layout.MainLayout
import androidx.core.content.edit
import com.ioline.ithink.ai.presentation.components.AppLoading
import com.ioline.ithink.ai.presentation.screens.face_list.FaceListScreen

class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WakeLock().unlockScreen(this@MainActivity)

        permissionManager = PermissionManager(this) {

            setContent {


                // Estado de loading movido para o nível de setContent (global para o app)
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "main_layout"
                ) {
                    composable("main_layout") {
                        MainLayout(
                            navController = navController
                        )
                    }
                }






            }

        }

        permissionManager.requestAll()
    }
}



