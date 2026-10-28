package com.phonehardware.diagnostic.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phonehardware.diagnostic.core.DiagnosticResult
import com.phonehardware.diagnostic.data.SessionEntity
import kotlinx.coroutines.flow.first

@Composable fun HistoryScreen(vm: MainViewModel, onOpen:(String)->Unit, onCompare:()->Unit) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Text("Saved diagnostic sessions",style=MaterialTheme.typography.headlineSmall) }
        item { OutlinedButton(onClick=onCompare,enabled=sessions.size>=2) { Text("Compare two saved sessions") } }
        if(sessions.isEmpty()) item { Text("No saved sessions yet. Run a scan or a functional test.") }
        items(sessions) { s ->
            Card(onClick={onOpen(s.id)}) { Column(Modifier.fillMaxWidth().padding(15.dp)) {
                Text(s.deviceName,fontWeight=FontWeight.Bold)
                Text("${s.androidVersion} · ${java.util.Date(s.startedAt)}")
                Text("View measurements, warnings and export options",style=MaterialTheme.typography.bodySmall)
            } }
        }
    }
}
@Composable fun SessionScreen(vm: MainViewModel, id: String) {
    val results by remember(id) { vm.savedResults(id) }.collectAsStateWithLifecycle(initialValue=emptyList())
    var deleteConfirm by remember { mutableStateOf(false) }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { u -> u?.let { vm.export(id,it,"pdf") } }
    val json = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u -> u?.let { vm.export(id,it,"json") } }
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { u -> u?.let { vm.export(id,it,"csv") } }
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item { Text("Session report",style=MaterialTheme.typography.headlineSmall) }
        item { Text("${results.size} records. Detection is not functional verification; no overall health percentage is calculated.") }
        item { Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            Button(onClick={pdf.launch("diagnostic.pdf")},modifier=Modifier.weight(1f)) { Text("PDF") }
            OutlinedButton(onClick={json.launch("diagnostic.json")},modifier=Modifier.weight(1f)) { Text("JSON") }
            OutlinedButton(onClick={csv.launch("diagnostic.csv")},modifier=Modifier.weight(1f)) { Text("CSV") }
        } }
        items(results) { r ->
            Card { Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Row { Text(r.name,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f)); Text(r.outcome.name,color=statusColor(r.outcome),style=MaterialTheme.typography.labelMedium) }
                Text(r.explanation,style=MaterialTheme.typography.bodySmall)
                r.metrics.forEach { m -> Text("${m.name}: ${m.value} ${m.unit} · ${m.evidence}",style=MaterialTheme.typography.bodySmall) }
                if(r.warning.isNotEmpty()) Text("Warning: ${r.warning}",style=MaterialTheme.typography.bodySmall)
                Text("Source: ${r.source}",style=MaterialTheme.typography.labelSmall)
            } }
        }
        item { TextButton(onClick={deleteConfirm=true}) { Text("Delete this session") } }
    }
    if(deleteConfirm) AlertDialog(onDismissRequest={deleteConfirm=false},title={Text("Delete session?")},text={Text("This erases all results saved in this session.")},
        confirmButton={TextButton(onClick={vm.deleteSession(id);deleteConfirm=false}){Text("Delete")}},
        dismissButton={TextButton(onClick={deleteConfirm=false}){Text("Cancel")}})
}
@Composable fun ComparisonScreen(vm: MainViewModel) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    var first by remember { mutableStateOf<String?>(null) }
    var second by remember { mutableStateOf<String?>(null) }
    var left by remember { mutableStateOf<List<DiagnosticResult>>(emptyList()) }
    var right by remember { mutableStateOf<List<DiagnosticResult>>(emptyList()) }
    LaunchedEffect(first,second) {
        left = first?.let { vm.savedResults(it).first() } ?: emptyList()
        right = second?.let { vm.savedResults(it).first() } ?: emptyList()
    }
    val names = (left.map{it.id}+right.map{it.id}).distinct()
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item { Text("Local report comparison",style=MaterialTheme.typography.headlineSmall) }
        item { Text("Tap two sessions. Measurements are shown side-by-side; test conditions may differ.") }
        items(sessions) { s ->
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                FilterChip(selected=first==s.id,onClick={first=s.id},label={Text("A: ${s.deviceName}")})
                FilterChip(selected=second==s.id,onClick={second=s.id},label={Text("B: ${java.util.Date(s.startedAt)}")})
            }
        }
        if(first!=null && second!=null && first!=second) items(names) { testId ->
            val a = left.lastOrNull{it.id==testId}
            val b = right.lastOrNull{it.id==testId}
            Card { Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text(a?.name ?: b?.name ?: testId,fontWeight=FontWeight.SemiBold)
                Text("A: ${a?.outcome ?: "Not tested"}   |   B: ${b?.outcome ?: "Not tested"}")
                Text("A: ${a?.metrics?.take(2)?.joinToString { "${it.name}=${it.value} ${it.unit}" } ?: "—"}",style=MaterialTheme.typography.bodySmall)
                Text("B: ${b?.metrics?.take(2)?.joinToString { "${it.name}=${it.value} ${it.unit}" } ?: "—"}",style=MaterialTheme.typography.bodySmall)
            } }
        }
    }
}
@Composable fun SettingsScreen(vm: MainViewModel) {
    val dark by vm.darkMode.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("Settings & privacy",style=MaterialTheme.typography.headlineSmall)
        Row { Text("Dark mode",modifier=Modifier.weight(1f)); Switch(checked=dark,onCheckedChange=vm::setDark) }
        HorizontalDivider()
        Text("All test history remains on this device. The app does not upload hardware reports or read device identifiers such as IMEI/IMSI.")
        Text("Microphone buffers, temporary camera frames, and exact GPS coordinates are not saved by the tests in this release.")
        Text("Each dangerous permission is requested on entering its test. Export uses Android's document picker.")
        Text("A system-reported component is not automatically marked working. A missing optional sensor is not automatically defective.")
        Text("Offline-first. The current release performs no internet speed tests or server communication.")
        OutlinedButton(onClick={confirm=true}) { Text("Delete all saved history") }
    }
    if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Delete all saved results?")},
        text={Text("This cannot be undone.")},confirmButton={TextButton(onClick={vm.clearHistory();confirm=false}){Text("Delete all")}},
        dismissButton={TextButton(onClick={confirm=false}){Text("Cancel")}})
}
