package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DownloadAppDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DownloadForOffline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "تحميل وتثبيت التطبيق على هاتفك 📲",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "يمكنك تثبيت تطبيق 'ملاحظات المحاضرات الذكية' كملف APK حقيقي على هاتفك الأندرويد بسهولة عبر الخطوات التالية:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Step 1
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("1️⃣", fontSize = 18.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("تصدير APK من لوحة AI Studio", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("من القائمة العلوية أو قائمة الإعدادات (Settings / Export) في منصة Google AI Studio، اختر 'Export APK' أو 'Download Project ZIP'.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Step 2
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("2️⃣", fontSize = 18.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("تثبيت الملف على هاتفك الأندرويد", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("افتح ملف (app-debug.apk) المحمّل في هاتفك، ووافق على 'التثبيت من هذا المصدر' (Install unknown apps) ليعمل التطبيق مباشرة.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Step 3
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("3️⃣", fontSize = 18.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("ضبط مفتاح Gemini لتفادي خطأ 403", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("بمجرد فتح التطبيق على هاتفك، اضغط على أيقونة المفتاح 🔑 بأعلى الشاشة وألصق مفتاح Gemini المجاني لتفعيل كامل ميزات الذكاء الاصطناعي.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("حسناً، فهمت")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
