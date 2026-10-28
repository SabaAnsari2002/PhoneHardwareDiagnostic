package com.phonehardware.diagnostic.ui.screens

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.phonehardware.diagnostic.MainActivity
import com.phonehardware.diagnostic.core.*
import com.phonehardware.diagnostic.ui.MainViewModel
import java.io.File

@Composable fun CameraCaptureScreen(vm:MainViewModel, activity:MainActivity) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(activity) }
    var facing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    var status by remember { mutableStateOf("Opening camera…") }
    DisposableEffect(facing,lifecycleOwner) {
        var bound:ProcessCameraProvider?=null
        var disposed=false
        val future=ProcessCameraProvider.getInstance(activity)
        future.addListener({
            if(!disposed) try {
                val provider=future.get()
                val selector=CameraSelector.Builder().requireLensFacing(facing).build()
                val preview=Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val shot=ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner,selector,preview,shot)
                bound=provider;capture=shot;status="Camera preview connected. Capture an actual image to test the pipeline."
            } catch(e:Exception) {capture=null;status="Camera unavailable: ${e.javaClass.simpleName}"}
        },ContextCompat.getMainExecutor(activity))
        onDispose { disposed=true;capture=null;bound?.unbindAll() }
    }
    Text("Live CameraX preview. A successful capture verifies only the exercised image pipeline, not sharpness or lens quality.")
    AndroidView(factory={previewView},modifier=Modifier.fillMaxWidth().height(330.dp))
    Text(status)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick={facing=if(facing==CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK}) {Text("Switch lens")}
        Button(enabled=capture!=null,onClick={
            val shot=capture ?: return@Button
            val file=File.createTempFile("diagnostic-capture", ".jpg",activity.cacheDir)
            shot.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(),ContextCompat.getMainExecutor(activity),
                object:ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output:ImageCapture.OutputFileResults) {
                        val size=file.length(); file.delete()
                        if(size>0L) {
                            status="Captured ${size} bytes. Temporary image deleted."
                            vm.store(DiagnosticResult("camera_capture","Camera preview & capture","Camera",Outcome.PASSED,
                                listOf(Metric("Captured file bytes",size.toString(),"bytes",Evidence.MEASURED),
                                    Metric("Requested facing",if(facing==CameraSelector.LENS_FACING_BACK) "Rear" else "Front",evidence=Evidence.SYSTEM_REPORTED)),
                                "CameraX Preview / ImageCapture","Bound lifecycle preview and saved JPEG into app cache",
                                "Camera pipeline captured a nonempty file.",warning="Visual quality, autofocus, video and optical performance remain untested."))
                        } else status="Captured file was empty; result inconclusive."
                    }
                    override fun onError(exception: ImageCaptureException) {
                        file.delete()
                        status="Capture failed: ${exception.imageCaptureError}"
                        vm.store(DiagnosticResult("camera_capture","Camera preview & capture","Camera",Outcome.ERROR,
                            source="CameraX",method="ImageCapture.takePicture",explanation="Camera capture threw an error.",errorCode="CAMERA_${exception.imageCaptureError}"))
                    }
                })
        }) {Text("Capture test photo")}
    }
}

@Composable fun BiometricScreen(vm:MainViewModel, activity:MainActivity) {
    var status by remember { mutableStateOf("Check enrolled biometrics, then authenticate using the genuine system prompt.") }
    val manager=remember { BiometricManager.from(activity) }
    val auth=BiometricManager.Authenticators.BIOMETRIC_STRONG
    val availability=manager.canAuthenticate(auth)
    Text("Strong biometric availability code: $availability")
    Text("This test proves a successful strong biometric authorization, not necessarily fingerprint. Face, iris or fingerprint may have been used.")
    Button(enabled=availability==BiometricManager.BIOMETRIC_SUCCESS,onClick={
        val prompt=BiometricPrompt(activity,ContextCompat.getMainExecutor(activity),object:BiometricPrompt.AuthenticationCallback(){
            override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult) {
                status="Android confirmed strong biometric authentication. Modality not determined."
                vm.store(DiagnosticResult("biometric_auth","Biometric authentication","Biometrics",Outcome.PASSED,
                    listOf(Metric("Biometric authentication","System accepted",evidence=Evidence.SYSTEM_REPORTED)),
                    "AndroidX BiometricPrompt","Strong biometrics, no credential fallback",
                    "System verified a biometric authenticator. Fingerprint-only verification is NOT claimed.",
                    warning="If multiple modalities are enrolled, the precise one may be unknown."))
            }
            override fun onAuthenticationFailed() { status="Attempt not recognized; prompt remains available." }
            override fun onAuthenticationError(errorCode:Int,errString:CharSequence) {
                status="System returned: $errString ($errorCode)"
                val outcome=when(errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON, BiometricPrompt.ERROR_CANCELED -> Outcome.USER_CANCELLED
                    BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> Outcome.UNAVAILABLE
                    BiometricPrompt.ERROR_HW_UNAVAILABLE -> Outcome.UNAVAILABLE
                    else -> Outcome.INCONCLUSIVE
                }
                vm.store(DiagnosticResult("biometric_auth","Biometric authentication","Biometrics",outcome,
                    source="AndroidX BiometricPrompt",method="Strong biometric authentication",
                    explanation="System authentication ended without a verified success: $errString",
                    nextAction="Check enrollment or retry after lockout.",errorCode="BIOMETRIC_$errorCode"))
            }
        })
        val info=BiometricPrompt.PromptInfo.Builder().setTitle("Hardware biometric check")
            .setSubtitle("Use an enrolled biometric; device PIN does not count")
            .setAllowedAuthenticators(auth).setNegativeButtonText("Cancel").build()
        prompt.authenticate(info)
    }) {Text("Authenticate with system prompt")}
    Text(status)
    if(availability!=BiometricManager.BIOMETRIC_SUCCESS) Text("If missing enrollment or unavailable, this does not establish broken biometric hardware.",style=MaterialTheme.typography.bodySmall)
}
