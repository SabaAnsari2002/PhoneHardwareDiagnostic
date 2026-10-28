package com.phonehardware.diagnostic.ui.screens

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phonehardware.diagnostic.MainActivity
import com.phonehardware.diagnostic.core.*
import com.phonehardware.diagnostic.ui.MainViewModel

@Composable
fun TestDetail(spec: TestSpec, vm: MainViewModel, activity: MainActivity) {
    val results by vm.results.collectAsStateWithLifecycle()
    val busy by vm.activeId.collectAsStateWithLifecycle()
    val latest = results.lastOrNull { it.id == spec.id }
    var permissionGranted by remember(spec.id) {
        mutableStateOf(spec.permission == null || ContextCompat.checkSelfPermission(activity,spec.permission)==PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        permissionGranted = allowed
        if(allowed && spec.kind==TestKind.AUTOMATED) vm.runOne(spec.id)
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text(spec.category.uppercase(),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
        Text(spec.title,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        Text(spec.description)
        Text("Mode: ${spec.kind} · Minimum API ${spec.minApi}",style=MaterialTheme.typography.labelSmall)
        if(!permissionGranted) {
            Text("Permission required for this selected test. Denial is not a hardware failure.")
            Button(onClick={spec.permission?.let(launcher::launch)}) { Text("Grant test permission") }
        } else when(spec.id) {
            "touch_grid" -> TouchGrid(vm)
            "pixel_check" -> PixelScreen(vm)
            "camera_capture" -> CameraCaptureScreen(vm,activity)
            "speaker_heard" -> SpeakerScreen(vm)
            "vibration_felt" -> VibrationScreen(vm,activity)
            "torch_on" -> TorchScreen(vm,activity)
            "volume_buttons" -> VolumeButtonsScreen(vm,activity)
            "biometric_auth" -> BiometricScreen(vm,activity)
            "sensor_explorer" -> SensorExplorerScreen(vm,activity)
            "battery_trend" -> BatteryTrendScreen(vm,activity)
            "charge_change" -> ChargingChangeScreen(vm,activity)
            "wifi_link" -> WifiLinkScreen(vm,activity)
            "ble_scan" -> BleScanScreen(vm,activity)
            "nfc_tag" -> NfcTagScreen(vm,activity)
            "usb_presence" -> UsbPresenceScreen(vm,activity)
            else -> {
                Button(onClick={vm.runOne(spec.id)},enabled=busy==null,modifier=Modifier.fillMaxWidth()) { Text("Read / start real test") }
                if(busy==spec.id) { LinearProgressIndicator(Modifier.fillMaxWidth()); OutlinedButton(onClick=vm::cancel){Text("Stop test")} }
            }
        }
        if(latest!=null) {
            HorizontalDivider()
            Text("LATEST RESULT · ${latest.outcome}",fontWeight=FontWeight.Bold)
            Text(latest.explanation)
            latest.metrics.forEach { m ->
                ListItem(headlineContent={Text("${m.name}: ${m.value} ${m.unit}")},supportingContent={Text("${m.evidence} · ${latest.source}")})
            }
            if(latest.warning.isNotEmpty()) Text("Limitation: ${latest.warning}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.tertiary)
            if(latest.nextAction.isNotEmpty()) Text(latest.nextAction,style=MaterialTheme.typography.bodySmall)
        }
        Text("Results identify what was reported, measured or user-confirmed; this is not a lab certification.",style=MaterialTheme.typography.labelMedium)
    }
}
