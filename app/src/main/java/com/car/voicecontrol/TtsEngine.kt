package com.car.voicecontrol

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TtsEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var currentLang = "uz"

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                setLanguage(currentLang)
            }
        }
    }

    fun setLanguage(lang: String) {
        currentLang = lang
        val locale = when (lang) {
            "ru" -> Locale("ru", "RU")
            "en" -> Locale.ENGLISH
            else -> Locale("uz", "UZ").let { uz ->
                // O'zbek TTS yo'q bo'lsa, ruscha ishlatamiz
                val result = tts?.isLanguageAvailable(uz) ?: TextToSpeech.LANG_NOT_SUPPORTED
                if (result == TextToSpeech.LANG_NOT_SUPPORTED ||
                    result == TextToSpeech.LANG_MISSING_DATA) {
                    Locale("ru", "RU")
                } else uz
            }
        }
        tts?.language = locale
        tts?.setSpeechRate(1.1f)
        tts?.setPitch(1.0f)
    }

    fun speak(text: String) {
        if (!isReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_${System.currentTimeMillis()}")
    }

    fun speakResult(result: CommandResult, lang: String) {
        val text = when (lang) {
            "ru" -> result.responseRu
            "en" -> result.responseEn
            else -> result.responseUz
        }
        speak(text)
    }

    fun speakLangSwitch(newLang: String) {
        val text = when (newLang) {
            "uz" -> "O'zbek tiliga o'tdim"
            "ru" -> "Переключился на русский"
            "en" -> "Switched to English"
            else -> "Language changed"
        }
        speak(text)
    }

    fun speakWakeWord(lang: String) {
        val text = when (lang) {
            "ru" -> "Слушаю"
            "en" -> "Listening"
            else -> "Eshityapman"
        }
        speak(text)
    }

    fun speakError(lang: String) {
        val text = when (lang) {
            "ru" -> "Команда не распознана"
            "en" -> "Command not recognized"
            else -> "Buyruq tushunilmadi"
        }
        speak(text)
    }

    fun speakModelMissing(missingLang: String, currentLang: String) {
        val text = when (currentLang) {
            "ru" -> when (missingLang) {
                "uz" -> "Узбекская модель не установлена"
                "en" -> "Английская модель не установлена"
                else -> "Модель не найдена"
            }
            "en" -> when (missingLang) {
                "uz" -> "Uzbek model not installed"
                "ru" -> "Russian model not installed"
                else -> "Model not found"
            }
            else -> when (missingLang) {
                "ru" -> "Rus modeli o'rnatilmagan"
                "en" -> "Ingliz modeli o'rnatilmagan"
                else -> "Model topilmadi"
            }
        }
        speak(text)
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
