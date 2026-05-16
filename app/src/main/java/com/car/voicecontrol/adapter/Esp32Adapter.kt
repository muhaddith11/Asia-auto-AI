package com.car.voicecontrol.adapter

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.car.voicecontrol.BluetoothCarControl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ESP32 + 8-kanal Relay uchun adapter.
 * Bluetooth SPP orqali ishlaydi.
 * Har qanday mashina uchun universal fallback.
 */
class Esp32Adapter(private val context: Context) : CarAdapter {

    private val bluetooth = BluetoothCarControl(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    var onConnected: ((String) -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null

    init {
        bluetooth.onConnectionChanged = { connected, name ->
            if (connected) onConnected?.invoke(name)
            else onDisconnected?.invoke()
        }
    }

    fun connectTo(device: BluetoothDevice) {
        scope.launch { bluetooth.connect(device) }
    }

    fun disconnect() = bluetooth.disconnect()

    override fun isAvailable() = bluetooth.isConnected
    override fun getName() = "ESP32 Relay (Bluetooth)"

    private fun send(code: String) {
        scope.launch { bluetooth.sendCommand(code) }
    }

    // ─── Konditsioner ───────────────────────────────
    override fun setAC(on: Boolean)         { send(if (on) "AC_ON" else "AC_OFF") }
    override fun setTemperature(level: Int) { send("AC_TEMP_$level") }

    // ─── Isitish ────────────────────────────────────
    override fun setHeating(on: Boolean)       { send(if (on) "HEAT_ON" else "HEAT_OFF") }
    override fun setVentilation(level: Int)    { send("VENT_$level") }

    // ─── Oynalar ────────────────────────────────────
    override fun setAllWindows(open: Boolean) {
        send(if (open) "WIN_ALL_OPEN" else "WIN_ALL_CLOSE")
    }
    override fun setWindow(which: String, open: Boolean) {
        send("WIN_${which}_${if (open) "OPEN" else "CLOSE"}")
    }

    // ─── Lyuk ───────────────────────────────────────
    override fun setSunroof(open: Boolean) {
        send(if (open) "SUNROOF_OPEN" else "SUNROOF_CLOSE")
    }

    // ─── Chiroqlar ──────────────────────────────────
    override fun setLights(on: Boolean)   { send(if (on) "LIGHTS_ON" else "LIGHTS_OFF") }
    override fun setHighBeam(on: Boolean) { send(if (on) "LIGHTS_HIGH" else "LIGHTS_LOW") }

    // ─── Bagaj ──────────────────────────────────────
    override fun setTrunk(open: Boolean) {
        send(if (open) "TRUNK_OPEN" else "TRUNK_CLOSE")
    }

    // ─── Signal ─────────────────────────────────────
    override fun beepHorn() { send("HORN_BEEP") }

    // ─── Media ──────────────────────────────────────
    override fun mediaPlay()  { send("MUSIC_PLAY") }
    override fun mediaPause() { send("MUSIC_PAUSE") }
    override fun mediaNext()  { send("MUSIC_NEXT") }
    override fun mediaPrev()  { send("MUSIC_PREV") }
    override fun volumeUp()   { send("VOL_UP") }
    override fun volumeDown() { send("VOL_DOWN") }
    override fun volumeMute() { send("VOL_MUTE") }
}
