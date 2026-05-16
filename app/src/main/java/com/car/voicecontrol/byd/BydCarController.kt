package com.car.voicecontrol.byd

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.view.KeyEvent

/**
 * BYD mashina boshqaruv kontrolleri.
 *
 * Agar BYD mashinasi bo'lsa → CommunicationBinder orqali to'g'ridan-to'g'ri
 * Agar BYD yo'q bo'lsa → isAvailable = false, ESP32 ishlatiladi
 */
class BydCarController(private val context: Context) {

    private var binder: IBinder? = null
    var isAvailable = false
        private set

    var onConnected: (() -> Unit)? = null

    // Device turlari
    object Device {
        const val WINDOW  = 1001
        const val CLIMATE = 1023
        const val SAFETY  = 1038
    }

    // Funksiya kodlari (BYD Atto 3 UZ)
    object Event {
        const val SMOKING_MODE          = 1125122104
        const val AIR_CONDITIONER       = 1125122100
        const val DRIVER_HEATING        = 1125122068
        const val DRIVER_VENTILATION    = 1125122076
        const val DRIVER_TEMPERATURE    = 1125122064
        const val PASSENGER_TEMPERATURE = 1125122072
        const val ADAS_DISABLE          =  944767020
        const val AEB_DISABLE           = 1324560400
        const val WHEEL_HEATING         =  944767029
    }

    // BYD binder broadcast'ini eshituvchi
    private val binderReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val b = intent?.getBundleExtra("extras")?.getBinder(CommunicationBinder.BINDER_KEY)
                ?: intent?.extras?.getBinder(CommunicationBinder.BINDER_KEY)
            if (b != null) {
                binder = b
                isAvailable = true
                onConnected?.invoke()
            }
        }
    }

    fun connect() {
        // BYD broadcast'ini tinglash
        val filter = IntentFilter("ACTION_voice_assistant_process_started")
        context.registerReceiver(binderReceiver, filter)

        // CommunicationProcess ni ishga tushirishga urinish
        tryStartCommunicationProcess()

        // Yoki ServiceManager orqali to'g'ridan-to'g'ri ulanishga urinish
        tryDirectServiceConnection()
    }

    private fun tryStartCommunicationProcess() {
        try {
            // BYD app o'rnatilganmi?
            val bydPackages = listOf(
                "com.byd.carcontrol",
                "com.byd.assistant",
                "com.evtech.carcontrol"
            )
            for (pkg in bydPackages) {
                try {
                    context.packageManager.getPackageInfo(pkg, 0)
                    // BYD app topildi — uning servisiga ulanish
                    val intent = Intent().apply {
                        setPackage(pkg)
                        action = "ACTION_voice_assistant_process_started"
                    }
                    context.sendBroadcast(intent)
                    break
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun tryDirectServiceConnection() {
        try {
            // Android ServiceManager orqali BYD tizim servisiga ulanish
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getService = serviceManagerClass.getMethod("getService", String::class.java)

            val serviceNames = listOf(
                "byd_vehicle_service",
                "vehicle_service",
                "car_control_service",
                "byd_car_service"
            )

            for (name in serviceNames) {
                val service = getService.invoke(null, name) as? IBinder
                if (service != null) {
                    binder = service
                    isAvailable = true
                    onConnected?.invoke()
                    break
                }
            }
        } catch (_: Exception) {}
    }

    // ─── Mashina buyruqlari ─────────────────────────────

    fun setAirConditioner(on: Boolean): Int {
        return send(Device.CLIMATE, Event.AIR_CONDITIONER, if (on) 1 else 0)
    }

    fun setDriverHeating(level: Int): Int {
        return send(Device.CLIMATE, Event.DRIVER_HEATING, level.coerceIn(0, 3))
    }

    fun setDriverVentilation(level: Int): Int {
        return send(Device.CLIMATE, Event.DRIVER_VENTILATION, level.coerceIn(0, 3))
    }

    fun setDriverTemperature(level: Int): Int {
        return send(Device.CLIMATE, Event.DRIVER_TEMPERATURE, level.coerceIn(0, 3))
    }

    fun setPassengerTemperature(level: Int): Int {
        return send(Device.CLIMATE, Event.PASSENGER_TEMPERATURE, level.coerceIn(0, 3))
    }

    fun setSmokingMode(on: Boolean): Int {
        return send(Device.WINDOW, Event.SMOKING_MODE, if (on) 5 else 0)
    }

    fun setWheelHeating(on: Boolean): Int {
        return send(Device.CLIMATE, Event.WHEEL_HEATING, if (on) 1 else 0)
    }

    fun disableAdas(): Int {
        return send(Device.SAFETY, Event.ADAS_DISABLE, 0)
    }

    fun disableAeb(): Int {
        return send(Device.SAFETY, Event.AEB_DISABLE, 0)
    }

    // Media tugmalar
    fun mediaPlay() = dispatchKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    fun mediaNext() = dispatchKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    fun mediaPrev() = dispatchKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    fun volumeUp()  = dispatchKey(KeyEvent.KEYCODE_VOLUME_UP)
    fun volumeDown() = dispatchKey(KeyEvent.KEYCODE_VOLUME_DOWN)

    private fun send(deviceType: Int, eventType: Int, value: Int): Int {
        val b = binder ?: return -1
        return CommunicationBinder.setInt(b, deviceType, eventType, value)
    }

    private fun dispatchKey(keyCode: Int) {
        val b = binder ?: return
        val down = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val up   = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        CommunicationBinder.dispatchKeyEvent(b, down)
        CommunicationBinder.dispatchKeyEvent(b, up)
    }

    fun sendFromContentObject(obj: ContentObject): Int {
        val event = obj.event ?: return -1
        val state = obj.state ?: return -1
        return send(obj.system, event, state)
    }

    fun disconnect() {
        try {
            context.unregisterReceiver(binderReceiver)
        } catch (_: Exception) {}
        binder = null
        isAvailable = false
    }
}
