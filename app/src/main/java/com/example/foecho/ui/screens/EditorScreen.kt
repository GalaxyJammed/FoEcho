package com.example.foecho.ui.screens

import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.foecho.data.AudioInfo
import com.example.foecho.data.EditedSegment
import com.example.foecho.data.exportSegment
import com.example.foecho.data.loadAudioInfo
import com.example.foecho.data.loadEdits
import com.example.foecho.data.saveEdits
import com.example.foecho.ui.components.ScreenHeader
import com.example.foecho.ui.components.ShieldCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

val LocalEditRequest = compositionLocalOf<(String, String, Long) -> Unit> { { _, _, _ -> } }

private val TealAccent = Color(0xFF2EC4B0)
private val MinPartMs = 1000L

private enum class Side { Left, Right }

@Composable
fun EditorScreen(uri: String, name: String, dateCreated: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioUri = remember(uri) { Uri.parse(uri) }
    val sourceName = remember(name) { name.substringBeforeLast('.') }

    var info by remember { mutableStateOf<AudioInfo?>(null) }
    LaunchedEffect(uri) { info = loadAudioInfo(context, audioUri) }
    val total = info?.durationMs ?: 0L

    var allEdits by remember { mutableStateOf(loadEdits(context)) }
    val segments = allEdits.filter { it.sourceUri == uri }

    var playhead by remember { mutableLongStateOf(0L) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    var segPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var segPlayingId by remember { mutableStateOf<String?>(null) }
    var pendingSide by remember { mutableStateOf<Side?>(null) }
    var partName by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var widthPx by remember { mutableFloatStateOf(1f) }

    fun stopMain() {
        player?.release()
        player = null
        playing = false
    }

    fun stopSeg() {
        segPlayer?.release()
        segPlayer = null
        segPlayingId = null
    }

    fun stopAll() {
        stopMain()
        stopSeg()
    }

    fun seekTo(ms: Long) {
        val clamped = ms.coerceIn(0L, total)
        playhead = clamped
        player?.seekTo(clamped.toInt())
    }

    fun togglePlay() {
        stopSeg()
        val current = player
        if (current != null) {
            if (playing) {
                current.pause()
                playing = false
            } else {
                current.start()
                playing = true
            }
            return
        }
        val p = MediaPlayer()
        runCatching {
            p.setDataSource(context, audioUri)
            p.prepare()
            p.setOnCompletionListener {
                playhead = 0L
                stopMain()
            }
            p.seekTo(playhead.toInt())
            p.start()
            player = p
            playing = true
        }.onFailure {
            p.release()
        }
    }

    fun toggleSegment(seg: EditedSegment) {
        if (segPlayingId == seg.id) {
            stopSeg()
            return
        }
        stopAll()
        val p = MediaPlayer()
        runCatching {
            p.setDataSource(context, Uri.parse(seg.outputUri))
            p.prepare()
            p.setOnCompletionListener { stopSeg() }
            p.start()
            segPlayer = p
            segPlayingId = seg.id
        }.onFailure {
            p.release()
            Toast.makeText(context, "Couldn't play this part", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(playing) {
        while (playing) {
            player?.let { playhead = it.currentPosition.toLong() }
            delay(100)
        }
    }

    DisposableEffect(Unit) {
        onDispose { stopAll() }
    }

    val leftOk = playhead >= MinPartMs
    val rightOk = total - playhead >= MinPartMs

    pendingSide?.let { side ->
        val (start, end) = if (side == Side.Left) 0L to playhead else playhead to total
        AlertDialog(
            onDismissRequest = { if (!saving) pendingSide = null },
            title = { Text("Name this part") },
            text = {
                Column {
                    Text(
                        "${formatTime(start)} - ${formatTime(end)} (${formatTime(end - start)})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = partName,
                        onValueChange = { partName = it },
                        label = { Text("Part name") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = partName.isNotBlank() && !saving,
                    onClick = {
                        val part = partName.trim()
                        saving = true
                        scope.launch {
                            val out = withContext(Dispatchers.IO) {
                                exportSegment(context, audioUri, sourceName, part, start, end)
                            }
                            saving = false
                            pendingSide = null
                            if (out == null) {
                                Toast.makeText(context, "Couldn't save this part", Toast.LENGTH_SHORT).show()
                            } else {
                                val seg = EditedSegment(
                                    id = UUID.randomUUID().toString(),
                                    sourceUri = uri,
                                    sourceName = sourceName,
                                    partName = part,
                                    startMs = start,
                                    endMs = end,
                                    outputUri = out
                                )
                                allEdits = allEdits + seg
                                saveEdits(context, allEdits)
                                Toast.makeText(context, "Saved $part", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSide = null }, enabled = !saving) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = sourceName,
            navIcon = Icons.AutoMirrored.Filled.ArrowBack,
            navDescription = "Back",
            onNavClick = {
                stopAll()
                onBack()
            }
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                ShieldCard {
                    Column(Modifier.padding(16.dp)) {
                        Text("Length: ${formatTime(total)}", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Created: ${formatDate(dateCreated)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                ShieldCard {
                    Column(Modifier.padding(16.dp)) {
                        val bars = info?.bars
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .onSizeChanged { widthPx = it.width.toFloat() }
                                .pointerInput(total) {
                                    detectTapGestures { offset ->
                                        if (total > 0L) seekTo((offset.x / widthPx * total).toLong())
                                    }
                                }
                        ) {
                            if (bars == null) {
                                Text(
                                    "Loading waveform...",
                                    modifier = Modifier.align(Alignment.Center),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                Canvas(Modifier.fillMaxSize()) {
                                    val barW = size.width / bars.size
                                    val mid = size.height / 2f
                                    val split = if (total > 0L) size.width * playhead / total else 0f
                                    val idle = Color.Gray.copy(alpha = 0.5f)
                                    bars.forEachIndexed { i, amp ->
                                        val half = (amp * size.height * 0.45f).coerceAtLeast(2f)
                                        val x = i * barW + barW / 2f
                                        drawLine(
                                            color = if (x <= split) TealAccent else idle,
                                            start = Offset(x, mid - half),
                                            end = Offset(x, mid + half),
                                            strokeWidth = barW * 0.5f,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                    drawLine(
                                        color = Color.White,
                                        start = Offset(split, 0f),
                                        end = Offset(split, size.height),
                                        strokeWidth = 3.dp.toPx()
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { togglePlay() }) {
                                Icon(
                                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (playing) "Pause" else "Play"
                                )
                            }
                            Text(
                                "${formatTime(playhead)} / ${formatTime(total)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tap the waveform to place the line, then split at it.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    pendingSide = Side.Left
                                    partName = "Part ${segments.size + 1}"
                                },
                                enabled = leftOk && !saving && total > 0L,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Left part")
                            }
                            Button(
                                onClick = {
                                    pendingSide = Side.Right
                                    partName = "Part ${segments.size + 1}"
                                },
                                enabled = rightOk && !saving && total > 0L,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Right part")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Categories",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                if (segments.isEmpty()) {
                    Text("No parts yet.", style = MaterialTheme.typography.bodyMedium)
                }
            }

            items(segments, key = { it.id }) { seg ->
                ShieldCard {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { toggleSegment(seg) }) {
                            Icon(
                                imageVector = if (segPlayingId == seg.id) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (segPlayingId == seg.id) "Pause" else "Play"
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${seg.sourceName}: ${seg.partName}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                formatTime(seg.endMs - seg.startMs),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun formatDate(ms: Long): String =
    if (ms <= 0L) "Unknown" else SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date(ms))