package com.nimith.echonote.presentation.features.recording

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.nimith.echonote.presentation.common.Constants.ACTION_START
import com.nimith.echonote.presentation.common.Constants.ACTION_STOP

@Composable
fun RecordingScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Recording Screen")
        Button(onClick = {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            onBack()
        }) {
            Text("Stop Recording")
        }
    }
}