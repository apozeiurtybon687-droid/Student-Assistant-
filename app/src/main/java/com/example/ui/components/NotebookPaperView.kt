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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.LectureEntity

/**
 * A realistic university ruled notebook paper view for displaying
 * lectures transcribed, summarized, and explained with Gemini 3.8 Flash.
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
    var currentMode by remember { mutableStateOf(contentMode) }

    val activeText = when (currentMode) {
        NotebookContentMode.TRANSCRIPT -> lecture.transcript.ifBlank { "لا يوجد تفريغ صوتي مسجل في هذه الورقة بعد." }
        NotebookContentMode.SUMMARY -> lecture.summary.ifBlank { "لا يوجد تلخيص متاح. اضغط على زر تلخيص الورقة أعلاه." }
        NotebookContentMode.EXPLANATION -> lecture.explanation.ifBlank { "لا يوجد شرح متاح حتى الآن." }
        NotebookContentMode.TRANSLATION -> lecture.translatedText?.ifBlank { "لا توجد ترجمة مسجلة. اضغط على ترجمة الورقة لاختيار لغة." }
            ?: "لا توجد ترجمة مسجلة. اضغط على ترجمة الورقة لاختيار لغة."
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE2D9CC), RoundedCornerShape(16.dp))
            .testTag("notebook_paper_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBF7)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Drawn notebook paper ruled lines and red margin line
            Canvas(modifier = Modifier.matchParentSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Notebook vertical red margin line on the right for RTL Arabic
                val marginX = canvasWidth - 36.dp.toPx()
                drawLine(
                    color = Color(0xFFEF4444).copy(alpha = 0.45f),
                    start = Offset(marginX, 0f),
                    end = Offset(marginX, canvasHeight),
                    strokeWidth = 2.dp.toPx()
                )

                // Ruled blue horizontal notebook lines
                val lineSpacing = 32.dp.toPx()
                var currentY = 120.dp.toPx()
                while (currentY < canvasHeight) {
                    drawLine(
                        color = Color(0xFF93C5FD).copy(alpha = 0.35f),
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
                            text = "📓 ورقة المحاضرة الجامعية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1E293B)
                        )
                        Surface(
                            color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Gemini 3.8 Flash ⚡",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8),
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
                                tint = Color(0xFF64748B),
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
                                tint = Color(0xFF64748B),
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
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = "المجلد: ${lecture.folderName}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                HorizontalDivider(color = Color(0xFFE2D9CC), thickness = 1.dp)

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
                            selectedContainerColor = Color(0xFF2563EB).copy(alpha = 0.15f),
                            selectedLabelColor = Color(0xFF1D4ED8)
                        )
                    )
                    FilterChip(
                        selected = currentMode == NotebookContentMode.SUMMARY,
                        onClick = { currentMode = NotebookContentMode.SUMMARY },
                        label = { Text("الملخص والملاحظات", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF059669).copy(alpha = 0.15f),
                            selectedLabelColor = Color(0xFF047857)
                        )
                    )
                    FilterChip(
                        selected = currentMode == NotebookContentMode.TRANSLATION,
                        onClick = { currentMode = NotebookContentMode.TRANSLATION },
                        label = { Text("الترجمة", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF7C3AED).copy(alpha = 0.15f),
                            selectedLabelColor = Color(0xFF6D28D9)
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
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF059669))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تلخيص الورقة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
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
                        Icon(Icons.Default.GTranslate, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF7C3AED))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ترجمة الورقة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
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
                            tint = Color(0xFFD97706)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isSpeaking) "إيقاف" else "قراءة الورقة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                    }
                }

                // The Written Text on the Ruled Paper
                Text(
                    text = activeText,
                    fontSize = 15.sp,
                    lineHeight = 32.sp, // Aligned with the 32dp ruled lines
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF0F172A),
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
