package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import com.example.ui.viewmodel.LectureViewModel
import kotlinx.coroutines.launch
import java.io.File

/**
 * Dedicated Audio Transcription dialog using model: gemini-3.5-transcribe
 * Allows users to input audio with their microphone and get word-for-word AI transcription.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTranscriptionDialog(
    viewModel: LectureViewModel,
    onDismiss: () -> Unit,
    onTranscriptionComplete: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
            viewModel.recorder.startRecording("تفريغ_صوتي_سريع")
        }
    }

    val isRecording by viewModel.recorder.isRecording.collectAsState()
    val isPaused by viewModel.recorder.isPaused.collectAsState()
    val durationSeconds by viewModel.recorder.durationSeconds.collectAsState()

    var isTranscribing by remember { mutableStateOf(false) }
    var transcriptionResult by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var recordedAudioFile by remember { mutableStateOf<File?>(null) }
    var showApiKeyDialogInSheet by remember { mutableStateOf(false) }

    if (showApiKeyDialogInSheet) {
        ApiKeySettingsDialog(
            apiKeyManager = viewModel.apiKeyManager,
            onDismiss = { showApiKeyDialogInSheet = false }
        )
    }

    // Pulse animation for recording microphone
    val infiniteTransition = rememberInfiniteTransition(label = "transcribePulse")
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
        modifier = Modifier.testTag("audio_transcription_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = "التفريغ الصوتي الذكي الحرفي (Gemini Audio) 🎙️",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "تحدث بصوتك وسيقوم النموذج بتحويل كلامك الصوتي مباشرة إلى نص مكتوب بدقة فائقة.",
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
                            text = "يرجى منح إذن الميكروفون للتحدث وتسجيل الصوت",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            modifier = Modifier.testTag("transcribe_grant_mic_button")
                        ) {
                            Text("منح الإذن الآن")
                        }
                    }
                }
            } else {
                // Microphone Visualizer
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(110.dp)
                        .padding(vertical = 4.dp)
                ) {
                    if (isRecording && !isPaused) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(70.dp)
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
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                // Timer & Status
                val minutes = durationSeconds / 60
                val seconds = durationSeconds % 60
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = if (isRecording && !isPaused) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = when {
                        isTranscribing -> "جاري تحويل الصوت إلى نص عبر gemini-3.5-transcribe... ⏳"
                        isRecording -> "جاري الاستماع لصوتك عبر الميكروفون... 🔴"
                        transcriptionResult != null -> "تم التفريغ بنجاح! ✅"
                        else -> "اضغط على زر التسجيل وتحدث بصوتك"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Recording Actions Row
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isRecording) {
                        Button(
                            onClick = {
                                transcriptionResult = null
                                errorMessage = null
                                val file = viewModel.recorder.startRecording("transcribe_${System.currentTimeMillis()}")
                                recordedAudioFile = file
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("start_transcribe_record_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تحدث الآن بالميكروفون للتفريغ 🎙️", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                val file = viewModel.recorder.stopRecording() ?: recordedAudioFile
                                coroutineScope.launch {
                                    isTranscribing = true
                                    errorMessage = null
                                    try {
                                        val targetFile = file
                                        if (targetFile == null || !targetFile.exists() || targetFile.length() < 300) {
                                            errorMessage = "لم يتم التقاط أي صوت، يرجى التحدث بوضوح بالقرب من الميكروفون."
                                            return@launch
                                        }

                                        if (!viewModel.apiKeyManager.hasValidKey()) {
                                            errorMessage = "لتفريغ الصوت المسجل بالذكاء الاصطناعي، يرجى إدخال مفتاح Gemini API."
                                            return@launch
                                        }

                                        val text = viewModel.repository.transcribeAudio(targetFile)
                                        if (text.isNotBlank()) {
                                            transcriptionResult = text
                                            onTranscriptionComplete?.invoke(text)
                                        } else {
                                            errorMessage = "لم يتمكن النموذج من استخراج كلمات واضحة من الصوت المسجل."
                                        }
                                    } catch (e: Exception) {
                                        val err = e.message ?: "حدث خطأ أثناء التفريغ"
                                        errorMessage = "تعذر التفريغ: $err"
                                    } finally {
                                        isTranscribing = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .testTag("stop_and_transcribe_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("⏹️ إيقاف وتفريغ الصوت المسجل بدقة", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Loading Indicator
                AnimatedVisibility(visible = isTranscribing) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("جاري الاستماع وتفريغ الكلمات المنطوقة بدقة بالغة...", fontSize = 12.sp)
                    }
                }

                // Error Notice & API Key helper
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            if (!viewModel.apiKeyManager.hasValidKey()) {
                                Button(
                                    onClick = { showApiKeyDialogInSheet = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إدخال مفتاح Gemini API الآن 🔑", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Transcribed Result Card
                if (!transcriptionResult.isNullOrBlank()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("transcription_result_card")
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
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Article, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = "النص المفرغ من الصوت:",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Transcribed Text", transcriptionResult))
                                            Toast.makeText(context, "تم نسخ النص إلى الحافظة!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            Text(
                                text = transcriptionResult ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 22.sp,
                                modifier = Modifier.testTag("transcribed_text_display")
                            )

                            // Quick Action: Save to Notebook / Lecture
                            Button(
                                onClick = {
                                    val text = transcriptionResult ?: ""
                                    viewModel.stopRecording(
                                        lectureTitle = "تفريغ صوتي ${System.currentTimeMillis() % 1000}",
                                        selectedFolder = null,
                                        autoAnalyze = true,
                                        userNotes = text
                                    )
                                    onDismiss()
                                    Toast.makeText(context, "تم إنشاء ورقة المحاضرة وحفظ التفريغ!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حفظ كنوتة ومحاضرة في الورقة الدفترية 📝")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
