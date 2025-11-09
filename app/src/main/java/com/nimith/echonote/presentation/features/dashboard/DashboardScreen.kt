package com.nimith.echonote.presentation.features.dashboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nimith.echonote.R
import com.nimith.echonote.ui.theme.Border
import com.nimith.echonote.ui.theme.DateColor
import com.nimith.echonote.ui.theme.TextColor

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToRecording: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPermissionDeniedDialogFor by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    val context = LocalContext.current
    val activity = LocalActivity.current

    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_PHONE_STATE
        )
    } else {
        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsMap ->
            val deniedPermissions = permissionsMap.filter { !it.value }.keys
            if (deniedPermissions.isEmpty()) {
                viewModel.onStartNewRecording()
                onNavigateToRecording()
            } else {
                showPermissionDeniedDialogFor = deniedPermissions.toList()
            }
        }
    )

    if (showPermissionDeniedDialogFor.isNotEmpty()) {
        val shouldShowRationale = showPermissionDeniedDialogFor.any {
            activity?.shouldShowRequestPermissionRationale(it) == true
        }

        PermissionDeniedDialog(
            onDismiss = { showPermissionDeniedDialogFor = emptyList() },
            onConfirm = {
                showPermissionDeniedDialogFor = emptyList()
                if (shouldShowRationale) {
                    permissionLauncher.launch(requiredPermissions)
                } else {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            },
            goToSettings = !shouldShowRationale,
            deniedPermissions = showPermissionDeniedDialogFor
        )
    }

    DashboardContent(
        state = state,
        onDeleteTranscript = viewModel::onDeleteTranscript,
        onCaptureNotesClick = {
            val permissionsToRequest = requiredPermissions.filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray()

            if (permissionsToRequest.isEmpty()) {
                viewModel.onStartNewRecording()
                onNavigateToRecording()
            } else {
                permissionLauncher.launch(permissionsToRequest)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    state: DashboardState,
    onDeleteTranscript: (String) -> Unit,
    onCaptureNotesClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(id = R.string.dashboard_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = TextColor
                )
            )
        },
        bottomBar = {
            Button(
                onClick = onCaptureNotesClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(id = R.string.capture_notes_button))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            state.transcripts.forEach { (date, transcripts) ->
                item {
                    Text(
                        text = date,
                        color = DateColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(transcripts) { transcript ->
                    TranscriptListItem(transcript, onDelete = {
                        onDeleteTranscript(transcript.id)
                    })
                }
            }
        }
    }
}

@Composable
fun PermissionDeniedDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    goToSettings: Boolean,
    deniedPermissions: List<String>
) {
    val permissionNames = deniedPermissions.joinToString {
        when (it) {
            Manifest.permission.RECORD_AUDIO -> "microphone"
            Manifest.permission.POST_NOTIFICATIONS -> "notifications"
            Manifest.permission.READ_PHONE_STATE -> "phone state"
            else -> it.substringAfterLast('.')
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.permission_denied_title)) },
        text = {
            Text(
                if (goToSettings) stringResource(
                    R.string.permission_denied_settings_message,
                    permissionNames
                )
                else stringResource(R.string.mandatory_permission_message, permissionNames)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    if (goToSettings) stringResource(R.string.go_to_settings)
                    else stringResource(R.string.grant_permission)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}


@Composable
fun TranscriptListItem(transcript: TranscriptItem, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { showMenu = true }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        color = Color.LightGray,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Article,
                    contentDescription = stringResource(id = R.string.transcript_icon_content_description),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(30),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Border)
            ) {
                Column(Modifier.padding(vertical = 12.dp, horizontal = 12.dp)) {
                    Text(
                        text = transcript.title,
                        color = TextColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                    Text(
                        text = "${transcript.time} • ${transcript.duration}",
                        color = DateColor,
                        fontSize = 14.sp,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
            }
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.delete_menu_item)) },
                onClick = {
                    showMenu = false
                    onDelete()
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardContentPreview() {
    val transcripts = mapOf(
        "10/07/2024" to listOf(
            TranscriptItem("1", "Brief Audio Fragment - Motivational", "10:38 AM", "52s"),
            TranscriptItem("2", "Lecture Summary", "11:30 AM", "30m")
        ),
        "09/07/2024" to listOf(
            TranscriptItem("3", "Brainstorming Session", "2:00 PM", "15m")
        )
    )
    DashboardContent(
        state = DashboardState(transcripts),
        onDeleteTranscript = {},
        onCaptureNotesClick = {}
    )

}
