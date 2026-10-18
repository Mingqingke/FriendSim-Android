package com.friendsim.app.core.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

/**
 * 音频焦点 + 路由：录音/播放前抢焦点，结束后释放；
 * 检测到蓝牙耳机时切 SCO，避免录成杂音或从扬声器漏音。
 */
object AudioFocusHelper {

    private var focusRequest: AudioFocusRequest? = null
    private var scoBusy = false

    fun acquire(context: Context, forRecording: Boolean): Boolean {
        val manager = context.getSystemService(AudioManager::class.java) ?: return false
        val usage = if (forRecording) {
            AudioAttributes.USAGE_VOICE_COMMUNICATION
        } else {
            AudioAttributes.USAGE_MEDIA
        }
        val attributes = AudioAttributes.Builder()
            .setUsage(usage)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(attributes)
                    .setOnAudioFocusChangeListener { }
                    .build()
                focusRequest = request
                manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                manager.requestAudioFocus(
                    { },
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE,
                ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        }.getOrDefault(false)
    }

    fun abandon(context: Context) {
        val manager = context.getSystemService(AudioManager::class.java) ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { manager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                manager.abandonAudioFocus(null)
            }
            focusRequest = null
        }
    }

    /** 有蓝牙耳机连着 → 打开 SCO，录音走耳机麦克风。 */
    fun routeToBluetooth(context: Context) {
        val manager = context.getSystemService(AudioManager::class.java) ?: return
        runCatching {
            val hasHeadset = manager.getDevices(AudioManager.GET_DEVICES_INPUTS).any {
                it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    it.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET
            }
            if (hasHeadset && manager.isBluetoothScoAvailableOffCall) {
                manager.startBluetoothSco()
                @Suppress("DEPRECATION")
                manager.isBluetoothScoOn = true
                scoBusy = true
            }
        }
    }

    fun restoreRoute(context: Context) {
        if (!scoBusy) return
        val manager = context.getSystemService(AudioManager::class.java) ?: return
        runCatching {
            @Suppress("DEPRECATION")
            manager.isBluetoothScoOn = false
            @Suppress("DEPRECATION")
            manager.stopBluetoothSco()
        }
        scoBusy = false
    }
}
