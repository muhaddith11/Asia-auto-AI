package com.car.voicecontrol

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.car.voicecontrol.adapter.CarAdapterManager

class CarVoiceService : Service() {

    private var voiceEngine: OfflineVoiceEngine? = null
    private var ttsEngine: TtsEngine? = null
    private var adapterManager: CarAdapterManager? = null
    private var waitingForCommand = false
    private var currentLang = "uz"
    private var commandTimeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())

    companion object {
        const val CHANNEL_ID = "car_voice_channel"
        const val NOTIF_ID = 1

        val WAKE_WORDS = listOf(
            "машина", "машину", "машине",
            "mashina", "mashinа",
            "авто", "auto",
            "привет машина", "hey car"
        )

        val LANG_SWITCH = mapOf(
            "uz" to listOf(
                "ozbekcha", "o'zbekcha", "uzbekcha", "uzbek", "uzbek tili",
                "o'zbek tili", "o'zbek", "uzb"
            ),
            "ru" to listOf(
                "ruscha", "po russki", "po-russki", "по русски", "по-русски",
                "russkiy", "русский", "на русском", "russ"
            ),
            "en" to listOf(
                "english", "inglizcha", "ingliz", "switch to english",
                "in english", "angliycha"
            )
        )

        var onWakeWord: (() -> Unit)? = null
        var onCommand: ((String) -> Unit)? = null
        var onStatus: ((String) -> Unit)? = null
        var onLangChanged: ((String) -> Unit)? = null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Ishga tushmoqda..."))
        ttsEngine = TtsEngine(this)
        initAdapters()
        startEngine()
    }

    private fun initAdapters() {
        adapterManager = CarAdapterManager(this)
        adapterManager?.onAdapterDetected = { name ->
            updateNotification("✅ $name ulandi")
            onStatus?.invoke("adapter_connected")
        }
        adapterManager?.initialize()
    }

    private fun startEngine() {
        currentLang = getSharedPreferences("car_prefs", Context.MODE_PRIVATE)
            .getString("model_lang", "uz") ?: "uz"

        if (!ModelManager.isModelReady(this, currentLang)) {
            updateNotification("Model yo'q — Setup ni oching")
            onStatus?.invoke("model_missing")
            return
        }

        voiceEngine?.destroy()
        voiceEngine = OfflineVoiceEngine(this)

        ttsEngine?.setLanguage(currentLang)

        voiceEngine?.onReady = {
            val langName = when (currentLang) {
                "uz" -> "🇺🇿 O'zbek"
                "ru" -> "🇷🇺 Русский"
                "en" -> "🇬🇧 English"
                else -> currentLang
            }
            updateNotification("$langName — Doim eshitib turaman")
            onStatus?.invoke("ready")
            voiceEngine?.startListening()
        }

        voiceEngine?.onResult = { text ->
            processText(text)
        }

        voiceEngine?.onError = { error ->
            updateNotification("Xato: $error")
        }

        val modelPath = ModelManager.getModelPath(this, currentLang)
        Thread { voiceEngine?.loadModel(modelPath, currentLang) }.start()
    }

    private fun processText(text: String) {
        val lower = text.lowercase().trim()
        val hasWakeWord = WAKE_WORDS.any { lower.contains(it) }

        if (hasWakeWord) {
            onWakeWord?.invoke()
            ttsEngine?.speakWakeWord(currentLang)
            val command = extractCommand(lower)

            if (command.isNotBlank()) {
                handleCommand(command)
            } else {
                waitingForCommand = true
                updateNotification("Buyruqni ayting...")
                onStatus?.invoke("waiting")

                commandTimeoutHandler.removeCallbacksAndMessages(null)
                commandTimeoutHandler.postDelayed({
                    waitingForCommand = false
                    updateNotification("Doim eshitib turaman...")
                    onStatus?.invoke("ready")
                }, 6000)
            }
        } else if (waitingForCommand) {
            waitingForCommand = false
            commandTimeoutHandler.removeCallbacksAndMessages(null)
            handleCommand(lower)
        }
    }

    private fun handleCommand(text: String) {
        // 1. Til almashtirish?
        val switchedLang = detectLangSwitch(text)
        if (switchedLang != null) {
            switchLanguage(switchedLang)
            return
        }

        // 2. Ilova ochish buyrug'i?
        val appCmd = AppLauncher.detect(text)
        if (appCmd != null) {
            val launched = AppLauncher.launch(this, appCmd)
            val response = AppLauncher.getResponseText(appCmd, currentLang)
            ttsEngine?.speak(response)
            onCommand?.invoke(text)
            updateNotification(response)
            resetStatus()
            return
        }

        // 3. Sistema buyrug'i (WiFi, Mobile, Home)?
        val sysCmd = SystemController.detect(text)
        if (sysCmd != null) {
            SystemController.execute(this, sysCmd)
            val response = SystemController.getResponseText(sysCmd, currentLang)
            ttsEngine?.speak(response)
            onCommand?.invoke(text)
            updateNotification(response)
            resetStatus()
            return
        }

        // 4. Mashina buyrug'i (oyna, lyuk, musiqa...)
        val result = CommandProcessor.process(text)
        if (result.command != CarCommand.UNKNOWN) {
            executeCarCommand(result)
            ttsEngine?.speakResult(result, currentLang)
            onCommand?.invoke(text)
            updateNotification(result.responseUz)
            resetStatus()
        } else {
            ttsEngine?.speakError(currentLang)
            onCommand?.invoke(text)
            resetStatus()
        }
    }

    private fun executeCarCommand(result: CommandResult) {
        val mgr = adapterManager ?: return
        val C = CarAdapterManager.CommandType

        when (result.command) {
            CarCommand.AC_ON          -> mgr.getAdapterFor(C.AC)?.setAC(true)
            CarCommand.AC_OFF         -> mgr.getAdapterFor(C.AC)?.setAC(false)
            CarCommand.AC_TEMP_UP     -> mgr.getAdapterFor(C.AC)?.setTemperature(3)
            CarCommand.AC_TEMP_DOWN   -> mgr.getAdapterFor(C.AC)?.setTemperature(1)
            CarCommand.HEATER_ON      -> mgr.getAdapterFor(C.HEATING)?.setHeating(true)
            CarCommand.HEATER_OFF     -> mgr.getAdapterFor(C.HEATING)?.setHeating(false)
            CarCommand.WINDOW_ALL_OPEN   -> mgr.getAdapterFor(C.WINDOW)?.setAllWindows(true)
            CarCommand.WINDOW_ALL_CLOSE  -> mgr.getAdapterFor(C.WINDOW)?.setAllWindows(false)
            CarCommand.WINDOW_FRONT_LEFT_OPEN  -> mgr.getAdapterFor(C.WINDOW)?.setWindow("FL", true)
            CarCommand.WINDOW_FRONT_LEFT_CLOSE -> mgr.getAdapterFor(C.WINDOW)?.setWindow("FL", false)
            CarCommand.WINDOW_FRONT_RIGHT_OPEN  -> mgr.getAdapterFor(C.WINDOW)?.setWindow("FR", true)
            CarCommand.WINDOW_FRONT_RIGHT_CLOSE -> mgr.getAdapterFor(C.WINDOW)?.setWindow("FR", false)
            CarCommand.WINDOW_BACK_LEFT_OPEN   -> mgr.getAdapterFor(C.WINDOW)?.setWindow("BL", true)
            CarCommand.WINDOW_BACK_LEFT_CLOSE  -> mgr.getAdapterFor(C.WINDOW)?.setWindow("BL", false)
            CarCommand.WINDOW_BACK_RIGHT_OPEN  -> mgr.getAdapterFor(C.WINDOW)?.setWindow("BR", true)
            CarCommand.WINDOW_BACK_RIGHT_CLOSE -> mgr.getAdapterFor(C.WINDOW)?.setWindow("BR", false)
            CarCommand.SUNROOF_OPEN   -> mgr.getAdapterFor(C.SUNROOF)?.setSunroof(true)
            CarCommand.SUNROOF_CLOSE  -> mgr.getAdapterFor(C.SUNROOF)?.setSunroof(false)
            CarCommand.LIGHTS_ON      -> mgr.getAdapterFor(C.LIGHTS)?.setLights(true)
            CarCommand.LIGHTS_OFF     -> mgr.getAdapterFor(C.LIGHTS)?.setLights(false)
            CarCommand.LIGHTS_HIGH    -> mgr.getAdapterFor(C.LIGHTS)?.setHighBeam(true)
            CarCommand.LIGHTS_LOW     -> mgr.getAdapterFor(C.LIGHTS)?.setHighBeam(false)
            CarCommand.TRUNK_OPEN     -> mgr.getAdapterFor(C.TRUNK)?.setTrunk(true)
            CarCommand.TRUNK_CLOSE    -> mgr.getAdapterFor(C.TRUNK)?.setTrunk(false)
            CarCommand.HORN_BEEP      -> mgr.getAdapterFor(C.HORN)?.beepHorn()
            CarCommand.MUSIC_PLAY     -> mgr.getAdapterFor(C.MEDIA)?.mediaPlay()
            CarCommand.MUSIC_PAUSE    -> mgr.getAdapterFor(C.MEDIA)?.mediaPause()
            CarCommand.MUSIC_NEXT     -> mgr.getAdapterFor(C.MEDIA)?.mediaNext()
            CarCommand.MUSIC_PREV     -> mgr.getAdapterFor(C.MEDIA)?.mediaPrev()
            CarCommand.VOLUME_UP      -> mgr.getAdapterFor(C.VOLUME)?.volumeUp()
            CarCommand.VOLUME_DOWN    -> mgr.getAdapterFor(C.VOLUME)?.volumeDown()
            CarCommand.VOLUME_MUTE    -> mgr.getAdapterFor(C.VOLUME)?.volumeMute()
            else -> {}
        }
    }

    private fun resetStatus() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            updateNotification("Doim eshitib turaman...")
            onStatus?.invoke("ready")
        }, 2000)
    }

    private fun detectLangSwitch(text: String): String? {
        for ((lang, keywords) in LANG_SWITCH) {
            if (keywords.any { text.contains(it) }) return lang
        }
        return null
    }

    private fun switchLanguage(newLang: String) {
        if (!ModelManager.isModelReady(this, newLang)) {
            ttsEngine?.speakModelMissing(newLang, currentLang)
            val msg = when (newLang) {
                "uz" -> "O'zbek modeli yo'q — Setup dan yuklab oling"
                "ru" -> "Русская модель не установлена"
                "en" -> "English model not found"
                else -> "Model not found"
            }
            updateNotification(msg)
            return
        }

        getSharedPreferences("car_prefs", Context.MODE_PRIVATE)
            .edit().putString("model_lang", newLang).apply()

        ttsEngine?.speakLangSwitch(newLang)
        onLangChanged?.invoke(newLang)

        commandTimeoutHandler.postDelayed({
            startEngine()
        }, 1500)
    }

    private fun extractCommand(text: String): String {
        for (wake in WAKE_WORDS) {
            if (text.contains(wake)) {
                val idx = text.indexOf(wake) + wake.length
                val rest = text.substring(idx).trim().trimStart(',', ' ', '-')
                if (rest.isNotBlank()) return rest
            }
        }
        return ""
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        voiceEngine?.destroy()
        ttsEngine?.destroy()
        adapterManager?.destroy()
        commandTimeoutHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Car Voice Control",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setSound(null, null)
            description = "Ovoz buyruqlari"
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Smart Car Control")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pi)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String) {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIF_ID, buildNotification(text))
    }
}
