package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.FolderEntity
import com.example.ui.viewmodel.LectureViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordLectureBottomSheet(
    viewModel: LectureViewModel,
    folders: List<FolderEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            viewModel.startRecording("محاضرة جديدة")
        }
    }

    var lectureTitle by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf<FolderEntity?>(folders.firstOrNull()) }
    var autoAnalyzeWithGemini by remember { mutableStateOf(true) }
    var quickNotes by remember { mutableStateOf("") }

    val isRecording by viewModel.recorder.isRecording.collectAsState()
    val isPaused by viewModel.recorder.isPaused.collectAsState()
    val durationSeconds by viewModel.recorder.durationSeconds.collectAsState()
    val amplitude by viewModel.recorder.maxAmplitude.collectAsState()

    // Pulse animation for recording microphone
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording && !isPaused) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    ModalBottomSheet(
        onDismissRequest = {
            if (isRecording) {
                viewModel.recorder.stopRecording()
            }
            onDismiss()
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("record_lecture_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "تسجيل محاضرة وكتابتها في ورقة 🎙️📝",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "يستمع التطبيق لصوت الأستاذ ويكتبه مباشرة في ورقة دفترية مع إمكانية التلخيص والترجمة عبر Gemini 3.8 Flash ⚡",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (!hasPermission) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "يرجى منح إذن الميكروفون لتسجيل صوت المحاضرة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            modifier = Modifier.testTag("grant_mic_permission_button")
                        ) {
                            Text("منح الإذن الآن")
                        }
                    }
                }
            } else {
                // Lecture Name Field
                OutlinedTextField(
                    value = lectureTitle,
                    onValueChange = { lectureTitle = it },
                    label = { Text("اسم أو عنوان المحاضرة") },
                    placeholder = { Text("مثال: المحاضرة 3 - الذكاء الاصطناعي") },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lecture_title_input")
                )

                // Folder Selection Chips
                if (folders.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "اختر المجلد / المادة:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            folders.take(4).forEach { folder ->
                                val isSelected = selectedFolder?.id == folder.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFolder = folder },
                                    label = { Text(folder.name) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null) }
                                    } else null,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }

                // Waveform / Pulse Visualizer and Timer
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(130.dp)
                        .padding(vertical = 8.dp)
                ) {
                    if (isRecording && !isPaused) {
                        Box(
                            modifier = Modifier
                                .size(115.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRecording) {
                                    if (isPaused) Color(0xFFF59E0B) else Color(0xFFDC2626)
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Mic else Icons.Default.MicNone,
                            contentDescription = "ميكروفون",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                // Timer Display
                val minutes = durationSeconds / 60
                val seconds = durationSeconds % 60
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = if (isRecording && !isPaused) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = when {
                        !isRecording -> "اضغط على بدء التسجيل عند بدء الدكتور بالشرح"
                        isPaused -> "التسجيل متوقف مؤقتاً ⏸️"
                        else -> "جاري الاستماع وتسجيل الأستاذ... 🔴"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick notes field during or before recording
                OutlinedTextField(
                    value = quickNotes,
                    onValueChange = { quickNotes = it },
                    label = { Text("ملاحظات للكتابة في الورقة مباشرة (اختياري)") },
                    placeholder = { Text("اكتب أي نقطة رئيسية ذكرها الدكتور...") },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_notes_input")
                )

                // Checkbox: Auto analyze with Gemini
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = autoAnalyzeWithGemini,
                        onCheckedChange = { autoAnalyzeWithGemini = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "كتابة المحاضرة في الورقة وتلخيصها بالذكاء الاصطناعي ✨",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Control Buttons Row
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isRecording) {
                        Button(
                            onClick = {
                                viewModel.startRecording(lectureTitle.ifBlank { "محاضرة جديدة" })
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_recording_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Color.Red)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("بدء الاستماع والتسجيل (عبر الميكروفون)", fontWeight = FontWeight.Bold)
                        }

                        // Instant Default Recorded Voice button
                        FilledTonalButton(
                            onClick = {
                                viewModel.createDefaultSampleLecture(autoSelect = true) {
                                    onDismiss()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("default_recorded_voice_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تسجيل بصوت افتراضي مسجل 🎧 (تعبئة الورقة فوراً)",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pause / Resume button
                            OutlinedButton(
                                onClick = {
                                    if (isPaused) viewModel.resumeRecording() else viewModel.pauseRecording()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("pause_resume_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPaused) "استئناف" else "إيقاف مؤقت")
                            }

                            // Stop & Save button
                            Button(
                                onClick = {
                                    viewModel.stopRecording(
                                        lectureTitle = lectureTitle,
                                        selectedFolder = selectedFolder,
                                        autoAnalyze = autoAnalyzeWithGemini,
                                        userNotes = quickNotes
                                    )
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(52.dp)
                                    .testTag("finish_save_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إنهاء وحفظ في الورقة", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
