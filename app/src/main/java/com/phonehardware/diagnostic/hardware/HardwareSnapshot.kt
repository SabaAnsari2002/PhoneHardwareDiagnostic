package com.phonehardware.diagnostic.hardware

import android.Manifest
import android.app.ActivityManager
import android.app.KeyguardManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.*
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import android.telephony.TelephonyManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.core.content.ContextCompat
import com.phonehardware.diagnostic.core.*
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.math.roundToInt

/** All values come from documented system APIs, not guessed device-spec databases. */
class HardwareSnapshot @Inject constructor(@ApplicationContext private val c: Context) {
    private val pm get() = c.packageManager
    private fun m(n: String, v: Any?, unit: String = "", evidence: Evidence = Evidence.SYSTEM_REPORTED) =
        Metric(n, v?.toString() ?: "Unavailable", unit, evidence)
    private fun reported(n: String, value: Number, unit: String, valid: Boolean = true): Metric =
        m(n, if(valid) value.toString() else "Not reported", unit)
    private fun result(id: String, source: String, values: List<Metric>, warning: String = ""):
        DiagnosticResult {
        val spec = TestCatalog.byId(id)
        return DiagnosticResult(id, spec.title, spec.category, Outcome.NOT_TESTED, values, source,
            "Passive, on-device read; no functional test executed",
            "Capabilities and system reports gathered. Functionality has not been proven.",
            "Run the relevant active test when one is available.", warning)
    }
    fun read(id: String): DiagnosticResult = try {
        when (id) {
            "device" -> device()
            "battery" -> battery()
            "display" -> display()
            "camera" -> camera()
            "audio" -> audio()
            "sensors" -> sensors()
            "location" -> location()
            "wifi" -> wifi()
            "bluetooth" -> bluetooth()
            "cellular" -> cellular()
            "advanced" -> advanced()
            "cpu" -> cpu()
            "ram" -> ram()
            "storage" -> storage()
            "biometric" -> biometric()
            "buttons" -> buttons()
            "security" -> security()
            "thermal" -> thermal()
            "network" -> network()
            else -> error("No passive implementation: $id")
        }
    } catch (e: SecurityException) {
        val s = TestCatalog.byId(id)
        DiagnosticResult(id,s.title,s.category,Outcome.PERMISSION_REQUIRED,emptyList(),"Android permissions",
            "Capability API access","Android denied access to this information.","Open app permissions and retry.",errorCode="SECURITY_EXCEPTION")
    } catch (e: Exception) {
        val s = TestCatalog.byId(id)
        DiagnosticResult(id,s.title,s.category,Outcome.ERROR,emptyList(),"Android API",
            "Platform information query", "Could not read this information: ${e.javaClass.simpleName}",
            "Retry on this device.", errorCode=e.javaClass.simpleName)
    }
    private fun device(): DiagnosticResult {
        val features = pm.systemAvailableFeatures.mapNotNull { it.name }.sorted()
        val result = listOf(
            m("Manufacturer (سازنده)", Build.MANUFACTURER),m("Brand (برند)",Build.BRAND),m("Model (مدل)",Build.MODEL),
            m("Device (دستگاه)",Build.DEVICE),m("Product (محصول)",Build.PRODUCT),m("Hardware (سخت‌افزار)",Build.HARDWARE),
            m("Build ID (شناسه ساخت)",Build.ID),m("Build display (نمایش ساخت)",Build.DISPLAY),m("Android (نسخه اندروید)",Build.VERSION.RELEASE),
            m("API (سطح API)",Build.VERSION.SDK_INT),m("Security patch (وصله امنیتی)", if(Build.VERSION.SDK_INT>=23) Build.VERSION.SECURITY_PATCH else "Unavailable"),
            m("ABIs (معماری‌های پردازنده)",Build.SUPPORTED_ABIS.joinToString()),m("Uptime (زمان روشن بودن)", SystemClock.elapsedRealtime()/1000,"s"),
            m("Feature count (تعداد ویژگی‌ها)",features.size),m("Reported features (ویژگی‌های گزارش‌شده)",features.joinToString("\n"))
        )
        return result("device","Build / PackageManager / SystemClock",result)
    }
    private fun battery(): DiagnosticResult {
        val battery = c.getSystemService(BatteryManager::class.java)
        val i = c.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val props = mutableListOf<Metric>()
        if (i == null) return result("battery","BatteryManager",listOf(m("Battery broadcast (وضعیت باتری)","Unavailable")))
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL,-1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE,-1)
        props += reported("Level (سطح شارژ)",if(level>=0 && scale>0) level*100/scale else -1,"%",level>=0 && scale>0)
        props += m("Charge status code (کد وضعیت شارژ)",i.getIntExtra(BatteryManager.EXTRA_STATUS,-1))
        props += m("System health category code (کد وضعیت سلامت)",i.getIntExtra(BatteryManager.EXTRA_HEALTH,-1))
        props += m("Plugged source code (کد منبع اتصال شارژر)",i.getIntExtra(BatteryManager.EXTRA_PLUGGED,-1))
        props += reported("Temperature (دما)",i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,-1)/10.0,"°C",i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,-1)>=0)
        props += reported("Voltage (ولتاژ)",i.getIntExtra(BatteryManager.EXTRA_VOLTAGE,-1),"mV",i.getIntExtra(BatteryManager.EXTRA_VOLTAGE,-1)>=0)
        for ((key,name,unit) in listOf(Triple(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW,"Instant current (جریان لحظه‌ای)","µA"),
            Triple(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE,"Average current (میانگین جریان)","µA"),
            Triple(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER,"Charge counter (شمارنده شارژ)","µAh"))) {
            val v = battery.getIntProperty(key)
            props += m(name,if(v==Int.MIN_VALUE) "Not reported" else v.toString(),unit)
        }
        if (Build.VERSION.SDK_INT >= 28) {
            val v = battery.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
            props += m("Remaining energy (انرژی باقی‌مانده)",if(v==Long.MIN_VALUE) "Not reported" else v.toString(),"nWh")
        }
        if(Build.VERSION.SDK_INT>=34) {
            val cycles = i.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT,-1)
            props += m("Cycle count (تعداد چرخه شارژ)",if(cycles>=0) cycles else "Not reported")
        }
        val pmgr = c.getSystemService(PowerManager::class.java)
        props += m("Power saver (حالت ذخیره انرژی)",pmgr.isPowerSaveMode)
        return result("battery","ACTION_BATTERY_CHANGED / BatteryManager",props,
            "Android's reported health category is not battery capacity retention.")
    }
    private fun display(): DiagnosticResult {
        val dm = c.getSystemService(DisplayManager::class.java)
        val d = dm.displays.firstOrNull()
        val metrics = c.resources.displayMetrics
        val props = mutableListOf(m("Density DPI (تراکم پیکسل DPI)",metrics.densityDpi),m("Logical density (تراکم منطقی)",metrics.density))
        if(d!=null) {
            props += m("Current size (اندازه فعلی)", "${d.mode.physicalWidth} × ${d.mode.physicalHeight}","px")
            props += m("Refresh rate (نرخ نوسازی)",d.refreshRate,"Hz")
            props += m("Supported modes (حالت‌های پشتیبانی‌شده)",d.supportedModes.joinToString { "${it.physicalWidth}×${it.physicalHeight}@${it.refreshRate}Hz" })
            props += m("Rotation index (زاویه چرخش)",d.rotation)
            if(Build.VERSION.SDK_INT>=24) props += m("HDR types (انواع HDR)",d.hdrCapabilities?.supportedHdrTypes?.joinToString())
            if(Build.VERSION.SDK_INT>=26) props += m("Wide color gamut (طیف رنگی گسترده)",d.isWideColorGamut)
        }
        return result("display","DisplayManager / DisplayMetrics",props)
    }
    private fun camera(): DiagnosticResult {
        val manager = c.getSystemService(CameraManager::class.java)
        val props = mutableListOf<Metric>()
        val ids = manager.cameraIdList
        props += m("Accessible camera count (تعداد دوربین‌های در دسترس)",ids.size)
        ids.forEach { id ->
            val ch = manager.getCameraCharacteristics(id)
            val facing = ch.get(CameraCharacteristics.LENS_FACING)
            props += m("Camera $id facing (جهت دوربین $id)",when(facing) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Front (جلو)"
                CameraCharacteristics.LENS_FACING_BACK -> "Back (پشت)"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "External (خارجی)"
                else -> "Unspecified (نامشخص)"
            })
            props += m("Camera $id flash (فلش دوربین $id)",ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE))
            props += m("Camera $id focal lengths (فاصله کانونی دوربین $id)",ch.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.joinToString(),"mm")
            props += m("Camera $id hardware level (سطح سخت‌افزار دوربین $id)",ch.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL))
            props += m("Camera $id sensitivity range (محدوده حساسیت ISO دوربین $id)",ch.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE))
        }
        return result("camera","CameraManager / CameraCharacteristics",props,
            "Only cameras exposed to this application are listed; not all physical lenses are independently accessible.")
    }
    private fun audio(): DiagnosticResult {
        val a = c.getSystemService(AudioManager::class.java)
        val outputs = a.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val inputs = a.getDevices(AudioManager.GET_DEVICES_INPUTS)
        return result("audio","AudioManager.getDevices",listOf(
            m("Output routes (مسیرهای خروجی صدا)",outputs.joinToString { "type=${it.type} ${it.productName}" }),
            m("Input routes (مسیرهای ورودی صدا)",inputs.joinToString { "type=${it.type} ${it.productName}" }),
            m("Music stream volume (حجم صدای رسانه)",a.getStreamVolume(AudioManager.STREAM_MUSIC)),
            m("Music stream max (حداکثر حجم صدای رسانه)",a.getStreamMaxVolume(AudioManager.STREAM_MUSIC))))
    }
    private fun sensors(): DiagnosticResult {
        val sm = c.getSystemService(SensorManager::class.java)
        val all = sm.getSensorList(Sensor.TYPE_ALL)
        val props = mutableListOf(m("Sensor count (تعداد سنسورها)",all.size))
        all.forEach { s ->
            props += m("${s.name} [${s.type}]", "vendor=${s.vendor}, version=${s.version}, resolution=${s.resolution}, range=${s.maximumRange}, " +
                "minDelay=${s.minDelay}µs, power=${s.power}mA, wakeUp=${s.isWakeUpSensor}, mode=${s.reportingMode}")
        }
        return result("sensors","SensorManager.getSensorList(TYPE_ALL)",props,
            "Listing a sensor does not establish that it returns accurate values.")
    }
    private fun location(): DiagnosticResult {
        val lm = c.getSystemService(android.location.LocationManager::class.java)
        val providers = lm.allProviders
        return result("location","LocationManager",listOf(
            m("Reported providers (ارائه‌دهندگان مکان)",providers.joinToString()),
            m("GPS provider reported (گزارش ارائه‌دهنده GPS)",providers.contains(android.location.LocationManager.GPS_PROVIDER)),
            m("GPS provider enabled (فعال بودن ارائه‌دهنده GPS)",lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER))))
    }
    private fun wifi(): DiagnosticResult {
        val manager = c.applicationContext.getSystemService(WifiManager::class.java)
        val props = mutableListOf(m("Wi-Fi feature (قابلیت وای‌فای)",pm.hasSystemFeature(PackageManager.FEATURE_WIFI)),
            m("Wi-Fi enabled (فعال بودن وای‌فای)",manager.isWifiEnabled))
        if(Build.VERSION.SDK_INT<31) {
            @Suppress("DEPRECATION")
            val wi = manager.connectionInfo
            props += m("Reported RSSI (قدرت سیگنال RSSI)",if(wi.rssi == -127) "Unavailable" else wi.rssi,"dBm")
            props += m("Reported link rate (سرعت اتصال گزارش‌شده)",if(wi.linkSpeed < 0) "Unavailable" else wi.linkSpeed,"Mbps")
            props += m("Frequency (فرکانس)",if(wi.frequency <= 0) "Unavailable" else wi.frequency,"MHz")
        } else {
            val cm = c.getSystemService(ConnectivityManager::class.java)
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            val info = caps?.transportInfo as? WifiInfo
            props += m("Wi-Fi transport connected (اتصال وای‌فای)",caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true)
            if (info != null) props += m("RSSI (قدرت سیگنال RSSI)",info.rssi,"dBm")
        }
        return result("wifi","PackageManager / WifiManager / ConnectivityManager",props,
            "Wi-Fi link rate is not measured internet throughput; privacy restrictions may hide network details.")
    }
    private fun bluetooth(): DiagnosticResult {
        val available = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        val props = mutableListOf(m("Bluetooth feature (قابلیت بلوتوث)",available),
            m("Bluetooth LE (بلوتوث کم‌مصرف LE)",pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)))
        if(Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(c, Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED) {
            props += m("Adapter state (وضعیت آداپتور)","Restricted: BLUETOOTH_CONNECT required")
        } else {
            val manager = c.getSystemService(BluetoothManager::class.java)
            props += m("Adapter present (وجود آداپتور)",manager.adapter!=null)
            props += m("Adapter enabled (فعال بودن آداپتور)",manager.adapter?.isEnabled ?: "Unavailable")
        }
        return result("bluetooth","PackageManager / BluetoothManager",props)
    }
    private fun cellular(): DiagnosticResult {
        val props = mutableListOf(m("Telephony feature (قابلیت شبکه همراه)",pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)))
        if(pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            val tel = c.getSystemService(TelephonyManager::class.java)
            props += m("SIM state code (کد وضعیت سیم‌کارت)",tel.simState)
            if (Build.VERSION.SDK_INT>=29) props += m("Multi-SIM (چند سیم‌کارته)",pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION))
        }
        return result("cellular","PackageManager / TelephonyManager",props,
            "No SIM or no service does not indicate a hardware defect.")
    }
    private fun advanced(): DiagnosticResult {
        val ir = c.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        val nfc = NfcAdapter.getDefaultAdapter(c)
        val usb = c.getSystemService(android.hardware.usb.UsbManager::class.java)
        return result("advanced","NfcAdapter / ConsumerIrManager / UsbManager",listOf(
            m("NFC hardware (سخت‌افزار NFC)",nfc!=null),m("NFC enabled (فعال بودن NFC)",nfc?.isEnabled ?: "Not detected"),
            m("Infrared transmitter (فرستنده مادون قرمز)",ir?.hasIrEmitter() ?: false),
            m("USB host (میزبان USB)",pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)),
            m("Attached USB devices (دستگاه‌های USB متصل)",usb.deviceList.size),
            m("UWB feature (قابلیت UWB)",if(Build.VERSION.SDK_INT>=31) pm.hasSystemFeature("android.hardware.uwb") else "API unavailable")))
    }
    private fun cpu(): DiagnosticResult = result("cpu","Build / Runtime",listOf(
        m("ABIs (معماری‌های پردازنده)",Build.SUPPORTED_ABIS.joinToString()),m("Logical processors available to process (تعداد هسته‌های منطقی پردازنده)",Runtime.getRuntime().availableProcessors()),
        m("Board (برد اصلی)",Build.BOARD),m("Hardware (سخت‌افزار)",Build.HARDWARE)),"CPU clock and temperature are not publicly reliable on every Android device.")
    private fun ram(): DiagnosticResult {
        val am = c.getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo(); am.getMemoryInfo(mi)
        return result("ram","ActivityManager / Runtime",listOf(
            m("Total system RAM (کل حافظه رم)",mi.totalMem/1048576,"MiB"),m("Available system RAM (حافظه رم آزاد)",mi.availMem/1048576,"MiB"),
            m("Low memory (کمبود حافظه)",mi.lowMemory),m("Memory class (کلاس حافظه برنامه)",am.memoryClass,"MiB"),
            m("Large memory class (کلاس حافظه بزرگ برنامه)",am.largeMemoryClass,"MiB"),
            m("App heap max (حداکثر حافظه هیپ برنامه)",Runtime.getRuntime().maxMemory()/1048576,"MiB")))
    }
    private fun storage(): DiagnosticResult {
        val stat = StatFs(c.filesDir.absolutePath)
        val total = stat.totalBytes; val free = stat.availableBytes
        return result("storage","StatFs (application volume)",listOf(m("Total accessible (کل فضای قابل دسترس)",total/1048576,"MiB"),
            m("Free accessible (فضای آزاد قابل دسترس)",free/1048576,"MiB"),m("Used (فضای مصرف‌شده)",(total-free)/1048576,"MiB")),
            "This describes the app's filesystem, not NAND endurance or wear.")
    }
    private fun biometric(): DiagnosticResult {
        val manager = BiometricManager.from(c)
        return result("biometric","PackageManager / AndroidX BiometricManager",listOf(
            m("Fingerprint feature (قابلیت اثر انگشت)",pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)),
            m("Strong biometric availability code (کد دسترسی زیست‌سنجی قوی)",manager.canAuthenticate(BIOMETRIC_STRONG)),
            m("Weak biometric availability code (کد دسترسی زیست‌سنجی ضعیف)",manager.canAuthenticate(BIOMETRIC_WEAK))),
            "Biometric capability is not proof of fingerprint authentication. Enrollment may be missing.")
    }
    private fun buttons(): DiagnosticResult {
        val vib = c.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        return result("buttons","Vibrator / PackageManager",listOf(
            m("Vibrator reported (گزارش ویبراتور)",vib.hasVibrator()),m("Touchscreen reported (گزارش صفحه لمسی)",pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)),
            m("Physical keyboard feature (قابلیت کیبورد فیزیکی)",pm.hasSystemFeature("android.hardware.keyboard"))))
    }
    private fun security(): DiagnosticResult {
        val km = c.getSystemService(KeyguardManager::class.java)
        return result("security","Build / KeyguardManager",listOf(
            m("Security patch (وصله امنیتی)",Build.VERSION.SECURITY_PATCH),m("Secure lock configured (قفل ایمن تنظیم‌شده)",km.isDeviceSecure)),
            "This is not a trusted boot-integrity attestation.")
    }
    private fun thermal(): DiagnosticResult {
        val p = c.getSystemService(PowerManager::class.java)
        val values = mutableListOf<Metric>()
        if(Build.VERSION.SDK_INT>=29) values += m("Android thermal status (وضعیت حرارتی اندروید)",p.currentThermalStatus)
        else values += m("Android thermal status (وضعیت حرارتی اندروید)","API unavailable")
        if(Build.VERSION.SDK_INT>=30) {
            val headroom = p.getThermalHeadroom(0)
            values += m("Thermal headroom (آستانه حرارتی)",if(headroom.isNaN()) "Not reported" else headroom)
        }
        val batt = c.registerReceiver(null,android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        if(batt!=null) values += m("Battery temperature (دمای باتری)",batt.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)/10.0,"°C")
        return result("thermal","PowerManager / battery broadcast",values,
            "No CPU or GPU temperature is asserted unless an official source provides it.")
    }
    private fun network(): DiagnosticResult {
        val cm = c.getSystemService(ConnectivityManager::class.java)
        val n = cm.activeNetwork
        val cap = cm.getNetworkCapabilities(n)
        return result("network","ConnectivityManager",listOf(
            m("Active network (شبکه فعال)",n!=null),
            m("Wi-Fi transport (اتصال وای‌فای)",cap?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)),
            m("Cellular transport (اتصال دیتا همراه)",cap?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)),
            m("Validated by Android (تاییدشده توسط اندروید)",cap?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))),
            "No remote server is contacted by the passive scan.")
    }
}
