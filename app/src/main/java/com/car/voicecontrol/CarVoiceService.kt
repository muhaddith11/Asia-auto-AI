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

        var onWakeWord: (() -> Unit)? = null
        var onCommand: ((String) -> Unit)? = null
        var onStatus: ((String) -> Unit)? = null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Ishga tushmoqda..."))
        startEngine()
    }

    private fun startEngine() {
        val lang = getSharedPreferences("car_prefs", Context.MODE_PRIVATE)
            .getString("model_lang", "ru") ?: "ru"

        if (!ModelManager.isModelReady(this, lang)) {
            updateNotification("Model yo'q — Setup ni oching")
            onStatus?.invoke("model_missing")
            return
        }

        voiceEngine = OfflineVoiceEngine(this)

        voiceEngine?.onReady = {
            updateNotification("Doim eshitib turaman...")
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
                // "Mashina, lyukni och" — bir gapda
                executeCommand(command)
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
            executeCommand(lower)
        }
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
