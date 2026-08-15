package com.kastlg.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kastlg.app.di.AppContainer
import com.kastlg.app.presentation.KastLgApp
import com.kastlg.app.presentation.theme.KastLgTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContainer.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            var ready by remember { mutableStateOf(false) }

            // Wait for the TMDB repository (token read from DataStore) to be ready
            // before showing the app, avoiding the "TMDB not initialized" crash.
            LaunchedEffect(Unit) {
                AppContainer.initializeTmdbRepository()
                ready = true
            }

            if (ready) {
                KastLgTheme {
                    KastLgApp()
                }
            } else {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) { }
            }
        }
    }
}
