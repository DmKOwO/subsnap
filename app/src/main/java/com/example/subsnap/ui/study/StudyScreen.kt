package com.example.subsnap.ui.study

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subsnap.data.ReviewRating
import com.example.subsnap.data.SpacedRepetition
import com.example.subsnap.data.model.AnkiCard
import com.example.subsnap.tts.TtsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    allCards: List<AnkiCard>,
    onCardUpdated: (AnkiCard) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        onBack()
    }

    val context = LocalContext.current
    val ttsHelper = remember { TtsHelper.getInstance(context) }

    // Session queue (initialized once per session)
    val studyQueue = remember {
        val due = SpacedRepetition.getDueCards(allCards)
        val initialList = if (due.isNotEmpty()) due else allCards
        mutableStateListOf<AnkiCard>().apply { addAll(initialList) }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var sessionCompleted by remember { mutableStateOf(false) }
    var reviewedCount by remember { mutableIntStateOf(0) }
    var againCount by remember { mutableIntStateOf(0) }
    var hardCount by remember { mutableIntStateOf(0) }
    var goodCount by remember { mutableIntStateOf(0) }
    var easyCount by remember { mutableIntStateOf(0) }

    // 3D Flip animation
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "CardFlipAnimation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Интервальное повторение",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!sessionCompleted && studyQueue.isNotEmpty()) {
                            Text(
                                text = "Карточка ${currentIndex + 1} из ${studyQueue.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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
            if (studyQueue.isEmpty()) {
                EmptyDeckStudyPlaceholder(onBack = onBack)
            } else if (sessionCompleted) {
                StudyCompletedScreen(
                    totalReviewed = reviewedCount,
                    againCount = againCount,
                    hardCount = hardCount,
                    goodCount = goodCount,
                    easyCount = easyCount,
                    onRestart = {
                        studyQueue.clear()
                        studyQueue.addAll(allCards)
                        currentIndex = 0
                        isFlipped = false
                        sessionCompleted = false
                        reviewedCount = 0
                        againCount = 0
                        hardCount = 0
                        goodCount = 0
                        easyCount = 0
                    },
                    onBack = onBack
                )
            } else {
                val currentCard = studyQueue.getOrNull(currentIndex)
                if (currentCard == null) {
                    sessionCompleted = true
                    return@Column
                }

                // Progress Bar
                LinearProgressIndicator(
                    progress = { (currentIndex.toFloat() / studyQueue.size.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Flip Flashcard with 3D perspective
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .graphicsLayer {
                                rotationY = rotation
                                cameraDistance = 14f * density
                            }
                            .clickable {
                                isFlipped = !isFlipped
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (rotation <= 90f) {
                            // FRONT SIDE
                            CardFront(
                                card = currentCard,
                                onTtsClick = { ttsHelper.speak(currentCard.sentence) }
                            )
                        } else {
                            // BACK SIDE (mirrored so text reads normally)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { rotationY = 180f }
                            ) {
                                CardBack(
                                    card = currentCard,
                                    onTtsWordClick = { ttsHelper.speak(currentCard.targetWord) },
                                    onTtsSentenceClick = { ttsHelper.speak(currentCard.sentence) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons below card
                    if (!isFlipped) {
                        Button(
                            onClick = { isFlipped = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.TouchApp, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Показать ответ", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        // Rating buttons: Again, Hard, Good, Easy
                        RatingButtonsRow(
                            card = currentCard,
                            onRatingSelected = { rating ->
                                val updated = SpacedRepetition.calculateNextReview(currentCard, rating)
                                onCardUpdated(updated)
                                reviewedCount++

                                when (rating) {
                                    ReviewRating.AGAIN -> {
                                        againCount++
                                        // Re-queue card in current session to review again
                                        studyQueue.add(updated)
                                    }
                                    ReviewRating.HARD -> hardCount++
                                    ReviewRating.GOOD -> goodCount++
                                    ReviewRating.EASY -> easyCount++
                                }

                                if (currentIndex + 1 < studyQueue.size) {
                                    isFlipped = false
                                    currentIndex++
                                } else {
                                    sessionCompleted = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CardFront(
    card: AnkiCard,
    onTtsClick: () -> Unit
) {
    val bitmap = rememberCardBitmap(card.screenshotFile)

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Screenshot Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Scene screenshot",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Highlighted Target Word in Sentence
            val annotatedSentence = buildAnnotatedString {
                val full = card.sentence
                val target = card.targetWord.trim()
                val idx = if (target.isNotBlank()) full.indexOf(target, ignoreCase = true) else -1

                if (idx >= 0) {
                    append(full.substring(0, idx))
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    ) {
                        append(full.substring(idx, idx + target.length))
                    }
                    append(full.substring(idx + target.length))
                } else {
                    append(full)
                }
            }

            Text(
                text = annotatedSentence,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Audio button
            IconButton(
                onClick = onTtsClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Озвучить предложение",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Нажмите, чтобы перевернуть",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CardBack(
    card: AnkiCard,
    onTtsWordClick: () -> Unit,
    onTtsSentenceClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Target Word Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = card.targetWord,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onTtsWordClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Озвучить слово",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (card.transcription.isNotBlank()) {
                Text(
                    text = card.transcription,
                    fontSize = 16.sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Target Word Russian Translation
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = card.wordTranslation,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Original Sentence + Translation
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Предложение в контексте:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = onTtsSentenceClick, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = card.sentence,
                        fontSize = 15.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = card.sentenceTranslation,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (card.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "💡 Объяснение нюансов:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = card.explanation,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RatingButtonsRow(
    card: AnkiCard,
    onRatingSelected: (ReviewRating) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Again
        RatingButtonItem(
            title = "Снова",
            subtitle = SpacedRepetition.formatIntervalPreview(card, ReviewRating.AGAIN),
            containerColor = Color(0xFFEF5350),
            contentColor = Color.White,
            modifier = Modifier.weight(1f),
            onClick = { onRatingSelected(ReviewRating.AGAIN) }
        )

        // Hard
        RatingButtonItem(
            title = "Трудно",
            subtitle = SpacedRepetition.formatIntervalPreview(card, ReviewRating.HARD),
            containerColor = Color(0xFFFFA726),
            contentColor = Color.White,
            modifier = Modifier.weight(1f),
            onClick = { onRatingSelected(ReviewRating.HARD) }
        )

        // Good
        RatingButtonItem(
            title = "Хорошо",
            subtitle = SpacedRepetition.formatIntervalPreview(card, ReviewRating.GOOD),
            containerColor = Color(0xFF66BB6A),
            contentColor = Color.White,
            modifier = Modifier.weight(1f),
            onClick = { onRatingSelected(ReviewRating.GOOD) }
        )

        // Easy
        RatingButtonItem(
            title = "Легко",
            subtitle = SpacedRepetition.formatIntervalPreview(card, ReviewRating.EASY),
            containerColor = Color(0xFF42A5F5),
            contentColor = Color.White,
            modifier = Modifier.weight(1f),
            onClick = { onRatingSelected(ReviewRating.EASY) }
        )
    }
}

@Composable
fun RatingButtonItem(
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(text = subtitle, fontSize = 10.sp, color = contentColor.copy(alpha = 0.85f), maxLines = 1)
        }
    }
}

@Composable
fun StudyCompletedScreen(
    totalReviewed: Int,
    againCount: Int,
    hardCount: Int,
    goodCount: Int,
    easyCount: Int,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Сессия завершена!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Отличная работа! Карточки распределены по интервалам памяти.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatRow("Всего ответов", "$totalReviewed")
                    StatRow("Снова (повторено)", "$againCount")
                    StatRow("Трудно", "$hardCount")
                    StatRow("Хорошо", "$goodCount")
                    StatRow("Легко", "$easyCount")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Вернуться к колоде", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Повторить снова")
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EmptyDeckStudyPlaceholder(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Нет карточек для повторения",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Создайте карточки из скриншотов во вкладке «Снимки» с помощью кнопки ✨ ИИ Gemini.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onBack) {
                Text("Назад к приложению")
            }
        }
    }
}

@Composable
fun rememberCardBitmap(file: File): Bitmap? {
    return produceState<Bitmap?>(initialValue = null, key1 = file.absolutePath) {
        value = withContext(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 2
                }
                BitmapFactory.decodeFile(file.absolutePath, options)
            } catch (e: Exception) {
                null
            }
        }
    }.value
}
