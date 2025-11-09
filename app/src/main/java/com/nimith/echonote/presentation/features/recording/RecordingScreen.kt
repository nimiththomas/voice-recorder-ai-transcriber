package com.nimith.echonote.presentation.features.recording

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.delay
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

    BackHandler(enabled = state.isRecording) {
        showStopRecordingDialog = true
    }

    RecordingContent(
        state = state,
        onStopRecording = viewModel::onStopRecording,
        onNavigateBack = {
            if (state.isRecording) {
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
    val pagerState = rememberPagerState(pageCount = { tabs.size }, initialPage = if (state.isRecording) 1 else 0)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.isRecording) {
        if (!state.isRecording) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(0)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (state.isRecording) {
                        if (state.isPaused) {
                            Text("Paused")
                        } else {
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
                        }
                    } else {
                        Text(
                            state.summary.split(" ").take(2).joinToString(" ")
                        )
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
            if (state.isRecording) {
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
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                indicator = {
                    Box(
                        Modifier
                            .tabIndicatorOffset(pagerState.currentPage)
                            .fillMaxWidth() // Fills full width of the *tab*
                            .height(3.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                            )
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(text = title) },
                        modifier = Modifier.weight(1f) // makes each tab take equal space
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
    if (state.isRecording) {
        SummaryLoading(state)
    } else {
        if (state.summary.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp), contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Too short to generate summary",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val summaryWords = state.summary.split(" ")
            val summaryTitle = summaryWords.take(2).joinToString(" ")
            val summaryContent = summaryWords.drop(2).joinToString(" ")

            var hasAnimated by rememberSaveable(state.summary) { mutableStateOf(false) }
            var displayedText by remember { mutableStateOf(if (hasAnimated) summaryContent else "") }

            LaunchedEffect(state.summary) {
                if (!hasAnimated) {
                    summaryContent.forEach { char ->
                        displayedText += char
                        delay(10)
                    }
                    hasAnimated = true
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                item {
                    Text(
                        text = summaryTitle,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                item {
                    Text(
                        text = displayedText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextColor
                    )
                }
            }
        }
    }
}


@Composable
fun TranscriptScreen(state: RecordingState) {
    if (state.transcripts.isEmpty()) {
        if (state.isRecording) {
            LiveTranscriptLoading(state)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp), contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No transcript found",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(state.transcripts) { transcript ->
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(
                        text = transcript.time,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DateColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = transcript.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextColor
                    )
                    HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
                }
            }
        }
    }
}

@Composable
fun LiveTranscriptLoading(state: RecordingState) {
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
            text = if (state.isPaused) "Recording Paused" else "Live Transcript",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = if (state.isPaused) "The recording is paused. Resume to continue." else "Your conversation will appear here with a 30-second delay. The Transcript updates automatically as you speak.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = DateColor
        )
    }
}

@Composable
fun SummaryLoading(state: RecordingState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = if (state.isPaused) "Recording Paused" else "Recording in progress...",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = if (state.isPaused) "The recording is paused. Resume to continue." else "The summary will be generated here after the recording is complete.",
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
        state = RecordingState(
            isRecording = false,
            summary = "This is a summary of the recording.",
            transcripts = listOf(
                Transcript("Roses, roses everywhere.", "12:14"),
                Transcript("Can I see my pretty flower right there?", "12:15")
            )
        ),
        onStopRecording = {},
        onNavigateBack = {}
    )
}

@Preview(showBackground = true)
@Composable
fun RecordingScreenLoadingPreview() {
    RecordingContent(
        state = RecordingState(isRecording = true, timer = "00:15"),
        onStopRecording = {},
        onNavigateBack = {}
    )
}

@Preview(showBackground = true)
@Composable
fun RecordingScreenPausedPreview() {
    RecordingContent(
        state = RecordingState(isRecording = true, isPaused = true, timer = "00:15"),
        onStopRecording = {},
        onNavigateBack = {}
    )
}
