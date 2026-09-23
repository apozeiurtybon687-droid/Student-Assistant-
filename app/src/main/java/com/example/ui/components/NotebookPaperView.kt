package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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

/**
 * A realistic university ruled notebook paper view for displaying
 * lectures transcribed, summarized, and explained with Gemini 3.8 Flash.
 * Supports dynamic Male (Blue) / Female (Pink) themes and Day / Night modes.
 */
@Composable
fun NotebookPaperView(
    lecture: LectureEntity,
    contentMode: NotebookContentMode = NotebookContentMode.TRANSCRIPT,
    onSummarizeClick: () -> Unit = {},
    onTranslateClick: () -> Unit = {},
    onSpeakClick: (String) -> Unit = {},
    isSpeaking: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkMode = LocalIsDarkMode.current
    val userGender = LocalUserGender.current

    var currentMode by remember { mutableStateOf(contentMode) }

    val activeText = when (currentMode) {
        NotebookContentMode.TRANSCRIPT -> lecture.transcript.ifBlank { "لا يوجد تفريغ صوتي مسجل في هذه الورقة بعد." }
        NotebookContentMode.SUMMARY -> lecture.summary.ifBlank { "لا يوجد تلخيص متاح. اضغط على زر تلخيص الورقة أعلاه." }
        NotebookContentMode.EXPLANATION -> lecture.explanation.ifBlank { "لا يوجد شرح متاح حتى الآن." }
        NotebookContentMode.TRANSLATION -> lecture.translatedText?.ifBlank { "لا توجد ترجمة مسجلة. اضغط على ترجمة الورقة لاختيار لغة." }
            ?: "لا توجد ترجمة مسجلة. اضغط على ترجمة الورقة لاختيار لغة."
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
                            text = if (userGender == UserGender.FEMALE) "📓 ورقة المحاضرة (طالبة 👩‍🎓)" else "📓 ورقة المحاضرة (طالب 👨‍🎓)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = paperTextColor
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Gemini 3.8 Flash ⚡",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Share or Copy
                    Row {
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
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "مشاركة",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("ورقة المحاضرة", activeText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ محتوى الورقة!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
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
                        .padding(top = 8.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "العنوان: ${lecture.title}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "المجلد: ${lecture.folderName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                HorizontalDivider(color = paperBorderColor, thickness = 1.dp)

                // Quick Mode Switcher on the Paper
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = currentMode == NotebookContentMode.TRANSCRIPT,
                        onClick = { currentMode = NotebookContentMode.TRANSCRIPT },
                        label = { Text("كلام الأستاذ (المفرّغ)", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    FilterChip(
                        selected = currentMode == NotebookContentMode.SUMMARY,
                        onClick = { currentMode = NotebookContentMode.SUMMARY },
                        label = { Text("الملخص والملاحظات", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                    FilterChip(
                        selected = currentMode == NotebookContentMode.TRANSLATION,
                        onClick = { currentMode = NotebookContentMode.TRANSLATION },
                        label = { Text("الترجمة", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(14.dp)) },
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
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Summarize Button
                    OutlinedButton(
                        onClick = onSummarizeClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("notebook_summarize_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
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
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.GTranslate, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ترجمة الورقة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }

                    // Speak Aloud Button
                    OutlinedButton(
                        onClick = { onSpeakClick(activeText) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("notebook_speak_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isSpeaking) "إيقاف" else "قراءة الورقة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    }
                }

                // The Written Text on the Ruled Paper
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
