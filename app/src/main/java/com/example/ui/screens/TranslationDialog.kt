package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val SUPPORTED_LANGUAGES = listOf(
    "العربية (Arabic)" to "اللغة العربية",
    "الإنجليزية (English)" to "English",
    "الفرنسية (French)" to "Français",
    "الألمانية (German)" to "Deutsch",
    "الإسبانية (Spanish)" to "Español",
    "التركية (Turkish)" to "Türkçe",
    "الإيطالية (Italian)" to "Italiano",
    "الروسية (Russian)" to "Русский",
    "الصينية (Chinese)" to "中文",
    "اليابانية (Japanese)" to "日本語",
    "الأوردو (Urdu)" to "اردو"
)

@Composable
fun TranslationDialog(
    onDismiss: () -> Unit,
    onSelectLanguage: (String) -> Unit
) {
    var selectedLang by remember { mutableStateOf(SUPPORTED_LANGUAGES.first().second) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = "ترجمة",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "ترجمة المحاضرة والملاحظات 🌐",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
            ) {
                Text(
                    text = "اختر اللغة التي ترغب في ترجمة كلام وتفريغ وشرح الأستاذ إليها بدقة عالية وبدون أخطاء:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(SUPPORTED_LANGUAGES) { (displayName, langValue) ->
                        val isSelected = selectedLang == langValue
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedLang = langValue }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = displayName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedLang = langValue }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelectLanguage(selectedLang) },
                modifier = Modifier.testTag("confirm_translate_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("بدء الترجمة عبر جيميني")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
