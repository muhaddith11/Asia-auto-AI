package com.car.voicecontrol.adapter

import android.content.Context
import android.view.KeyEvent

/**
 * Android Automotive OS (AAOS) uchun adapter.
 *
 * Qo'llab-quvvatlanadigan brendlar:
 *   - Geely (yangi modellari)
 *   - Li Auto
 *   - Xpeng
 *   - Nio (ba'zi modellari)
 *   - BYD (yangi AAOS modellari)
 *   - Renault, Volvo, GM va boshqalar
 *
 * Reflection ishlatiladi — oddiy Android'da crash bo'lmaydi.
 */
class AaosAdapter(private val context: Context) : CarAdapter {

    private var carInstance: Any? = null
    private var hvacManager: Any? = null
    private var propertyManager: Any? = null
    private var available = false

    companion object {
        // HVAC property ID'lari (AAOS standart)
        const val HVAC_AC_ON          = 356517120
        const val HVAC_TEMPERATURE    = 356517124
        const val HVAC_FAN_SPEED      = 356517121
        const val HVAC_SEAT_HEATING   = 356517131

        // Window property ID'lari
        const val WINDOW_MOVE         = 320865540

        // Media KeyEvent'lar
        val KEYCODE_MEDIA_PLAY_PAUSE = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        val KEYCODE_MEDIA_NEXT       = KeyEvent.KEYCODE_MEDIA_NEXT
        val KEYCODE_MEDIA_PREV       = KeyEvent.KEYCODE_MEDIA_PREVIOUS
        val KEYCODE_VOLUME_UP        = KeyEvent.KEYCODE_VOLUME_UP
        val KEYCODE_VOLUME_DOWN      = KeyEvent.KEYCODE_VOLUME_DOWN
    }

    fun connect() {
        try {
            // android.car.Car klassini reflection orqali topamiz
            val carClass = Class.forName("android.car.Car")
            val createCar = carClass.getMethod("createCar", Context::class.java)
            carInstance = createCar.invoke(null, context)

            if (carInstance != null) {
                // HVAC manager
                val getCarManager = carClass.getMethod("getCarManager", String::class.java)
                hvacManager = getCarManager.invoke(carInstance, "hvac")
                propertyManager = getCarManager.invoke(carInstance, "property")
                available = true
            }
        } catch (e: ClassNotFoundException) {
            // AAOS emas — oddiy Android
            available = false
        } catch (e: Exception) {
            available = false
        }
    }

    fun disconnect() {
        try {
            carInstance?.let {
                val disconnect = it.javaClass.getMethod("disconnect")
                disconnect.invoke(it)
            }
        } catch (_: Exception) {}
        available = false
    }

    override fun isAvailable() = available
    override fun getName() = "Android Automotive OS (AAOS)"

    // ─── Konditsioner ───────────────────────────────
    override fun setAC(on: Boolean) {
        setHvacBool(HVAC_AC_ON, on)
    }

    override fun setTemperature(level: Int) {
        // 1=18°C, 2=22°C, 3=26°C
        val temp = when (level) {
            1 -> 18.0f
            3 -> 26.0f
            else -> 22.0f
        }
        setHvacFloat(HVAC_TEMPERATURE, temp)
    }

    // ─── Isitish ────────────────────────────────────
    override fun setHeating(on: Boolean) {
        setHvacInt(HVAC_SEAT_HEATING, if (on) 2 else 0)
    }

    override fun setVentilation(level: Int) {
        setHvacInt(HVAC_FAN_SPEED, level)
    }

    // ─── Oynalar (AAOS standart) ─────────────────────
    override fun setAllWindows(open: Boolean) {
        val value = if (open) 1 else -1
        setProperty(WINDOW_MOVE, 1, value)  // FL
        setProperty(WINDOW_MOVE, 4, value)  // FR
        setProperty(WINDOW_MOVE, 64, value) // BL
        setProperty(WINDOW_MOVE, 16, value) // BR
    }

    override fun setWindow(which: String, open: Boolean) {
        val areaId = when (which) {
            "FL" -> 1; "FR" -> 4; "BL" -> 64; "BR" -> 16
            else -> return
        }
        setProperty(WINDOW_MOVE, areaId, if (open) 1 else -1)
    }

    // ─── Lyuk ───────────────────────────────────────
    override fun setSunroof(open: Boolean) {
        setProperty(WINDOW_MOVE, 256, if (open) 1 else -1)
    }

    // ─── Chiroq, Bagaj, Signal — AAOS standart API'sida cheklangan
    override fun setLights(on: Boolean)    { sendKeyEvent(if (on) 501 else 502) }
    override fun setHighBeam(on: Boolean)  { }
    override fun setTrunk(open: Boolean)   { }
    override fun beepHorn()               { sendKeyEvent(KeyEvent.KEYCODE_UNKNOWN) }

    // ─── Media ──────────────────────────────────────
    override fun mediaPlay()  { sendKeyEvent(KEYCODE_MEDIA_PLAY_PAUSE) }
    override fun mediaPause() { sendKeyEvent(KEYCODE_MEDIA_PLAY_PAUSE) }
    override fun mediaNext()  { sendKeyEvent(KEYCODE_MEDIA_NEXT) }
    override fun mediaPrev()  { sendKeyEvent(KEYCODE_MEDIA_PREV) }
    override fun volumeUp()   { sendKeyEvent(KEYCODE_VOLUME_UP) }
    override fun volumeDown() { sendKeyEvent(KEYCODE_VOLUME_DOWN) }
    override fun volumeMute() { sendKeyEvent(KeyEvent.KEYCODE_VOLUME_MUTE) }

    // ─── Yordamchi metodlar ──────────────────────────

    private fun setHvacBool(propertyId: Int, value: Boolean) {
        try {
            val mgr = hvacManager ?: return
            val method = mgr.javaClass.getMethod(
                "setBooleanProperty", Int::class.java, Int::class.java, Boolean::class.java
            )
            method.invoke(mgr, propertyId, 0, value)
        } catch (_: Exception) {}
    }

    private fun setHvacFloat(propertyId: Int, value: Float) {
        try {
            val mgr = hvacManager ?: return
            val method = mgr.javaClass.getMethod(
                "setFloatProperty", Int::class.java, Int::class.java, Float::class.java
            )
            method.invoke(mgr, propertyId, 0, value)
        } catch (_: Exception) {}
    }

    private fun setHvacInt(propertyId: Int, value: Int) {
        try {
            val mgr = hvacManager ?: return
            val method = mgr.javaClass.getMethod(
                "setIntProperty", Int::class.java, Int::class.java, Int::class.java
            )
            method.invoke(mgr, propertyId, 0, value)
        } catch (_: Exception) {}
    }

    private fun setProperty(propertyId: Int, areaId: Int, value: Int) {
        try {
            val mgr = propertyManager ?: return
            val method = mgr.javaClass.getMethod(
                "setIntProperty", Int::class.java, Int::class.java, Int::class.java
            )
            method.invoke(mgr, propertyId, areaId, value)
        } catch (_: Exception) {}
    }

    private fun sendKeyEvent(keyCode: Int) {
        try {
            val down = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val up   = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            context.applicationContext.let { ctx ->
                val instrClass = Class.forName("android.app.Instrumentation")
                val instr = instrClass.newInstance()
                instrClass.getMethod("sendKeySync", KeyEvent::class.java)
                    .invoke(instr, down)
                instrClass.getMethod("sendKeySync", KeyEvent::class.java)
                    .invoke(instr, up)
            }
        } catch (_: Exception) {}
    }
}
