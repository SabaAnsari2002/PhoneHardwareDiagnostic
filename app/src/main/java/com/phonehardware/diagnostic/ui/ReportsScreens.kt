package com.phonehardware.diagnostic.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phonehardware.diagnostic.core.DiagnosticResult
import com.phonehardware.diagnostic.data.SessionEntity
import kotlinx.coroutines.flow.first

@Composable
fun HistoryScreen(vm: MainViewModel, onOpen: (String) -> Unit, onCompare: () -> Unit) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 18.dp, 16.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item { PageHeading("LOCAL ARCHIVE", "تاریخچه جلسات تشخیصی", "مشاهده و مرور نتایج ذخیره‌شده روی همین دستگاه") }
        item {
            NeonPanel(accent = NeonCyan) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    FeatureIcon(Icons.Default.History, accent = NeonCyan, modifier = Modifier.size(54.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${sessions.size} جلسه ذخیره‌شده", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Saved diagnostic sessions", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlineActionButton(
                    "مقایسه دو جلسه",
                    onClick = onCompare,
                    enabled = sessions.size >= 2,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.CompareArrows
                )
            }
        }
        if (sessions.isEmpty()) {
            item {
                NeonPanel(accent = MaterialTheme.colorScheme.outline) {
                    FeatureIcon(Icons.Default.FolderOpen, accent = Color(0xFF9CB5CA))
                    Spacer(Modifier.height(9.dp))
                    Text("هنوز جلسه‌ای ذخیره نشده است", fontWeight = FontWeight.Bold)
                    Text("یک اسکن سریع یا تست عملکردی اجرا کنید تا گزارش آن در این بخش ثبت شود.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        items(sessions) { session ->
            Card(
                onClick = { onOpen(session.id) },
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureIcon(Icons.Default.Smartphone, accent = NeonCyan)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(session.deviceName, fontWeight = FontWeight.Bold)
                        Text("${session.androidVersion} · ${java.util.Date(session.startedAt)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Text("مشاهده اندازه‌گیری‌ها، هشدارها و خروجی گزارش", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun SessionScreen(vm: MainViewModel, id: String) {
    val results by remember(id) { vm.savedResults(id) }.collectAsStateWithLifecycle(initialValue = emptyList())
    var deleteConfirm by remember { mutableStateOf(false) }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri -> uri?.let { vm.export(id, it, "pdf") } }
    val json = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let { vm.export(id, it, "json") } }
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let { vm.export(id, it, "csv") } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 18.dp, 16.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PageHeading("DIAGNOSTIC REPORT", "گزارش جلسه", "جزئیات اندازه‌گیری‌ها و نتایج ثبت‌شده") }
        item {
            NeonPanel(accent = NeonCyan) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${results.size} رکورد", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Text("Session measurements", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    FeatureIcon(Icons.Default.Assessment, accent = NeonCyan, modifier = Modifier.size(52.dp))
                }
                Spacer(Modifier.height(8.dp))
                InfoCallout("تشخیص وجود قطعه به معنی تأیید عملکرد آن نیست؛ درصد سلامت کلی نیز محاسبه نمی‌شود.")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                AccentActionButton("PDF", onClick = { pdf.launch("diagnostic.pdf") }, modifier = Modifier.weight(1f), icon = Icons.Default.PictureAsPdf)
                OutlineActionButton("JSON", onClick = { json.launch("diagnostic.json") }, modifier = Modifier.weight(1f), icon = Icons.Default.Code)
                OutlineActionButton("CSV", onClick = { csv.launch("diagnostic.csv") }, modifier = Modifier.weight(1f), icon = Icons.Default.TableChart)
            }
        }
        items(results) { result ->
            NeonPanel(accent = statusColor(result.outcome)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(result.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    StatusPill(result.outcome.name.replace('_', ' '), statusColor(result.outcome))
                }
                Spacer(Modifier.height(5.dp))
                Text(result.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                result.metrics.forEach { metric ->
                    Text("${metric.name}: ${metric.value} ${metric.unit} · ${metric.evidence}", style = MaterialTheme.typography.bodySmall)
                }
                if (result.warning.isNotEmpty()) {
                    Text("هشدار: ${result.warning}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                }
                Text("Source: ${result.source}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            OutlineActionButton("حذف این جلسه", onClick = { deleteConfirm = true }, modifier = Modifier.fillMaxWidth(), icon = Icons.Default.Delete)
        }
    }

    if (deleteConfirm) {
        AlertDialog(
            onDismissRequest = { deleteConfirm = false },
            title = { Text("حذف جلسه؟") },
            text = { Text("تمام نتایج ذخیره‌شده در این جلسه پاک می‌شوند.") },
            confirmButton = { TextButton(onClick = { vm.deleteSession(id); deleteConfirm = false }) { Text("حذف") } },
            dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text("انصراف") } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ComparisonScreen(vm: MainViewModel) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    var first by remember { mutableStateOf<String?>(null) }
    var second by remember { mutableStateOf<String?>(null) }
    var left by remember { mutableStateOf<List<DiagnosticResult>>(emptyList()) }
    var right by remember { mutableStateOf<List<DiagnosticResult>>(emptyList()) }
    LaunchedEffect(first, second) {
        left = first?.let { vm.savedResults(it).first() } ?: emptyList()
        right = second?.let { vm.savedResults(it).first() } ?: emptyList()
    }
    val testIds = (left.map { it.id } + right.map { it.id }).distinct()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 18.dp, 16.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PageHeading("REPORT COMPARISON", "مقایسه گزارش‌ها", "دو جلسه را انتخاب کنید تا اندازه‌گیری‌ها کنار هم نمایش داده شوند") }
        item {
            NeonPanel(accent = NeonCyan) {
                Text("انتخاب جلسه اول و دوم", fontWeight = FontWeight.Bold)
                Text("شرایط انجام تست‌ها ممکن است متفاوت باشد.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        items(sessions) { session ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = first == session.id,
                    onClick = { first = session.id },
                    label = { Text("A · ${session.deviceName}", maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                FilterChip(
                    selected = second == session.id,
                    onClick = { second = session.id },
                    label = { Text("B · ${java.util.Date(session.startedAt)}", maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
        if (first != null && second != null && first != second) {
            items(testIds) { testId ->
                val a = left.lastOrNull { it.id == testId }
                val b = right.lastOrNull { it.id == testId }
                NeonPanel(accent = NeonBlue) {
                    Text(a?.name ?: b?.name ?: testId, fontWeight = FontWeight.Bold)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("جلسه A: ${a?.outcome ?: "تست نشده"}", color = a?.let { statusColor(it.outcome) } ?: MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("جلسه B: ${b?.outcome ?: "تست نشده"}", color = b?.let { statusColor(it.outcome) } ?: MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("A: ${a?.metrics?.take(2)?.joinToString { "${it.name}=${it.value} ${it.unit}" } ?: "—"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("B: ${b?.metrics?.take(2)?.joinToString { "${it.name}=${it.value} ${it.unit}" } ?: "—"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val dark by vm.darkMode.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 18.dp, 16.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item { PageHeading("CONTROL CENTER", "تنظیمات و حریم خصوصی", "شفافیت درباره داده‌ها و دسترسی‌های برنامه") }
        item {
            NeonPanel(accent = NeonCyan) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureIcon(Icons.Default.DarkMode, accent = NeonCyan)
                    Column(Modifier.weight(1f)) {
                        Text("حالت تاریک", fontWeight = FontWeight.Bold)
                        Text("Dark mode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = dark,
                        onCheckedChange = vm::setDark,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        }
        item {
            PrivacyFeature(Icons.Default.Storage, "تاریخچه فقط روی دستگاه", "نتایج و سوابق تست‌ها به‌صورت محلی ذخیره می‌شوند و به فضای ابری ارسال نمی‌شوند.")
        }
        item {
            PrivacyFeature(Icons.Default.Security, "عدم خواندن شناسه‌های حساس", "برنامه شناسه‌هایی مانند IMEI و IMSI را نمی‌خواند.")
        }
        item {
            PrivacyFeature(Icons.Default.PermCameraMic, "مجوزها فقط هنگام نیاز", "مجوز دوربین، میکروفون یا موقعیت مکانی فقط هنگام ورود به تست مرتبط درخواست می‌شود. اسکن سریع این قابلیت‌ها را فعال نمی‌کند.")
        }
        item {
            PrivacyFeature(Icons.Default.WifiOff, "اولویت آفلاین", "این نسخه برای بررسی‌های سخت‌افزاری به ارتباط با سرور نیاز ندارد و تست سرعت اینترنت انجام نمی‌دهد.")
        }
        item {
            InfoCallout("یک قطعه که توسط سیستم گزارش شده، لزوماً از نظر عملکردی سالم نیست. نبود یک حسگر اختیاری نیز به‌تنهایی نشانه خرابی نیست.")
        }
        item {
            OutlineActionButton("حذف تمام تاریخچه ذخیره‌شده", onClick = { confirm = true }, modifier = Modifier.fillMaxWidth(), icon = Icons.Default.Delete)
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("حذف تمام نتایج ذخیره‌شده؟") },
            text = { Text("این عملیات قابل بازگشت نیست.") },
            confirmButton = { TextButton(onClick = { vm.clearHistory(); confirm = false }) { Text("حذف همه") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("انصراف") } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PrivacyFeature(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    NeonPanel() {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureIcon(icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
