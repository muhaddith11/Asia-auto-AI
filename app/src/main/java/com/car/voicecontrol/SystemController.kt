package com.car.voicecontrol

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.provider.Settings

object SystemController {

    data class SystemCommand(
        val keywords: List<String>,
        val action: SystemAction,
        val nameUz: String,
        val nameRu: String,
        val nameEn: String
    )

    enum class SystemAction {
        WIFI_ON, WIFI_OFF,
        MOBILE_ON, MOBILE_OFF,
        HOME_SCREEN,
        SCREEN_BRIGHT_UP, SCREEN_BRIGHT_DOWN,
        VOLUME_MEDIA_UP, VOLUME_MEDIA_DOWN
    }

    private val commands = listOf(
        SystemCommand(
            keywords = listOf("wifi yoq", "wifi on", "вайфай включи", "wifi yoqish", "вай фай включи"),
            action = SystemAction.WIFI_ON,
            nameUz = "WiFi yoqildi",
            nameRu = "WiFi включён",
            nameEn = "WiFi turned on"
        ),
        SystemCommand(
            keywords = listOf("wifi ochir", "wifi off", "вайфай выключи", "wifi o'chir"),
            action = SystemAction.WIFI_OFF,
            nameUz = "WiFi o'chirildi",
            nameRu = "WiFi выключен",
            nameEn = "WiFi turned off"
        ),
        SystemCommand(
            keywords = listOf(
                "mobil yoq", "mobil internet yoq", "мобильный включи",
                "mobile on", "internet yoq", "mobil data yoq"
            ),
            action = SystemAction.MOBILE_ON,
            nameUz = "Mobil internet yoqildi",
            nameRu = "Мобильный интернет включён",
            nameEn = "Mobile data turned on"
        ),
        SystemCommand(
            keywords = listOf(
                "mobil ochir", "mobil internet ochir", "мобильный выключи",
                "mobile off", "internet ochir", "mobil data ochir"
            ),
            action = SystemAction.MOBILE_OFF,
            nameUz = "Mobil internet o'chirildi",
            nameRu = "Мобильный интернет выключен",
            nameEn = "Mobile data turned off"
        ),
        SystemCommand(
            keywords = listOf(
                "bosh sahifa", "главный экран", "home", "asosiy ekran",
                "домой", "bosh ekran"
            ),
            action = SystemAction.HOME_SCREEN,
            nameUz = "Bosh sahifaga o'tildi",
            nameRu = "Перешёл на главный экран",
            nameEn = "Went to home screen"
        ),
        SystemCommand(
            keywords = listOf("ekran yorug", "brightness up", "яркость увеличь"),
            action = SystemAction.SCREEN_BRIGHT_UP,
            nameUz = "Ekran yoruqligi oshirildi",
            nameRu = "Яркость увеличена",
            nameEn = "Brightness increased"
        ),
        SystemCommand(
            keywords = listOf("ekran qorong'i", "brightness down", "яркость уменьши"),
            action = SystemAction.SCREEN_BRIGHT_DOWN,
            nameUz = "Ekran yoruqligi kamaytirildi",
            nameRu = "Яркость уменьшена",
            nameEn = "Brightness decreased"
        )
    )

    fun detect(text: String): SystemCommand? {
        val lower = text.lowercase()
        return commands.firstOrNull { cmd ->
            cmd.keywords.any { keyword -> lower.contains(keyword) }
        }
    }

    fun execute(context: Context, cmd: SystemCommand): Boolean {
        return try {
            when (cmd.action) {
                SystemAction.WIFI_ON -> setWifi(context, true)
                SystemAction.WIFI_OFF -> setWifi(context, false)
                SystemAction.MOBILE_ON -> setMobileData(true)
                SystemAction.MOBILE_OFF -> setMobileData(false)
                SystemAction.HOME_SCREEN -> goHome(context)
                SystemAction.SCREEN_BRIGHT_UP -> setBrightness(context, +50)
                SystemAction.SCREEN_BRIGHT_DOWN -> setBrightness(context, -50)
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun setWifi(context: Context, enable: Boolean): Boolean {
        return try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.isWifiEnabled = enable
            true
        } catch (e: Exception) {
            // Android 10+ da ruxsat yo'q — Settings ochib beramiz
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            false
        }
    }

    private fun setMobileData(enable: Boolean): Boolean {
        return try {
            // Root talab qiladi — avtomatik urinib ko'ramiz
            val cmd = if (enable) "svc data enable" else "svc data disable"
            Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun goHome(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return true
    }

    private fun setBrightness(context: Context, delta: Int): Boolean {
        return try {
            val current = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, 128
            )
            val newVal = (current + delta).coerceIn(10, 255)
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                newVal
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getResponseText(cmd: SystemCommand, lang: String): String {
        return when (lang) {
            "ru" -> cmd.nameRu
            "en" -> cmd.nameEn
            else -> cmd.nameUz
        }
    }
}
