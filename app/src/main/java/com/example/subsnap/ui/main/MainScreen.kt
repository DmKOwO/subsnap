package com.example.subsnap.ui.main

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Warning
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.subsnap.ocr.OcrSubtitleDetector
import com.example.subsnap.ota.AppReleaseRecord
import com.example.subsnap.ota.VersionDiff
import com.example.subsnap.ocr.SubtitleDetectionResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.subsnap.data.CapturedScreenshot
import com.example.subsnap.data.SettingsRepository
import com.example.subsnap.data.SpacedRepetition
import com.example.subsnap.data.model.AnkiCard
import com.example.subsnap.service.ScreenCaptureService
import com.example.subsnap.tts.TtsHelper
import com.example.subsnap.ui.study.StudyScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.subsnap.BuildConfig
import com.example.subsnap.ota.AppUpdateInfo
import com.example.subsnap.ota.GitHubUpdateManager
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val context = LocalContext.current
    val serviceState by viewModel.serviceState.collectAsStateWithLifecycle()
    val screenshots by viewModel.screenshots.collectAsStateWithLifecycle()
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val filterEmptyScreenshots by viewModel.filterEmptyScreenshots.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val analysisError by viewModel.analysisError.collectAsStateWithLifecycle()
    val lastFailedScreenshot by viewModel.lastFailedScreenshot.collectAsStateWithLifecycle()
    val generatedCard by viewModel.generatedCard.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()
    val updateMessage by viewModel.updateMessage.collectAsStateWithLifecycle()
    val githubRepo by viewModel.githubRepo.collectAsStateWithLifecycle()
    val filteredCards by viewModel.filteredCards.collectAsStateWithLifecycle()
    val cardSearchQuery by viewModel.cardSearchQuery.collectAsStateWithLifecycle()
    val ttsSpeed by viewModel.ttsSpeed.collectAsStateWithLifecycle()
    val ttsLocale by viewModel.ttsLocale.collectAsStateWithLifecycle()
    val ocrRegion by viewModel.ocrRegion.collectAsStateWithLifecycle()
    val skipDuplicateSubtitles by viewModel.skipDuplicateSubtitles.collectAsStateWithLifecycle()
    val autoCaptureIntervalSec by viewModel.autoCaptureIntervalSec.collectAsStateWithLifecycle()
    val autoStartAutoCapture by viewModel.autoStartAutoCapture.collectAsStateWithLifecycle()
    val smartDetectionEnabled by viewModel.smartDetectionEnabled.collectAsStateWithLifecycle()
    val releasesHistory by viewModel.releasesHistory.collectAsStateWithLifecycle()
    val versionDiff by viewModel.versionDiff.collectAsStateWithLifecycle()
    val isLoadingHistory by viewModel.isLoadingHistory.collectAsStateWithLifecycle()
    val todayCardsCount by viewModel.todayCardsCount.collectAsStateWithLifecycle()
    val todayScreenshotsCount by viewModel.todayScreenshotsCount.collectAsStateWithLifecycle()

    val ttsHelper = remember { TtsHelper.getInstance(context) }
    var isInStudyMode by remember { mutableStateOf(false) }
    val dueCards = remember(cards) { SpacedRepetition.getDueCards(cards) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showReleaseHistoryDialog by remember { mutableStateOf(false) }
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }
    var showClearCardsConfirmDialog by remember { mutableStateOf(false) }
    var selectedScreenshotForPreview by remember { mutableStateOf<CapturedScreenshot?>(null) }
    var selectedCardForPreview by remember { mutableStateOf<AnkiCard?>(null) }

    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    LaunchedEffect(Unit) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
        viewModel.checkForUpdates(userInitiated = false)
    }

    LaunchedEffect(updateMessage) {
        updateMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    if (isInStudyMode) {
        StudyScreen(
            allCards = cards,
            onCardUpdated = { viewModel.updateCard(it) },
            onBack = { isInStudyMode = false },
            modifier = modifier
        )
        return
    }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScreenCaptureService.start(
                context,
                result.resultCode,
                result.data!!,
                autoStart = autoStartAutoCapture
            )
            val msg = if (autoStartAutoCapture)
                "Служба запущена! Авто-захват субтитров АКТИВЕН"
            else
                "Служба запущена! Нажимайте на кружок для снимка"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Разрешение на захват экрана не получено", Toast.LENGTH_SHORT).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SubSnap", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        ServiceStatusBadge(serviceState.isRunning)
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Настройки ИИ",
                            tint = if (apiKey.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.refreshScreenshots() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs: Screenshots vs Anki Cards vs Study Mode
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Снимки (${screenshots.size})")
                        }
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Колода (${cards.size})")
                        }
                    }
                )
                Tab(
                    selected = false,
                    onClick = { isInStudyMode = true },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BadgedBox(
                                badge = {
                                    if (dueCards.isNotEmpty()) {
                                        Badge { Text("${dueCards.size}") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Учить")
                        }
                    }
                )
            }

            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "TabContentTransition"
            ) { tabIndex ->
                if (tabIndex == 0) {
                    // TAB 1: SCREENSHOTS
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    if (apiKey.isBlank()) {
                        ApiKeyWarningBanner(onClick = { showSettingsDialog = true })
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    TodayStatsCard(
                        todayScreenshots = todayScreenshotsCount,
                        todayCards = todayCardsCount,
                        dueCardsCount = dueCards.size
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ControlPanelCard(
                        hasOverlay = hasOverlayPermission,
                        hasNotification = hasNotificationPermission,
                        serviceState = serviceState,
                        autoCaptureInterval = autoCaptureIntervalSec,
                        isSmartDetection = smartDetectionEnabled,
                        onToggleAutoCapture = { viewModel.toggleAutoCapture() },
                        onRequestOverlay = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        },
                        onRequestNotification = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onStartService = {
                            if (!hasOverlayPermission) {
                                Toast.makeText(context, "Сначала разрешите отображение поверх окон!", Toast.LENGTH_LONG).show()
                                return@ControlPanelCard
                            }
                            val projectionManager =
                                context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
                        },
                        onStopService = {
                            ScreenCaptureService.stop(context)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Кадры (${screenshots.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (screenshots.isNotEmpty()) {
                            TextButton(onClick = { showClearAllConfirmDialog = true }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Очистить", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (screenshots.isEmpty()) {
                        EmptyScreenshotsPlaceholder(serviceRunning = serviceState.isRunning)
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(screenshots, key = { it.id }) { item ->
                                ScreenshotItemCard(
                                    item = item,
                                    onClick = { selectedScreenshotForPreview = item },
                                    onDelete = { viewModel.deleteScreenshot(item.id) },
                                    onAiClick = {
                                        if (apiKey.isBlank()) {
                                            showSettingsDialog = true
                                        } else {
                                            viewModel.analyzeScreenshot(item)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // TAB 2: ANKI CARDS
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Колода Anki (${cards.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row {
                            if (cards.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        viewModel.exportDeck { exportFile ->
                                            shareExportFile(context, exportFile)
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Экспорт .txt", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = { showClearCardsConfirmDialog = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Очистить колоду",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (cards.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isInStudyMode = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        Icons.Default.School,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (dueCards.isNotEmpty()) "Повторение: ${com.example.subsnap.data.SpacedRepetition.formatCardsCount(dueCards.size)}" else "Все карточки повторены!",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = if (dueCards.isNotEmpty()) "SM-2 интервалы готовы к тренировке" else "Нажмите для предварительного повторения",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                Button(
                                    onClick = { isInStudyMode = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Учить", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (cards.isEmpty()) {
                        EmptyCardsPlaceholder()
                    } else {
                        // Search bar
                        OutlinedTextField(
                            value = cardSearchQuery,
                            onValueChange = { viewModel.setCardSearchQuery(it) },
                            placeholder = { Text("Поиск слова или перевода...", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (cardSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.setCardSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Очистить", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (filteredCards.isEmpty() && cardSearchQuery.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Ничего не найдено по запросу «$cardSearchQuery»",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 24.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredCards, key = { it.id }) { card ->
                                    AnkiCardItem(
                                        card = card,
                                        onClick = { selectedCardForPreview = card },
                                        onDelete = { viewModel.deleteCard(card.id) },
                                        onTtsClick = { ttsHelper.speak(card.targetWord, ttsSpeed, ttsLocale) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    // AI Analysis Loading Dialog
    if (isAnalyzing) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Нейросеть Gemini анализирует кадр...",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Распознаем субтитры, переводим контекст и готовим карточку Anki",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // AI Analysis Error Dialog
    analysisError?.let { err ->
        val failedScreenshot = lastFailedScreenshot
        val isOcrWarning = err.startsWith("ML Kit OCR")
        AlertDialog(
            onDismissRequest = { viewModel.dismissGeneratedCard() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isOcrWarning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isOcrWarning) "Проверка субтитров" else "Ошибка генерации")
                }
            },
            text = { Text(err) },
            confirmButton = {
                if (isOcrWarning && failedScreenshot != null) {
                    Button(onClick = {
                        viewModel.analyzeScreenshot(failedScreenshot, forceSend = true)
                    }) {
                        Text("Всё равно отправить")
                    }
                } else {
                    Button(onClick = { viewModel.dismissGeneratedCard() }) {
                        Text("Понятно")
                    }
                }
            },
            dismissButton = {
                if (isOcrWarning && failedScreenshot != null) {
                    OutlinedButton(onClick = { viewModel.dismissGeneratedCard() }) {
                        Text("Отмена")
                    }
                }
            }
        )
    }

    // Generated Card Review Dialog
    generatedCard?.let { card ->
        GeneratedCardReviewDialog(
            card = card,
            ttsHelper = ttsHelper,
            onSave = { updatedCard ->
                viewModel.saveGeneratedCard(updatedCard)
                selectedTabIndex = 1 // Switch to Cards tab
                Toast.makeText(context, "Карточка сохранена в колоду!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { viewModel.dismissGeneratedCard() }
        )
    }

    // Card Detail Preview Dialog
    selectedCardForPreview?.let { card ->
        CardDetailDialog(
            card = card,
            ttsHelper = ttsHelper,
            onDismiss = { selectedCardForPreview = null },
            onDelete = {
                viewModel.deleteCard(card.id)
                selectedCardForPreview = null
            }
        )
    }

    // Screenshot Fullscreen Preview Dialog
    selectedScreenshotForPreview?.let { item ->
        ScreenshotPreviewDialog(
            item = item,
            onDismiss = { selectedScreenshotForPreview = null },
            onDelete = {
                viewModel.deleteScreenshot(item.id)
                selectedScreenshotForPreview = null
            },
            onAiAnalyze = {
                selectedScreenshotForPreview = null
                if (apiKey.isBlank()) {
                    showSettingsDialog = true
                } else {
                    viewModel.analyzeScreenshot(item)
                }
            }
        )
    }

    // OTA Update Dialog
    if (updateInfo != null) {
        UpdateAvailableDialog(
            updateInfo = updateInfo!!,
            downloadState = downloadState,
            onDismiss = { viewModel.dismissUpdate() },
            onDownload = { url -> viewModel.downloadAndInstallUpdate(url) },
            onOpenBrowser = { url ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Не удалось открыть браузер", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Settings / API Key Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentApiKey = apiKey,
            currentModel = selectedModel,
            currentFilterEmpty = filterEmptyScreenshots,
            currentGithubRepo = githubRepo,
            currentTtsSpeed = ttsSpeed,
            currentTtsLocale = ttsLocale,
            currentOcrRegion = ocrRegion,
            currentSkipDuplicates = skipDuplicateSubtitles,
            currentAutoCaptureInterval = autoCaptureIntervalSec,
            currentAutoStartAutoCapture = autoStartAutoCapture,
            currentSmartDetection = smartDetectionEnabled,
            isCheckingUpdate = isCheckingUpdate,
            onOpenReleaseHistory = {
                showReleaseHistoryDialog = true
                viewModel.loadReleaseHistory()
            },
            onTestTts = { speed, locale ->
                ttsHelper.speak("Hello! This is a pronunciation test for SubSnap.", speed, locale)
            },
            onCheckForUpdates = { viewModel.checkForUpdates(userInitiated = true) },
            onSave = { newKey, newModel, newFilterEmpty, newRepo, newTtsSpeed, newTtsLocale, newOcrRegion, newSkipDuplicates, newInterval, newAutoStart, newSmartDetection ->
                viewModel.setApiKey(newKey)
                viewModel.setSelectedModel(newModel)
                viewModel.setFilterEmptyScreenshots(newFilterEmpty)
                viewModel.setGithubRepo(newRepo)
                viewModel.setTtsSpeed(newTtsSpeed)
                viewModel.setTtsLocale(newTtsLocale)
                viewModel.setOcrRegion(newOcrRegion)
                viewModel.setSkipDuplicateSubtitles(newSkipDuplicates)
                viewModel.setAutoCaptureIntervalSec(newInterval)
                viewModel.setAutoStartAutoCapture(newAutoStart)
                viewModel.setSmartDetectionEnabled(newSmartDetection)
                showSettingsDialog = false
                Toast.makeText(context, "Настройки сохранены", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Release History & Version Diff Dialog
    if (showReleaseHistoryDialog) {
        ReleaseHistoryDialog(
            currentVersion = BuildConfig.VERSION_NAME,
            versionDiff = versionDiff,
            releases = releasesHistory,
            isLoading = isLoadingHistory,
            downloadState = downloadState,
            onRefresh = { viewModel.loadReleaseHistory() },
            onDownloadRelease = { url -> viewModel.downloadAndInstallUpdate(url) },
            onOpenUrl = { url ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Не удалось открыть браузер", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showReleaseHistoryDialog = false }
        )
    }

    // Confirmation: Clear all screenshots
    if (showClearAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmDialog = false },
            title = { Text("Удалить все снимки?") },
            text = { Text("Все сохраненные кадры (${screenshots.size} шт.) будут удалены.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllScreenshots()
                        showClearAllConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearAllConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Confirmation: Clear all cards
    if (showClearCardsConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearCardsConfirmDialog = false },
            title = { Text("Очистить колоду Anki?") },
            text = { Text("Все карточки (${cards.size} шт.) будут удалены.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllCards()
                        showClearCardsConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Очистить")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearCardsConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun ApiKeyWarningBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "API-ключ Gemini не указан",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Нажмите здесь, чтобы ввести бесплатный ключ Google AI Studio для генерации карточек.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ScreenshotItemCard(
    item: CapturedScreenshot,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onAiClick: () -> Unit
) {
    val bitmap = rememberThumbnail(item.file)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Screenshot",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // AI button on image
                IconButton(
                    onClick = onAiClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(Color(0xCC6366F1), CircleShape)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "Создать карточку с ИИ",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.formattedDate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.sizeBytes / 1024} KB",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AnkiCardItem(
    card: AnkiCard,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onTtsClick: () -> Unit
) {
    val bitmap = rememberThumbnail(card.screenshotFile)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(70.dp, 50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = card.targetWord,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (card.transcription.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = card.transcription,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = card.wordTranslation,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = card.sentence,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // SM-2 status chip
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val badgeColor = if (card.isDue) Color(0xFFEF5350) else Color(0xFF4CAF50)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (card.isDue) "Повторить: ${card.intervalStatusText}" else card.intervalStatusText,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Audio pronunciation
            IconButton(onClick = onTtsClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Озвучить слово",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить карточку",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun GeneratedCardReviewDialog(
    card: AnkiCard,
    ttsHelper: TtsHelper,
    onSave: (AnkiCard) -> Unit,
    onDismiss: () -> Unit
) {
    var word by remember { mutableStateOf(card.targetWord) }
    var transcription by remember { mutableStateOf(card.transcription) }
    var wordTranslation by remember { mutableStateOf(card.wordTranslation) }
    var sentence by remember { mutableStateOf(card.sentence) }
    var sentenceTranslation by remember { mutableStateOf(card.sentenceTranslation) }
    var explanation by remember { mutableStateOf(card.explanation) }

    val bitmap = rememberThumbnail(card.screenshotFile)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✨ Карточка сгенерирована",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mini thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = word,
                    onValueChange = { word = it },
                    label = { Text("Ключевое слово / Идиома") },
                    trailingIcon = {
                        IconButton(onClick = { ttsHelper.speak(word) }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Озвучить слово", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = transcription,
                    onValueChange = { transcription = it },
                    label = { Text("Транскрипция (IPA)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = wordTranslation,
                    onValueChange = { wordTranslation = it },
                    label = { Text("Перевод слова") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = sentence,
                    onValueChange = { sentence = it },
                    label = { Text("Оригинальное предложение") },
                    trailingIcon = {
                        IconButton(onClick = { ttsHelper.speak(sentence) }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Озвучить предложение", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = sentenceTranslation,
                    onValueChange = { sentenceTranslation = it },
                    label = { Text("Перевод предложения") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = explanation,
                    onValueChange = { explanation = it },
                    label = { Text("Объяснение в контексте") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена")
                    }

                    Button(
                        onClick = {
                            val updated = card.copy(
                                targetWord = word.trim(),
                                transcription = transcription.trim(),
                                wordTranslation = wordTranslation.trim(),
                                sentence = sentence.trim(),
                                sentenceTranslation = sentenceTranslation.trim(),
                                explanation = explanation.trim()
                            )
                            onSave(updated)
                        },
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("В колоду Anki")
                    }
                }
            }
        }
    }
}

@Composable
fun CardDetailDialog(
    card: AnkiCard,
    ttsHelper: TtsHelper,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val bitmap = rememberFullBitmap(card.screenshotFile)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Карточка Anki", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Front & Back details
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = card.targetWord, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { ttsHelper.speak(card.targetWord) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Озвучить слово",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (card.transcription.isNotBlank()) {
                    Text(text = card.transcription, fontSize = 14.sp, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Перевод: ${card.wordTranslation}", fontSize = 16.sp, fontWeight = FontWeight.Medium)

                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Цитата:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { ttsHelper.speak(card.sentence) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Озвучить предложение", modifier = Modifier.size(16.dp))
                    }
                }
                Text(text = card.sentence, fontSize = 14.sp, fontStyle = FontStyle.Italic)

                Spacer(modifier = Modifier.height(6.dp))
                Text(text = card.sentenceTranslation, fontSize = 14.sp)

                if (card.explanation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Контекст:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = card.explanation, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Удалить")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    currentApiKey: String,
    currentModel: String,
    currentFilterEmpty: Boolean,
    currentGithubRepo: String,
    currentTtsSpeed: Float,
    currentTtsLocale: String,
    currentOcrRegion: String,
    currentSkipDuplicates: Boolean,
    currentAutoCaptureInterval: Float,
    currentAutoStartAutoCapture: Boolean,
    currentSmartDetection: Boolean,
    isCheckingUpdate: Boolean,
    onOpenReleaseHistory: () -> Unit,
    onTestTts: (Float, String) -> Unit,
    onCheckForUpdates: () -> Unit,
    onSave: (
        apiKey: String,
        model: String,
        filterEmpty: Boolean,
        githubRepo: String,
        ttsSpeed: Float,
        ttsLocale: String,
        ocrRegion: String,
        skipDuplicates: Boolean,
        autoCaptureInterval: Float,
        autoStartAutoCapture: Boolean,
        smartDetection: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKeyText by remember { mutableStateOf(currentApiKey) }
    var selectedModel by remember { mutableStateOf(currentModel) }
    var filterEmpty by remember { mutableStateOf(currentFilterEmpty) }
    var githubRepoText by remember { mutableStateOf(currentGithubRepo) }
    var ttsSpeed by remember { mutableStateOf(currentTtsSpeed) }
    var ttsLocale by remember { mutableStateOf(currentTtsLocale) }
    var ocrRegion by remember { mutableStateOf(currentOcrRegion) }
    var skipDuplicates by remember { mutableStateOf(currentSkipDuplicates) }
    var autoCaptureInterval by remember { mutableStateOf(currentAutoCaptureInterval) }
    var autoStartAutoCapture by remember { mutableStateOf(currentAutoStartAutoCapture) }
    var smartDetection by remember { mutableStateOf(currentSmartDetection) }
    var isModelDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 660.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Настройки", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 1. ИИ И РАСПОЗНАВАНИЕ
                Text(
                    text = "ИИ И РАСПОЗНАВАНИЕ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Gemini API от Google AI Studio для перевода и извлечения контекста.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("AIzaSy...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Model Selector
                ExposedDropdownMenuBox(
                    expanded = isModelDropdownExpanded,
                    onExpandedChange = { isModelDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedModel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Модель Gemini") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isModelDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = isModelDropdownExpanded,
                        onDismissRequest = { isModelDropdownExpanded = false }
                    ) {
                        SettingsRepository.AVAILABLE_MODELS.forEach { model ->
                            val label = when (model) {
                                "gemini-3.8-flash" -> "gemini-3.8-flash (Рекомендуется)"
                                "gemini-3.5-flash" -> "gemini-3.5-flash (Быстрая)"
                                "gemini-3.1-flash-lite" -> "gemini-3.1-flash-lite (Экономичная)"
                                "gemini-flash-latest" -> "gemini-flash-latest (Авто)"
                                else -> model
                            }
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedModel = model
                                    isModelDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Subtitle OCR Region Selection
                Text(
                    text = "Область субтитров (ML Kit OCR):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = ocrRegion == "LOWER_THIRD",
                        onClick = { ocrRegion = "LOWER_THIRD" },
                        label = { Text("Нижняя треть", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = ocrRegion == "FULL_SCREEN",
                        onClick = { ocrRegion = "FULL_SCREEN" },
                        label = { Text("Весь экран", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // OCR Empty Frame Filter Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Фильтр пустых кадров",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Пропускать кадры без субтитров при авто-захвате",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = filterEmpty,
                        onCheckedChange = { filterEmpty = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Duplicate Filter Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Защита от дубликатов",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Пропускать одинаковые субтитры подряд",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = skipDuplicates,
                        onCheckedChange = { skipDuplicates = it }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. АВТО-ЗАХВАТ СУБТИТРОВ
                Text(
                    text = "АВТО-ЗАХВАТ СУБТИТРОВ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Smart Subtitle Detection Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Умный детектор субтитров",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Stage 1 diff",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = if (smartDetection)
                                "⚡ Анализирует полосу субтитров (<0.1 мс CPU). OCR запускается только в момент появления нового текста."
                            else
                                "Выключен. Используется периодический опрос по таймеру ниже.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = smartDetection,
                        onCheckedChange = { smartDetection = it }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Интервал проверки экрана:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", autoCaptureInterval)} сек",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = autoCaptureInterval,
                    onValueChange = { autoCaptureInterval = (it * 2).toInt() / 2f },
                    valueRange = 1.5f..8.0f,
                    steps = 12,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "⚡ Быстрое сканирование (1.5–3.0 сек) позволяет не пропускать короткие реплики в YouTube и фильмах. ML Kit оффлайн анализирует кадр за ~30 мс и сохраняет скриншот только при появлении нового текста.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Авто-старт при запуске",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Сразу включать авто-захват при старте виджета",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoStartAutoCapture,
                        onCheckedChange = { autoStartAutoCapture = it }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 3. ОЗВУЧКА СЛОВ (TTS)
                Text(
                    text = "ОЗВУЧКА СЛОВ (TTS)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Акцент произношения:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = ttsLocale == "US",
                        onClick = { ttsLocale = "US" },
                        label = { Text("US (Американский)", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = ttsLocale == "UK",
                        onClick = { ttsLocale = "UK" },
                        label = { Text("UK (Британский)", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Скорость речи:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.2f", ttsSpeed)}x",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = ttsSpeed,
                    onValueChange = { ttsSpeed = (it * 100).toInt() / 100f },
                    valueRange = 0.75f..1.35f,
                    steps = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = { onTestTts(ttsSpeed, ttsLocale) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Прослушать пример озвучки", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 3. ОБНОВЛЕНИЯ (OTA) & РАЗНИЦА ВЕРСИЙ
                Text(
                    text = "ОБНОВЛЕНИЯ И РАЗНИЦА ВЕРСИЙ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "SubSnap v${BuildConfig.VERSION_NAME} • Просмотр истории релизов и ченджлогов прямо в приложении.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = githubRepoText,
                    onValueChange = { githubRepoText = it },
                    label = { Text("GitHub репозиторий (owner/repo)") },
                    placeholder = { Text("DmKOwO/subsnap") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // PROMINENT BUTTON: Version Diff & Releases History
                Button(
                    onClick = onOpenReleaseHistory,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("История версий и что нового (Changelog)")
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = onCheckForUpdates,
                    enabled = !isCheckingUpdate && githubRepoText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Проверка обновлений...")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Проверить обновления сейчас")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = {
                            onSave(
                                apiKeyText.trim(),
                                selectedModel,
                                filterEmpty,
                                githubRepoText.trim(),
                                ttsSpeed,
                                ttsLocale,
                                ocrRegion,
                                skipDuplicates,
                                autoCaptureInterval,
                                autoStartAutoCapture,
                                smartDetection
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

@Composable
fun ReleaseHistoryDialog(
    currentVersion: String,
    versionDiff: VersionDiff?,
    releases: List<AppReleaseRecord>,
    isLoading: Boolean,
    downloadState: GitHubUpdateManager.DownloadState,
    onRefresh: () -> Unit,
    onDownloadRelease: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Разница версий и релизы",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Установлена версия: v$currentVersion",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row {
                        IconButton(onClick = onRefresh, enabled = !isLoading) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // 1. Version Diff Banner
                    item {
                        if (versionDiff != null) {
                            if (versionDiff.hasUpdate) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Что нового (v${versionDiff.currentVersion} ➔ v${versionDiff.latestVersion})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Badge {
                                                Text("+${versionDiff.newerReleasesCount}")
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = versionDiff.aggregatedChangelog,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        when (downloadState) {
                                            is GitHubUpdateManager.DownloadState.Downloading -> {
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    LinearProgressIndicator(
                                                        progress = { downloadState.progressPercent / 100f },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Скачивание: ${downloadState.progressPercent}%",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                            is GitHubUpdateManager.DownloadState.ReadyToInstall -> {
                                                Button(
                                                    onClick = {
                                                        GitHubUpdateManager.getInstance(context)
                                                            .installApk(downloadState.apkFile)
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Установить готовое обновление")
                                                }
                                            }
                                            else -> {
                                                val latestNewerRelease = releases.firstOrNull { it.isNewer }
                                                val apkUrl = latestNewerRelease?.apkDownloadUrl
                                                if (apkUrl != null) {
                                                    Button(
                                                        onClick = { onDownloadRelease(apkUrl) },
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Скачать обновление v${versionDiff.latestVersion}")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "У вас самая актуальная версия (v$currentVersion)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "Все новые возможности и исправления уже активны.",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. All Releases Section Header
                    item {
                        Text(
                            text = "ИСТОРИЯ РЕЛИЗОВ GITHUB (${releases.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (releases.isEmpty() && !isLoading) {
                        item {
                            Text(
                                text = "Нет данных о релизах. Нажмите «Обновить» для загрузки из GitHub.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }

                    // 3. Release Items List
                    items(releases, key = { it.tagName }) { release ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (release.isCurrent) {
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = release.tagName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        if (release.isCurrent) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "ТЕКУЩАЯ",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else if (release.isNewer) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.tertiary,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "НОВАЯ",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onTertiary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (release.publishedAt.isNotBlank()) {
                                        Text(
                                            text = release.publishedAt.take(10),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (release.releaseName.isNotBlank() && release.releaseName != release.tagName) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = release.releaseName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (release.changelog.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = release.changelog.trim(),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (release.apkDownloadUrl != null) {
                                        val sizeMb = if (release.apkSizeBytes > 0) {
                                            " (${release.apkSizeBytes / (1024 * 1024)} МБ)"
                                        } else ""
                                        TextButton(
                                            onClick = { onDownloadRelease(release.apkDownloadUrl) },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Скачать APK$sizeMb", fontSize = 11.sp)
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }

                                    TextButton(
                                        onClick = { onOpenUrl(release.htmlUrl) },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("GitHub", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Закрыть")
                }
            }
        }
    }
}

@Composable
fun TodayStatsCard(
    todayScreenshots: Int,
    todayCards: Int,
    dueCardsCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "$todayScreenshots",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Снимков сегодня",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Style,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "$todayCards",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Новых карточек",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = if (dueCardsCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "$dueCardsCount",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (dueCardsCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "К повторению",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun UpdateAvailableDialog(
    updateInfo: AppUpdateInfo,
    downloadState: GitHubUpdateManager.DownloadState,
    onDismiss: () -> Unit,
    onDownload: (String) -> Unit,
    onOpenBrowser: (String) -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Новое обновление!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Версия ${updateInfo.tagName}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "(у вас v${updateInfo.currentVersion})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (updateInfo.changelog.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Что нового:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = updateInfo.changelog,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Download Progress / Status
                when (downloadState) {
                    is GitHubUpdateManager.DownloadState.Downloading -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Скачивание APK...", fontSize = 12.sp)
                                Text("${downloadState.progressPercent}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { downloadState.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                    is GitHubUpdateManager.DownloadState.ReadyToInstall -> {
                        Text(
                            text = "Файл APK готов к установке!",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    is GitHubUpdateManager.DownloadState.Error -> {
                        Text(
                            text = "Ошибка загрузки: ${downloadState.message}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    is GitHubUpdateManager.DownloadState.Idle -> {
                        if (updateInfo.apkDownloadUrl != null) {
                            Text(
                                text = "Обновление будет скачано и установлено прямо в приложении (OTA).",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "APK не прикреплен к релизу. Вы можете открыть релиз на странице GitHub.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Позже")
                    }

                    val isDownloading = downloadState is GitHubUpdateManager.DownloadState.Downloading
                    val isReady = downloadState is GitHubUpdateManager.DownloadState.ReadyToInstall

                    Button(
                        onClick = {
                            when {
                                downloadState is GitHubUpdateManager.DownloadState.ReadyToInstall -> {
                                    GitHubUpdateManager.getInstance(context).installApk(downloadState.apkFile)
                                }
                                updateInfo.apkDownloadUrl != null -> {
                                    onDownload(updateInfo.apkDownloadUrl)
                                }
                                else -> {
                                    onOpenBrowser(updateInfo.htmlUrl)
                                }
                            }
                        },
                        enabled = !isDownloading,
                        modifier = Modifier.weight(1f)
                    ) {
                        when {
                            isDownloading -> Text("Загрузка...")
                            isReady -> Text("Установить")
                            updateInfo.apkDownloadUrl != null -> Text("Скачать")
                            else -> Text("На GitHub")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyCardsPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Style,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Колода пока пуста",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Перейдите во вкладку «Снимки» и нажмите кнопку ✨ со звездочками на любом кадре. Нейросеть распознает субтитры и сформирует карточку.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun ScreenshotPreviewDialog(
    item: CapturedScreenshot,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onAiAnalyze: () -> Unit
) {
    val fullBitmap = rememberFullBitmap(item.file)
    val ocrResult by produceState<SubtitleDetectionResult?>(initialValue = null, key1 = item.file.absolutePath) {
        value = withContext(Dispatchers.IO) {
            OcrSubtitleDetector.getInstance().detectSubtitles(item.file)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Просмотр кадра",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.formattedDate,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (fullBitmap != null) {
                        Image(
                            bitmap = fullBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // OCR Subtitle Detection Preview
                val currentOcr = ocrResult
                if (currentOcr != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (currentOcr.hasSubtitles) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (currentOcr.hasSubtitles) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (currentOcr.hasSubtitles) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentOcr.hasSubtitles) {
                                        "ML Kit: Субтитры найдены (${currentOcr.englishWordCount} сл.)"
                                    } else {
                                        "ML Kit: Текст субтитров не обнаружен"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentOcr.hasSubtitles) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                            if (currentOcr.detectedText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "«${currentOcr.detectedText.take(160)}»",
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Удалить")
                    }

                    Button(
                        onClick = onAiAnalyze,
                        modifier = Modifier.weight(1.4f)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Создать с ИИ")
                    }
                }
            }
        }
    }
}

private fun shareExportFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "SubSnap Anki Deck")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Экспорт колоды Anki"))
    } catch (e: Exception) {
        Toast.makeText(context, "Файл экспортирован: ${file.name}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun ServiceStatusBadge(isRunning: Boolean) {
    val bg = if (isRunning) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
    val text = if (isRunning) "АКТИВЕН" else "ОСТАНОВЛЕН"

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(bg)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, color = bg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ControlPanelCard(
    hasOverlay: Boolean,
    hasNotification: Boolean,
    serviceState: ScreenCaptureService.Companion.ServiceState,
    autoCaptureInterval: Float,
    isSmartDetection: Boolean = true,
    onToggleAutoCapture: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestNotification: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Управление захватом",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            PermissionRow(
                icon = Icons.Default.Layers,
                title = "Поверх других приложений",
                isGranted = hasOverlay,
                actionText = "Разрешить",
                onActionClick = onRequestOverlay
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(modifier = Modifier.height(8.dp))
                PermissionRow(
                    icon = Icons.Default.Notifications,
                    title = "Уведомление службы",
                    isGranted = hasNotification,
                    actionText = "Разрешить",
                    onActionClick = onRequestNotification
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!serviceState.isRunning) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 При запросе разрешения Android обязательно выберите «Весь экран», чтобы захват не отключался в полноэкранном режиме и видеоплеерах.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onStartService,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Запустить плавающий виджет", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onStopService,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Остановить захват", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Auto-capture toggle switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Авто-захват субтитров",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (serviceState.isAutoCapture) Color(0xFF10B981) else Color(0xFF9E9E9E)
                            ) {
                                Text(
                                    text = if (serviceState.isAutoCapture) "АКТИВЕН" else "ВЫКЛ",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (serviceState.isAutoCapture) {
                                if (isSmartDetection)
                                    "⚡ Умный детектор субтитров (захват только при появлении нового текста)"
                                else
                                    "Сканирует экран каждые ${String.format(java.util.Locale.US, "%.1f", autoCaptureInterval)}с"
                            } else {
                                "Кадры делаются по нажатию на плавающую кнопку"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = serviceState.isAutoCapture,
                        onCheckedChange = { onToggleAutoCapture() }
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isGranted: Boolean,
    actionText: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, fontSize = 14.sp)
        }

        if (isGranted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("ОК", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            OutlinedButton(
                onClick = onActionClick,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(actionText, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun EmptyScreenshotsPlaceholder(serviceRunning: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Снимков пока нет",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (serviceRunning) {
                    "Служба активна! Откройте YouTube, плеер или игру и нажмите на плавающий кружок, чтобы захватить субтитры."
                } else {
                    "Запустите службу захвата выше, чтобы появился плавающий виджет для создания скриншотов поверх любых приложений."
                },
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun rememberThumbnail(file: File): Bitmap? {
    return produceState<Bitmap?>(initialValue = null, key1 = file.absolutePath) {
        value = withContext(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 4
                }
                BitmapFactory.decodeFile(file.absolutePath, options)
            } catch (e: Exception) {
                null
            }
        }
    }.value
}

@Composable
fun rememberFullBitmap(file: File): Bitmap? {
    return produceState<Bitmap?>(initialValue = null, key1 = file.absolutePath) {
        value = withContext(Dispatchers.IO) {
            try {
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (e: Exception) {
                null
            }
        }
    }.value
}

