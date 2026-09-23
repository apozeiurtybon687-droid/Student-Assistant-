package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.ThemeMode
import com.example.data.local.ThemePreferences
import com.example.data.local.UserGender
import com.example.ui.theme.FemalePinkPrimary
import com.example.ui.theme.MaleBluePrimary

@Composable
fun ThemeAndProfileDialog(
    themePreferences: ThemePreferences,
    onDismiss: () -> Unit
) {
    val currentGender by themePreferences.gender.collectAsState()
    val currentMode by themePreferences.themeMode.collectAsState()

    var selectedGender by remember { mutableStateOf(currentGender) }
    var selectedMode by remember { mutableStateOf(currentMode) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("theme_and_profile_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "تخصيص المظهر والهوية 🎨",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "اختر هوية الطالب والوضع النهاري أو الليلي",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Section 1: Gender / Theme Color (ذكر / أزرق vs فتاة / وردي)
                Text(
                    text = "1. هوية المستخدم ولون التطبيق:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Male Option (Blue)
                    val isMaleSelected = selectedGender == UserGender.MALE
                    val maleBorderColor by animateColorAsState(
                        if (isMaleSelected) MaleBluePrimary else Color.Transparent,
                        label = "male_border"
                    )

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, maleBorderColor, RoundedCornerShape(16.dp))
                            .clickable {
                                selectedGender = UserGender.MALE
                                themePreferences.setGender(UserGender.MALE)
                            }
                            .testTag("select_gender_male"),
                        color = if (isMaleSelected) MaleBluePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "👨‍🎓", fontSize = 32.sp)
                            Text(
                                text = "طالب جامعي (ذكر)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isMaleSelected) MaleBluePrimary else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Surface(
                                color = MaleBluePrimary,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "الأزرق الملكي 💙",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Female Option (Pink)
                    val isFemaleSelected = selectedGender == UserGender.FEMALE
                    val femaleBorderColor by animateColorAsState(
                        if (isFemaleSelected) FemalePinkPrimary else Color.Transparent,
                        label = "female_border"
                    )

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, femaleBorderColor, RoundedCornerShape(16.dp))
                            .clickable {
                                selectedGender = UserGender.FEMALE
                                themePreferences.setGender(UserGender.FEMALE)
                            }
                            .testTag("select_gender_female"),
                        color = if (isFemaleSelected) FemalePinkPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "👩‍🎓", fontSize = 32.sp)
                            Text(
                                text = "طالبة جامعية (فتاة)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isFemaleSelected) FemalePinkPrimary else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Surface(
                                color = FemalePinkPrimary,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "الوردي الأنيق 💖",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Section 2: Mode (Day / Night / System)
                Text(
                    text = "2. وضع الخلفية والشاشة:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeModeButton(
                        title = "نهاري ☀️",
                        subtitle = "خلفية ناصعة",
                        isSelected = selectedMode == ThemeMode.LIGHT,
                        onClick = {
                            selectedMode = ThemeMode.LIGHT
                            themePreferences.setThemeMode(ThemeMode.LIGHT)
                        },
                        modifier = Modifier.weight(1f).testTag("select_mode_light")
                    )

                    ThemeModeButton(
                        title = "ليلي 🌙",
                        subtitle = "مريح للعين",
                        isSelected = selectedMode == ThemeMode.DARK,
                        onClick = {
                            selectedMode = ThemeMode.DARK
                            themePreferences.setThemeMode(ThemeMode.DARK)
                        },
                        modifier = Modifier.weight(1f).testTag("select_mode_dark")
                    )

                    ThemeModeButton(
                        title = "تلقائي ⚙️",
                        subtitle = "حسب النظام",
                        isSelected = selectedMode == ThemeMode.SYSTEM,
                        onClick = {
                            selectedMode = ThemeMode.SYSTEM
                            themePreferences.setThemeMode(ThemeMode.SYSTEM)
                        },
                        modifier = Modifier.weight(1f).testTag("select_mode_system")
                    )
                }

                // Live Preview Indicator
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "تم التفعيل: ${selectedGender.displayNameAr} مع ${selectedMode.displayNameAr}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Confirm / Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_theme_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تم وحفظ الإعدادات ✨", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ThemeModeButton(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "mode_border"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
