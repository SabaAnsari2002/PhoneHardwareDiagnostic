package com.phonehardware.diagnostic

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.phonehardware.diagnostic.ui.App
import com.phonehardware.diagnostic.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()
    val volumeUpCount = MutableStateFlow(0)
    val volumeDownCount = MutableStateFlow(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App(viewModel, this) }
    }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if(event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            when(event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> volumeUpCount.value += 1
                KeyEvent.KEYCODE_VOLUME_DOWN -> volumeDownCount.value += 1
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
