package com.phonehardware.diagnostic.ui.screens

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.BatteryManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.phonehardware.diagnostic.MainActivity
import com.phonehardware.diagnostic.core.*
import com.phonehardware.diagnostic.ui.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable fun WifiLinkScreen(vm:MainViewModel,activity:MainActivity) {
    val cm=remember {activity.getSystemService(ConnectivityManager::class.java)}
    val wifi=remember {activity.applicationContext.getSystemService(WifiManager::class.java)}
    val lifecycle=LocalLifecycleOwner.current
    var running by remember {mutableStateOf(false)}
    var observed by remember {mutableIntStateOf(0)}
    var rssi by remember {mutableStateOf<Int?>(null)}
    var link by remember {mutableStateOf<Int?>(null)}
    var connected by remember {mutableStateOf(false)}
    LaunchedEffect(running) { if(running) lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        repeat(6) {
            val caps=cm.getNetworkCapabilities(cm.activeNetwork)
            connected=caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)==true
            if(connected) {
                if(Build.VERSION.SDK_INT>=31) {
                    val info=caps?.transportInfo as? WifiInfo
                    rssi=info?.rssi?.takeIf{it> -127 && it <0}
                    link=info?.linkSpeed?.takeIf{it>0}
                } else {
                    @Suppress("DEPRECATION") val info=wifi.connectionInfo
                    rssi=info.rssi.takeIf{it> -127 && it<0}
                    link=info.linkSpeed.takeIf{it>0}
                }
                observed++
            }
            delay(1500)
        }
        running=false
    } }
    Text("Local Wi-Fi observations only. Link rates are negotiated radio rates, never internet-speed measurements.")
    Text("Connected via Wi-Fi: $connected · Samples: $observed")
    Text("RSSI: ${rssi?.toString() ?: "Restricted/unavailable"} dBm · Link: ${link?.toString() ?: "Unavailable"} Mbps")
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Button(enabled=!running,onClick={observed=0;rssi=null;link=null;running=true}) {Text("Observe 9 seconds")}
        OutlinedButton(enabled=running,onClick={running=false}){Text("Stop")}
    }
    Button(enabled=!running,onClick={
        val status=if(observed>0) Outcome.PASSED else Outcome.INCONCLUSIVE
        vm.store(DiagnosticResult("wifi_link","Wi-Fi link observation","Wi-Fi",status,
            listOf(Metric("Wi-Fi connected",connected.toString(),evidence=Evidence.SYSTEM_REPORTED),
                Metric("Observed samples",observed.toString(),evidence=Evidence.MEASURED),
                Metric("RSSI",rssi?.toString() ?: "Unavailable","dBm",Evidence.SYSTEM_REPORTED),
                Metric("Link rate",link?.toString() ?: "Unavailable","Mbps",Evidence.SYSTEM_REPORTED)),
            "ConnectivityManager / WifiManager","Local passive Wi-Fi link observation",
            if(observed>0) "A Wi-Fi transport was observed. No internet-speed or radio integrity proof is claimed." else "No connected Wi-Fi transport was sampled.",
            warning="Disconnected Wi-Fi may be due to settings or access-point availability."))
    }) {Text("Save Wi-Fi observation")}
}

@Composable fun BleScanScreen(vm:MainViewModel,activity:MainActivity) {
    val bt=remember {activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager}
    val required=if(Build.VERSION.SDK_INT>=31) Manifest.permission.BLUETOOTH_SCAN else Manifest.permission.ACCESS_FINE_LOCATION
    var allowed by remember {mutableStateOf(ContextCompat.checkSelfPermission(activity,required)==PackageManager.PERMISSION_GRANTED)}
    val ask=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){allowed=it}
    var scanning by remember {mutableStateOf(false)}
    var packets by remember {mutableIntStateOf(0)}
    var note by remember {mutableStateOf("")}
    val adapter=remember {bt.adapter}
    // Scan callbacks may be received on binder threads; UI state is updated on the main thread.
    DisposableEffect(scanning,allowed) {
        val scanner=if(scanning && allowed) runCatching{adapter?.bluetoothLeScanner}.getOrNull() else null
        val callback=object:ScanCallback() {
            override fun onScanResult(callbackType:Int,result:ScanResult) {
                activity.runOnUiThread { packets++ }
            }
            override fun onBatchScanResults(results:MutableList<ScanResult>) {
                activity.runOnUiThread { packets+=results.size }
            }
            override fun onScanFailed(errorCode:Int) {activity.runOnUiThread {note="Scanner error $errorCode";scanning=false}}
        }
        if(scanner!=null) runCatching { scanner.startScan(callback) }.onFailure {note="Scan unavailable: ${it.javaClass.simpleName}";scanning=false}
        onDispose { scanner?.let { runCatching{it.stopScan(callback)} } }
    }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val observer=androidx.lifecycle.LifecycleEventObserver { _,event ->
            if(event==Lifecycle.Event.ON_STOP) scanning=false
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(scanning) {if(scanning) {delay(8000);scanning=false}}
    Text("Scan nearby advertising BLE devices for 8 seconds. No pairing or connection occurs. Nothing found is inconclusive, not proof of radio failure.")
    if(!allowed) Button(onClick={ask.launch(required)}){Text("Grant nearby-device scan permission")}
    else Button(enabled=!scanning,onClick={packets=0;note="";scanning=true}){Text("Start BLE scan")}
    if(scanning) OutlinedButton(onClick={scanning=false}) {Text("Stop scan")}
    Text("BLE advertisement packets received: $packets. $note")
    Button(enabled=!scanning,onClick={
        vm.store(DiagnosticResult("ble_scan","BLE advertisement scan","Bluetooth",when {
            !allowed -> Outcome.PERMISSION_REQUIRED
            packets>0 -> Outcome.PASSED
            else -> Outcome.INCONCLUSIVE
        },listOf(Metric("Advertisement callbacks",packets.toString(),evidence=Evidence.MEASURED)),
            "BluetoothLeScanner / ScanCallback","User-triggered 8-second receive-only scan",
            if(packets>0) "BLE advertising radio packets were received." else "No BLE advertiser was observed, or scan was unavailable.",
            warning="BLE discovery does not verify pairing, classic Bluetooth, or connection reliability. MAC addresses are not stored."))
    }) {Text("Save BLE result")}
}

@Composable fun NfcTagScreen(vm:MainViewModel,activity:MainActivity) {
    val adapter=remember {NfcAdapter.getDefaultAdapter(activity)}
    var listening by remember {mutableStateOf(false)}
    var techs by remember {mutableStateOf<List<String>?>(null)}
    var message by remember {mutableStateOf("")}
    DisposableEffect(adapter,listening) {
        if(adapter!=null && listening) {
            try {
                adapter.enableReaderMode(activity,{ tag ->
                    val names=tag.techList.map{it.substringAfterLast('.')}
                    activity.runOnUiThread { techs=names;message="Compatible tag detected";listening=false }
                },NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V,null)
            } catch(e:Exception) {message="Reader unavailable: ${e.javaClass.simpleName}";listening=false}
        }
        onDispose { if(adapter!=null) runCatching { adapter.disableReaderMode(activity) } }
    }
    Text(if(adapter==null) "NFC adapter not reported." else "Bring a tag you own near the phone. The reader records technology names only, never payload or identifier.")
    Button(enabled=adapter!=null && adapter.isEnabled && !listening,onClick={techs=null;listening=true}) {Text("Start NFC reader")}
    if(listening) OutlinedButton(onClick={listening=false}) {Text("Stop NFC reader")}
    Text("${techs?.joinToString() ?: "No tag read"} $message")
    Button(enabled=techs!=null,onClick={
        vm.store(DiagnosticResult("nfc_tag","NFC tag reader","Advanced",Outcome.PASSED,
            listOf(Metric("Tag technologies",techs.orEmpty().joinToString(),evidence=Evidence.MEASURED)),
            "NfcAdapter.enableReaderMode","Read technology list from a user-supplied tag",
            "An NFC tag was detected and its compatible technologies queried.",warning="The result does not verify all NFC modes or payment features."))
    }){Text("Save NFC observation")}
}

@Composable fun UsbPresenceScreen(vm:MainViewModel,activity:MainActivity) {
    val usb=remember {activity.getSystemService(Context.USB_SERVICE) as UsbManager}
    val lifecycle=LocalLifecycleOwner.current
    var running by remember {mutableStateOf(false)}
    var maxDevices by remember {mutableIntStateOf(0)}
    var changes by remember {mutableIntStateOf(0)}
    var count by remember {mutableIntStateOf(usb.deviceList.size)}
    LaunchedEffect(running) {if(running) lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        repeat(10) {
            val latest=usb.deviceList.size
            if(latest!=count) changes++
            count=latest;maxDevices=maxOf(maxDevices,count)
            delay(1000)
        }
        running=false
    } }
    Text("Connect or disconnect a compatible USB device. This test detects app-visible host attachments, not cable electrical integrity.")
    Text("Devices now: $count · Highest seen: $maxDevices · Changes observed: $changes")
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Button(enabled=!running,onClick={maxDevices=count;changes=0;running=true}){Text("Watch 10 seconds")}
        OutlinedButton(enabled=running,onClick={running=false}){Text("Stop")}
    }
    Button(enabled=!running,onClick={
        vm.store(DiagnosticResult("usb_presence","USB attachment observation","Advanced",
            if(maxDevices>0 || changes>0) Outcome.PASSED else Outcome.INCONCLUSIVE,
            listOf(Metric("Maximum attached devices",maxDevices.toString(),evidence=Evidence.MEASURED),
                Metric("Connection count changes",changes.toString(),evidence=Evidence.MEASURED)),
            "UsbManager.deviceList","10-second app-visible USB host device polling",
            "USB host attachment observation completed.",warning="No device detected is inconclusive without a compatible peripheral; no USB communication was attempted."))
    }) {Text("Save USB observation")}
}

@Composable fun ChargingChangeScreen(vm:MainViewModel,activity:MainActivity) {
    val lifecycle=LocalLifecycleOwner.current
    var running by remember {mutableStateOf(false)}
    var previous by remember {mutableIntStateOf(-1)}
    var transitions by remember {mutableIntStateOf(0)}
    var samples by remember {mutableIntStateOf(0)}
    LaunchedEffect(running) {if(running) lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        repeat(20) {
            val battery=activity.registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val plugged=battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED,-1) ?: -1
            if(previous>=0 && plugged>=0 && plugged!=previous) transitions++
            previous=plugged;samples++
            delay(1000)
        }
        running=false
    } }
    Text("During the 20-second test, plug in or unplug a known-good charger safely. Android charging-source status codes are observed, not electrical port diagnostics.")
    Text("Status code: $previous · Changes observed: $transitions · Samples: $samples")
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Button(enabled=!running,onClick={previous=-1;transitions=0;samples=0;running=true}){Text("Start 20-second watch")}
        OutlinedButton(enabled=running,onClick={running=false}){Text("Stop")}
    }
    Button(enabled=!running,onClick={vm.store(DiagnosticResult("charge_change","Charging cable transitions","Battery",
        if(transitions>0) Outcome.PASSED else Outcome.INCONCLUSIVE,
        listOf(Metric("Source state transitions",transitions.toString(),evidence=Evidence.MEASURED),
            Metric("Samples",samples.toString(),evidence=Evidence.MEASURED)),
        "ACTION_BATTERY_CHANGED","Repeated charging-source status observation",
        if(transitions>0) "A change in platform charging-source state was detected." else "No charging-source transition was observed.",
        warning="No transition may simply mean the source was not changed; no connector defect is inferred."))}) {Text("Save charging observation")}
}
