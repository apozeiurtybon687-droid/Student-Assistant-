package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.ui.viewmodel.LiveVoiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveVoiceScreen(
    liveVoiceViewModel: LiveVoiceViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    val conversation by liveVoiceViewModel.conversation.collectAsState()
    val isSpeakingToLive by liveVoiceViewModel.isSpeakingToLive.collectAsState()
    val isLiveResponding by liveVoiceViewModel.isLiveResponding.collectAsState()
    val recordDuration by liveVoiceViewModel.recorder.durationSeconds.collectAsState()

    var textFallback by remember { mutableStateOf("") }

    // Pulse animation for live voice ring
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSpeakingToLive) 1.35f else if (isLiveResponding) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSpeakingToLive) 600 else 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveScale"
    )

    LaunchedEffect(conversation.size) {
        if (conversation.isNotEmpty()) {
            listState.animateScrollToItem(conversation.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "المحادثة الصوتية المباشرة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Badge(containerColor = Color(0xFF0D9488)) {
                                Text("gemini-3.8-live", color = Color.White, fontSize = 9.sp)
                            }
                        }
                        Text(
                            text = "تحدث صوتياً واحصل على ردود ذكية وفورية من Live API",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            liveVoiceViewModel.stopAudioPlayback()
                            onBack()
                        },
                        modifier = Modifier.testTag("live_voice_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { liveVoiceViewModel.stopAudioPlayback() }) {
                        Icon(Icons.AutoMirrored.Filled.VolumeMute, contentDescription = "كتم الصوت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Status Header Banner
            Surface(
                color = if (isSpeakingToLive) Color(0xFFDC2626).copy(alpha = 0.15f)
                else if (isLiveResponding) Color(0xFF0D9488).copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val statusText = when {
                        isSpeakingToLive -> "جاري الاستماع لصوتك ($recordDuration ثانية)... اضغط مرة أخرى للإنهاء 🔴"
                        isLiveResponding -> "جيميني يجهز الرد الصوتي المباشر عبر gemini-3.8-live... ⏳"
                        else -> "Live API متصل وجاهز للمحادثة الصوتية 🟢"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSpeakingToLive) Color(0xFFDC2626) else if (isLiveResponding) Color(0xFF0D9488) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Conversation history thread
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(conversation) { turn ->
                    val isStudent = turn.speaker == "الطالب"
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isStudent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isStudent) Icons.Default.Person else Icons.Default.RecordVoiceOver,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isStudent) MaterialTheme.colorScheme.primary else Color(0xFF0D9488)
                                    )
                                    Text(
                                        text = turn.speaker,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                if (turn.hasAudio) {
                                    Badge(containerColor = Color(0xFF0D9488)) {
                                        Text("صوت 🔊", color = Color.White, fontSize = 10.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = turn.text,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }

            // Big Animated Live Voice Microphone Centerpiece
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(110.dp)
                ) {
                    if (isSpeakingToLive || isLiveResponding) {
                        Box(
                            modifier = Modifier
                                .size(105.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    if (isSpeakingToLive) Color(0xFFDC2626).copy(alpha = 0.3f)
                                    else Color(0xFF0D9488).copy(alpha = 0.3f)
                                )
                        )
                    }

                    FloatingActionButton(
                        onClick = {
                            if (!hasMicPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@FloatingActionButton
                            }
                            if (isSpeakingToLive) {
                                liveVoiceViewModel.finishSpeakingAndSend()
                            } else {
                                liveVoiceViewModel.startSpeaking()
                            }
                        },
                        modifier = Modifier
                            .size(76.dp)
                            .testTag("live_mic_toggle_button"),
                        shape = CircleShape,
                        containerColor = if (isSpeakingToLive) Color(0xFFDC2626) else if (isLiveResponding) Color(0xFF0D9488) else MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = if (isSpeakingToLive) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "تحدث مباشرة",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Text(
                    text = if (isSpeakingToLive) "اضغط لإرسال صوتك لنموذج Live" else "اضغط على الميكروفون وابدأ الحديث مع جيميني",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Text Input Fallback
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = textFallback,
                        onValueChange = { textFallback = it },
                        placeholder = { Text("أو اكتب سؤالك لـ Live API هنا...", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("live_voice_text_input"),
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (textFallback.isNotBlank()) {
                                val t = textFallback
                                textFallback = ""
                                liveVoiceViewModel.sendTextToLive(t)
                            }
                        },
                        enabled = textFallback.isNotBlank() && !isLiveResponding,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (textFallback.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray)
                            .testTag("live_voice_send_text_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
