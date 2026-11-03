package com.phonehardware.diagnostic.ui.screens

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phonehardware.diagnostic.MainActivity
import com.phonehardware.diagnostic.core.*
import com.phonehardware.diagnostic.ui.*
import com.phonehardware.diagnostic.ui.MainViewModel

@Composable
fun TestDetail(spec: TestSpec, vm: MainViewModel, activity: MainActivity) {
    val results by vm.results.collectAsStateWithLifecycle()
    val busy by vm.activeId.collectAsStateWithLifecycle()
    val latest = results.lastOrNull { it.id == spec.id }
    var permissionGranted by remember(spec.id) {
        mutableStateOf(spec.permission == null || ContextCompat.checkSelfPermission(activity, spec.permission) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        permissionGranted = allowed
        if (allowed && spec.kind == TestKind.AUTOMATED) vm.runOne(spec.id)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PageHeading(
            eyebrow = spec.category,
            title = spec.title,
            subtitle = spec.description
        )

        NeonPanel(accent = NeonCyan) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("روش بررسی", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                StatusPill(kindLabel(spec.kind), when (spec.kind) {
                    TestKind.PASSIVE -> NeonCyan
                    TestKind.AUTOMATED -> NeonGreen
                    TestKind.ASSISTED -> Color(0xFFFFB34D)
                    TestKind.INTERACTIVE -> Color(0xFFB69CFF)
                })
            }
            Spacer(Modifier.height(7.dp))
            Text("حداقل نسخه اندروید: API ${spec.minApi}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (spec.permission != null) {
                Text("این آزمون برای اجرا به مجوز مرتبط نیاز دارد.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (!permissionGranted) {
            NeonPanel(accent = Color(0xFFFFB34D)) {
                FeatureIcon(Icons.Default.Lock, accent = Color(0xFFFFB34D))
                Spacer(Modifier.height(9.dp))
                Text("مجوز لازم است", fontWeight = FontWeight.Bold)
                Text("این مجوز فقط برای اجرای همین آزمون درخواست می‌شود. رد کردن مجوز به معنی خرابی قطعه نیست.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                AccentActionButton(
                    "درخواست مجوز",
                    onClick = { spec.permission?.let(launcher::launch) },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Security
                )
            }
        } else {
            NeonPanel(accent = NeonBlue) {
                Text("اجرای آزمون", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                when (spec.id) {
                    "touch_grid" -> TouchGrid(vm)
                    "pixel_check" -> PixelScreen(vm)
                    "camera_capture" -> CameraCaptureScreen(vm, activity)
                    "speaker_heard" -> SpeakerScreen(vm)
                    "vibration_felt" -> VibrationScreen(vm, activity)
                    "torch_on" -> TorchScreen(vm, activity)
                    "volume_buttons" -> VolumeButtonsScreen(vm, activity)
                    "biometric_auth" -> BiometricScreen(vm, activity)
                    "sensor_explorer" -> SensorExplorerScreen(vm, activity)
                    "battery_trend" -> BatteryTrendScreen(vm, activity)
                    "charge_change" -> ChargingChangeScreen(vm, activity)
                    "wifi_link" -> WifiLinkScreen(vm, activity)
                    "ble_scan" -> BleScanScreen(vm, activity)
                    "nfc_tag" -> NfcTagScreen(vm, activity)
                    "usb_presence" -> UsbPresenceScreen(vm, activity)
                    else -> {
                        AccentActionButton(
                            "خواندن اطلاعات / شروع تست",
                            onClick = { vm.runOne(spec.id) },
                            enabled = busy == null,
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.PlayArrow
                        )
                        if (busy == spec.id) {
                            Spacer(Modifier.height(9.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                            OutlineActionButton("توقف تست", onClick = vm::cancel, modifier = Modifier.fillMaxWidth(), icon = Icons.Default.Stop)
                        }
                    }
                }
            }
        }

        if (latest != null) {
            NeonPanel(accent = statusColor(latest.outcome)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("آخرین نتیجه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("LATEST RESULT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    StatusPill(latest.outcome.name.replace('_', ' '), statusColor(latest.outcome))
                }
                Spacer(Modifier.height(7.dp))
                Text(latest.explanation, style = MaterialTheme.typography.bodyMedium)
                latest.metrics.forEach { metric ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(Modifier.padding(11.dp)) {
                            Text("${metric.name}: ${metric.value} ${metric.unit}", fontWeight = FontWeight.SemiBold)
                            Text("${metric.evidence} · ${latest.source}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (latest.warning.isNotEmpty()) {
                    Text("محدودیت: ${latest.warning}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                }
                if (latest.nextAction.isNotEmpty()) {
                    Text(latest.nextAction, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        InfoCallout("نتایج نشان می‌دهند چه چیزی توسط سیستم گزارش، اندازه‌گیری یا توسط کاربر تأیید شده است؛ این بررسی گواهی آزمایشگاهی سلامت قطعه نیست.")
    }
}

private fun kindLabel(kind: TestKind): String = when (kind) {
    TestKind.PASSIVE -> "اطلاعات سیستم"
    TestKind.AUTOMATED -> "خودکار"
    TestKind.ASSISTED -> "با تأیید کاربر"
    TestKind.INTERACTIVE -> "تعاملی"
}
