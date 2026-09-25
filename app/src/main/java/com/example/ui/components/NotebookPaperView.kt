package com.example.ui.components

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.LectureEntity
import com.example.data.local.UserGender
import com.example.ui.theme.LocalIsDarkMode
import com.example.ui.theme.LocalUserGender
import java.util.Locale

/**
 * A realistic university ruled notebook paper view for displaying
 * lectures transcribed, summarized, and explained with Gemini.
 * Supports manual writing/editing, speech dictation, dynamic Male/Female themes and Day/Night modes.
 */
@Composable
fun NotebookPaperView(
    lecture: LectureEntity,
    contentMode: NotebookContentMode = NotebookContentMode.TRANSCRIPT,
    onSummarizeClick: () -> Unit = {},
    onTranslateClick: () -> Unit = {},
    onSpeakClick: (String) -> Unit = {},
    onPlayRecordedAudio: (() -> Unit)? = null,
    isPlayingRecordedAudio: Boolean = false,
    onContentSaved: ((String, NotebookContentMode) -> Unit)? = null,
    isSpeaking: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkMode = LocalIsDarkMode.current
    val userGender = LocalUserGender.current

    var currentMode by remember { mutableStateOf(contentMode) }
    var isEditing by remember { mutableStateOf(false) }

    val rawText = when (currentMode) {
        NotebookContentMode.TRANSCRIPT -> lecture.transcript
        NotebookContentMode.SUMMARY -> lecture.summary
        NotebookContentMode.EXPLANATION -> lecture.explanation
        NotebookContentMode.TRANSLATION -> lecture.translatedText ?: ""
    }

    var editedText by remember(rawText, isEditing) { mutableStateOf(rawText) }

    val activeText = when (currentMode) {
        NotebookContentMode.TRANSCRIPT -> lecture.transcript.ifBlank { "لا يوجد كلام مسجل في هذه الورقة بعد. اضغط على أيقونة القلم ✏️ للكتابة أو استخدم الإملاء الصوتي 🎙️." }
        NotebookContentMode.SUMMARY -> lecture.summary.ifBlank { "لا يوجد تلخيص متاح. اضغط على زر تلخيص الورقة أعلاه." }
        NotebookContentMode.EXPLANATION -> lecture.explanation.ifBlank { "لا يوجد شرح متاح حتى الآن." }
        NotebookContentMode.TRANSLATION -> lecture.translatedText?.ifBlank { "لا توجد ترجمة مسجلة. اضغط على ترجمة الورقة لاختيار لغة." }
            ?: "لا توجد ترجمة مسجلة. اضغط على ترجمة الورقة لاختيار لغة."
    }

    // Android Speech Recognizer Launcher for direct voice dictation into the notebook paper
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenWords = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = spokenWords?.firstOrNull()
            if (!recognizedText.isNullOrBlank()) {
                val newContent = if (isEditing) {
                    if (editedText.isBlank()) recognizedText else "$editedText\n$recognizedText"
                } else {
                    if (rawText.isBlank() || rawText.startsWith("🎙️") || rawText.startsWith("📓")) {
                        recognizedText
                    } else {
                        "$rawText\n$recognizedText"
                    }
                }
                if (isEditing) {
                    editedText = newContent
                } else {
                    onContentSaved?.invoke(newContent, currentMode)
                }
                Toast.makeText(context, "تمت إضافة الكلام للورقة!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Dynamic Paper styling for Light vs Dark mode and Gender
    val paperBackgroundColor = if (isDarkMode) {
        if (userGender == UserGender.FEMALE) Color(0xFF241520) else Color(0xFF131D33)
    } else {
        if (userGender == UserGender.FEMALE) Color(0xFFFFF9FA) else Color(0xFFFDFBF7)
    }

    val paperBorderColor = if (isDarkMode) {
        if (userGender == UserGender.FEMALE) Color(0xFF4A203E) else Color(0xFF28385E)
    } else {
        if (userGender == UserGender.FEMALE) Color(0xFFFCE7F3) else Color(0xFFE2D9CC)
    }

    val ruledLineColor = if (isDarkMode) {
        if (userGender == UserGender.FEMALE) Color(0xFFF472B6).copy(alpha = 0.20f) else Color(0xFF60A5FA).copy(alpha = 0.20f)
    } else {
        if (userGender == UserGender.FEMALE) Color(0xFFF472B6).copy(alpha = 0.28f) else Color(0xFF93C5FD).copy(alpha = 0.35f)
    }

    val marginLineColor = if (userGender == UserGender.FEMALE) {
        Color(0xFFFB7185).copy(alpha = if (isDarkMode) 0.55f else 0.45f)
    } else {
        Color(0xFFEF4444).copy(alpha = if (isDarkMode) 0.55f else 0.45f)
    }

    val paperTextColor = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(if (isDarkMode) 8.dp else 4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, paperBorderColor, RoundedCornerShape(16.dp))
            .testTag("notebook_paper_card"),
        colors = CardDefaults.cardColors(containerColor = paperBackgroundColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Drawn notebook paper ruled lines and margin line
            Canvas(modifier = Modifier.matchParentSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Notebook vertical margin line on the right for RTL Arabic
                val marginX = canvasWidth - 36.dp.toPx()
                drawLine(
                    color = marginLineColor,
                    start = Offset(marginX, 0f),
                    end = Offset(marginX, canvasHeight),
                    strokeWidth = 2.dp.toPx()
                )

                // Ruled horizontal notebook lines
                val lineSpacing = 32.dp.toPx()
                var currentY = 120.dp.toPx()
                while (currentY < canvasHeight) {
                    drawLine(
                        color = ruledLineColor,
                        start = Offset(0f, currentY),
                        end = Offset(canvasWidth, currentY),
                        strokeWidth = 1.dp.toPx()
                    )
                    currentY += lineSpacing
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                // Notebook Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (userGender == UserGender.FEMALE) "📓 دفتر المحاضرات (طالبة 👩‍🎓)" else "📓 دفتر المحاضرات (طالب 👨‍🎓)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = paperTextColor
                        )
                    }

                    // Action buttons on top-right: Edit pen, Voice dictation, Share, Copy
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Dictate button (speech to text into paper)
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث لإضافة كلامك إلى ورقة المحاضرة...")
                                    }
                                    speechLauncher.launch(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "التعرف على الصوت غير مدعوم على هذا الجهاز", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "إملاء صوتي في الورقة",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Edit / Pen Button
                        IconButton(
                            onClick = {
                                if (isEditing) {
                                    // Save changes
                                    onContentSaved?.invoke(editedText, currentMode)
                                    isEditing = false
                                    Toast.makeText(context, "تم حفظ ما كتبته في الورقة! 📝", Toast.LENGTH_SHORT).show()
                                } else {
                                    editedText = rawText
                                    isEditing = true
                                }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = if (isEditing) "حفظ ما كُتب" else "كتابة وتعديل في الورقة",
                                tint = if (isEditing) Color(0xFF10B981) else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Share
                        IconButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TITLE, lecture.title)
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "محاضرة: ${lecture.title}\nالمادة: ${lecture.folderName}\n\n$activeText"
                                    )
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "مشاركة ورقة المحاضرة")
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "مشاركة",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Copy
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("ورقة المحاضرة", if (isEditing) editedText else activeText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ محتوى الورقة!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "نسخ",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Paper metadata info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "العنوان: ${lecture.title}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "المادة: ${lecture.folderName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                HorizontalDivider(color = paperBorderColor, thickness = 1.dp)

                // Verified Recorded Audio & Paper Status Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isPlayingRecordedAudio) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp)
                        .testTag("recorded_audio_paper_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isPlayingRecordedAudio) Icons.Default.GraphicEq else Icons.Default.Mic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "📝 الورقة مملوءة بما تم تسجيله صوتياً",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (isPlayingRecordedAudio) "جاري تشغيل الصوت المسجل متزامناً مع الورقة 🔊"
                                    else "المدة: ${lecture.durationSeconds} ثانية • النص مكتوب ومطابق للصوت",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        if (!lecture.audioPath.isNullOrEmpty() && onPlayRecordedAudio != null) {
                            FilledTonalButton(
                                onClick = onPlayRecordedAudio,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("play_from_paper_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlayingRecordedAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPlayingRecordedAudio) "إيقاف" else "استمع للصوت",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Quick Mode Switcher on the Paper
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = currentMode == NotebookContentMode.TRANSCRIPT,
                        onClick = {
                            if (isEditing) {
                                onContentSaved?.invoke(editedText, currentMode)
                                isEditing = false
                            }
                            currentMode = NotebookContentMode.TRANSCRIPT
                        },
                        label = { Text("كلام الأستاذ المسجل (المفرغ في الورقة)", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    FilterChip(
                        selected = currentMode == NotebookContentMode.SUMMARY,
                        onClick = {
                            if (isEditing) {
                                onContentSaved?.invoke(editedText, currentMode)
                                isEditing = false
                            }
                            currentMode = NotebookContentMode.SUMMARY
                        },
                        label = { Text("الملخص والملاحظات", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                    FilterChip(
                        selected = currentMode == NotebookContentMode.EXPLANATION,
                        onClick = {
                            if (isEditing) {
                                onContentSaved?.invoke(editedText, currentMode)
                                isEditing = false
                            }
                            currentMode = NotebookContentMode.EXPLANATION
                        },
                        label = { Text("الشرح", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                }

                // Action Bar inside the notebook paper
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Summarize Button
                    OutlinedButton(
                        onClick = onSummarizeClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("notebook_summarize_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تلخيص الورقة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    // Translate Button
                    OutlinedButton(
                        onClick = onTranslateClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("notebook_translate_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.GTranslate, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ترجمة الورقة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }

                    // Speak Aloud Button
                    OutlinedButton(
                        onClick = { onSpeakClick(if (isEditing) editedText else activeText) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("notebook_speak_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isSpeaking) "إيقاف" else "قراءة الورقة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    }
                }

                // If in Edit Mode, show editable text area with save button
                if (isEditing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "✍️ وضع الكتابة والتدوين المباشر في الورقة",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(
                                        onClick = { isEditing = false },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("إلغاء", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = {
                                            onContentSaved?.invoke(editedText, currentMode)
                                            isEditing = false
                                            Toast.makeText(context, "تم حفظ التعديلات في دفتر المحاضرات!", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("حفظ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = editedText,
                            onValueChange = { editedText = it },
                            placeholder = { Text("اكتب ملاحظاتك ونقاط المحاضرة هنا...", color = paperTextColor.copy(alpha = 0.5f)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 160.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = paperTextColor,
                                unfocusedTextColor = paperTextColor,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = paperBorderColor
                            )
                        )
                    }
                } else {
                    // Display Written Text on the Ruled Paper
                    Text(
                        text = activeText,
                        fontSize = 15.sp,
                        lineHeight = 32.sp, // Aligned with the 32dp ruled lines
                        fontWeight = FontWeight.Normal,
                        color = paperTextColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 16.dp)
                            .testTag("notebook_paper_text_content")
                    )

                    // Quick prompt to write if paper has placeholder or default text
                    if (rawText.isBlank() || rawText.startsWith("🎙️") || rawText.startsWith("📓")) {
                        OutlinedButton(
                            onClick = {
                                editedText = rawText
                                isEditing = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.BorderColor, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("كتابة وتدوين ملاحظات يدوياً في هذه الورقة ✍️", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

enum class NotebookContentMode {
    TRANSCRIPT,
    SUMMARY,
    EXPLANATION,
    TRANSLATION
}
