package com.example.foecho.ui.screens

import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.foecho.data.AudioInfo
import com.example.foecho.data.EditedSegment
import com.example.foecho.data.exportSegment
import com.example.foecho.data.loadAudioInfo
import com.example.foecho.data.loadEdits
import com.example.foecho.data.renameRecording
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
import kotlin.math.abs
import androidx.compose.material3.LinearProgressIndicator

val LocalEditRequest = compositionLocalOf<(String, String, Long) -> Unit> { { _, _, _ -> } }

private val TealAccent = Color(0xFF2EC4B0)
private val SeparatorYellow = Color(0xFFFFC107)
private val DangerRed = Color(0xFFD32F2F)
private val MinPartMs = 1000L
private const val MaxLines = 5

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
    var lines by remember { mutableStateOf<List<Long>>(emptyList()) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    var segPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var segPlayingId by remember { mutableStateOf<String?>(null) }
    var pendingRange by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    var partName by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var saveProgress by remember { mutableFloatStateOf(0f) }
    var renameTarget by remember { mutableStateOf<EditedSegment?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<EditedSegment?>(null) }
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

    val bounds = listOf(0L) + lines + listOf(total)
    val regions = bounds.zipWithNext()
    val canAddLine = total > 0L &&
            lines.size < MaxLines &&
            playhead >= MinPartMs &&
            total - playhead >= MinPartMs &&
            lines.none { abs(it - playhead) < MinPartMs }

    pendingRange?.takeIf { !saving }?.let { (start, end) ->
        AlertDialog(
            onDismissRequest = { if (!saving) pendingRange = null },
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
                        saveProgress = 0f
                        scope.launch {
                            val existing = allEdits.firstOrNull {
                                it.sourceUri == uri && it.startMs == start && it.endMs == end
                            }
                            val out = withContext(Dispatchers.IO) {
                                val created = exportSegment(
                                    context,
                                    audioUri,
                                    sourceName,
                                    part,
                                    start,
                                    end
                                ) { p -> scope.launch { saveProgress = p } }
                                if (created != null && existing != null) {
                                    runCatching {
                                        context.contentResolver.delete(Uri.parse(existing.outputUri), null, null)
                                    }
                                }
                                created
                            }
                            saving = false
                            pendingRange = null
                            if (out == null) {
                                Toast.makeText(context, "Couldn't save this part", Toast.LENGTH_SHORT).show()
                            } else {
                                val seg = EditedSegment(
                                    id = existing?.id ?: UUID.randomUUID().toString(),
                                    sourceUri = uri,
                                    sourceName = sourceName,
                                    partName = part,
                                    startMs = start,
                                    endMs = end,
                                    outputUri = out
                                )
                                allEdits = allEdits.filterNot { it.id == seg.id } + seg
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
                TextButton(onClick = { pendingRange = null }, enabled = !saving) {
                    Text("Cancel")
                }
            }
        )
    }
    if (saving) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Saving part") },
            text = {
                Column {
                    LinearProgressIndicator(
                        progress = { saveProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${(saveProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {}
        )
    }

    renameTarget?.let { seg ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename part") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Part name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = renameText.isNotBlank(),
                    onClick = {
                        val newPart = renameText.trim()
                        val renamed = renameRecording(
                            context,
                            Uri.parse(seg.outputUri),
                            "${seg.sourceName} - $newPart"
                        )
                        if (renamed) {
                            allEdits = allEdits.map {
                                if (it.id == seg.id) it.copy(partName = newPart) else it
                            }
                            saveEdits(context, allEdits)
                            Toast.makeText(context, "Renamed", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Couldn't rename this part", Toast.LENGTH_SHORT).show()
                        }
                        renameTarget = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    deleteTarget?.let { seg ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete part") },
            text = { Text("Delete \"${seg.partName}\" from your device? This can't be undone.") },
            confirmButton = {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = DangerRed),
                    onClick = {
                        deleteTarget = null
                        if (segPlayingId == seg.id) stopSeg()
                        scope.launch {
                            val deleted = withContext(Dispatchers.IO) {
                                runCatching {
                                    context.contentResolver.delete(Uri.parse(seg.outputUri), null, null) > 0
                                }.getOrDefault(false)
                            }
                            if (deleted) {
                                allEdits = allEdits.filterNot { it.id == seg.id }
                                saveEdits(context, allEdits)
                                Toast.makeText(context, "Part deleted", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Couldn't delete this part", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
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
                        Text(
                            "Tap the waveform to move the line. Add up to $MaxLines lines, then tap a part below to name and save it.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(10.dp))
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
                                    if (total > 0L) {
                                        lines.forEach { line ->
                                            val x = size.width * line / total
                                            drawLine(
                                                color = SeparatorYellow,
                                                start = Offset(x, 0f),
                                                end = Offset(x, size.height),
                                                strokeWidth = 2.dp.toPx()
                                            )
                                        }
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
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = { lines = (lines + playhead).sorted() },
                                enabled = canAddLine && !saving
                            ) {
                                Text("Add Line")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (total > 0L) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                regions.forEachIndexed { i, (start, end) ->
                                    val saved = segments.firstOrNull { it.startMs == start && it.endMs == end }
                                    val defaultName = "Part ${i + 1}"
                                    val shade = if (i % 2 == 0) {
                                        TealAccent.copy(alpha = 0.35f)
                                    } else {
                                        TealAccent.copy(alpha = 0.55f)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(shade)
                                            .clickable {
                                                if (end - start < MinPartMs) {
                                                    Toast.makeText(
                                                        context,
                                                        "This part is too short to save",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    partName = saved?.partName ?: defaultName
                                                    pendingRange = start to end
                                                }
                                            }
                                            .padding(horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = saved?.partName ?: defaultName,
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = formatTime(end - start),
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (lines.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                lines.forEachIndexed { i, line ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(
                                                if (i % 2 == 0) SeparatorYellow.copy(alpha = 0.25f)
                                                else SeparatorYellow.copy(alpha = 0.45f)
                                            )
                                            .clickable { seekTo(line) }
                                            .padding(horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Line ${i + 1}",
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = formatTime(line),
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (lines.isNotEmpty()) {
                            TextButton(
                                onClick = { lines = emptyList() },
                                colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                            ) {
                                Text("Clear lines")
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
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { toggleSegment(seg) }) {
                            Icon(
                                imageVector = if (segPlayingId == seg.id) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (segPlayingId == seg.id) "Pause" else "Play"
                            )
                        }
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
                        IconButton(onClick = {
                            renameText = seg.partName
                            renameTarget = seg
                        }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Rename part")
                        }
                        IconButton(onClick = { deleteTarget = seg }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete part",
                                tint = DangerRed
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