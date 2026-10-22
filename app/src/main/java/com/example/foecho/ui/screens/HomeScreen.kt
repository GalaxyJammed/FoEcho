package com.example.foecho.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.foecho.data.AddedAudio
import com.example.foecho.data.AudioCategory
import com.example.foecho.data.SavedRecording
import com.example.foecho.data.SortOption
import com.example.foecho.data.VoiceMemo
import com.example.foecho.data.VoiceRecorder
import com.example.foecho.data.loadCategories
import com.example.foecho.data.loadDuration
import com.example.foecho.data.loadFavorites
import com.example.foecho.data.loadSort
import com.example.foecho.data.loadVoiceMemos
import com.example.foecho.data.loadWaveform
import com.example.foecho.data.renameRecording
import com.example.foecho.data.saveCategories
import com.example.foecho.data.saveFavorites
import com.example.foecho.data.saveSort
import com.example.foecho.ui.components.BarAction
import com.example.foecho.ui.components.FoEchoBottomBar
import com.example.foecho.ui.components.ScreenHeader
import com.example.foecho.ui.components.ShieldCard
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.IconButton
import com.example.foecho.data.EditedSegment
import com.example.foecho.data.loadEdits
import com.example.foecho.data.saveEdits
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.example.foecho.data.ReleaseInfo
import com.example.foecho.data.UpdateStatus
import com.example.foecho.data.checkForUpdate
import com.example.foecho.data.installedVersion
import com.example.foecho.data.isNewer
import com.example.foecho.data.notifyUpdate
import com.example.foecho.data.AppLock
import com.example.foecho.data.NotificationPermission
import com.example.foecho.data.rememberRelease
import com.example.foecho.data.startUpdateDownload
import com.example.foecho.data.InstallReady
import com.example.foecho.data.installApk
import com.example.foecho.data.FilePicker
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.compositionLocalOf
import com.example.foecho.data.MediaDelete
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.automirrored.filled.OpenInNew

private val TealAccent = Color(0xFF2EC4B0)
private val DangerRed = Color(0xFFD32F2F)
private val FavoriteYellow = Color(0xFFFFC107)

private sealed interface Tab {
    data object All : Tab
    data object Raw : Tab
    data object Edited : Tab
    data object Favorites : Tab
    data object Add : Tab
    data class User(val name: String) : Tab
}

private sealed interface Pick {
    data class New(val name: String) : Pick
    data class Into(val name: String) : Pick
}

private enum class Confirm { Remove, Delete }

private data class AudioItem(
    val key: String,
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val dateCreated: Long,
    val dateModified: Long,
    val sizeBytes: Long
)

private sealed interface ListRow {
    val rowKey: String

    data class Header(val title: String) : ListRow {
        override val rowKey: String get() = "header:$title"
    }

    data class Entry(val item: AudioItem) : ListRow {
        override val rowKey: String get() = item.key
    }
}

val LocalScreenActive = compositionLocalOf { true }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onSettingsClick: () -> Unit) {
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
    }
    var memos by remember { mutableStateOf<List<VoiceMemo>>(emptyList()) }
    var memosVersion by remember { mutableIntStateOf(0) }

    var categories by remember { mutableStateOf(loadCategories(context)) }
    var edits by remember { mutableStateOf(loadEdits(context)) }
    val editedIds = edits.map { it.outputId() }.toSet()
    var favorites by remember { mutableStateOf(loadFavorites(context)) }
    var sortOption by remember { mutableStateOf(loadSort(context)) }
    var showSort by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf<Tab>(Tab.All) }
    var showNameDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var pick by remember { mutableStateOf<Pick?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var selectedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingMemoDeletes by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    var pendingNotify by remember { mutableStateOf<ReleaseInfo?>(null) }

    LaunchedEffect(Unit) {
        InstallReady.load(context)
        if (UpdateStatus.checked) return@LaunchedEffect
        val release = checkForUpdate()
        UpdateStatus.checked = true
        if (release == null || !isNewer(release.version, installedVersion(context))) {
            return@LaunchedEffect
        }
        rememberRelease(context, release)
        UpdateStatus.latestVersion = release.version
        UpdateStatus.releaseUrl = release.url
        val activity = context as? Activity
        val needsPermission = Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        if (needsPermission && activity != null) {
            NotificationPermission.request(activity) { granted ->
                if (granted) notifyUpdate(context, release)
            }
        } else if (!needsPermission) {
            notifyUpdate(context, release)
        }
    }

    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocus = remember { FocusRequester() }

    var recording by remember { mutableStateOf(false) }
    var recordingPaused by remember { mutableStateOf(false) }
    var elapsedSec by remember { mutableStateOf(0L) }
    var pendingSave by remember { mutableStateOf<SavedRecording?>(null) }
    var saveName by remember { mutableStateOf("") }
    var showPlayWarning by remember { mutableStateOf(false) }
    var playingAny by remember { mutableStateOf(false) }
    var stopPlaybackSignal by remember { mutableIntStateOf(0) }
    val recorder = remember { VoiceRecorder(context) }

    var preciseMs by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }

    val menuProgress by animateFloatAsState(
        targetValue = if (menuOpen) 1f else 0f,
        animationSpec = tween(260),
        label = "menu"
    )
    val menuVisible = menuOpen || menuProgress > 0f

    val selectionMode = selectedKeys.isNotEmpty()
    val currentCategory = (tab as? Tab.User)?.let { t -> categories.firstOrNull { it.name == t.name } }
    val showAddFiles = tab == Tab.Add || tab is Tab.User

    val sourceItems = remember(memos, categories) {
        (memos.map { it.toItem() } + categories.flatMap { c -> c.audios.map { it.toItem() } })
            .distinctBy { it.key }
    }
    val allMemoItems = remember(memos, sortOption, preciseMs, searchQuery) {
        memos.map { it.toItem() }.applySort(sortOption, preciseMs).matching(searchQuery)
    }
    val favoriteItems = remember(sourceItems, favorites, sortOption, preciseMs, searchQuery) {
        sourceItems.filter { it.key in favorites }.applySort(sortOption, preciseMs).matching(searchQuery)
    }
    val userLists = remember(categories, sortOption, preciseMs, searchQuery) {
        categories.associate { c ->
            c.name to c.audios.map { it.toItem() }.applySort(sortOption, preciseMs).matching(searchQuery)
        }
    }
    val editedItems = remember(memos, editedIds, sortOption, preciseMs, searchQuery) {
        memos.filter { it.id.toString() in editedIds }
            .map { it.toItem() }
            .applySort(sortOption, preciseMs)
            .matching(searchQuery)
    }

    LaunchedEffect(granted, memosVersion) {
        if (granted) memos = loadVoiceMemos(context)
    }

    LaunchedEffect(sourceItems) {
        sourceItems.forEach { item ->
            if (item.key !in preciseMs) {
                val ms = loadDuration(context, item.uri, item.durationMs)
                preciseMs = preciseMs + (item.key to ms)
            }
        }
    }

    LaunchedEffect(searchOpen) {
        if (searchOpen) searchFocus.requestFocus()
    }

    LaunchedEffect(recording) {
        while (recording) {
            elapsedSec = recorder.elapsedMs / 1000
            delay(250)
        }
    }

    DisposableEffect(Unit) {
        onDispose { recorder.stop() }
    }

    BackHandler(enabled = selectionMode) { selectedKeys = emptySet() }
    BackHandler(enabled = menuOpen) { menuOpen = false }
    BackHandler(enabled = searchOpen) {
        searchOpen = false
        searchQuery = ""
    }

    val onPicked: (List<Uri>) -> Unit = { uris ->
        val target = pick
        pick = null
        if (uris.isNotEmpty() && target != null) {
            uris.forEach { persistAccess(context, it) }
            val picked = uris.map { toAddedAudio(context, it) }
            categories = when (target) {
                is Pick.New -> categories + AudioCategory(target.name, picked)
                is Pick.Into -> categories.map { category ->
                    if (category.name == target.name) {
                        category.copy(audios = (category.audios + picked).distinctBy { it.uri })
                    } else {
                        category
                    }
                }
            }
            saveCategories(context, categories)
            if (target is Pick.New) tab = Tab.User(target.name)
        }
    }

    val openAddFiles: () -> Unit = {
        when (val current = tab) {
            Tab.Add -> {
                newCategoryName = ""
                showNameDialog = true
            }
            is Tab.User -> {
                pick = Pick.Into(current.name)
                AppLock.skipNextStop()
                FilePicker.open(context as Activity, onPicked)
            }
            else -> Unit
        }
    }

    val beginRecording: () -> Unit = {
        if (recorder.start()) {
            elapsedSec = 0L
            recordingPaused = false
            recording = true
        } else {
            Toast.makeText(context, "Couldn't start recording", Toast.LENGTH_SHORT).show()
        }
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            beginRecording()
        } else {
            Toast.makeText(context, "Microphone permission is needed to record", Toast.LENGTH_SHORT).show()
        }
    }

    val checkAndStartRecording: () -> Unit = {
        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasMic) beginRecording() else recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val startRecording: () -> Unit = {
        if (!recording) {
            if (playingAny) showPlayWarning = true else checkAndStartRecording()
        }
    }

    val togglePause: () -> Unit = {
        if (recordingPaused) {
            if (recorder.resume()) recordingPaused = false
        } else {
            if (recorder.pause()) recordingPaused = true
        }
    }

    val stopRecording: () -> Unit = {
        val saved = recorder.stop()
        recording = false
        recordingPaused = false
        if (saved != null) {
            saveName = saved.baseName
            pendingSave = saved
        } else {
            Toast.makeText(context, "Recording was too short", Toast.LENGTH_SHORT).show()
        }
    }

    val closeSave: () -> Unit = {
        pendingSave = null
        memosVersion += 1
    }

    val toggleFavorites: () -> Unit = {
        val toAdd = selectedKeys - favorites
        val toRemove = selectedKeys intersect favorites
        favorites = (favorites - toRemove) + toAdd
        saveFavorites(context, favorites)
        val message = when {
            toAdd.isNotEmpty() && toRemove.isNotEmpty() ->
                "Added ${toAdd.size}, removed ${toRemove.size} from favorites"
            toAdd.isNotEmpty() -> "Added to favorites"
            else -> "Removed from favorites"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        selectedKeys = emptySet()
    }

    val removeSelected: () -> Unit = {
        val category = currentCategory
        if (category != null) {
            categories = categories.map { c ->
                if (c.name == category.name) {
                    c.copy(audios = c.audios.filterNot { it.uri in selectedKeys })
                } else {
                    c
                }
            }
            saveCategories(context, categories)
        }
        selectedKeys = emptySet()
    }

    val deleteSelected: () -> Unit = {
        val category = currentCategory
        if (category != null) {
            val deleted = selectedKeys.filter { key ->
                runCatching {
                    DocumentsContract.deleteDocument(context.contentResolver, Uri.parse(key))
                }.getOrDefault(false)
            }.toSet()
            categories = categories.map { c ->
                if (c.name == category.name) {
                    c.copy(audios = c.audios.filterNot { it.uri in deleted })
                } else {
                    c
                }
            }
            saveCategories(context, categories)
            if (deleted.size < selectedKeys.size) {
                Toast.makeText(context, "Some files couldn't be deleted", Toast.LENGTH_SHORT).show()
            }
            selectedKeys = emptySet()
        } else {
            val keys = selectedKeys.toList()
            val ids = keys.toSet()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val uris = keys.map {
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, it.toLong())
                }
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
                MediaDelete.launch(context as Activity, pendingIntent.intentSender) { deleted ->
                    if (deleted) {
                        memos = memos.filterNot { it.id.toString() in ids }
                        edits = edits.filterNot { it.outputId() in ids }
                        saveEdits(context, edits)
                    }
                    selectedKeys = emptySet()
                }
            } else {
                val deleted = keys.filter { key ->
                    runCatching {
                        context.contentResolver.delete(
                            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, key.toLong()),
                            null,
                            null
                        ) > 0
                    }.getOrDefault(false)
                }.toSet()
                memos = memos.filterNot { it.id.toString() in deleted }
                edits = edits.filterNot { it.outputId() in deleted }
                saveEdits(context, edits)
                if (deleted.size < keys.size) {
                    Toast.makeText(context, "Some files couldn't be deleted", Toast.LENGTH_SHORT).show()
                }
                selectedKeys = emptySet()
            }
        }
    }

    val openEditor = LocalEditRequest.current
    val jumpToOriginal: () -> Unit = {
        val key = selectedKeys.firstOrNull()
        val seg = edits.firstOrNull { it.outputId() == key }
        if (seg == null) {
            Toast.makeText(context, "Part not found", Toast.LENGTH_SHORT).show()
        } else {
            val exists = runCatching {
                context.contentResolver.openFileDescriptor(Uri.parse(seg.sourceUri), "r")?.use { true }
            }.getOrNull() == true
            if (!exists) {
                Toast.makeText(context, "The original file no longer exists", Toast.LENGTH_SHORT).show()
            } else {
                val original = memos.firstOrNull {
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, it.id).toString() == seg.sourceUri
                }
                val name = original?.name ?: seg.sourceName
                val date = original?.dateAdded?.times(1000L) ?: 0L
                selectedKeys = emptySet()
                openEditor(seg.sourceUri, name, date)
            }
        }
    }

    if (showSort) {
        SortSheet(
            current = sortOption,
            showDateEdited = tab != Tab.Edited,
            onSelect = { option ->
                sortOption = option
                saveSort(context, option)
                showSort = false
            },
            onDismiss = { showSort = false }
        )
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Add audio files") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newCategoryName.isNotBlank(),
                    onClick = {
                        val name = newCategoryName.trim()
                        if (categories.any { it.name.equals(name, ignoreCase = true) }) {
                            Toast.makeText(context, "A category with that name already exists", Toast.LENGTH_SHORT).show()
                        } else {
                            showNameDialog = false
                            pick = Pick.New(name)
                            AppLock.skipNextStop()
                            FilePicker.open(context as Activity, onPicked)
                        }
                    }
                ) {
                    Text("Choose files")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPlayWarning) {
        AlertDialog(
            onDismissRequest = { showPlayWarning = false },
            title = { Text("Audio is playing") },
            text = { Text("Recording now will stop the current playback. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    showPlayWarning = false
                    stopPlaybackSignal += 1
                    checkAndStartRecording()
                }) {
                    Text("Stop and record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPlayWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    pendingSave?.let { saved ->
        AlertDialog(
            onDismissRequest = closeSave,
            title = { Text("Name your recording") },
            text = {
                OutlinedTextField(
                    value = saveName,
                    onValueChange = { saveName = it },
                    label = { Text("Recording name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = saveName.isNotBlank(),
                    onClick = {
                        if (!renameRecording(context, saved.uri, saveName)) {
                            Toast.makeText(context, "Couldn't rename, kept default name", Toast.LENGTH_SHORT).show()
                        }
                        closeSave()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = closeSave) {
                    Text("Keep default")
                }
            }
        )
    }

    confirm?.let { kind ->
        val count = selectedKeys.size
        val noun = if (count == 1) "audio file" else "audio files"
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = {
                Text(if (kind == Confirm.Remove) "Remove from category" else "Delete audio files")
            },
            text = {
                Text(
                    if (kind == Confirm.Remove) {
                        "Remove $count $noun from this category? They stay on your device."
                    } else {
                        "Delete $count $noun from your device? This can't be undone."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = DangerRed),
                    onClick = {
                        confirm = null
                        if (kind == Confirm.Remove) removeSelected() else deleteSelected()
                    }
                ) {
                    Text(if (kind == Confirm.Remove) "Remove" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    categoryToDelete?.let { name ->
        val count = categories.firstOrNull { it.name == name }?.audios?.size ?: 0
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete category") },
            text = {
                Text("Delete \"$name\"? Its $count audio files stay on your device.")
            },
            confirmButton = {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = DangerRed),
                    onClick = {
                        categories = categories.filterNot { it.name == name }
                        saveCategories(context, categories)
                        if (tab == Tab.User(name)) tab = Tab.All
                        selectedKeys = emptySet()
                        categoryToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    val tabs: List<Tab> = listOf(Tab.All, Tab.Raw, Tab.Edited, Tab.Favorites) +
            categories.map { Tab.User(it.name) } +
            Tab.Add

    val menuActions = if (selectionMode) {
        listOf(
            BarAction(Icons.Filled.Remove, "Remove from category", (-85).dp, (-130).dp, DangerRed, Color.White) {
                menuOpen = false
                confirm = Confirm.Remove
            },
            BarAction(Icons.Filled.Delete, "Delete", 85.dp, (-130).dp, DangerRed, Color.White) {
                menuOpen = false
                confirm = Confirm.Delete
            }
        )
    } else {
        listOf(
            BarAction(Icons.Filled.Mic, "New Recording", (-85).dp, (-130).dp, TealAccent, Color.Black) {
                menuOpen = false
                startRecording()
            },
            BarAction(Icons.Filled.Add, "Add audio files", 85.dp, (-130).dp, TealAccent, Color.Black) {
                menuOpen = false
                openAddFiles()
            }
        )
    }

    val centerIcon: ImageVector = when {
        recording -> Icons.Filled.Stop
        showAddFiles -> Icons.Filled.Add
        selectionMode -> Icons.Filled.Delete
        else -> Icons.Filled.Mic
    }
    val centerColor = if (recording || selectionMode) DangerRed else TealAccent
    val centerContent = if (recording || selectionMode) Color.White else Color.Black
    val centerLabel = when {
        recording && recordingPaused -> "Paused ${formatDuration(elapsedSec * 1000)}"
        recording -> "Recording ${formatDuration(elapsedSec * 1000)}"
        showAddFiles -> ""
        selectionMode -> "Delete"
        else -> "New Recording"
    }
    val onCenterClick: () -> Unit = {
        when {
            recording -> stopRecording()
            showAddFiles -> menuOpen = !menuOpen
            selectionMode -> confirm = Confirm.Delete
            else -> startRecording()
        }
    }

    val closeSearch: () -> Unit = {
        searchOpen = false
        searchQuery = ""
    }

    fun emptyFor(base: String): String =
        if (searchQuery.isBlank()) base else "No matches for \"${searchQuery.trim()}\""

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(16.dp)
        ) {
            ScreenHeader(
                title = "All Audios",
                navIcon = Icons.Filled.Settings,
                navDescription = "Settings",
                onNavClick = onSettingsClick,
                rightIcon = Icons.Filled.FilterList,
                rightDescription = "Sort",
                onRightClick = { showSort = true }
            )
            UpdateBanner(
                current = remember(context) { installedVersion(context) },
                latest = UpdateStatus.latestVersion,
                url = UpdateStatus.releaseUrl
            )
            UpdateReadyDialog()
            Spacer(Modifier.height(12.dp))
            if (searchOpen) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFocus),
                    label = { Text("Search audio") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(12.dp))
            }
            TabPills(
                tabs = tabs,
                selected = tab,
                onSelect = { selected ->
                    tab = selected
                    selectedKeys = emptySet()
                    if (selected == Tab.Add) {
                        newCategoryName = ""
                        showNameDialog = true
                    }
                },
                onLongSelect = { selected ->
                    if (selected is Tab.User) categoryToDelete = selected.name
                }
            )
            Spacer(Modifier.height(12.dp))
            Box(Modifier.weight(1f)) {
                when (val current = tab) {
                    Tab.All, Tab.Raw -> {
                        if (granted) {
                            val rawItems = if (tab == Tab.Raw) {
                                allMemoItems.filter { it.key !in editedIds }
                            } else {
                                allMemoItems
                            }
                            AudioList(
                                list = rawItems,
                                emptyText = emptyFor("No audio files found on this device."),
                                selectionMode = selectionMode,
                                selectedKeys = selectedKeys,
                                onLongPress = { key -> selectedKeys = selectedKeys + key },
                                onSelectTap = { key ->
                                    selectedKeys = if (key in selectedKeys) selectedKeys - key else selectedKeys + key
                                },
                                stopSignal = stopPlaybackSignal,
                                onPlayingChange = { playingAny = it },
                                favorites = favorites,
                                sortKey = sortOption,
                                durations = preciseMs,
                                listKey = tab
                            )
                        } else {
                            PermissionCard(onAllow = { permissionLauncher.launch(permission) })
                        }
                    }
                    Tab.Edited -> AudioList(
                        list = editedItems,
                        emptyText = emptyFor("No edited audios yet."),
                        selectionMode = selectionMode,
                        selectedKeys = selectedKeys,
                        onLongPress = { key -> selectedKeys = selectedKeys + key },
                        onSelectTap = { key ->
                            selectedKeys = if (key in selectedKeys) selectedKeys - key else selectedKeys + key
                        },
                        stopSignal = stopPlaybackSignal,
                        onPlayingChange = { playingAny = it },
                        favorites = favorites,
                        sortKey = sortOption,
                        durations = preciseMs,
                        listKey = tab
                    )
                    Tab.Favorites -> AudioList(
                        list = favoriteItems,
                        emptyText = emptyFor("No favorites yet. Hold a recording and tap the star."),
                        selectionMode = selectionMode,
                        selectedKeys = selectedKeys,
                        onLongPress = { key -> selectedKeys = selectedKeys + key },
                        onSelectTap = { key ->
                            selectedKeys = if (key in selectedKeys) selectedKeys - key else selectedKeys + key
                        },
                        stopSignal = stopPlaybackSignal,
                        onPlayingChange = { playingAny = it },
                        favorites = favorites,
                        sortKey = sortOption,
                        durations = preciseMs,
                        listKey = tab
                    )
                    Tab.Add -> EmptyText("Tap Add to create a category.")
                    is Tab.User -> AudioList(
                        list = userLists[current.name].orEmpty(),
                        emptyText = emptyFor("No audio files in this category yet."),
                        selectionMode = selectionMode,
                        selectedKeys = selectedKeys,
                        onLongPress = { key -> selectedKeys = selectedKeys + key },
                        onSelectTap = { key ->
                            selectedKeys = if (key in selectedKeys) selectedKeys - key else selectedKeys + key
                        },
                        stopSignal = stopPlaybackSignal,
                        onPlayingChange = { playingAny = it },
                        favorites = favorites,
                        sortKey = sortOption,
                        durations = preciseMs,
                        listKey = tab
                    )
                }
            }
        }

        if (menuVisible) {
            MenuOverlay(progress = menuProgress, onClose = { menuOpen = false })
        }

        FoEchoBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            centerIcon = centerIcon,
            centerLabel = centerLabel,
            centerColor = centerColor,
            centerContent = centerContent,
            rotateCenter = showAddFiles && !recording,
            menuVisible = menuVisible && !recording,
            menuProgress = menuProgress,
            actions = menuActions,
            onCenterClick = onCenterClick,
            sideIcon = when {
                recording -> if (recordingPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause
                selectionMode -> Icons.Filled.Star
                else -> null
            },
            sideLabel = when {
                recording -> if (recordingPaused) "Resume recording" else "Pause recording"
                else -> "Favorite"
            },
            onSideClick = { if (recording) togglePause() else toggleFavorites() },
            sideColor = if (!recording && selectionMode) FavoriteYellow else null,
            sideContent = if (!recording && selectionMode) Color.Black else null,
            leftIcon = when {
                recording -> null
                selectionMode && tab == Tab.Edited && selectedKeys.size == 1 ->
                    Icons.AutoMirrored.Filled.OpenInNew
                selectionMode -> null
                searchOpen -> Icons.Filled.Close
                else -> Icons.Filled.Search
            },
            leftLabel = when {
                selectionMode -> "Jump to original"
                searchOpen -> "Close search"
                else -> "Search"
            },
            onLeftClick = {
                when {
                    selectionMode -> jumpToOriginal()
                    searchOpen -> closeSearch()
                    else -> searchOpen = true
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortSheet(
    current: SortOption,
    showDateEdited: Boolean,
    onSelect: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(
                text = "Sort By",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(16.dp))
            SortSection(
                title = "Date Created",
                current = current,
                first = SortOption.DateCreatedNewest,
                second = SortOption.DateCreatedOldest,
                firstLabel = "Newest to Oldest",
                secondLabel = "Oldest to Newest",
                onSelect = onSelect
            )
            if (showDateEdited) {
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                SortSection(
                    title = "Date Edited",
                    current = current,
                    first = SortOption.DateEditedNewest,
                    second = SortOption.DateEditedOldest,
                    firstLabel = "Newest to Oldest",
                    secondLabel = "Oldest to Newest",
                    onSelect = onSelect
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            SortSection(
                title = "Alphabetical",
                current = current,
                first = SortOption.NameAZ,
                second = SortOption.NameZA,
                firstLabel = "A to Z",
                secondLabel = "Z to A",
                onSelect = onSelect
            )
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            SortSection(
                title = "Length",
                current = current,
                first = SortOption.LengthLongest,
                second = SortOption.LengthShortest,
                firstLabel = "Longest to Shortest",
                secondLabel = "Shortest to Longest",
                onSelect = onSelect
            )
        }
    }
}

@Composable
private fun SortSection(
    title: String,
    current: SortOption,
    first: SortOption,
    second: SortOption,
    firstLabel: String,
    secondLabel: String,
    onSelect: (SortOption) -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(Modifier.height(4.dp))
    SortRow(label = firstLabel, selected = current == first) { onSelect(first) }
    SortRow(label = secondLabel, selected = current == second) { onSelect(second) }
}

@Composable
private fun SortRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun TabPills(
    tabs: List<Tab>,
    selected: Tab,
    onSelect: (Tab) -> Unit,
    onLongSelect: (Tab) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tabs) { tab ->
            val isSelected = selected == tab
            Surface(
                modifier = Modifier.combinedClickable(
                    onClick = { onSelect(tab) },
                    onLongClick = { onLongSelect(tab) }
                ),
                shape = RoundedCornerShape(50),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = tab.label(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun MenuOverlay(progress: Float, onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f * progress))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose
            )
    )
}

@Composable
private fun PermissionCard(onAllow: () -> Unit) {
    ShieldCard {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Allow FoEcho to read the voice memos on this device?",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onAllow) {
                Text("Yes")
            }
        }
    }
}

@Composable
private fun EmptyText(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun UpdateBanner(current: String, latest: String?, url: String?) {
    if (latest == null) return
    val context = LocalContext.current
    Column {
        Spacer(Modifier.height(12.dp))
        Card(
            onClick = { startUpdateDownload(context) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = DangerRed,
                contentColor = Color.White
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Your version is outdated",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "You have $current. Version $latest is available. Tap to download.",
                    style = MaterialTheme.typography.bodyMedium
                )
                url?.let {
                    TextButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Text("View ChangeNotes")
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateReadyDialog() {
    val context = LocalContext.current
    val ready = InstallReady.current ?: return
    AlertDialog(
        onDismissRequest = { InstallReady.dismissForNow() },
        title = { Text("Update ready") },
        text = {
            Text("FoEcho ${ready.version} has finished downloading. Install it now?")
        },
        confirmButton = {
            TextButton(onClick = {
                InstallReady.finish(context)
                installApk(context, ready.uri)
            }) {
                Text("Install")
            }
        },
        dismissButton = {
            TextButton(onClick = { InstallReady.dismissForNow() }) {
                Text("Later")
            }
        }
    )
}

@Composable
private fun AudioList(
    list: List<AudioItem>,
    emptyText: String,
    selectionMode: Boolean,
    selectedKeys: Set<String>,
    onLongPress: (String) -> Unit,
    onSelectTap: (String) -> Unit,
    stopSignal: Int,
    onPlayingChange: (Boolean) -> Unit,
    favorites: Set<String>,
    sortKey: SortOption,
    durations: Map<String, Long>,
    listKey: Any
) {
    val context = LocalContext.current
    val onEdit = LocalEditRequest.current
    val listState = remember(listKey) { LazyListState(0, 0) }
    var playingKey by remember { mutableStateOf<String?>(null) }
    var isPaused by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(0L) }
    var playerDuration by remember { mutableStateOf(0L) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            player?.release()
            player = null
            onPlayingChange(false)
        }
    }

    LaunchedEffect(sortKey) {
        listState.scrollToItem(0)
    }

    fun stopPlayback() {
        player?.release()
        player = null
        playingKey = null
        isPaused = false
        position = 0L
        playerDuration = 0L
    }

    fun togglePlayback(item: AudioItem) {
        val current = player
        if (playingKey == item.key && current != null) {
            if (isPaused) {
                current.start()
                isPaused = false
            } else {
                current.pause()
                isPaused = true
            }
            return
        }
        stopPlayback()
        val newPlayer = MediaPlayer()
        runCatching {
            newPlayer.setDataSource(context, item.uri)
            newPlayer.setOnCompletionListener { stopPlayback() }
            newPlayer.prepare()
            playerDuration = newPlayer.duration.toLong().takeIf { it > 0 } ?: item.durationMs
            newPlayer.start()
        }.onSuccess {
            player = newPlayer
            playingKey = item.key
        }.onFailure {
            newPlayer.release()
        }
    }

    val screenActive = LocalScreenActive.current
    LaunchedEffect(screenActive) {
        if (!screenActive) stopPlayback()
    }

    LaunchedEffect(playingKey, isPaused) {
        while (playingKey != null && !isPaused) {
            position = player?.currentPosition?.toLong() ?: 0L
            delay(200)
        }
    }

    LaunchedEffect(playingKey) {
        onPlayingChange(playingKey != null)
    }

    LaunchedEffect(stopSignal) {
        if (stopSignal > 0) stopPlayback()
    }

    LaunchedEffect(list) {
        val key = playingKey
        if (key != null && list.none { it.key == key }) stopPlayback()
    }

    if (list.isEmpty()) {
        EmptyText(emptyText)
        return
    }

    val rows = remember(list, sortKey, durations) { buildRows(list, sortKey, durations) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 14.dp, bottom = 130.dp)
        ) {
            items(rows, key = { it.rowKey }) { row ->
                when (row) {
                    is ListRow.Header -> Text(
                        text = row.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                    is ListRow.Entry -> {
                        val item = row.item
                        val waveform by produceState<FloatArray?>(initialValue = null, item.uri) {
                            value = loadWaveform(context, item.uri)
                        }
                        val preciseMs by produceState(initialValue = item.durationMs, item.uri, item.durationMs) {
                            value = loadDuration(context, item.uri, item.durationMs)
                        }
                        val isActive = playingKey == item.key
                        val isPlaying = isActive && !isPaused
                        val shownMs = if (isActive) {
                            (playerDuration - position).coerceAtLeast(0L)
                        } else {
                            preciseMs
                        }
                        val selected = item.key in selectedKeys
                        ShieldCard(
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    if (selectionMode) onSelectTap(item.key) else togglePlayback(item)
                                },
                                onLongClick = { onLongPress(item.key) }
                            )
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .drawWaveform(waveform, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selectionMode) {
                                        Icon(
                                            imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                            contentDescription = if (selected) "Selected" else "Not selected",
                                            tint = if (selected) TealAccent else MaterialTheme.colorScheme.onSurface
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause" else "Play"
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = formatDuration(shownMs),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    if (!selectionMode) {
                                        IconButton(
                                            onClick = { onEdit(item.uri.toString(), item.name, item.dateCreated) }
                                        ) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                        }
                                    }
                                }
                                if (item.key in favorites) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "Favorite",
                                        tint = FavoriteYellow,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val navBottom = with(LocalDensity.current) {
            WindowInsets.navigationBars.getBottom(this).toDp()
        }
        ListScrollbar(
            listState = listState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .padding(end = 2.dp, bottom = 80.dp + navBottom + 12.dp)
        )
    }
}

@Composable
private fun ListScrollbar(listState: LazyListState, modifier: Modifier) {
    val thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    Canvas(modifier.width(5.dp)) {
        val info = listState.layoutInfo
        val total = info.totalItemsCount
        val visible = info.visibleItemsInfo.size
        if (total > visible && visible > 0) {
            val trackHeight = size.height
            val thumbHeight = max(trackHeight * visible / total, 40.dp.toPx())
            val scrollable = (total - visible).toFloat()
            val fraction = listState.firstVisibleItemIndex / scrollable
            val thumbTop = fraction * (trackHeight - thumbHeight)
            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(0f, thumbTop),
                size = Size(size.width, thumbHeight),
                cornerRadius = CornerRadius(size.width / 2f)
            )
        }
    }
}

private fun buildRows(
    list: List<AudioItem>,
    option: SortOption,
    durations: Map<String, Long>
): List<ListRow> {
    val groups: Map<String, List<AudioItem>> = when (option) {
        SortOption.DateCreatedNewest, SortOption.DateCreatedOldest ->
            list.groupBy { dateLabel(it.dateCreated) }
        SortOption.DateEditedNewest, SortOption.DateEditedOldest ->
            list.groupBy { dateLabel(it.dateModified) }
        SortOption.NameAZ, SortOption.NameZA ->
            list.groupBy { letterLabel(it.name) }
        SortOption.LengthLongest, SortOption.LengthShortest ->
            list.groupBy { lengthLabel(durations[it.key] ?: it.durationMs) }
    }
    return groups.flatMap { (title, items) ->
        listOf<ListRow>(ListRow.Header(title)) + items.map { ListRow.Entry(it) }
    }
}

private fun dateLabel(ms: Long): String =
    SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date(ms))

private fun letterLabel(name: String): String {
    val first = name.firstOrNull()?.uppercaseChar() ?: return "#"
    return if (first in 'A'..'Z') first.toString() else "#"
}

private fun lengthLabel(ms: Long): String = when {
    ms < 30_000L -> "Under 30 seconds"
    ms < 120_000L -> "30 seconds - 2 minutes"
    ms < 600_000L -> "2 - 10 minutes"
    else -> "Over 10 minutes"
}

private fun List<AudioItem>.matching(query: String): List<AudioItem> {
    val q = query.trim()
    if (q.isEmpty()) return this
    return filter { it.name.contains(q, ignoreCase = true) }
}

private fun Modifier.drawWaveform(waveform: FloatArray?, color: Color): Modifier =
    if (waveform == null) {
        this
    } else {
        drawBehind {
            val barWidth = size.width / waveform.size
            val mid = size.height / 2f
            waveform.forEachIndexed { i, amp ->
                val half = (amp * size.height * 0.4f).coerceAtLeast(1f)
                val x = i * barWidth + barWidth / 2f
                drawLine(
                    color = color,
                    start = Offset(x, mid - half),
                    end = Offset(x, mid + half),
                    strokeWidth = barWidth * 0.5f,
                    cap = StrokeCap.Round
                )
            }
        }
    }

private fun List<AudioItem>.applySort(
    option: SortOption,
    durations: Map<String, Long>
): List<AudioItem> = when (option) {
    SortOption.DateCreatedNewest -> sortedByDescending { it.dateCreated }
    SortOption.DateCreatedOldest -> sortedBy { it.dateCreated }
    SortOption.DateEditedNewest -> sortedByDescending { it.dateModified }
    SortOption.DateEditedOldest -> sortedBy { it.dateModified }
    SortOption.NameAZ -> sortedBy { it.name.lowercase() }
    SortOption.NameZA -> sortedByDescending { it.name.lowercase() }
    SortOption.LengthLongest -> sortedByDescending { durations[it.key] ?: it.durationMs }
    SortOption.LengthShortest -> sortedBy { durations[it.key] ?: it.durationMs }
}

private fun Tab.label(): String = when (this) {
    Tab.All -> "All"
    Tab.Raw -> "Raw"
    Tab.Edited -> "Edited"
    Tab.Favorites -> "Favorites"
    Tab.Add -> "+ Add"
    is Tab.User -> name
}

private fun VoiceMemo.toItem(): AudioItem = AudioItem(
    key = id.toString(),
    uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
    name = name,
    durationMs = durationMs,
    dateCreated = dateAdded * 1000L,
    dateModified = dateModified * 1000L,
    sizeBytes = sizeBytes
)

private fun AddedAudio.toItem(): AudioItem = AudioItem(
    key = uri,
    uri = Uri.parse(uri),
    name = name,
    durationMs = durationMs,
    dateCreated = dateCreated,
    dateModified = dateModified,
    sizeBytes = sizeBytes
)

private fun persistAccess(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
}

private fun toAddedAudio(context: Context, uri: Uri): AddedAudio {
    val durationMs = runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
        }
    }.getOrNull() ?: 0L
    val (sizeBytes, modified) = queryFileMeta(context, uri)
    return AddedAudio(
        uri = uri.toString(),
        name = displayName(context, uri),
        durationMs = durationMs,
        dateCreated = modified,
        dateModified = modified,
        sizeBytes = sizeBytes
    )
}

private fun queryFileMeta(context: Context, uri: Uri): Pair<Long, Long> {
    return runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) Pair(cursor.getLong(0), cursor.getLong(1)) else Pair(0L, 0L)
        }
    }.getOrNull() ?: Pair(0L, 0L)
}

private fun displayName(context: Context, uri: Uri): String {
    return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        ?: uri.lastPathSegment
        ?: "Unknown"
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms + 999) / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun EditedSegment.outputId(): String =
    Uri.parse(outputUri).lastPathSegment.orEmpty()

private fun EditedSegment.sourceUriString(): String = sourceUri