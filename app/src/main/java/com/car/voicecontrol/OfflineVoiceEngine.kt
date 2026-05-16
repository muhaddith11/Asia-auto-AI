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
    private var currentLang: String = "uz"

    var onResult: ((String) -> Unit)? = null
    var onReady: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    fun loadModel(modelPath: String, lang: String = "uz") {
        currentLang = lang
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
            val grammar = buildGrammar(currentLang)
            val recognizer = Recognizer(m, 16000f, grammar)
            recognizer.setMaxAlternatives(3)

            speechService = SpeechService(recognizer, 16000f)
            speechService?.startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String?) {
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

    private fun buildGrammar(lang: String): String {
        val words = when (lang) {
            "uz" -> listOf(
                // Wake so'zlari
                "mashina", "avto",
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
                "[unk]"
            )

            "ru" -> listOf(
                // Wake so'zlari
                "машина", "машину", "машине", "привет машина",
                // Oyna
                "открой все окна", "закрой все окна",
                "водительское окно открыть", "водительское окно закрыть",
                "пассажирское окно открыть", "пассажирское окно закрыть",
                "левое окно", "правое окно",
                // Lyuk
                "открой люк", "закрой люк", "люк открыть", "люк закрыть",
                // Musiqa
                "включи музыку", "выключи музыку", "поставь музыку",
                "пауза", "стоп музыка",
                "следующий", "следующая песня",
                "предыдущий", "предыдущая песня", "назад",
                // Ovoz
                "громче", "увеличить громкость",
                "тише", "уменьшить громкость",
                "замолчи", "выключить звук",
                // Konditsioner
                "включи кондиционер", "выключи кондиционер",
                "кондиционер включить", "кондиционер выключить",
                "теплее", "холоднее",
                // Chiroq
                "включи фары", "выключи фары",
                "фары включить", "фары выключить",
                "дальний свет",
                // Bagaj
                "открой багажник", "закрой багажник",
                "багажник открыть", "багажник закрыть",
                // Signal
                "посигналь", "сигнал", "бибикни",
                "[unk]"
            )

            "en" -> listOf(
                // Wake so'zlari
                "hey car", "auto",
                // Oyna
                "open all windows", "close all windows",
                "driver window open", "driver window close",
                "passenger window open", "passenger window close",
                "front left window", "front right window",
                // Lyuk
                "open sunroof", "close sunroof", "sunroof open", "sunroof close",
                // Musiqa
                "play music", "pause music", "stop music", "music on", "music off",
                "next song", "next track", "skip",
                "previous song", "previous track", "back",
                // Ovoz
                "volume up", "louder",
                "volume down", "quieter",
                "mute", "silence",
                // Konditsioner
                "ac on", "ac off",
                "air conditioner on", "air conditioner off",
                "warmer", "cooler",
                // Chiroq
                "lights on", "lights off",
                "headlights on", "headlights off",
                "high beam",
                // Bagaj
                "open trunk", "close trunk", "trunk open", "trunk close",
                // Signal
                "beep", "horn", "honk",
                "[unk]"
            )

            else -> listOf("mashina", "машина", "[unk]")
        }

        return JSONArray(words).toString()
    }
}
