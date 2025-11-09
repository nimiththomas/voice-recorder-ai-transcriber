package com.nimith.echonote.presentation.features.recording

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nimith.echonote.R
import com.nimith.echonote.ui.theme.DateColor
import com.nimith.echonote.ui.theme.TextColor
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showStopRecordingDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onStart()
        viewModel.action.collectLatest { action ->
            when (action) {
                is RecordingAction.Service -> {
                    Intent(context, RecordingService::class.java).also { intent ->
                        context.startService(intent.apply { this.action = action.action })
                    }
                }
            }
        }
    }

    if (showStopRecordingDialog) {
        AlertDialog(
            onDismissRequest = { showStopRecordingDialog = false },
            title = { Text(stringResource(R.string.stop_recording_dialog_title)) },
            text = { Text(stringResource(R.string.stop_recording_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showStopRecordingDialog = false
                    viewModel.onStopRecording()
                    onNavigateBack()
                }) {
                    Text(stringResource(R.string.stop_recording_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopRecordingDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    BackHandler(enabled = state.isLoading) {
        showStopRecordingDialog = true
    }

    RecordingContent(
        state = state,
        onStopRecording = viewModel::onStopRecording,
        onNavigateBack = {
            if (state.isLoading) {
                showStopRecordingDialog = true
            } else {
                onNavigateBack()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecordingContent(
    state: RecordingState,
    onStopRecording: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val tabs = listOf("Summary", "Transcript")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(0)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (state.isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Circle,
                                contentDescription = "Recording",
                                tint = Color.Red,
                                modifier = Modifier.height(12.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(state.timer)
                        }
                    } else {
                        Text("Brief Poetic Expression...")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = TextColor
                )
            )
        },
        bottomBar = {
            if (state.isLoading) {
                Button(
                    onClick = onStopRecording,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(id = R.string.stop_recording_button))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(text = title) }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> SummaryScreen(state)
                    1 -> TranscriptScreen(state)
                }
            }
        }
    }
}

@Composable
fun SummaryScreen(state: RecordingState) {
    if (state.isLoading) {
        SummaryLoading()
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = "Poetic Expression",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            item {
                BulletPoint("Speaker makes lyrical statement about roses being everywhere")
                BulletPoint("Requests to see a \"pretty flower\" in specific location (\"right there\")")
                BulletPoint("Brief, romantic or appreciative tone regarding flowers/nature")
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Action Items",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "No specific action items identified from this brief poetic expression",
                    color = DateColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.padding(bottom = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Circle,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 8.dp, end = 8.dp)
                .height(6.dp),
            tint = TextColor
        )
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = TextColor)
    }
}

@Composable
fun TranscriptScreen(state: RecordingState) {
    if (state.isLoading) {
        LiveTranscriptLoading()
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Text(
                    "11:36",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextColor
                )
                Text(
                    "Roses, roses everywhere. Can I see my pretty flower right there?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextColor
                )
            }
        }
    }
}

@Composable
fun LiveTranscriptLoading() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Live Transcript",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Your conversation will appear here with a 30-second delay. The Transcript updates automatically as you speak.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = DateColor
        )
    }
}

@Composable
fun SummaryLoading() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Recording in progress...",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "The summary will be generated here after the recording is complete.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = DateColor
        )
    }
}


@Preview(showBackground = true)
@Composable
fun RecordingScreenPreview() {
    RecordingContent(
        state = RecordingState(isLoading = false),
        onStopRecording = {},
        onNavigateBack = {}
    )
}

@Preview(showBackground = true)
@Composable
fun RecordingScreenLoadingPreview() {
    RecordingContent(
        state = RecordingState(isLoading = true, timer = "00:15"),
        onStopRecording = {},
        onNavigateBack = {}
    )
}
