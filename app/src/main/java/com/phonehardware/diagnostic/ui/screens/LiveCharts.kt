package com.phonehardware.diagnostic.ui.screens

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.os.BatteryManager
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.phonehardware.diagnostic.MainActivity
import com.phonehardware.diagnostic.core.*
import com.phonehardware.diagnostic.ui.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable fun SimpleTrend(samples: List<Float>, unit: String, label: String) {
    Text("$label · ${samples.lastOrNull()?.toString() ?: "No samples"} $unit")
    Canvas(Modifier.fillMaxWidth().height(110.dp)) {
        if(samples.size>=2) {
            val low=samples.minOrNull() ?: 0f; val high=samples.maxOrNull() ?: 0f
            val span=(high-low).coerceAtLeast(0.0001f)
            val path=Path()
            samples.forEachIndexed { i,v ->
                val x=i.toFloat()/(samples.size-1)*size.width
                val y=size.height - (v-low)/span*size.height
                if(i==0) path.moveTo(x,y) else path.lineTo(x,y)
            }
            drawPath(path,Color(0xFF54A9FA),style=androidx.compose.ui.graphics.drawscope.Stroke(width=3.dp.toPx()))
        }
    }
}

@Composable fun SensorExplorerScreen(vm:MainViewModel,activity:MainActivity) {
    val sm=remember {activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager}
    val sensors=remember { sm.getSensorList(Sensor.TYPE_ALL).sortedWith(compareBy({it.type},{it.name})) }
    var selected by remember { mutableIntStateOf(0) }
    var listening by remember { mutableStateOf(false) }
    var latest by remember { mutableStateOf<List<Float>>(emptyList()) }
    val trail=remember { mutableStateListOf<Float>() }
    var count by remember { mutableIntStateOf(0) }
    var observedHz by remember { mutableStateOf(0f) }
    var lastNs by remember { mutableLongStateOf(0L) }
    val lifecycle=LocalLifecycleOwner.current
    val sensor=sensors.getOrNull(selected)
    DisposableEffect(sensor,listening,lifecycle) {
        var listener:SensorEventListener?=null
        var trigger:TriggerEventListener?=null
        var registered=false
        fun stop() {
            listener?.let(sm::unregisterListener)
            trigger?.let { if(sensor!=null) sm.cancelTriggerSensor(it,sensor) }
            registered=false
        }
        fun start() {
            if(sensor==null || registered || !listening) return
            if(sensor.reportingMode==Sensor.REPORTING_MODE_ONE_SHOT) {
                val t=object:TriggerEventListener() {
                    override fun onTrigger(event:TriggerEvent) {
                        latest=event.values.toList();count++;trail.add(event.values.firstOrNull() ?: 0f)
                    }
                }
                trigger=t; registered=runCatching {sm.requestTriggerSensor(t,sensor)}.getOrDefault(false)
            } else {
                val l=object:SensorEventListener {
                    override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int) = Unit
                    override fun onSensorChanged(event:SensorEvent) {
                        val previous=lastNs
                        if(previous!=0L && event.timestamp>previous) observedHz=(1_000_000_000.0/(event.timestamp-previous)).toFloat()
                        lastNs=event.timestamp
                        latest=event.values.toList()
                        count++
                        trail.add(event.values.firstOrNull() ?: 0f)
                        if(trail.size>80) trail.removeAt(0)
                    }
                }
                listener=l; registered=runCatching {sm.registerListener(l,sensor,SensorManager.SENSOR_DELAY_UI)}.getOrDefault(false)
            }
        }
        val observer=LifecycleEventObserver { _, event ->
            when(event) {
                Lifecycle.Event.ON_START -> start()
                Lifecycle.Event.ON_STOP -> stop()
                else -> Unit
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        if(lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) start()
        onDispose { lifecycle.lifecycle.removeObserver(observer); stop() }
    }
    Text("Every sensor below comes from SensorManager.TYPE_ALL, including vendor sensors. Values are raw Android events.")
    if(sensors.isEmpty()) { Text("No accessible sensors were reported."); return }
    var expanded by remember {mutableStateOf(false)}
    Box {
        OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) {
            Text("${sensor?.name ?: "Select sensor"} (type ${sensor?.type ?: "—"})")
        }
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
            sensors.forEachIndexed { i,s -> DropdownMenuItem(text={Text("${s.name} (${s.type})")},onClick={
                selected=i;expanded=false;listening=false;latest=emptyList();trail.clear();count=0;lastNs=0
            }) }
        }
    }
    sensor?.let {s ->
        Text("Vendor: ${s.vendor} · Resolution: ${s.resolution} · Maximum range: ${s.maximumRange}")
        Text("Power: ${s.power} mA · Reporting mode ${s.reportingMode} · Wake-up ${s.isWakeUpSensor}")
    }
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Button(onClick={listening=true},enabled=!listening){Text("Start events")}
        OutlinedButton(onClick={listening=false},enabled=listening){Text("Stop")}
    }
    Text("Events observed: $count · Approx. recent rate: ${"%.1f".format(observedHz)} Hz")
    Text("Latest: ${latest.joinToString(prefix="[",postfix="]")}")
    SimpleTrend(trail.toList(),"raw units","First reported component")
    Button(enabled=count>0,onClick={
        val s=sensor ?: return@Button
        vm.store(DiagnosticResult("sensor_explorer","Live sensor explorer","Sensors",Outcome.PASSED,
            listOf(Metric("Sensor",s.name,evidence=Evidence.SYSTEM_REPORTED),
                Metric("Sensor type",s.type.toString(),evidence=Evidence.SYSTEM_REPORTED),
                Metric("Event count",count.toString(),evidence=Evidence.MEASURED),
                Metric("Last values",latest.joinToString(),evidence=Evidence.MEASURED)),
            "SensorManager / SensorEventListener / TriggerEventListener","Real event reception from selected sensor",
            "Events were received; this verifies app-visible data delivery, not accuracy.",
            warning="Update frequencies and live values depend on lifecycle, device and sensor reporting mode."))
    }){Text("Save observed event evidence")}
}

private data class BatterySample(val t:Long,val level:Float,val temp:Float,val current:Float?,val voltage:Float)
@Composable fun BatteryTrendScreen(vm:MainViewModel,activity:MainActivity) {
    var recording by remember { mutableStateOf(false) }
    val samples=remember { mutableStateListOf<BatterySample>() }
    val lifecycle=LocalLifecycleOwner.current
    LaunchedEffect(recording) {
        if(recording) {
            lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val started=SystemClock.elapsedRealtime()
                while(isActive && SystemClock.elapsedRealtime()-started<15_000) {
                    val i=activity.registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    if(i!=null) {
                        val level=i.getIntExtra(BatteryManager.EXTRA_LEVEL,-1)
                        val scale=i.getIntExtra(BatteryManager.EXTRA_SCALE,-1)
                        val current=(activity.getSystemService(BatteryManager::class.java)).getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                        samples.add(BatterySample(SystemClock.elapsedRealtime(),if(level>=0 && scale>0) level*100f/scale else Float.NaN,
                            i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)/10f,
                            if(current==Int.MIN_VALUE) null else current/1000f,
                            i.getIntExtra(BatteryManager.EXTRA_VOLTAGE,0).toFloat()))
                    }
                    delay(1500)
                }
                recording=false
            }
        }
    }
    Text("Observe platform-reported battery values over a short interval. Charging behavior and capacity health cannot be inferred from this short test.")
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Button(enabled=!recording,onClick={samples.clear();recording=true}){Text("Record 15 seconds")}
        OutlinedButton(enabled=recording,onClick={recording=false}){Text("Stop")}
    }
    Text("Collected readings: ${samples.size}")
    SimpleTrend(samples.map { it.level }.filter { it.isFinite() },"%","Battery level")
    SimpleTrend(samples.map {it.temp},"°C","Battery temperature")
    SimpleTrend(samples.mapNotNull {it.current},"mA","Current (system reported)")
    SimpleTrend(samples.map {it.voltage},"mV","Voltage")
    Button(enabled=samples.size>=2,onClick={
        vm.store(DiagnosticResult("battery_trend","Live battery observation","Battery",Outcome.PASSED,
            listOf(Metric("Observations",samples.size.toString(),evidence=Evidence.MEASURED),
                Metric("Start level",samples.first().level.toString(),"%",Evidence.SYSTEM_REPORTED),
                Metric("End level",samples.last().level.toString(),"%",Evidence.SYSTEM_REPORTED),
                Metric("Start temperature",samples.first().temp.toString(),"°C",Evidence.SYSTEM_REPORTED),
                Metric("End temperature",samples.last().temp.toString(),"°C",Evidence.SYSTEM_REPORTED)),
            "ACTION_BATTERY_CHANGED / BatteryManager","User-started 1.5-second spaced readings",
            "Android returned repeated battery measurements; battery condition not established.",warning="Battery percentages can remain constant during short valid samples; missing current is not a failure."))
    }) {Text("Save battery observation")}
}
