package com.car.voicecontrol

import android.content.Context
import android.content.Intent
import android.net.Uri

object AppLauncher {

    data class AppCommand(
        val keywords: List<String>,
        val packageName: String,
        val fallbackUrl: String? = null,
        val nameUz: String,
        val nameRu: String,
        val nameEn: String
    )

    private val appCommands = listOf(
        AppCommand(
            keywords = listOf(
                "youtube", "yutub", "ютуб", "видео",
                "youtube och", "yutub och"
            ),
            packageName = "com.google.android.youtube",
            fallbackUrl = "https://youtube.com",
            nameUz = "YouTube ochildi",
            nameRu = "YouTube открыт",
            nameEn = "YouTube opened"
        ),
        AppCommand(
            keywords = listOf(
                "yandex music", "yandeks muzika", "musiqa ilovasi",
                "яндекс музыка", "музыкальное приложение"
            ),
            packageName = "ru.yandex.music",
            nameUz = "Yandex Music ochildi",
            nameRu = "Яндекс Музыка открыта",
            nameEn = "Yandex Music opened"
        ),
        AppCommand(
            keywords = listOf(
                "navigatsiya", "navigator", "yandex navigator",
                "навигатор", "яндекс навигатор", "yo'l ko'rsat",
                "navigation", "maps", "xarita"
            ),
            packageName = "ru.yandex.yandexnaps",
            fallbackUrl = "yandexnavi://",
            nameUz = "Navigator ochildi",
            nameRu = "Навигатор открыт",
            nameEn = "Navigator opened"
        ),
        AppCommand(
            keywords = listOf(
                "google maps", "xarita", "карта", "google xarita"
            ),
            packageName = "com.google.android.apps.maps",
            nameUz = "Google Maps ochildi",
            nameRu = "Google Maps открыт",
            nameEn = "Google Maps opened"
        ),
        AppCommand(
            keywords = listOf(
                "telefon", "qo'ng'iroq", "звонок", "позвони", "call",
                "telefon och", "dialer"
            ),
            packageName = "com.android.dialer",
            nameUz = "Telefon ochildi",
            nameRu = "Телефон открыт",
            nameEn = "Phone opened"
        ),
        AppCommand(
            keywords = listOf(
                "sozlamalar", "nastroyki", "settings", "настройки",
                "sozlama", "setting"
            ),
            packageName = "com.android.settings",
            nameUz = "Sozlamalar ochildi",
            nameRu = "Настройки открыты",
            nameEn = "Settings opened"
        ),
        AppCommand(
            keywords = listOf(
                "spotify", "спотифай"
            ),
            packageName = "com.spotify.music",
            nameUz = "Spotify ochildi",
            nameRu = "Spotify открыт",
            nameEn = "Spotify opened"
        )
    )

    fun detect(text: String): AppCommand? {
        val lower = text.lowercase()
        return appCommands.firstOrNull { app ->
            app.keywords.any { keyword -> lower.contains(keyword) }
        }
    }

    fun launch(context: Context, app: AppCommand): Boolean {
        // Avval ilova o'rnatilganmi tekshir
        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } else if (app.fallbackUrl != null) {
            // Ilova yo'q — brauzer yoki deep link orqali ochamiz
            try {
                val uri = Uri.parse(app.fallbackUrl)
                val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    fun getResponseText(app: AppCommand, lang: String): String {
        return when (lang) {
            "ru" -> app.nameRu
            "en" -> app.nameEn
            else -> app.nameUz
        }
    }
}
