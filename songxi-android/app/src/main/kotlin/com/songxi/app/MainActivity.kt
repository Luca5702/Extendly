package com.songxi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.songxi.app.core.designsystem.LocalSongxiSkin
import com.songxi.app.core.designsystem.SongxiTheme

// M1 scaffold stub — M5 wires SongxiAppContainer, SplashAdScreen gate, and full 5-tab NavHost.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SongxiTheme {
                val skin = LocalSongxiSkin.current
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(skin.backgroundColor)
                        .testTag("home_screen"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "松息 · 一松一息，身自舒展",
                        color = skin.inkPrimaryColor
                    )
                }
            }
        }
    }
}
