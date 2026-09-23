package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val FOLDER_COLORS = listOf(
    "#2563EB" to Color(0xFF2563EB),
    "#7C3AED" to Color(0xFF7C3AED),
    "#059669" to Color(0xFF059669),
    "#DC2626" to Color(0xFFDC2626),
    "#D97706" to Color(0xFFD97706),
    "#0891B2" to Color(0xFF0891B2),
    "#4F46E5" to Color(0xFF4F46E5),
    "#DB2777" to Color(0xFFDB2777)
)

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf(FOLDER_COLORS.first().first) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إنشاء مجلد / مادة جديدة 📁",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("اسم المادة أو المجلد") },
                    placeholder = { Text("مثال: هندسة البرمجيات، الكيمياء...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folder_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "اختر لون المجلد:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FOLDER_COLORS.take(5).forEach { (hex, color) ->
                        val isSelected = hex == selectedColorHex
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (folderName.isNotBlank()) {
                        onConfirm(folderName.trim(), selectedColorHex)
                    }
                },
                enabled = folderName.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_folder_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("إضافة المجلد")
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
