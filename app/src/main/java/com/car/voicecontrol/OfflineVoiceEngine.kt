package com.car.voicecontrol

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService

class OfflineVoiceEngine(private val context: Context) {

    private var model: Model? = null
    private var speechService: SpeechService? = null

    var onResult: ((String) -> Unit)? = null
    var onReady: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    // Grammar - faqat shu so'zlarni taniydi (aniqlik oshadi)
    private val grammar = buildGrammar()

    fun loadModel(modelPath: String) {
        try {
            model = Model(modelPath)
            onReady?.invoke()
        } catch (e: Exception) {
            onError?.invoke("Model yuklanmadi: ${e.message}")
        }
    }

    fun startListening() {
        val m = model ?: return
        try {
            // Grammar bilan recognizer - faqat buyruq so'zlarini taniydi
            val recognizer = Recognizer(m, 16000f, grammar)
            recognizer.setMaxAlternatives(3)
            speechService = SpeechService(recognizer, 16000f)
            speechService?.startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String?) {
                    // Partial natijani ham tekshiramiz - tezroq javob berish uchun
                    val text = parseText(hypothesis)
                    if (text.length > 3) onResult?.invoke(text)
                }

                override fun onResult(hypothesis: String?) {
                    val text = parseText(hypothesis)
                    if (text.isNotBlank()) onResult?.invoke(text)
                }

                override fun onFinalResult(hypothesis: String?) {
                    val text = parseText(hypothesis)
                    if (text.isNotBlank()) onResult?.invoke(text)
                }

                override fun onError(exception: Exception?) {
                    restart()
                }

                override fun onTimeout() {
                    restart()
                }
            })
        } catch (e: Exception) {
            onError?.invoke("Tinglash xatosi: ${e.message}")
        }
    }

    private fun restart() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            stopListening()
            startListening()
        }, 300)
    }

    fun stopListening() {
        try {
            speechService?.stop()
            speechService?.shutdown()
            speechService = null
        } catch (_: Exception) {}
    }

    fun destroy() {
        stopListening()
        model?.close()
        model = null
    }

    private fun parseText(json: String?): String {
        if (json.isNullOrBlank()) return ""
        return try {
            JSONObject(json).optString("text", "").trim()
        } catch (_: Exception) { "" }
    }

    private fun buildGrammar(): String {
        val words = mutableListOf(
            // === WAKE SO'ZLARI ===
            "mashina", "машина", "avto", "auto", "hey car",

            // === O'ZBEK BUYRUQLAR ===
            // Oyna
            "barcha oyna och", "hamma oyna och", "oynalarni och",
            "barcha oyna yop", "hamma oyna yop", "oynalarni yop",
            "old chap oyna", "chap oyna och", "haydovchi oyna",
            "old chap oyna yop", "chap oyna yop",
            "old ong oyna", "ong oyna och", "yolovchi oyna",
            "old ong oyna yop", "ong oyna yop",
            // Lyuk
            "lyuk och", "lyukni och", "luk och",
            "lyuk yop", "lyukni yop", "luk yop",
            // Musiqa
            "musiqa yoq", "musiqa boshla", "musiqa quy",
            "musiqa toxtat", "musiqa ochir", "pauza",
            "keyingi qoshiq", "keyingisi", "keyingi",
            "oldingi qoshiq", "oldingisi", "orqaga",
            // Ovoz
            "ovozni oshir", "balandroq", "ovoz oshir",
            "ovozni past", "sekinroq", "past qil",
            "jim", "ovozni ochir",
            // Konditsioner
            "konditsioner yoq", "kond yoq", "sovutgich yoq",
            "konditsioner ochir", "kond ochir",
            "harorat oshir", "issiq qil",
            "harorat past", "salqin qil",
            // Chiroq
            "chiroq yoq", "faralar yoq", "yoruglik yoq",
            "chiroq ochir", "faralar ochir",
            "uzun nur", "katta chiroq",
            // Bagaj
            "bagaj och", "bagajni och",
            "bagaj yop", "bagajni yop",
            // Signal
            "signal ber", "signalini ber", "bib",

            // === RUS BUYRUQLAR ===
            "открой все окна", "закрой все окна",
            "открой люк", "закрой люк",
            "включи музыку", "выключи музыку", "пауза",
            "следующий", "предыдущий",
            "громче", "тише", "замолчи",
            "включи кондиционер", "выключи кондиционер",
            "теплее", "холоднее",
            "включи фары", "выключи фары",
            "открой багажник", "закрой багажник",
            "посигналь", "сигнал",

            // === INGLIZ BUYRUQLAR ===
            "open all windows", "close all windows",
            "open sunroof", "close sunroof",
            "play music", "pause music", "stop music",
            "next song", "previous song",
            "volume up", "volume down", "mute",
            "ac on", "ac off",
            "lights on", "lights off",
            "open trunk", "close trunk",
            "horn", "beep",

            // Noma'lum so'z uchun
            "[unk]"
        )

        return JSONArray(words).toString()
    }
}
