package com.ioline.ithink.ai.UpdateChecker


import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.ioline.ithink.ai.presentation.theme.FaceNetAndroidTheme
import kotlinx.coroutines.launch

class UpdaterActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apkUrl = intent.getStringExtra("apkUrl") ?: ""
        val latestVersionName = intent.getStringExtra("latestVersionName") ?: ""


        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        setContent {
            // Usa o teu theme aqui, se tiveres (ex: FaceNetAndroidTheme / MordomusTheme)
            FaceNetAndroidTheme() {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    UpdaterScreen(
                        apkUrl = apkUrl,
                        latestVersionName = latestVersionName,
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}
