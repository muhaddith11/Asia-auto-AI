package com.car.voicecontrol.adapter

import android.content.Context

/**
 * Avtomatik mashina adapteri tanlash.
 *
 * Tekshirish tartibi:
 *   1. BYD (CommunicationBinder) — BYD Atto 3 UZ
 *   2. AAOS (Android Automotive OS) — Geely, Xpeng, Li Auto, Nio...
 *   3. ESP32 Relay (Bluetooth) — universal, har qanday mashina
 *
 * Bir nechta adapter bir vaqtda ishlashi mumkin:
 *   - BYD: AC, isitish, media → BYD API
 *   - ESP32: Oyna, lyuk, chiroq → Relay
 */
class CarAdapterManager(private val context: Context) {

    val bydAdapter  = BydAdapter(context)
    val aaosAdapter = AaosAdapter(context)
    val esp32Adapter = Esp32Adapter(context)

    var onAdapterDetected: ((String) -> Unit)? = null

    fun initialize() {
        // 1. BYD ulanishga urinish
        bydAdapter.onConnected = {
            onAdapterDetected?.invoke("BYD: ${bydAdapter.getName()}")
        }
        bydAdapter.connect()

        // 2. AAOS ulanishga urinish
        aaosAdapter.connect()
        if (aaosAdapter.isAvailable()) {
            onAdapterDetected?.invoke("AAOS: ${aaosAdapter.getName()}")
        }
    }

    /**
     * Berilgan buyruq uchun eng yaxshi adapterni qaytaradi.
     * Buyruq turiga qarab tanlanadi:
     *   - AC/isitish → BYD yoki AAOS (agar mavjud bo'lsa)
     *   - Oyna/Lyuk/Chiroq → AAOS yoki ESP32
     *   - Media → BYD yoki AAOS yoki ESP32
     */
    fun getAdapterFor(commandType: CommandType): CarAdapter? {
        return when (commandType) {
            CommandType.AC, CommandType.HEATING, CommandType.VENTILATION -> {
                when {
                    bydAdapter.isAvailable()  -> bydAdapter
                    aaosAdapter.isAvailable() -> aaosAdapter
                    esp32Adapter.isAvailable() -> esp32Adapter
                    else -> null
                }
            }
            CommandType.WINDOW, CommandType.SUNROOF, CommandType.TRUNK,
            CommandType.LIGHTS, CommandType.HORN -> {
                when {
                    aaosAdapter.isAvailable()  -> aaosAdapter
                    esp32Adapter.isAvailable() -> esp32Adapter
                    else -> null
                }
            }
            CommandType.MEDIA, CommandType.VOLUME -> {
                when {
                    bydAdapter.isAvailable()   -> bydAdapter
                    aaosAdapter.isAvailable()  -> aaosAdapter
                    esp32Adapter.isAvailable() -> esp32Adapter
                    else -> null
                }
            }
        }
    }

    /** Mavjud adapterlar ro'yxati */
    fun getAvailableAdapters(): List<String> {
        val list = mutableListOf<String>()
        if (bydAdapter.isAvailable())   list.add("✅ BYD")
        if (aaosAdapter.isAvailable())  list.add("✅ AAOS")
        if (esp32Adapter.isAvailable()) list.add("✅ ESP32")
        if (list.isEmpty()) list.add("❌ Adapter topilmadi")
        return list
    }

    fun destroy() {
        bydAdapter.disconnect()
        aaosAdapter.disconnect()
        esp32Adapter.disconnect()
    }

    enum class CommandType {
        AC, HEATING, VENTILATION,
        WINDOW, SUNROOF, TRUNK,
        LIGHTS, HORN,
        MEDIA, VOLUME
    }
}
