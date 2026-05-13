package com.car.voicecontrol

import android.content.Context
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
            val recognizer = Recognizer(m, 16000f)
            speechService = SpeechService(recognizer, 16000f)
            speechService?.startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String?) {}

                override fun onResult(hypothesis: String?) {
                    val text = parseText(hypothesis)
                    if (text.isNotBlank()) onResult?.invoke(text)
                }

                override fun onFinalResult(hypothesis: String?) {
                    val text = parseText(hypothesis)
                    if (text.isNotBlank()) onResult?.invoke(text)
                }

                override fun onError(exception: Exception?) {
                    // Avtomatik qayta boshlash
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
}
