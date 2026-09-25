package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val audioGranted = perms[Manifest.permission.RECORD_AUDIO] ?: false
        hasAudioPermission = audioGranted
        if (audioGranted) {
            // Permission granted
        }
    }

    var lectureTitle by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf<FolderEntity?>(folders.firstOrNull()) }
    var autoAnalyzeWithGemini by remember { mutableStateOf(true) }
    var quickNotes by remember { mutableStateOf("") }

    val isRecording by viewModel.recorder.isRecording.collectAsState()
    val isPaused by viewModel.recorder.isPaused.collectAsState()
    val durationSeconds by viewModel.recorder.durationSeconds.collectAsState()

    // Pulse animation for recording microphone
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording && !isPaused) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
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
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.FiberManualRecord else Icons.Default.Mic,
                    contentDescription = null,
                    tint = if (isRecording) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (isRecording) "جاري تسجيل المحاضرة الآن 🔴" else "تسجيل محاضرة جديدة 🎙️📝",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = if (isRecording)
                    "يتم الآن تسجيل الصوت بجودة عالية، اضغط على زر الإيقاف والحفظ بالأسفل لإنهاء التسجيل وتعبئة الورقة فوراً."
                else
                    "اضغط على زر بدء التسجيل بالميكروفون لبدء تسجيل كلام الأستاذ وحفظه دائماً في ذاكرة هاتفك.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Background Recording & Notification Control Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "التسجيل يعمل في الخلفية حتى بعد الخروج من التطبيق مع أزرار تحكم كاملة في شريط الإشعارات 📲",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (!hasAudioPermission) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "يرجى منح إذن الميكروفون والإشعارات لتسجيل صوت المحاضرة في الخلفية",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = {
                                val perms = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    arrayOf(Manifest.permission.RECORD_AUDIO)
                                }
                                permissionsLauncher.launch(perms)
                            },
                            modifier = Modifier.testTag("grant_mic_permission_button")
                        ) {
                            Text("منح الأذونات الآن")
                        }
                    }
                }
            } else {
                // If NOT recording: Show Title input and folder selector
                AnimatedVisibility(visible = !isRecording) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                                    text = "اختر المادة / المجلد:",
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
                                            label = { Text(folder.name, fontSize = 11.sp) },
                                            leadingIcon = if (isSelected) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                            } else null,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Quick notes
                        OutlinedTextField(
                            value = quickNotes,
                            onValueChange = { quickNotes = it },
                            label = { Text("ملاحظات يدوية للورقة (اختياري)") },
                            placeholder = { Text("اكتب أي نقطة رئيسية...") },
                            maxLines = 2,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_notes_input")
                        )
                    }
                }

                // Visualizer & Live Timer Card (Ultra-visible during recording)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRecording) {
                            if (isPaused) Color(0xFFFEF3C7) else Color(0xFFFEE2E2)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pulse Mic Circle
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(100.dp)
                        ) {
                            if (isRecording && !isPaused) {
                                Box(
                                    modifier = Modifier
                                        .size(92.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDC2626).copy(alpha = 0.25f))
                                )
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(72.dp)
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
                                    imageVector = if (isRecording) {
                                        if (isPaused) Icons.Default.Pause else Icons.Default.Mic
                                    } else {
                                        Icons.Default.MicNone
                                    },
                                    contentDescription = "ميكروفون",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        // Big Bold Timer
                        val minutes = durationSeconds / 60
                        val seconds = durationSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 34.sp
                            ),
                            color = if (isRecording && !isPaused) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                        )

                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isRecording) {
                                if (isPaused) Color(0xFFD97706) else Color(0xFFDC2626)
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        ) {
                            Text(
                                text = when {
                                    !isRecording -> "الميكروفون جاهز للتسجيل 🎙️"
                                    isPaused -> "التسجيل متوقف مؤقتاً ⏸️"
                                    else -> "جاري الاستماع وتسجيل المحاضرة... 🔴"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // ==========================================
                // ULTRA-PROMINENT RECORDING & STOP CONTROLS
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isRecording) {
                        // 1. Big Start Recording Button (Primary Mic)
                        Button(
                            onClick = {
                                viewModel.startRecording(
                                    name = lectureTitle.ifBlank { "محاضرة جديدة" },
                                    selectedFolder = selectedFolder,
                                    userNotes = quickNotes
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .testTag("start_recording_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "بدء تسجيل المحاضرة (ميكروفون) 🎙️",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // RECORDING ACTIVE CONTROLS:
                        // 1. HUGE STOP & SAVE BUTTON (Front & Center)
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .testTag("finish_save_button"),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "⏹️ إيقاف التسجيل وحفظ في الورقة",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 2. PAUSE / RESUME & CANCEL ROW
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pause / Resume Button
                            FilledTonalButton(
                                onClick = {
                                    if (isPaused) viewModel.resumeRecording() else viewModel.pauseRecording()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("pause_resume_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isPaused) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                                )
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null,
                                    tint = if (isPaused) Color(0xFF10B981) else Color(0xFFD97706)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPaused) "استئناف" else "إيقاف مؤقت",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPaused) Color(0xFF10B981) else Color(0xFFD97706)
                                )
                            }

                            // Cancel Button
                            OutlinedButton(
                                onClick = {
                                    com.example.audio.AudioRecordingService.cancel(context)
                                    viewModel.recorder.stopRecording()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(50.dp)
                                    .testTag("cancel_recording_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إلغاء التسجيل")
                            }
                        }
                    }
                }
            }
        }
    }
}
