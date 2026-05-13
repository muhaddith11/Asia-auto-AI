package com.car.voicecontrol

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothControl: BluetoothCarControl
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private lateinit var tvStatus: TextView
    private lateinit var tvWakeWord: TextView
    private lateinit var tvCommand: TextView
    private lateinit var tvResponse: TextView
    private lateinit var tvBluetoothStatus: TextView
    private lateinit var btnBluetooth: ImageButton

    private val PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Model yo'q bo'lsa — Setup ga
        val prefs = getSharedPreferences("car_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("model_lang", null)
        if (lang == null || !ModelManager.isModelReady(this, lang)) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        bluetoothControl = BluetoothCarControl(this)
        initTts(lang)
        bindViews()
        setupButtons()
        setupServiceCallbacks()
        requestPermissionsAndStart()
    }

    private fun bindViews() {
        tvStatus = findViewById(R.id.tv_status)
        tvWakeWord = findViewById(R.id.tv_wake_word)
        tvCommand = findViewById(R.id.tv_command)
        tvResponse = findViewById(R.id.tv_response)
        tvBluetoothStatus = findViewById(R.id.tv_bluetooth_status)
        btnBluetooth = findViewById(R.id.btn_bluetooth)

        btnBluetooth.setOnClickListener { showBluetoothDevices() }

        bluetoothControl.onConnectionChanged = { connected, info ->
            runOnUiThread {
                tvBluetoothStatus.text = if (connected) "BT: $info ✓" else "BT: Ulanmagan"
                tvBluetoothStatus.setTextColor(
                    getColor(if (connected) R.color.green_active else R.color.red_error)
                )
            }
        }
    }

    private fun setupServiceCallbacks() {
        CarVoiceService.onWakeWord = {
            runOnUiThread {
                tvWakeWord.text = "FAOL"
                tvWakeWord.setTextColor(getColor(R.color.green_active))
                tvStatus.text = "Buyruqni ayting..."
                tvStatus.setTextColor(getColor(R.color.green_active))
            }
        }

        CarVoiceService.onCommand = { text ->
            runOnUiThread {
                tvCommand.text = "\"$text\""
                val result = CommandProcessor.process(text)
                val lang = getSharedPreferences("car_prefs", Context.MODE_PRIVATE)
                    .getString("model_lang", "ru") ?: "ru"

                val response = getResponse(result, lang)
                tvResponse.text = response
                tvWakeWord.text = "KUTMOQDA"
                tvWakeWord.setTextColor(getColor(R.color.text_secondary))
                tvStatus.text = "\"Mashina\" deb buyruq bering"
                tvStatus.setTextColor(getColor(R.color.accent_blue))

                speak(response)
                if (result.command != CarCommand.UNKNOWN) {
                    sendBluetooth(result.bluetoothCode)
                }
            }
        }

        CarVoiceService.onStatus = { status ->
            runOnUiThread {
                when (status) {
                    "ready" -> {
                        tvStatus.text = "\"Mashina\" deb buyruq bering"
                        tvStatus.setTextColor(getColor(R.color.accent_blue))
                    }
                    "waiting" -> {
                        tvStatus.text = "Buyruqni ayting..."
                        tvStatus.setTextColor(getColor(R.color.green_active))
                    }
                    "model_missing" -> {
                        startActivity(Intent(this, SetupActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }

    private fun setupButtons() {
        val map = mapOf(
            R.id.btn_win_all_open to CarCommand.WINDOW_ALL_OPEN,
            R.id.btn_win_all_close to CarCommand.WINDOW_ALL_CLOSE,
            R.id.btn_win_fl_open to CarCommand.WINDOW_FRONT_LEFT_OPEN,
            R.id.btn_win_fl_close to CarCommand.WINDOW_FRONT_LEFT_CLOSE,
            R.id.btn_win_fr_open to CarCommand.WINDOW_FRONT_RIGHT_OPEN,
            R.id.btn_win_fr_close to CarCommand.WINDOW_FRONT_RIGHT_CLOSE,
            R.id.btn_sunroof_open to CarCommand.SUNROOF_OPEN,
            R.id.btn_sunroof_close to CarCommand.SUNROOF_CLOSE,
            R.id.btn_music_play to CarCommand.MUSIC_PLAY,
            R.id.btn_music_pause to CarCommand.MUSIC_PAUSE,
            R.id.btn_music_next to CarCommand.MUSIC_NEXT,
            R.id.btn_music_prev to CarCommand.MUSIC_PREV,
            R.id.btn_vol_up to CarCommand.VOLUME_UP,
            R.id.btn_vol_down to CarCommand.VOLUME_DOWN,
            R.id.btn_ac_on to CarCommand.AC_ON,
            R.id.btn_ac_off to CarCommand.AC_OFF,
            R.id.btn_temp_up to CarCommand.AC_TEMP_UP,
            R.id.btn_temp_down to CarCommand.AC_TEMP_DOWN,
            R.id.btn_lights_on to CarCommand.LIGHTS_ON,
            R.id.btn_lights_off to CarCommand.LIGHTS_OFF,
            R.id.btn_trunk_open to CarCommand.TRUNK_OPEN,
            R.id.btn_horn to CarCommand.HORN_BEEP
        )

        val lang = getSharedPreferences("car_prefs", Context.MODE_PRIVATE)
            .getString("model_lang", "ru") ?: "ru"

        map.forEach { (viewId, command) ->
            findViewById<Button>(viewId)?.setOnClickListener {
                val result = CommandProcessor.processCommand(command)
                val response = getResponse(result, lang)
                tvCommand.text = command.name
                tvResponse.text = response
                speak(response)
                sendBluetooth(result.bluetoothCode)
            }
        }
    }

    private fun getResponse(result: CommandResult, lang: String): String = when (lang) {
        "en" -> result.responseEn
        else -> result.responseRu
    }

    private fun initTts(lang: String) {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                val locale = if (lang == "en") Locale.ENGLISH else Locale("ru", "RU")
                val res = tts?.setLanguage(locale)
                if (res == TextToSpeech.LANG_NOT_SUPPORTED || res == TextToSpeech.LANG_MISSING_DATA) {
                    tts?.setLanguage(Locale.ENGLISH)
                }
            }
        }
    }

    private fun speak(text: String) {
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cmd")
    }

    private fun sendBluetooth(code: String) {
        lifecycleScope.launch { bluetoothControl.sendCommand(code) }
    }

    private fun showBluetoothDevices() {
        val devices = bluetoothControl.getPairedDevices()
        if (devices.isEmpty()) {
            Toast.makeText(this, "Juftlashgan qurilma yo'q", Toast.LENGTH_SHORT).show()
            return
        }
        val names = devices.map { it.name ?: it.address }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("ESP32 qurilmasini tanlang")
            .setItems(names) { _, idx -> connectDevice(devices[idx]) }
            .show()
    }

    private fun connectDevice(device: BluetoothDevice) {
        tvBluetoothStatus.text = "BT: Ulanmoqda..."
        lifecycleScope.launch { bluetoothControl.connect(device) }
    }

    private fun requestPermissionsAndStart() {
        val missing = PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            startService()
        } else {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) startService()
    }

    private fun startService() {
        startForegroundService(Intent(this, CarVoiceService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.shutdown()
        bluetoothControl.disconnect()
    }
}
