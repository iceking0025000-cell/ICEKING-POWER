package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainScreen
import com.example.ui.TransferViewModel
import com.example.ui.theme.ICEPOWERTheme
import com.example.ui.theme.IceDeepNavy

class MainActivity : ComponentActivity() {

    private val viewModel: TransferViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ICEPOWERTheme(darkTheme = true) {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = IceDeepNavy
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        state = state
                    )
                }
            }
        }
    }
}
