package com.phonehardware.diagnostic.ui

import android.os.Build
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.phonehardware.diagnostic.MainActivity
import com.phonehardware.diagnostic.R
import com.phonehardware.diagnostic.core.*
import com.phonehardware.diagnostic.ui.screens.TestDetail
import com.phonehardware.diagnostic.ui.theme.DiagnosticTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun App(vm: MainViewModel, activity: MainActivity) {
    val dark by vm.darkMode.collectAsStateWithLifecycle()
    val running by vm.activeId.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    DiagnosticTheme(dark) {
        Scaffold(
            topBar = { TopAppBar(
                title = { Text("Phone Hardware Diagnostic", maxLines=1, overflow=TextOverflow.Ellipsis) },
                actions = { if(running != null) { CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp); Spacer(Modifier.width(12.dp)) } }
            ) },
            bottomBar = { NavigationBar {
                listOf(Triple("dashboard",Icons.Default.Dashboard,R.string.dashboard),
                    Triple("tests",Icons.Default.Science,R.string.all_tests),
                    Triple("history",Icons.Default.History,R.string.history),
                    Triple("settings",Icons.Default.Settings,R.string.settings)).forEach { (route,icon,label) ->
                    NavigationBarItem(selected = nav.currentBackStackEntryAsState().value?.destination?.route==route,
                        onClick={ nav.navigate(route) { launchSingleTop=true; popUpTo("dashboard") { saveState=true }; restoreState=true } },
                        icon={ Icon(icon,contentDescription=null) },label={Text(stringResource(label))})
                }
            } }
        ) { padding ->
            NavHost(navController=nav,startDestination="dashboard",modifier=Modifier.padding(padding)) {
                composable("dashboard") { Dashboard(vm, goTests={nav.navigate("tests")},goHistory={nav.navigate("history")},goDevice={nav.navigate("test/device")}) }
                composable("tests") { TestsScreen(onOpen={nav.navigate("test/$it")}) }
                composable("test/{id}", arguments=listOf(navArgument("id"){type=NavType.StringType})) { b ->
                    TestDetail(TestCatalog.byId(requireNotNull(b.arguments?.getString("id"))), vm, activity)
                }
                composable("history") { HistoryScreen(vm,onOpen={nav.navigate("history/$it")},onCompare={nav.navigate("compare")}) }
                composable("history/{id}") { b -> SessionScreen(vm,requireNotNull(b.arguments?.getString("id"))) }
                composable("compare") { ComparisonScreen(vm) }
                composable("settings") { SettingsScreen(vm) }
            }
            if(error != null) AlertDialog(onDismissRequest={vm.error.value=null},title={Text("Notice")},
                text={Text(error.orEmpty())},confirmButton={TextButton(onClick={vm.error.value=null}) {Text("OK")}})
        }
    }
}

@Composable private fun Dashboard(vm: MainViewModel, goTests:()->Unit, goHistory:()->Unit, goDevice:()->Unit) {
    val results by vm.results.collectAsStateWithLifecycle()
    val running by vm.activeId.collectAsStateWithLifecycle()
    val progress by vm.quickScanProgress.collectAsStateWithLifecycle()
    LazyColumn(contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {
            Surface(color=MaterialTheme.colorScheme.surfaceVariant,shape=RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("DEVICE PROFILE",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                    Text("${Build.MANUFACTURER} ${Build.MODEL}",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                    Text("Android ${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}",color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("On-device diagnostics • No cloud upload",style=MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            StatTile("Saved in session",results.size.toString(),Modifier.weight(1f))
            StatTile("Functional passes",results.count{it.outcome==Outcome.PASSED}.toString(),Modifier.weight(1f))
            StatTile("Failures",results.count{it.outcome==Outcome.FAILED}.toString(),Modifier.weight(1f))
        } }
        item {
            Card { Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("QUICK SCAN",fontWeight=FontWeight.Bold)
                Text("Read 19 reported hardware categories. No camera opening, microphone recording, GPS fix, or biometric prompt.")
                if(running=="quick_scan") LinearProgressIndicator(progress={ progress / TestCatalog.passive.size.toFloat() },modifier=Modifier.fillMaxWidth())
                Button(onClick={vm.scan()},enabled=running==null,modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.PlayArrow,null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.quick_scan)) }
                if(running!=null) OutlinedButton(onClick={vm.cancel()},modifier=Modifier.fillMaxWidth()) { Text(stringResource(R.string.cancel)) }
            } }
        }
        item { Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick=goTests,modifier=Modifier.weight(1f)) { Text("Run tests") }
            OutlinedButton(onClick=goDevice,modifier=Modifier.weight(1f)) { Text("Device info") }
        } }
        item { Text("LATEST RESULTS",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold) }
        if(results.isEmpty()) item { Text("Run a quick scan, or select a real functional test.",style=MaterialTheme.typography.bodyMedium) }
        items(results.takeLast(12).reversed()) { ResultRow(it) }
        item { TextButton(onClick=goHistory) {Text("View saved sessions") } }
    }
}
@Composable private fun StatTile(label:String,value:String,modifier:Modifier=Modifier) {
    Card(modifier=modifier) { Column(Modifier.padding(12.dp)) {
        Text(value,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text(label,style=MaterialTheme.typography.labelSmall)
    } }
}
@Composable fun ResultRow(r: DiagnosticResult) {
    Card { Column(Modifier.fillMaxWidth().padding(13.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text(r.name,modifier=Modifier.weight(1f),fontWeight=FontWeight.SemiBold)
            Text(r.outcome.name.replace('_',' '),style=MaterialTheme.typography.labelSmall,color=statusColor(r.outcome))
        }
        Text(r.category+" · "+r.method,style=MaterialTheme.typography.bodySmall,maxLines=2,overflow=TextOverflow.Ellipsis)
        r.metrics.take(3).forEach { metric -> Text("${metric.name}: ${metric.value} ${metric.unit}",style=MaterialTheme.typography.bodySmall) }
        if(r.warning.isNotEmpty()) Text(r.warning,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.tertiary)
    } }
}
@Composable fun statusColor(outcome: Outcome): Color = when(outcome) {
    Outcome.PASSED -> Color(0xFF26BF9A)
    Outcome.FAILED,Outcome.ERROR -> MaterialTheme.colorScheme.error
    Outcome.NOT_TESTED,Outcome.NOT_SUPPORTED,Outcome.NOT_DETECTED -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> Color(0xFFFFB75B)
}
@Composable private fun TestsScreen(onOpen:(String)->Unit) {
    var search by remember { mutableStateOf("") }
    val filtered = TestCatalog.all.filter { it.title.contains(search,true) || it.category.contains(search,true) }
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        item { OutlinedTextField(value=search,onValueChange={search=it},label={Text("Search all real tests")},modifier=Modifier.fillMaxWidth(),singleLine=true) }
        items(filtered) { spec ->
            Card(onClick={onOpen(spec.id)}) { Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(spec.title,fontWeight=FontWeight.SemiBold)
                    Text("${spec.category} · ${spec.kind}",style=MaterialTheme.typography.labelSmall)
                    Text(spec.description,style=MaterialTheme.typography.bodySmall,maxLines=2)
                }
                Icon(Icons.Default.ChevronRight,null)
            } }
        }
    }
}
