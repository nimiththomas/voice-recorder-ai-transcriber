package com.nimith.echonote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.nimith.echonote.presentation.navigation.EchoNoteNavGraph
import com.nimith.echonote.ui.theme.EchonoteTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EchonoteTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    EchoNoteNavGraph()
                }
            }
        }
    }
}
