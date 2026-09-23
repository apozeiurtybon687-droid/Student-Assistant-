package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FolderEntity
import com.example.data.local.LectureEntity
import com.example.data.local.ThemePreferences
import com.example.data.local.UserGender
import com.example.ui.theme.LocalIsDarkMode
import com.example.ui.viewmodel.LectureViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LectureViewModel,
    themePreferences: ThemePreferences = ThemePreferences.getInstance(LocalContext.current),
    onOpenLecture: (LectureEntity) -> Unit,
    onOpenLiveVoice: () -> Unit,
    onOpenChat: () -> Unit
) {
    val lectures by viewModel.lectures.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()

    val currentGender by themePreferences.gender.collectAsState()
    val isDarkMode = LocalIsDarkMode.current

    var showRecordSheet by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    if (showThemeDialog) {
        ThemeAndProfileDialog(
            themePreferences = themePreferences,
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showApiKeyDialog) {
        ApiKeySettingsDialog(
            apiKeyManager = viewModel.apiKeyManager,
            onDismiss = { showApiKeyDialog = false }
        )
    }

    if (showDownloadDialog) {
        DownloadAppDialog(
            onDismiss = { showDownloadDialog = false }
        )
    }

    if (showRecordSheet) {
        RecordLectureBottomSheet(
            viewModel = viewModel,
            folders = folders,
            onDismiss = { showRecordSheet = false }
        )
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name, color ->
                viewModel.createFolder(name, color)
                showCreateFolderDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { showThemeDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (currentGender == UserGender.FEMALE) "👩‍🎓" else "👨‍🎓",
                                fontSize = 22.sp
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "ملاحظات المحاضرات",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.clickable { showThemeDialog = true }
                                ) {
                                    Text(
                                        text = if (currentGender == UserGender.FEMALE) "طالبة 💖" else "طالب 💙",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isDarkMode) "الوضع الليلي 🌙" else "الوضع النهاري ☀️",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Theme & Profile Switcher Button (Palette)
                    IconButton(
                        onClick = { showThemeDialog = true },
                        modifier = Modifier.testTag("home_theme_settings_button")
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "تخصيص المظهر والهوية (أزرق/وردي وليلي/نهاري)",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showDownloadDialog = true },
                        modifier = Modifier.testTag("home_download_apk_button")
                    ) {
                        Icon(
                            Icons.Default.DownloadForOffline,
                            contentDescription = "تحميل التطبيق على الهاتف",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(
                        onClick = { showApiKeyDialog = true },
                        modifier = Modifier.testTag("home_api_key_settings_button")
                    ) {
                        Icon(
                            Icons.Default.VpnKey,
                            contentDescription = "إعدادات مفتاح Gemini",
                            tint = if (viewModel.apiKeyManager.hasValidKey()) MaterialTheme.colorScheme.primary else Color(0xFFDC2626)
                        )
                    }
                    IconButton(
                        onClick = onOpenChat,
                        modifier = Modifier.testTag("home_open_chat_button")
                    ) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = "المعلم الذكي",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onOpenLiveVoice,
                        modifier = Modifier.testTag("home_open_live_voice_button")
                    ) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = "محادثة صوتية مباشرة",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showRecordSheet = true },
                icon = { Icon(Icons.Default.Mic, contentDescription = null) },
                text = { Text("تسجيل محاضرة", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_record_lecture")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // API Key Warning Banner if missing or 403 error occurred
            val hasValidKey = viewModel.apiKeyManager.hasValidKey()
            if (!hasValidKey || (statusMsg?.contains("403") == true)) {
                Surface(
                    color = Color(0xFFDC2626).copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showApiKeyDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                            Text(
                                text = if (statusMsg?.contains("403") == true) "تنبيه: حدث خطأ 403 في Gemini. اضغط هنا لتعديل مفتاح API."
                                else "تنبيه: لتشغيل الذكاء الاصطناعي وتفادي خطأ 403، اضغط هنا لضبط مفتاح Gemini API.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(
                            onClick = { showApiKeyDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("ضبط المفتاح", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Status bar for ongoing analysis
            AnimatedVisibility(visible = isAnalyzing) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = statusMsg ?: "جاري تفريغ المحاضرة وتلخيصها بالذكاء الاصطناعي...",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Search Bar Across Previous Lectures
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("البحث في جميع المحاضرات، الشروحات، والملخصات...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_lectures_input")
            )

            // Quick Shortcut Action Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Live Voice Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenLiveVoice() }
                        .testTag("shortcut_live_voice"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D9488).copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFF0D9488))
                        Column {
                            Text("محادثة صوتية مباشرة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("gemini-3.8-live", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }

                // AI Tutor Chat Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenChat() }
                        .testTag("shortcut_ai_chat"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("المعلم والمراجع الذكي", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("أسئلة وشرح للمحاضرات", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // Folder Filter Row + Add Folder Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "المجلدات والمقررات 📂",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { showCreateFolderDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("add_folder_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مجلد جديد", fontSize = 12.sp)
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFolderId == null,
                        onClick = { viewModel.selectFolder(null) },
                        label = { Text("جميع المحاضرات") },
                        leadingIcon = if (selectedFolderId == null) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                items(folders) { folder ->
                    val isSelected = selectedFolderId == folder.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectFolder(folder.id) },
                        label = { Text(folder.name) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Lecture Cards Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "سجل المحاضرات (${lectures.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Lectures List
            if (lectures.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.MicNone,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "لا توجد نتائج مطابقة لبحثك" else "لا توجد محاضرات مسجلة بعد",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "اضغط على زر التسجيل بالأسفل عند بدء الدكتور بالشرح، وسيقوم التطبيق بالاستماع والتفريغ والتلخيص فوراً!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Button(
                            onClick = { showRecordSheet = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تسجيل أول محاضرة الآن")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(lectures, key = { it.id }) { lecture ->
                        LectureCard(
                            lecture = lecture,
                            onOpen = { onOpenLecture(lecture) },
                            onPlayAudio = { viewModel.playLectureAudio(lecture.audioPath) },
                            onDelete = { viewModel.deleteLecture(lecture) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LectureCard(
    lecture: LectureEntity,
    onOpen: () -> Unit,
    onPlayAudio: () -> Unit,
    onDelete: () -> Unit
) {
    val formattedDate = remember(lecture.createdAt) {
        val sdf = SimpleDateFormat("dd MMM - hh:mm a", Locale.forLanguageTag("ar"))
        sdf.format(Date(lecture.createdAt))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("lecture_card_${lecture.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Folder Tag
                SuggestionChip(
                    onClick = {},
                    label = { Text(lecture.folderName, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(8.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            // Title
            Text(
                text = lecture.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Summary snippet preview
            Text(
                text = lecture.summary.ifBlank { lecture.transcript.take(120) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            // Bottom row info & actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (lecture.durationSeconds > 0) {
                        val m = lecture.durationSeconds / 60
                        val s = lecture.durationSeconds % 60
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Text(String.format("%02d:%02d", m, s), fontSize = 11.sp, color = Color.Gray)
                    }

                    if (!lecture.translatedText.isNullOrBlank()) {
                        Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                            Text("مترجمة", color = Color.White, fontSize = 9.sp)
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!lecture.audioPath.isNullOrBlank()) {
                        IconButton(
                            onClick = onPlayAudio,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = "تشغيل الصوت", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
