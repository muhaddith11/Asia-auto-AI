package com.car.voicecontrol.adapter

/**
 * Barcha mashina adapterlari shu interfeysni amalga oshiradi.
 * CarVoiceService faqat shu interfeys bilan ishlaydi —
 * qaysi mashina ekanini bilishi shart emas.
 */
interface CarAdapter {

    /** Adapter ishga tayyormi? */
    fun isAvailable(): Boolean

    /** Adapter nomi (log uchun) */
    fun getName(): String

    // ─── Konditsioner ───────────────────────────────
    fun setAC(on: Boolean)
    fun setTemperature(level: Int)  // 1=sovuq, 2=o'rta, 3=issiq

    // ─── Isitish / Ventilyatsiya ─────────────────────
    fun setHeating(on: Boolean)
    fun setVentilation(level: Int)  // 0-3

    // ─── Oynalar ────────────────────────────────────
    fun setAllWindows(open: Boolean)
    fun setWindow(which: String, open: Boolean)  // "FL","FR","BL","BR"

    // ─── Lyuk ───────────────────────────────────────
    fun setSunroof(open: Boolean)

    // ─── Chiroqlar ──────────────────────────────────
    fun setLights(on: Boolean)
    fun setHighBeam(on: Boolean)

    // ─── Bagaj ──────────────────────────────────────
    fun setTrunk(open: Boolean)

    // ─── Signal ─────────────────────────────────────
    fun beepHorn()

    // ─── Media ──────────────────────────────────────
    fun mediaPlay()
    fun mediaPause()
    fun mediaNext()
    fun mediaPrev()
    fun volumeUp()
    fun volumeDown()
    fun volumeMute()
}
