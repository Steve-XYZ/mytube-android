package com.mytube.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mytube.android.ui.AppViewModel
import com.mytube.android.ui.theme.MyTubeTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            MyTubeTheme(themeMode = uiState.themeMode) {
                MyTubeApp(
                    themeMode = uiState.themeMode,
                    onThemeModeSelected = viewModel::selectTheme,
                )
            }
        }
    }
}
