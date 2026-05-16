package com.car.voicecontrol.adapter

import android.content.Context
import android.view.KeyEvent
import com.car.voicecontrol.byd.BydCarController
import com.car.voicecontrol.byd.CommunicationBinder

/**
 * BYD Atto 3 (O'zbekiston) uchun adapter.
 * CommunicationBinder IPC orqali to'g'ridan-to'g'ri ishlaydi.
 * ESP32/Relay kerak emas.
 */
class BydAdapter(context: Context) : CarAdapter {

    private val controller = BydCarController(context)

    var onConnected: (() -> Unit)? = null

    fun connect() {
        controller.onConnected = { onConnected?.invoke() }
        controller.connect()
    }

    fun disconnect() = controller.disconnect()

    override fun isAvailable() = controller.isAvailable
    override fun getName() = "BYD CommunicationBinder"

    // ─── Konditsioner ───────────────────────────────
    override fun setAC(on: Boolean) { controller.setAirConditioner(on) }
    override fun setTemperature(level: Int) { controller.setDriverTemperature(level) }

    // ─── Isitish ────────────────────────────────────
    override fun setHeating(on: Boolean) { controller.setDriverHeating(if (on) 2 else 0) }
    override fun setVentilation(level: Int) { controller.setDriverVentilation(level) }

    // ─── Oynalar — BYD API'sida yo'q, ESP32 kerak ──
    override fun setAllWindows(open: Boolean) { /* BYD API'sida oyna kodi yo'q */ }
    override fun setWindow(which: String, open: Boolean) { }

    // ─── Lyuk — BYD API'sida yo'q ───────────────────
    override fun setSunroof(open: Boolean) { }

    // ─── Chiroq — BYD API'sida yo'q ─────────────────
    override fun setLights(on: Boolean) { }
    override fun setHighBeam(on: Boolean) { }

    // ─── Bagaj — BYD API'sida yo'q ──────────────────
    override fun setTrunk(open: Boolean) { }

    // ─── Signal — BYD API'sida yo'q ─────────────────
    override fun beepHorn() { }

    // ─── Media (KeyEvent orqali) ─────────────────────
    override fun mediaPlay()  { controller.mediaPlay() }
    override fun mediaPause() { controller.mediaPlay() }
    override fun mediaNext()  { controller.mediaNext() }
    override fun mediaPrev()  { controller.mediaPrev() }
    override fun volumeUp()   { controller.volumeUp() }
    override fun volumeDown() { controller.volumeDown() }
    override fun volumeMute() { controller.volumeDown(); controller.volumeDown() }
}
