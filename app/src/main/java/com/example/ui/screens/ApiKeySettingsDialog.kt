package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ApiKeyManager

@Composable
fun ApiKeySettingsDialog(
    apiKeyManager: ApiKeyManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentKey = remember { apiKeyManager.getApiKey() }
    var inputKey by remember { mutableStateOf(currentKey) }
    val isCustomKey = remember { apiKeyManager.isUsingCustomKey() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "إعدادات مفتاح Gemini API 🔑",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "لتشغيل ميزات التفريغ الصوتي، التلخيص، الترجمة، والمحادثة على هاتفك، يلزم توفر مفتاح Gemini API صالح لتجنب خطأ 403.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Status chip
                Surface(
                    color = if (apiKeyManager.hasValidKey()) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (apiKeyManager.hasValidKey()) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (apiKeyManager.hasValidKey()) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (apiKeyManager.hasValidKey()) "المفتاح مفعل حالياً ✓" else "لم يتم ضبط مفتاح صالح (سبب خطأ 403)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (apiKeyManager.hasValidKey()) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }

                // Input Field
                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    label = { Text("أدخل مفتاح Gemini API هنا") },
                    placeholder = { Text("AIzaSy...") },
                    leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                    trailingIcon = {
                        if (inputKey.isNotEmpty()) {
                            IconButton(onClick = { inputKey = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input_field"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Helper link to Google AI Studio
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "الرابط: https://aistudio.google.com/app/apikey", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("الحصول على مفتاح مجاني من Google AI Studio", fontSize = 11.sp)
                }

                if (isCustomKey) {
                    TextButton(
                        onClick = {
                            apiKeyManager.clearCustomApiKey()
                            inputKey = apiKeyManager.getApiKey()
                            Toast.makeText(context, "تمت استعادة الإعدادات الافتراضية", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("استعادة المفتاح الافتراضي من النظام", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (inputKey.isNotBlank()) {
                        apiKeyManager.setCustomApiKey(inputKey.trim())
                        Toast.makeText(context, "تم حفظ مفتاح API بنجاح!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } else {
                        Toast.makeText(context, "يرجى كتابة المفتاح أولاً", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.testTag("save_api_key_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("حفظ المفتاح")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
