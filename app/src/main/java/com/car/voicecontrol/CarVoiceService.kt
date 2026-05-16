package com.car.voicecontrol

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class CarVoiceService : Service() {

    private var voiceEngine: OfflineVoiceEngine? = null
    private var waitingForCommand = false
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

        // Til almashtirish buyruqlari
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
        startEngine()
    }

    private fun startEngine() {
        val lang = getSharedPreferences("car_prefs", Context.MODE_PRIVATE)
            .getString("model_lang", "uz") ?: "uz"

        if (!ModelManager.isModelReady(this, lang)) {
            updateNotification("Model yo'q — Setup ni oching")
            onStatus?.invoke("model_missing")
            return
        }

        voiceEngine?.destroy()
        voiceEngine = OfflineVoiceEngine(this)

        voiceEngine?.onReady = {
            val langName = when (lang) {
                "uz" -> "O'zbek"
                "ru" -> "Русский"
                "en" -> "English"
                else -> lang
            }
            updateNotification("✓ $langName — Doim eshitib turaman")
            onStatus?.invoke("ready")
            voiceEngine?.startListening()
        }

        voiceEngine?.onResult = { text ->
            processText(text)
        }

        voiceEngine?.onError = { error ->
            updateNotification("Xato: $error")
        }

        val modelPath = ModelManager.getModelPath(this, lang)
        Thread { voiceEngine?.loadModel(modelPath, lang) }.start()
    }

    private fun processText(text: String) {
        val lower = text.lowercase().trim()
        val hasWakeWord = WAKE_WORDS.any { lower.contains(it) }

        if (hasWakeWord) {
            onWakeWord?.invoke()
            val command = extractCommand(lower)

            if (command.isNotBlank()) {
                // Til almashtirish buyrug'i ham bo'lishi mumkin
                val switchedLang = detectLangSwitch(command)
                if (switchedLang != null) {
                    switchLanguage(switchedLang)
                } else {
                    executeCommand(command)
                }
            } else {
                // Faqat "Mashina" — keyingi gapni kutish
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

            val switchedLang = detectLangSwitch(lower)
            if (switchedLang != null) {
                switchLanguage(switchedLang)
            } else {
                executeCommand(lower)
            }
        }
    }

    private fun detectLangSwitch(text: String): String? {
        for ((lang, keywords) in LANG_SWITCH) {
            if (keywords.any { text.contains(it) }) return lang
        }
        return null
    }

    private fun switchLanguage(newLang: String) {
        // Yangi model yuklab olinganmi tekshir
        if (!ModelManager.isModelReady(this, newLang)) {
            val msg = when (newLang) {
                "uz" -> "O'zbek modeli yo'q — Setup dan yuklab oling"
                "ru" -> "Русская модель не установлена — откройте Setup"
                "en" -> "English model not found — please open Setup"
                else -> "Model not found"
            }
            updateNotification(msg)
            onStatus?.invoke("model_missing_$newLang")
            return
        }

        // Tilni saqlash
        getSharedPreferences("car_prefs", Context.MODE_PRIVATE)
            .edit().putString("model_lang", newLang).apply()

        val langName = when (newLang) {
            "uz" -> "O'zbek tiliga o'tmoqda..."
            "ru" -> "Переключаюсь на русский..."
            "en" -> "Switching to English..."
            else -> "Switching..."
        }

        updateNotification(langName)
        onStatus?.invoke("switching_$newLang")
        onLangChanged?.invoke(newLang)

        // Dvigatelni qayta ishga tushirish
        commandTimeoutHandler.postDelayed({
            startEngine()
        }, 500)
    }

    private fun executeCommand(text: String) {
        val result = CommandProcessor.process(text)
        onCommand?.invoke(text)
        updateNotification("Bajarildi: $text")

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            updateNotification("Doim eshitib turaman...")
            onStatus?.invoke("ready")
        }, 2000)
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
