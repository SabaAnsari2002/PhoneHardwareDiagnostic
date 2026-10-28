package com.phonehardware.diagnostic.ui.screens

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phonehardware.diagnostic.MainActivity
import com.phonehardware.diagnostic.core.Evidence
import com.phonehardware.diagnostic.core.Metric
import com.phonehardware.diagnostic.ui.MainViewModel
import kotlinx.coroutines.delay
import java.lang.Math.PI
import kotlin.math.sin

@Composable fun SpeakerScreen(vm:MainViewModel) {
    var playbackCount by remember { mutableIntStateOf(0) }
    var attempted by remember { mutableStateOf(false) }
    Text("Set a comfortable device volume. Tap to play a short, low-amplitude tone. The app cannot determine if the speaker was actually audible to you.")
    Button(onClick={playbackCount++;attempted=true}) {Text("Play quiet test tone")}
    if(playbackCount>0) {
        LaunchedEffect(playbackCount) {
            val rate=16000
            val pcm=ShortArray(rate/2) { index ->
                val fade = minOf(index/800.0,(rate/2-index)/800.0,1.0).coerceAtLeast(0.0)
                (sin(index*2*PI*440/rate)*32767*0.12*fade).toInt().toShort()
            }
            val track=AudioTrack(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
                AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
                pcm.size*2,AudioTrack.MODE_STATIC,AudioManagerSession())
            try {
                if(track.state == AudioTrack.STATE_INITIALIZED) {
                    track.setVolume(0.15f)
                    track.write(pcm,0,pcm.size)
                    track.play(); delay(550)
                } else attempted=false
            } catch(_: Exception) { attempted=false }
            finally { runCatching{track.stop()};track.release() }
        }
    }
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Button(enabled=attempted,onClick={vm.interactiveResult("speaker_heard",true,"User heard controlled 440 Hz sample",listOf(Metric("User audibility", "Heard",evidence=Evidence.USER_CONFIRMED)))}) {Text("I heard it")}
        OutlinedButton(enabled=attempted,onClick={vm.interactiveResult("speaker_heard",false,"User did not hear tone",warning="Routing and media volume may explain absent sound; repeat before attributing failure.")}) {Text("No sound")}
    }
}
private fun AudioManagerSession(): Int = android.media.AudioManager.AUDIO_SESSION_ID_GENERATE

@Composable fun VibrationScreen(vm:MainViewModel,activity:MainActivity) {
    val vibrator = remember { activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    var activated by remember { mutableStateOf(false) }
    Text(if(vibrator.hasVibrator()) "Tap the test pattern, then confirm whether you felt the motor." else "Android does not report a vibration motor.")
    Button(enabled=vibrator.hasVibrator(),onClick={
        try {
            if(Build.VERSION.SDK_INT>=26) vibrator.vibrate(VibrationEffect.createOneShot(220,VibrationEffect.DEFAULT_AMPLITUDE))
            else vibrator.vibrate(220) // Legacy-only path for API 23–25.
            activated=true
        } catch (_: Exception) {activated=false}
    }) {Text("Vibrate briefly")}
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Button(enabled=activated,onClick={vm.interactiveResult("vibration_felt",true,"User confirmation after Android Vibrator command")}) {Text("Felt it")}
        OutlinedButton(enabled=activated,onClick={vm.interactiveResult("vibration_felt",false,"User confirmation after Android Vibrator command")}) {Text("Not felt")}
    }
}

@Composable fun TorchScreen(vm:MainViewModel,activity:MainActivity) {
    val cm=remember {activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager}
    val backId=remember { cm.cameraIdList.firstOrNull { id ->
        val ch=cm.getCameraCharacteristics(id)
        ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)==true && ch.get(CameraCharacteristics.LENS_FACING)==CameraCharacteristics.LENS_FACING_BACK
    } }
    var on by remember { mutableStateOf(false) }
    var litOnce by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    DisposableEffect(backId) { onDispose { backId?.let{runCatching{cm.setTorchMode(it,false)}} } }
    Text(if(backId==null) "No accessible rear torch was reported." else "Activate torch, visually verify illumination, then deactivate.")
    Button(enabled=backId!=null,onClick={
        try { cm.setTorchMode(backId!!,!on);on=!on;if(on)litOnce=true;message="" }
        catch(e:Exception){message="Torch unavailable: ${e.javaClass.simpleName}"}
    }) {Text(if(on) "Switch torch off" else "Switch torch on")}
    if(message.isNotBlank()) Text(message,color=MaterialTheme.colorScheme.error)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Button(enabled=litOnce,onClick={vm.interactiveResult("torch_on",true,"System torch activation and user confirmation",listOf(Metric("Visible torch illumination","Confirmed",evidence=Evidence.USER_CONFIRMED)))}) {Text("Light visible")}
        OutlinedButton(enabled=litOnce,onClick={vm.interactiveResult("torch_on",false,"System torch activation and user confirmation")}) {Text("Not visible")}
    }
}

@Composable fun VolumeButtonsScreen(vm:MainViewModel,activity:MainActivity) {
    val ups by activity.volumeUpCount.collectAsStateWithLifecycle()
    val downs by activity.volumeDownCount.collectAsStateWithLifecycle()
    var baseUp by remember { mutableIntStateOf(ups) }
    var baseDown by remember { mutableIntStateOf(downs) }
    Text("Press the physical Volume Up and Volume Down keys at least once. These are actual Activity key-down events.")
    Text("Up observed: ${ups-baseUp} · Down observed: ${downs-baseDown}")
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick={baseUp=ups;baseDown=downs}) {Text("Reset")}
        Button(onClick={vm.interactiveResult("volume_buttons",if(ups>baseUp && downs>baseDown) true else null,
            "Android KeyEvent.ACTION_DOWN for each volume key",listOf(
                Metric("Volume up events",(ups-baseUp).toString(),evidence=Evidence.MEASURED),
                Metric("Volume down events",(downs-baseDown).toString(),evidence=Evidence.MEASURED)))}) {Text("Save key events")}
    }
    Text("Power key interception is not universally available to normal Android apps.",style=MaterialTheme.typography.bodySmall)
}
