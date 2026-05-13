package com.car.voicecontrol

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceEngine(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    var onResult: ((String) -> Unit)? = null
    var onListeningStarted: (() -> Unit)? = null
    var onListeningStopped: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    enum class AppLanguage { UZBEK, RUSSIAN, ENGLISH }
    var currentLanguage = AppLanguage.UZBEK

    init {
        initTts()
        initSpeechRecognizer()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                updateTtsLanguage()
            }
        }
    }

    private fun updateTtsLanguage() {
        val locale = when (currentLanguage) {
            AppLanguage.UZBEK -> Locale("uz", "UZ")
            AppLanguage.RUSSIAN -> Locale("ru", "RU")
            AppLanguage.ENGLISH -> Locale.ENGLISH
        }
        val result = textToSpeech?.setLanguage(locale)
        if (result == TextToSpeech.LANG_NOT_SUPPORTED || result == TextToSpeech.LANG_MISSING_DATA) {
            textToSpeech?.setLanguage(Locale.ENGLISH)
        }
    }

    private fun initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { onListeningStarted?.invoke() }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { onListeningStopped?.invoke() }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank()) onResult?.invoke(text)
            }

            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Ovoz tanilmadi"
                    SpeechRecognizer.ERROR_NETWORK -> "Internet aloqasi yo'q"
                    SpeechRecognizer.ERROR_AUDIO -> "Mikrofon xatosi"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Ovoz kutilmadi"
                    else -> "Xato: $error"
                }
                onError?.invoke(msg)
                onListeningStopped?.invoke()
            }
        })
    }

    fun startListening() {
        val locale = when (currentLanguage) {
            AppLanguage.UZBEK -> "uz-UZ"
            AppLanguage.RUSSIAN -> "ru-RU"
            AppLanguage.ENGLISH -> "en-US"
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun speak(resultText: CommandResult) {
        if (!isTtsReady) return
        val text = when (currentLanguage) {
            AppLanguage.UZBEK -> resultText.responseUz
            AppLanguage.RUSSIAN -> resultText.responseRu
            AppLanguage.ENGLISH -> resultText.responseEn
        }
        updateTtsLanguage()
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "car_cmd")
    }

    fun setLanguage(lang: AppLanguage) {
        currentLanguage = lang
        updateTtsLanguage()
    }

    fun destroy() {
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
    }
}
