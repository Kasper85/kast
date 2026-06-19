package com.kastlg.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.kastlg.app.di.AppContainer
import com.kastlg.app.presentation.KastLgApp
import com.kastlg.app.presentation.theme.KastLgTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContainer.initialize(applicationContext)
        lifecycleScope.launch {
            AppContainer.initializeTmdbRepository()
        }
        enableEdgeToEdge()
        setContent {
            KastLgTheme {
                KastLgApp()
            }
        }
    }
}
