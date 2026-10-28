package com.phonehardware.diagnostic.hardware

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.phonehardware.diagnostic.core.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.math.*

/** Bounded, cancellable tests. Never substitutes a simulated reading. */
class ActiveDiagnostics @Inject constructor(@ApplicationContext private val c: Context) {
    private fun make(id: String, status: Outcome, m: List<Metric>, src: String, method: String,
        why: String, next: String = "", warning: String = "", code: String = "") =
        TestCatalog.byId(id).let { DiagnosticResult(id,it.title,it.category,status,m,src,method,why,next,warning,code) }

    suspend fun run(id: String): DiagnosticResult {
        val spec = TestCatalog.byId(id)
        val required = spec.permission
        if (required != null && ContextCompat.checkSelfPermission(c,required) != PackageManager.PERMISSION_GRANTED)
            return make(id,Outcome.PERMISSION_REQUIRED,emptyList(),"Android runtime permissions","Permission check",
                "The requested test has not run because permission is missing.","Grant permission and retry.")
        if (Build.VERSION.SDK_INT < spec.minApi) return make(id,Outcome.NOT_SUPPORTED,emptyList(),"SDK check", "API guard","This Android release lacks the required API.")
        return try {
            withTimeout(spec.timeoutMillis) {
                when(id) {
                    "storage_integrity" -> testStorage(id)
                    "ram_integrity" -> testMemory(id)
                    "cpu_work" -> testCpu(id)
                    "accelerometer_events" -> testSensor(id,Sensor.TYPE_ACCELEROMETER,"m/s²")
                    "light_events" -> testSensor(id,Sensor.TYPE_LIGHT,"lx")
                    "proximity_events" -> testSensor(id,Sensor.TYPE_PROXIMITY,"cm")
                    "mic_record" -> testMic(id)
                    "gps_fix" -> testGps(id)
                    else -> make(id,Outcome.NOT_TESTED,emptyList(),"Test catalog","Interactive workflow",
                        "This test requires the dedicated user-controlled screen.","Open the interactive test.")
                }
            }
        } catch(e: TimeoutCancellationException) {
            make(id,Outcome.INCONCLUSIVE,emptyList(),"Android API","Timed wait", "Test timed out; a missing response is not proof of a defect.", "Retry under suitable conditions.",code="TIMEOUT")
        } catch(e: CancellationException) { throw e }
          catch(e: SecurityException) {
            make(id,Outcome.PERMISSION_REQUIRED,emptyList(),"Android permissions","Test access", "Access was denied during execution.","Grant permission and retry.",code="SECURITY_EXCEPTION")
        } catch(e: Exception) {
            make(id,Outcome.ERROR,emptyList(),"Android platform","Bounded functional test", "Test could not complete: ${e.javaClass.simpleName}", "Retry when the component is available.",code=e.javaClass.simpleName)
        }
    }
    private suspend fun testStorage(id: String): DiagnosticResult = withContext(Dispatchers.IO) {
        val file = File.createTempFile("diagnostic", ".test", c.cacheDir)
        try {
            val payload = ByteArray(1024*1024) { ((it*37 + 11) and 0xff).toByte() }
            val reference = MessageDigest.getInstance("SHA-256").digest(payload)
            val t0 = SystemClock.elapsedRealtimeNanos()
            file.outputStream().use { it.write(payload); it.flush(); it.fdSyncSafely() }
            val writeNs = SystemClock.elapsedRealtimeNanos() - t0
            currentCoroutineContext().ensureActive()
            val readStart = SystemClock.elapsedRealtimeNanos()
            val bytes = file.readBytes()
            val readNs = SystemClock.elapsedRealtimeNanos() - readStart
            val matches = MessageDigest.getInstance("SHA-256").digest(bytes).contentEquals(reference)
            make(id, if(matches) Outcome.PASSED else Outcome.FAILED, listOf(
                Metric("Test file size (اندازه فایل تست)",payload.size.toString(),"bytes",Evidence.MEASURED),
                Metric("Write throughput (سرعت نوشتن)",String.format(java.util.Locale.US,"%.2f",1048576.0/max(writeNs,1)/1e-9/1048576),"MiB/s",Evidence.MEASURED),
                Metric("Read throughput (سرعت خواندن)",String.format(java.util.Locale.US,"%.2f",1048576.0/max(readNs,1)/1e-9/1048576),"MiB/s",Evidence.MEASURED),
                Metric("SHA-256 integrity (سلامت و تطابق SHA-256)",matches.toString(),evidence=Evidence.MEASURED)
            ),"Application cache / java.io / MessageDigest","1 MiB write/read SHA-256 compare",
                if(matches) "The app's temporary-file round-trip matched exactly." else "Read-back data differed from written bytes.",
                warning="Filesystem caching and workload affect throughput; this is not a NAND wear test.")
        } finally { file.delete() }
    }
    private fun java.io.FileOutputStream.fdSyncSafely() { try { fd.sync() } catch (_: java.io.SyncFailedException) { /* not an integrity success condition */ } }
    private suspend fun testMemory(id: String): DiagnosticResult = withContext(Dispatchers.Default) {
        val data = ByteArray(512*1024)
        for(i in data.indices) { if(i%8192 == 0) currentCoroutineContext().ensureActive(); data[i] = ((i*17) xor (i ushr 4)).toByte() }
        var mismatch = 0
        for(i in data.indices) { if(i%8192 == 0) currentCoroutineContext().ensureActive(); if(data[i] != ((i*17) xor (i ushr 4)).toByte()) mismatch++ }
        make(id,if(mismatch==0) Outcome.PASSED else Outcome.FAILED,listOf(
            Metric("Bytes sampled (حجم نمونه حافظه)",data.size.toString(),"bytes",Evidence.MEASURED),
            Metric("Mismatches (تعداد عدم تطابق)",mismatch.toString(),evidence=Evidence.MEASURED)),
            "Application process heap","Allocate, pattern-write, read-back verify",
            if(mismatch==0) "All bytes in this limited app memory sample matched." else "An app-buffer mismatch was observed.",
            warning="This cannot diagnose physical DRAM cells or memory outside the app.")
    }
    private suspend fun testCpu(id: String): DiagnosticResult = withContext(Dispatchers.Default) {
        val power = c.getSystemService(PowerManager::class.java)
        val start = SystemClock.elapsedRealtimeNanos()
        var loops = 0L; var checksum = 0.3125
        var tooHot = false
        while(SystemClock.elapsedRealtimeNanos()-start < 1_500_000_000L) {
            repeat(1000) { v -> checksum = checksum * 1.00000001 + sqrt((v+1).toDouble()) / 1000000.0 }
            loops += 1000
            currentCoroutineContext().ensureActive()
            if(Build.VERSION.SDK_INT >= 29 && power.currentThermalStatus >= PowerManager.THERMAL_STATUS_SEVERE) {
                tooHot = true; break
            }
        }
        val seconds = (SystemClock.elapsedRealtimeNanos()-start)/1e9
        make(id, if(tooHot) Outcome.INCONCLUSIVE else Outcome.PASSED,listOf(
            Metric("Iterations (تعداد تکرار محاسبات)",loops.toString(),evidence=Evidence.MEASURED),
            Metric("Duration (مدت زمان)",String.format(java.util.Locale.US,"%.3f",seconds),"s",Evidence.MEASURED),
            Metric("Iterations/second (تکرار در ثانیه)",String.format(java.util.Locale.US,"%.0f",loops/seconds),"ops/s",Evidence.MEASURED),
            Metric("Computation checksum (چک‌سام محاسباتی)",checksum.toString(),evidence=Evidence.MEASURED)
        ),"SystemClock / Kotlin math / PowerManager","Single-coroutine fixed-work floating point workload",
            if(tooHot) "Terminated early due to severe reported thermal state." else "The bounded computation executed successfully.",
            warning="Application-specific throughput, not a CPU health or cross-device benchmark.")
    }
    private suspend fun testSensor(id: String,type: Int, unit: String): DiagnosticResult {
        val mgr = c.getSystemService(SensorManager::class.java)
        val sensor = mgr.getDefaultSensor(type)
            ?: return make(id,Outcome.NOT_DETECTED,emptyList(),"SensorManager","getDefaultSensor","Sensor type is not reported by Android.")
        val reading = withContext(Dispatchers.Main.immediate) {
            withTimeoutOrNull(10_000) {
                suspendCancellableCoroutine<FloatArray?> { cont ->
                    val l = object : SensorEventListener {
                        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                        override fun onSensorChanged(event: SensorEvent) {
                            mgr.unregisterListener(this)
                            if(cont.isActive) cont.resume(event.values.clone())
                        }
                    }
                    val registered = mgr.registerListener(l,sensor,SensorManager.SENSOR_DELAY_NORMAL)
                    if(!registered && cont.isActive) cont.resume(null)
                    cont.invokeOnCancellation { mgr.unregisterListener(l) }
                }
            }
        }
        if(reading==null) return make(id,Outcome.INCONCLUSIVE,emptyList(),"SensorManager","SensorEventListener (10 s)",
            "No event was received within the sampling interval.","Try after actively moving or exposing the sensor.")
        return make(id,Outcome.PASSED,reading.mapIndexed { idx,v -> Metric("Axis/value $idx (محور/مقدار $idx)",v.toString(),unit,Evidence.MEASURED) },
            "SensorManager / SensorEventListener","Actual first sensor event", "A real sensor event arrived. Calibration and response quality were not verified.",
            warning="Event reception alone does not prove sensor accuracy.")
    }
    @android.annotation.SuppressLint("MissingPermission")
    private suspend fun testMic(id: String): DiagnosticResult = withContext(Dispatchers.IO) {
        val rate = 16000
        val size = AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)
        if(size <= 0) return@withContext make(id,Outcome.UNAVAILABLE,emptyList(),"AudioRecord","Query buffer size","PCM recording configuration is unsupported.")
        val rec = AudioRecord(MediaRecorder.AudioSource.MIC,rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,max(size,4096))
        try {
            if(rec.state!=AudioRecord.STATE_INITIALIZED) return@withContext make(id,Outcome.UNAVAILABLE,emptyList(),"AudioRecord","Initialize recording","Microphone recorder did not initialize.")
            rec.startRecording()
            val buf = ShortArray(2048)
            var samples = 0; var energy = 0.0; var maxAbs = 0
            val start = SystemClock.elapsedRealtime()
            while(SystemClock.elapsedRealtime()-start < 1000 && samples < rate*2) {
                currentCoroutineContext().ensureActive()
                val count = rec.read(buf,0,buf.size)
                if(count<0) return@withContext make(id,Outcome.ERROR,emptyList(),"AudioRecord","read() returned $count","Recording failed during read.",code="AUDIO_READ_$count")
                for(i in 0 until count) { val x=buf[i].toInt(); energy+=x.toDouble()*x; maxAbs=max(maxAbs,abs(x)) }
                samples+=count
            }
            val rms = if(samples>0) sqrt(energy/samples) else 0.0
            val dbfs = if(rms>0) 20*log10(rms/32768.0) else Double.NEGATIVE_INFINITY
            make(id,if(samples>0) Outcome.PASSED else Outcome.INCONCLUSIVE,listOf(
                Metric("Samples recorded (تعداد نمونه‌های ضبط‌شده)",samples.toString(),"samples",Evidence.MEASURED),
                Metric("RMS (مقدار جذر میانگین مربعات RMS)",String.format(java.util.Locale.US,"%.2f",rms),"PCM",Evidence.MEASURED),
                Metric("Relative signal level (سطح نسبی سیگنال)",if(dbfs.isFinite()) String.format(java.util.Locale.US,"%.1f",dbfs) else "Silence", "dBFS",Evidence.MEASURED),
                Metric("Peak (حداکثر دامنه سیگنال)",maxAbs.toString(),"PCM",Evidence.MEASURED)
            ),"AudioRecord","16 kHz mono PCM, 1-second bounded capture",
                if(samples>0) "Real microphone PCM samples were recorded; audibility and fidelity remain unverified." else "No samples captured.",
                warning="dBFS is NOT calibrated sound pressure (dB SPL); no recordings are persisted.")
        } finally { if(rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) rec.stop(); rec.release() }
    }
    private suspend fun testGps(id: String): DiagnosticResult {
        val lm = c.getSystemService(LocationManager::class.java)
        if(!lm.allProviders.contains(LocationManager.GPS_PROVIDER)) return make(id,Outcome.NOT_DETECTED,emptyList(),"LocationManager","Providers","GPS provider not reported.")
        if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) return make(id,Outcome.UNAVAILABLE,emptyList(),"LocationManager","Provider enabled","GPS location setting is switched off.","Enable location settings and retry.")
        val start = SystemClock.elapsedRealtime()
        val fix = withContext(Dispatchers.Main.immediate) {
            withTimeoutOrNull(24_000) {
                suspendCancellableCoroutine<Location?> { cont ->
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            lm.removeUpdates(this)
                            if(cont.isActive) cont.resume(location)
                        }
                        @Deprecated("Required for legacy API 23 provider callbacks")
                        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
                        override fun onProviderEnabled(provider: String) = Unit
                        override fun onProviderDisabled(provider: String) = Unit
                    }
                    try { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,0L,0f,listener,Looper.getMainLooper()) }
                    catch (e: SecurityException) { if(cont.isActive) cont.resume(null) }
                    cont.invokeOnCancellation { lm.removeUpdates(listener) }
                }
            }
        }
        return if(fix==null) make(id,Outcome.INCONCLUSIVE,emptyList(),"LocationManager","Fresh GPS provider request","No fresh GPS fix arrived within 24 seconds.","Try outdoors with a clear sky view.")
        else make(id,Outcome.PASSED,listOf(
            Metric("Time to first received fix (زمان دریافت نخستین موقعیت)",(SystemClock.elapsedRealtime()-start).toString(),"ms",Evidence.MEASURED),
            Metric("Platform estimated horizontal accuracy (دقت افقی برآوردی)",fix.accuracy.toString(),"m",Evidence.SYSTEM_REPORTED),
            Metric("Fix age (قدمت موقعیت دریافتی)",(SystemClock.elapsedRealtimeNanos()-fix.elapsedRealtimeNanos).coerceAtLeast(0L).div(1_000_000).toString(),"ms",Evidence.MEASURED)
        ),"LocationManager GPS_PROVIDER","Fresh GPS location update; coordinates not saved",
            "GPS provider delivered a location update, not an independently validated position.",
            warning="Android accuracy is an estimate; indoor difficulty is not proof of GPS failure.")
    }
}
