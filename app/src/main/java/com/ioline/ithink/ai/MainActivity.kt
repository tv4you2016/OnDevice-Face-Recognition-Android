package com.ioline.ithink.ai
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ioline.ai.PermissionManager
import com.ioline.ithink.ai.layout.MainLayout
class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WakeLock().unlockScreen(this@MainActivity)

        permissionManager = PermissionManager(this) {


            AppUtils.openTargetApp(applicationContext, true)


            setContent {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "main_layout"
                ) {
                    composable("main_layout") {
                        MainLayout()
                    }
                }







            }

        }

        permissionManager.requestAll()

    }
}



