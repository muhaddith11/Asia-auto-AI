package com.car.voicecontrol

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Mashina yoqilganda dastur avtomatik ishga tushadi
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val lang = context.getSharedPreferences("car_prefs", Context.MODE_PRIVATE)
                .getString("model_lang", null)

            if (lang != null && ModelManager.isModelReady(context, lang)) {
                context.startForegroundService(Intent(context, CarVoiceService::class.java))
            }
        }
    }
}
