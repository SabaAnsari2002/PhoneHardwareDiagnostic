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
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
@Composable
fun App(vm: MainViewModel, activity: MainActivity) {
    val dark by vm.darkMode.collectAsStateWithLifecycle()
    val running by vm.activeId.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "dashboard"
    val rootRoutes = setOf("dashboard", "tests", "history", "settings")
    val isRoot = route in rootRoutes
    val title = when (route) {
        "tests" -> "همه تست‌ها"
        "history" -> "تاریخچه تشخیص‌ها"
        "settings" -> "تنظیمات و حریم خصوصی"
        "compare" -> "مقایسه گزارش‌ها"
        "history/{id}" -> "گزارش جلسه"
        "test/{id}" -> "جزئیات تست"
        else -> "Phone Hardware Diagnostic"
    }

    DiagnosticTheme(dark) {
        // Read composable theme values before entering the non-composable SideEffect.
        val barColor = MaterialTheme.colorScheme.surface.toArgb()
        SideEffect {
            activity.window.statusBarColor = barColor
            activity.window.navigationBarColor = barColor
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        if (!isRoot) {
                            IconButton(onClick = { nav.navigateUp() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                            FeatureIcon(Icons.Default.Memory, modifier = Modifier.size(40.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    if (isRoot) "Phone Hardware Diagnostic" else title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (running != null) "در حال اجرای بررسی…" else "Hardware Test & Device Check",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (running != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        if (running != null) {
                            CircularProgressIndicator(
                                Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(14.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                if (isRoot) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 0.dp
                    ) {
                        listOf(
                            Triple("dashboard", Icons.Default.Dashboard, R.string.dashboard),
                            Triple("tests", Icons.Default.Science, R.string.all_tests),
                            Triple("history", Icons.Default.History, R.string.history),
                            Triple("settings", Icons.Default.Settings, R.string.settings)
                        ).forEach { (itemRoute, icon, label) ->
                            val selected = route == itemRoute
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    nav.navigate(itemRoute) {
                                        launchSingleTop = true
                                        popUpTo("dashboard") { saveState = true }
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(icon, contentDescription = null) },
                                label = { Text(stringResource(label), maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Surface(
                modifier = Modifier.fillMaxSize().padding(padding),
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(screenBackgroundBrush())
                ) {
                    NavHost(navController = nav, startDestination = "dashboard", modifier = Modifier.fillMaxSize()) {
                        composable("dashboard") {
                            Dashboard(
                                vm,
                                goTests = { nav.navigate("tests") },
                                goHistory = { nav.navigate("history") },
                                goDevice = { nav.navigate("test/device") }
                            )
                        }
                        composable("tests") { TestsScreen(onOpen = { nav.navigate("test/$it") }) }
                        composable("test/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                            TestDetail(TestCatalog.byId(requireNotNull(entry.arguments?.getString("id"))), vm, activity)
                        }
                        composable("history") {
                            HistoryScreen(vm, onOpen = { nav.navigate("history/$it") }, onCompare = { nav.navigate("compare") })
                        }
                        composable("history/{id}") { entry ->
                            SessionScreen(vm, requireNotNull(entry.arguments?.getString("id")))
                        }
                        composable("compare") { ComparisonScreen(vm) }
                        composable("settings") { SettingsScreen(vm) }
                    }
                }
            }
            if (error != null) {
                AlertDialog(
                    onDismissRequest = { vm.error.value = null },
                    title = { Text("Notice") },
                    text = { Text(error.orEmpty()) },
                    confirmButton = { TextButton(onClick = { vm.error.value = null }) { Text("OK") } },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Dashboard(vm: MainViewModel, goTests: () -> Unit, goHistory: () -> Unit, goDevice: () -> Unit) {
    val results by vm.results.collectAsStateWithLifecycle()
    val running by vm.activeId.collectAsStateWithLifecycle()
    val progress by vm.quickScanProgress.collectAsStateWithLifecycle()
    val passed = results.count { it.outcome == Outcome.PASSED }
    val failed = results.count { it.outcome == Outcome.FAILED || it.outcome == Outcome.ERROR }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            NeonPanel(accent = NeonBlue) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    FeatureIcon(Icons.Default.Smartphone, accent = NeonCyan, modifier = Modifier.size(58.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("شناسه دستگاه  ·  DEVICE PROFILE", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("${Build.MANUFACTURER} ${Build.MODEL}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text("Android ${Build.VERSION.RELEASE}  ·  API ${Build.VERSION.SDK_INT}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(11.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("پردازش روی دستگاه  ·  بدون آپلود ابری", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    StatusPill("OFFLINE-FIRST", NeonGreen)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                StatTile("ذخیره‌شده", "Saved", results.size.toString(), Icons.Default.AssignmentTurnedIn, NeonCyan, Modifier.weight(1f))
                StatTile("موفق", "Passed", passed.toString(), Icons.Default.CheckCircle, NeonGreen, Modifier.weight(1f))
                StatTile("خطا", "Issues", failed.toString(), Icons.Default.Warning, Color(0xFFFFA34A), Modifier.weight(1f))
            }
        }

        item {
            NeonPanel(accent = NeonCyan) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("اسکن سریع", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text("QUICK SCAN  ·  19 CATEGORIES", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    FeatureIcon(Icons.Default.Memory, accent = NeonCyan, modifier = Modifier.size(54.dp))
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    "بررسی ۱۹ دسته اطلاعات سخت‌افزاری گزارش‌شده توسط اندروید؛ بدون باز کردن دوربین، ضبط صدا، دریافت موقعیت GPS یا درخواست احراز هویت.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (running == "quick_scan") {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress / TestCatalog.passive.size.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text("$progress از ${TestCatalog.passive.size} دسته بررسی شد", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(13.dp))
                AccentActionButton(
                    text = if (running == "quick_scan") "اسکن در حال اجرا…" else "شروع اسکن سریع",
                    onClick = { vm.scan() },
                    enabled = running == null,
                    icon = Icons.Default.PlayArrow,
                    modifier = Modifier.fillMaxWidth()
                )
                if (running != null) {
                    Spacer(Modifier.height(8.dp))
                    OutlineActionButton("توقف بررسی", onClick = vm::cancel, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlineActionButton("همه تست‌ها", goTests, icon = Icons.Default.Science, modifier = Modifier.weight(1f))
                OutlineActionButton("اطلاعات دستگاه", goDevice, icon = Icons.Default.PhoneAndroid, modifier = Modifier.weight(1f))
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("آخرین نتایج", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("LATEST RESULTS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = goHistory) { Text("تاریخچه", color = MaterialTheme.colorScheme.primary) }
            }
        }
        if (results.isEmpty()) {
            item {
                NeonPanel(accent = MaterialTheme.colorScheme.outline) {
                    Text("هنوز نتیجه‌ای ثبت نشده است", fontWeight = FontWeight.Bold)
                    Text("برای مشاهده اطلاعات دستگاه، اسکن سریع را اجرا کنید یا یکی از تست‌های واقعی را انتخاب کنید.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        items(results.takeLast(12).reversed()) { ResultRow(it) }
    }
}

@Composable
private fun StatTile(label: String, english: String, value: String, icon: ImageVector, accent: Color, modifier: Modifier = Modifier) {
    val resolvedAccent = themedAccent(accent)
    Surface(
        modifier = modifier.border(1.dp, resolvedAccent.copy(alpha = 0.24f), RoundedCornerShape(19.dp)),
        shape = RoundedCornerShape(19.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = resolvedAccent, modifier = Modifier.size(21.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(english, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
fun ResultRow(r: DiagnosticResult) {
    NeonPanel(accent = statusColor(r.outcome)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(r.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            StatusPill(r.outcome.name.replace('_', ' '), statusColor(r.outcome))
        }
        Spacer(Modifier.height(5.dp))
        Text(r.category + " · " + r.method, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        r.metrics.take(3).forEach { metric ->
            Text("${metric.name}: ${metric.value} ${metric.unit}", style = MaterialTheme.typography.bodySmall)
        }
        if (r.warning.isNotEmpty()) Text(r.warning, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
fun statusColor(outcome: Outcome): Color {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    return when (outcome) {
        Outcome.PASSED -> if (isLight) Color(0xFF087A5B) else NeonGreen
        Outcome.FAILED, Outcome.ERROR -> if (isLight) Color(0xFFB32645) else Color(0xFFFF647C)
        Outcome.NOT_TESTED, Outcome.NOT_SUPPORTED, Outcome.NOT_DETECTED -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> if (isLight) Color(0xFF8A5100) else Color(0xFFFFB34D)
    }
}

@Composable
private fun TestsScreen(onOpen: (String) -> Unit) {
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("همه") }
    val categories = TestCatalog.all.map { it.category.substringBefore("(").trim() }.distinct()
    val filtered = TestCatalog.all.filter { spec ->
        val matchesSearch = search.isBlank() || spec.title.contains(search, true) || spec.category.contains(search, true) || spec.description.contains(search, true)
        val matchesCategory = selectedCategory == "همه" || spec.category.substringBefore("(").trim() == selectedCategory
        matchesSearch && matchesCategory
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item {
            PageHeading("HARDWARE LAB", "همه تست‌ها", "آزمون‌های واقعی و اطلاعات قابل مشاهده دستگاه")
        }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("جست‌وجوی تست‌ها…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                shape = RoundedCornerShape(17.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == "همه",
                    onClick = { selectedCategory = "همه" },
                    label = { Text("همه") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    )
                }
            }
        }
        item {
            Text("${filtered.size} آزمون در دسترس", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
        items(filtered) { spec ->
            Card(
                onClick = { onOpen(spec.id) },
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(19.dp)),
                shape = RoundedCornerShape(19.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureIcon(testIcon(spec.category), accent = testAccent(spec.category))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(spec.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text("${spec.category} · ${kindLabel(spec.kind)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(spec.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun kindLabel(kind: TestKind): String = when (kind) {
    TestKind.PASSIVE -> "اطلاعات سیستم"
    TestKind.AUTOMATED -> "خودکار"
    TestKind.ASSISTED -> "با تأیید کاربر"
    TestKind.INTERACTIVE -> "تعاملی"
}

@Composable
private fun testIcon(category: String): ImageVector = when {
    category.contains("Battery", true) -> Icons.Default.BatteryChargingFull
    category.contains("Display", true) -> Icons.Default.Monitor
    category.contains("Camera", true) -> Icons.Default.CameraAlt
    category.contains("Audio", true) -> Icons.Default.VolumeUp
    category.contains("Sensor", true) -> Icons.Default.Sensors
    category.contains("Location", true) -> Icons.Default.LocationOn
    category.contains("Wi-Fi", true) -> Icons.Default.Wifi
    category.contains("Bluetooth", true) -> Icons.Default.Bluetooth
    category.contains("Cellular", true) -> Icons.Default.SimCard
    category.contains("CPU", true) -> Icons.Default.Memory
    category.contains("RAM", true) -> Icons.Default.Memory
    category.contains("Storage", true) -> Icons.Default.Storage
    category.contains("Biometric", true) -> Icons.Default.Fingerprint
    category.contains("Security", true) -> Icons.Default.Security
    category.contains("Thermal", true) -> Icons.Default.Thermostat
    category.contains("Network", true) -> Icons.Default.NetworkCheck
    category.contains("Physical", true) -> Icons.Default.Vibration
    category.contains("Advanced", true) -> Icons.Default.Usb
    else -> Icons.Default.Devices
}

private fun testAccent(category: String): Color = when {
    category.contains("Battery", true) -> NeonGreen
    category.contains("Camera", true) -> Color(0xFFB79BFF)
    category.contains("Audio", true) -> Color(0xFFFFC45C)
    category.contains("Display", true) -> NeonBlue
    else -> NeonCyan
}
