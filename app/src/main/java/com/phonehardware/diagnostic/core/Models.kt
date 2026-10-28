package com.phonehardware.diagnostic.core

/** Presence, access, operation, performance and condition are distinct diagnostic concepts. */
enum class Outcome {
    PASSED, FAILED, NOT_SUPPORTED, NOT_DETECTED, UNAVAILABLE, PERMISSION_REQUIRED,
    NOT_TESTED, IN_PROGRESS, USER_CANCELLED, INCONCLUSIVE, RESTRICTED, ERROR
}
enum class Evidence { MEASURED, SYSTEM_REPORTED, ESTIMATED, USER_CONFIRMED }
enum class TestKind { PASSIVE, AUTOMATED, ASSISTED, INTERACTIVE }

data class Metric(
    val name: String,
    val value: String,
    val unit: String = "",
    val evidence: Evidence = Evidence.SYSTEM_REPORTED
)

data class DiagnosticResult(
    val id: String,
    val name: String,
    val category: String,
    val outcome: Outcome,
    val metrics: List<Metric> = emptyList(),
    val source: String,
    val method: String,
    val explanation: String,
    val nextAction: String = "",
    val warning: String = "",
    val errorCode: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val androidApi: Int = android.os.Build.VERSION.SDK_INT
)

data class TestSpec(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val kind: TestKind,
    val permission: String? = null,
    val minApi: Int = 23,
    val timeoutMillis: Long = 25_000
)

/** Production catalog contains only tests with actual implementations. */
object TestCatalog {
    val passive: List<TestSpec> = listOf(
        TestSpec("device", "Device inventory (شناسنامه دستگاه)", "Device (دستگاه)", "Build, features and interfaces as reported by Android (اطلاعات ساخت، ویژگی‌ها و رابط‌ها)", TestKind.PASSIVE),
        TestSpec("battery", "Battery snapshot (وضعیت باتری)", "Battery (باتری)", "Power, voltage, temperature and reported current (توان، ولتاژ، دما و جریان گزارش‌شده)", TestKind.PASSIVE),
        TestSpec("display", "Display capabilities (قابلیت‌های صفحه نمایش)", "Display & touch (صفحه نمایش و لمس)", "Modes, density, refresh rate (حالت‌ها، تراکم و نرخ نوسازی)", TestKind.PASSIVE),
        TestSpec("camera", "Camera inventory (شناسنامه دوربین)", "Camera (دوربین)", "Visible cameras and camera characteristics (دوربین‌های در دسترس و مشخصات فنی)", TestKind.PASSIVE),
        TestSpec("audio", "Audio devices (دستگاه‌های صوتی)", "Audio (صدا)", "Input and output route inventory (مسیرهای ورودی و خروجی صدا)", TestKind.PASSIVE),
        TestSpec("sensors", "All sensors (همه سنسورها)", "Sensors (سنسورها)", "Every SensorManager-reported sensor, without filtering (همه سنسورهای گزارش‌شده بدون فیلتر)", TestKind.PASSIVE),
        TestSpec("location", "GNSS availability (دسترس‌پذیری مکان‌نما)", "Location (مکان‌نما)", "Location providers and enabled status (ارائه‌دهندگان مکان و وضعیت فعال بودن)", TestKind.PASSIVE),
        TestSpec("wifi", "Wi-Fi capabilities (قابلیت‌های وای‌فای)", "Wi-Fi (وای‌فای)", "Feature presence and connection where accessible (وجود قابلیت و وضعیت اتصال)", TestKind.PASSIVE),
        TestSpec("bluetooth", "Bluetooth capabilities (قابلیت‌های بلوتوث)", "Bluetooth (بلوتوث)", "Adapter and reported feature (آداپتور و قابلیت‌های گزارش‌شده)", TestKind.PASSIVE),
        TestSpec("cellular", "Cellular and SIM (شبکه همراه و سیم‌کارت)", "Cellular (شبکه همراه)", "Radio feature and SIM state without private identifiers (قابلیت رادیویی و وضعیت سیم‌کارت)", TestKind.PASSIVE),
        TestSpec("advanced", "NFC, infrared & USB (NFC، مادون قرمز و USB)", "Advanced (پیشرفته)", "NFC, infrared, and USB host features (قابلیت‌های NFC، مادون قرمز و میزبان USB)", TestKind.PASSIVE),
        TestSpec("cpu", "CPU inventory (شناسنامه پردازنده)", "CPU (پردازنده)", "Runtime cores and architectures; no fabricated frequencies (هسته‌ها و معماری پردازنده)", TestKind.PASSIVE),
        TestSpec("ram", "RAM inventory (شناسنامه حافظه رم)", "RAM (حافظه رم)", "Available and system-reported memory (حافظه در دسترس و گزارش‌شده)", TestKind.PASSIVE),
        TestSpec("storage", "Storage inventory (شناسنامه حافظه داخلی)", "Storage (حافظه ذخیره‌سازی)", "Accessible app-volume filesystem space (فضای در دسترس سیستم فایل)", TestKind.PASSIVE),
        TestSpec("biometric", "Biometric capability (قابلیت‌های زیست‌سنجی)", "Biometrics (زیست‌سنجی)", "Feature flags and authenticators separately (پرچم‌های قابلیت و روش‌های احراز هویت)", TestKind.PASSIVE),
        TestSpec("buttons", "Physical interfaces (رابط‌های فیزیکی)", "Physical (فیزیکی)", "Vibration, flash and keyboard capabilities (قابلیت‌های ویبره، فلش و کیبورد)", TestKind.PASSIVE),
        TestSpec("security", "Security status (وضعیت امنیت)", "Security (امنیت)", "Patch level and secure lock (سطح وصله امنیتی و قفل ایمن)", TestKind.PASSIVE),
        TestSpec("thermal", "Thermal status (وضعیت حرارتی)", "Thermal (حرارت)", "Supported Android thermal and battery signals (سیگنال‌های حرارتی اندروید و باتری)", TestKind.PASSIVE),
        TestSpec("network", "Network status (وضعیت شبکه)", "Network (شبکه)", "Active transport and system validation (حالت فعال شبکه و تاییدیه سیستم)", TestKind.PASSIVE)
    )
    val active: List<TestSpec> = listOf(
        TestSpec("storage_integrity", "Storage integrity (سلامت حافظه ذخیره‌سازی)", "Storage (حافظه ذخیره‌سازی)", "Temporary-file SHA-256 write/read verification (تست خواندن/نوشتن و اعتبارسنجی SHA-256)", TestKind.AUTOMATED),
        TestSpec("ram_integrity", "Memory integrity sample (نمونه سلامت حافظه رم)", "RAM (حافظه رم)", "Bounded app-buffer read/write verification (تست و اعتبارسنجی بافر حافظه برنامه)", TestKind.AUTOMATED),
        TestSpec("cpu_work", "Bounded CPU benchmark (تست عملکرد پردازنده)", "CPU (پردازنده)", "Application-specific throughput sampled for 1.5 seconds (سنجش توان پردازشی در ۱.۵ ثانیه)", TestKind.AUTOMATED),
        TestSpec("accelerometer_events", "Accelerometer event reception (دریافت رویدادهای شتاب‌سنج)", "Sensors (سنسورها)", "Wait for actual sensor events (انتظار برای رویدادهای واقعی سنسور)", TestKind.AUTOMATED),
        TestSpec("light_events", "Ambient light events (رویدادهای سنسور نور)", "Sensors (سنسورها)", "Observe sensor values; change lighting for deeper assessment (مشاهده مقادیر سنسور نور محیط)", TestKind.AUTOMATED),
        TestSpec("proximity_events", "Proximity events (رویدادهای سنسور مجاورت)", "Sensors (سنسورها)", "Observe near/far event stream (مشاهده رویدادهای نزدیک/دور سنسور مجاورت)", TestKind.AUTOMATED),
        TestSpec("sensor_explorer", "Live sensor explorer (کاوشگر زنده سنسورها)", "Sensors (سنسورها)", "Select any accessible sensor and watch actual events (انتخاب و مشاهده زنده سنسورها)", TestKind.INTERACTIVE),
        TestSpec("battery_trend", "Live battery observation (پایش زنده باتری)", "Battery (باتری)", "Observe level, temperature, current and voltage over 15 seconds (پایش سطح شارژ، دما، جریان و ولتاژ در ۱۵ ثانیه)", TestKind.INTERACTIVE),
        TestSpec("charge_change", "Charging cable transitions (تغییرات اتصال شارژر)", "Battery (باتری)", "Watch actual plugged/unplugged system status (مشاهده وضعیت اتصال یا قطع کابل شارژ)", TestKind.INTERACTIVE),
        TestSpec("mic_record", "Microphone capture (ضبط صدای میکروفون)", "Audio (صدا)", "Bounded 1-second sample; RMS and dBFS (نمونه‌برداری ۱ ثانیه‌ای و سنجش سطح صدا)", TestKind.AUTOMATED, android.Manifest.permission.RECORD_AUDIO),
        TestSpec("gps_fix", "GNSS location acquisition (دریافت موقعیت GPS)", "Location (مکان‌نما)", "Ask platform for a fresh GPS fix (دریافت موقعیت جدید از ماهواره GPS)", TestKind.AUTOMATED, android.Manifest.permission.ACCESS_FINE_LOCATION, timeoutMillis = 30_000),
        TestSpec("touch_grid", "Touch coverage (پوشش لمس صفحه)", "Display & touch (صفحه نمایش و لمس)", "Trace each grid cell (تست لمس خانه‌های جدول صفحه)", TestKind.INTERACTIVE),
        TestSpec("pixel_check", "Screen color inspection (بررسی پیکسل‌های صفحه)", "Display & touch (صفحه نمایش و لمس)", "Full-screen colors with explicit user confirmation (نمایش رنگ‌های تمام‌صفحه جهت بررسی پیکسل سوخته)", TestKind.ASSISTED),
        TestSpec("camera_capture", "Camera preview & capture (پیش‌نمایش و عکاسی دوربین)", "Camera (دوربین)", "Bind CameraX and actually capture a frame (اتصال به دوربین و ثبت یک فریم)", TestKind.INTERACTIVE, android.Manifest.permission.CAMERA),
        TestSpec("speaker_heard", "Speaker check (تست بلندگو)", "Audio (صدا)", "Controlled quiet audio signal and explicit confirmation (پخش صدای کنترل‌شده و تایید شنیده شدن)", TestKind.ASSISTED),
        TestSpec("vibration_felt", "Vibration check (تست ویبره)", "Physical (فیزیکی)", "Vibrate briefly and confirm sensation (ارسال ویبره کوتاه و تایید احساس آن)", TestKind.ASSISTED),
        TestSpec("torch_on", "Torch activation (تست چراغ‌قوه)", "Physical (فیزیکی)", "Activate reported rear flashlight and confirm visibility (روشن کردن چراغ‌قوه و تایید عملکرد)", TestKind.ASSISTED),
        TestSpec("volume_buttons", "Volume button events (تست دکمه‌های ولوم)", "Physical (فیزیکی)", "Physically press each volume button (فشردن دکمه‌های کم و زیاد کردن صدا)", TestKind.INTERACTIVE),
        TestSpec("biometric_auth", "Biometric authentication (احراز هویت زیست‌سنجی)", "Biometrics (زیست‌سنجی)", "Real BiometricPrompt (تست واقعی احراز هویت با اثرانگشت/چهره)", TestKind.INTERACTIVE),
        TestSpec("wifi_link", "Wi-Fi link observation (پایش لینک وای‌فای)", "Wi-Fi (وای‌فای)", "Read local Wi-Fi link metrics without internet traffic (خوانش پارامترهای شبکه وای‌فای محلی)", TestKind.INTERACTIVE),
        TestSpec("ble_scan", "BLE advertisement scan (اسکن دستگاه‌های بلوتوث کم‌مصرف)", "Bluetooth (بلوتوث)", "User-initiated 8-second BLE scan, no pairing (اسکن ۸ ثانیه‌ای دستگاه‌های بلوتوث اطراف)", TestKind.INTERACTIVE),
        TestSpec("nfc_tag", "NFC tag reader (خواننده تگ NFC)", "Advanced (پیشرفته)", "Read only technologies of a user-supplied NFC tag (خواندن اطلاعات تگ NFC نزدیک‌شده)", TestKind.INTERACTIVE),
        TestSpec("usb_presence", "USB attachment observation (پایش اتصال USB)", "Advanced (پیشرفته)", "Observe attached USB host devices using system APIs (مشاهده دستگاه‌های USB متصل‌شده)", TestKind.INTERACTIVE)
    )
    val all: List<TestSpec> = passive + active
    fun byId(id: String): TestSpec = all.first { it.id == id }
}
