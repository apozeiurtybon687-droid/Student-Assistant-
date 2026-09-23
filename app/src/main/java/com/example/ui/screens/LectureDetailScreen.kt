package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.LectureEntity
import com.example.data.local.ThemePreferences
import com.example.ui.components.NotebookPaperView
import com.example.ui.viewmodel.LectureViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LectureDetailScreen(
    lecture: LectureEntity,
    viewModel: LectureViewModel,
    themePreferences: ThemePreferences = ThemePreferences.getInstance(LocalContext.current),
    onBack: () -> Unit,
    onOpenChat: (LectureEntity) -> Unit
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showTranslateDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    if (showThemeDialog) {
        ThemeAndProfileDialog(
            themePreferences = themePreferences,
            onDismiss = { showThemeDialog = false }
        )
    }

    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()

    // Player state
    val isPlaying by viewModel.player.isPlaying.collectAsState()
    val currentPositionMs by viewModel.player.currentPositionMs.collectAsState()
    val totalDurationMs by viewModel.player.totalDurationMs.collectAsState()
    val playbackSpeed by viewModel.player.playbackSpeed.collectAsState()

    // TTS state
    val isSpeaking by viewModel.tts.isSpeaking.collectAsState()

    val formattedDate = remember(lecture.createdAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy - hh:mm a", Locale.forLanguageTag("ar"))
        sdf.format(Date(lecture.createdAt))
    }

    if (showTranslateDialog) {
        TranslationDialog(
            onDismiss = { showTranslateDialog = false },
            onSelectLanguage = { lang ->
                showTranslateDialog = false
                viewModel.translateCurrentLecture(lang)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = lecture.title,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${lecture.folderName} • $formattedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showThemeDialog = true },
                        modifier = Modifier.testTag("detail_theme_settings_button")
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "المظهر والهوية",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.deleteLecture(lecture)
                            onBack()
                        },
                        modifier = Modifier.testTag("detail_delete_button")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "حذف المحاضرة",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Status banner for AI operations
            AnimatedVisibility(visible = isAnalyzing || isTranslating || statusMsg != null) {
                Surface(
                    color = if (isAnalyzing || isTranslating) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isAnalyzing || isTranslating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = statusMsg ?: if (isAnalyzing) "يقوم جيميني بالتحليل والتفريغ والشرح..." else "جاري المعالجة...",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Audio Player Bar (if audio file exists)
            if (!lecture.audioPath.isNullOrEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (totalDurationMs == 0) {
                                            viewModel.playLectureAudio(lecture.audioPath)
                                        } else {
                                            viewModel.togglePlayPause()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .testTag("audio_play_pause_button")
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل التسجيل",
                                        tint = Color.White
                                    )
                                }

                                Column {
                                    Text(
                                        text = "التسجيل الصوتي للأستاذ 🎙️",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val currentSec = currentPositionMs / 1000
                                    val totalSec = if (totalDurationMs > 0) totalDurationMs / 1000 else lecture.durationSeconds
                                    Text(
                                        text = String.format("%02d:%02d / %02d:%02d", currentSec / 60, currentSec % 60, totalSec / 60, totalSec % 60),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Speed selection chips
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                    val isSelected = playbackSpeed == speed
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .padding(2.dp)
                                    ) {
                                        Text(
                                            text = "${speed}x",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Seekbar
                        Slider(
                            value = if (totalDurationMs > 0) currentPositionMs.toFloat() else 0f,
                            onValueChange = { viewModel.seekAudio(it.toInt()) },
                            valueRange = 0f..(if (totalDurationMs > 0) totalDurationMs.toFloat() else 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .testTag("audio_seekbar")
                        )
                    }
                }
            }

            // Quick AI Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // TTS Read Aloud Button
                FilledTonalButton(
                    onClick = {
                        if (isSpeaking) {
                            viewModel.stopSpeaking()
                        } else {
                            val textToRead = when (selectedTabIndex) {
                                0 -> "${lecture.summary}\n${lecture.keyPoints}"
                                1 -> lecture.transcript
                                2 -> lecture.explanation
                                else -> lecture.translatedText ?: lecture.summary
                            }
                            viewModel.speakText(textToRead)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tts_read_aloud_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isSpeaking) "إيقاف الصوت" else "قراءة صوتية", fontSize = 12.sp)
                }

                // Translate Button
                FilledTonalButton(
                    onClick = { showTranslateDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("translate_lecture_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ترجمة فورية", fontSize = 12.sp)
                }

                // Ask Chatbot Button
                Button(
                    onClick = { onOpenChat(lecture) },
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("ask_chatbot_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اسأل Gemini 3.8", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Navigation Scrollable Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 12.dp
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("ورقة المحاضرة 📓", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Description, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("الملخص الشامل", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("التفريغ الصوتي", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    text = { Text("الشرح والتوضيح", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Lightbulb, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 4,
                    onClick = { selectedTabIndex = 4 },
                    text = { Text("الترجمة الفورية", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.GTranslate, contentDescription = null) }
                )
            }

            // Tab Contents Scrollable View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> NotebookPaperView(
                        lecture = lecture,
                        onSummarizeClick = {
                            viewModel.reanalyzeLecture(lecture)
                            Toast.makeText(context, "جاري إعادة التلخيص عبر Gemini 3.8 Flash...", Toast.LENGTH_SHORT).show()
                        },
                        onTranslateClick = { showTranslateDialog = true },
                        onSpeakClick = { text ->
                            if (isSpeaking) viewModel.stopSpeaking() else viewModel.speakText(text)
                        },
                        isSpeaking = isSpeaking
                    )
                    1 -> SummaryAndNotesTab(lecture, context)
                    2 -> TranscriptTab(lecture, context)
                    3 -> ExplanationsTab(lecture, context)
                    4 -> TranslationTab(lecture, onOpenTranslateDialog = { showTranslateDialog = true }, context)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SummaryAndNotesTab(lecture: LectureEntity, context: Context) {
    // Summary Card
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("الملخص الشامل للمحاضرة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
                IconButton(onClick = { copyToClipboard(context, lecture.summary) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(18.dp))
                }
            }
            Text(
                text = lecture.summary.ifBlank { "لا يوجد ملخص حتى الآن." },
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
        }
    }

    // Organized Notes & Key Points Card
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = Color(0xFF10B981))
                    Text("الملاحظات والنقاط الرئيسية المنظمة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
                IconButton(onClick = { copyToClipboard(context, lecture.keyPoints) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(18.dp))
                }
            }
            Text(
                text = lecture.keyPoints.ifBlank { "لا توجد ملاحظات منشأة." },
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
        }
    }

    // Potential Exam Questions Card
    if (lecture.examQuestions.isNotBlank()) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text("أسئلة اختبار متوقعة ومصطلحات هامة 🎯", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    text = lecture.examQuestions,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
fun TranscriptTab(lecture: LectureEntity, context: Context) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("النص الكامل المفرغ لكلام الأستاذ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
                IconButton(onClick = { copyToClipboard(context, lecture.transcript) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ النص")
                }
            }
            Text(
                text = lecture.transcript.ifBlank { "لا يوجد تفريغ متوفر حالياً." },
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
fun ExplanationsTab(lecture: LectureEntity, context: Context) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF59E0B))
                    Text("شرح وتبسيط كلام ومفاهيم الأستاذ عبر جيميني", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
                IconButton(onClick = { copyToClipboard(context, lecture.explanation) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ الشرح")
                }
            }
            Text(
                text = lecture.explanation.ifBlank { "جاري إعداد الشرح الأكاديمي التوضيحي..." },
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
fun TranslationTab(
    lecture: LectureEntity,
    onOpenTranslateDialog: () -> Unit,
    context: Context
) {
    if (lecture.translatedText.isNullOrBlank()) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Text("لا توجد ترجمة محفوظة لهذه المحاضرة بعد", fontWeight = FontWeight.Bold)
                Text(
                    "يدعم التطبيق ترجمة كلام وشرح وتلخيص الأستاذ إلى أي لغة (مثل العربية أو الإنجليزية أو غيرها) بدقة تامة وبدون أخطاء",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onOpenTranslateDialog,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("بدء ترجمة المحاضرة الآن 🌐")
                }
            }
        }
    } else {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("الترجمة الأكاديمية (${lecture.translatedLanguage})", fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { copyToClipboard(context, lecture.translatedText) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ الترجمة")
                    }
                }
                Text(
                    text = lecture.translatedText,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 24.sp
                )
                OutlinedButton(
                    onClick = onOpenTranslateDialog,
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("الترجمة إلى لغة أخرى")
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Lecture Note", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "تم النسخ إلى الحافظة", Toast.LENGTH_SHORT).show()
}
