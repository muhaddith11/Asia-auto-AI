package com.car.voicecontrol

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SetupActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvInfo: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgress: TextView
    private lateinit var btnDownloadRu: Button
    private lateinit var btnDownloadEn: Button
    private lateinit var btnDownloadUz: Button
    private lateinit var btnStart: Button
    private lateinit var layoutButtons: LinearLayout
    private lateinit var layoutProgress: LinearLayout

    private var selectedLang = "ru"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        bindViews()
        checkExistingModels()
    }

    private fun bindViews() {
        tvTitle = findViewById(R.id.tv_setup_title)
        tvInfo = findViewById(R.id.tv_setup_info)
        progressBar = findViewById(R.id.progress_bar)
        tvProgress = findViewById(R.id.tv_progress)
        btnDownloadRu = findViewById(R.id.btn_download_ru)
        btnDownloadEn = findViewById(R.id.btn_download_en)
        btnDownloadUz = findViewById(R.id.btn_download_uz)
        btnStart = findViewById(R.id.btn_start)
        layoutButtons = findViewById(R.id.layout_buttons)
        layoutProgress = findViewById(R.id.layout_progress)

        btnDownloadRu.setOnClickListener { downloadModel("ru") }
        btnDownloadEn.setOnClickListener { downloadModel("en") }
        btnDownloadUz.setOnClickListener { downloadModel("uz") }
        btnStart.setOnClickListener { goToMain() }
    }

    private fun checkExistingModels() {
        val ruReady = ModelManager.isModelReady(this, "ru")
        val enReady = ModelManager.isModelReady(this, "en")
        val uzReady = ModelManager.isModelReady(this, "uz")

        btnDownloadRu.text = if (ruReady) "✓ Rus tili (o'rnatilgan)" else "Rus tili yuklab olish (45 MB)"
        btnDownloadEn.text = if (enReady) "✓ Ingliz tili (o'rnatilgan)" else "Ingliz tili yuklab olish (40 MB)"
        btnDownloadUz.text = if (uzReady) "✓ O'zbek tili (o'rnatilgan)" else "O'zbek tili yuklab olish (20 MB)"

        btnStart.visibility = if (ruReady || enReady || uzReady) View.VISIBLE else View.GONE

        // Agar model bo'lsa — to'g'ri Main ga o'tamiz
        val prefs = getSharedPreferences("car_prefs", Context.MODE_PRIVATE)
        val savedLang = prefs.getString("model_lang", null)
        if (savedLang != null && ModelManager.isModelReady(this, savedLang)) {
            goToMain()
        }
    }

    private fun downloadModel(lang: String) {
        selectedLang = lang
        layoutButtons.visibility = View.GONE
        layoutProgress.visibility = View.VISIBLE

        val langName = when(lang) {
            "ru" -> "Rus tili"
            "en" -> "Ingliz tili"
            else -> "O'zbek tili"
        }
        tvTitle.text = "$langName yuklanmoqda..."
        tvInfo.text = "Iltimos kuting. Bu faqat bir marta amalga oshiriladi."

        lifecycleScope.launch {
            val success = ModelManager.downloadModel(this@SetupActivity, lang) { progress ->
                runOnUiThread {
                    progressBar.progress = progress
                    tvProgress.text = when {
                        progress < 90 -> "Yuklanmoqda: $progress%"
                        progress < 100 -> "O'rnatilmoqda..."
                        else -> "Tayyor! ✓"
                    }
                }
            }

            runOnUiThread {
                if (success) {
                    getSharedPreferences("car_prefs", Context.MODE_PRIVATE)
                        .edit().putString("model_lang", lang).apply()

                    tvTitle.text = "Model tayyor! ✓"
                    tvInfo.text = "Endi internet kerak emas. Dastur ishga tushmoqda..."

                    android.os.Handler(mainLooper).postDelayed({ goToMain() }, 1500)
                } else {
                    tvTitle.text = "Xato yuz berdi"
                    tvInfo.text = "Internet aloqasini tekshiring va qayta urinib ko'ring."
                    layoutButtons.visibility = View.VISIBLE
                    layoutProgress.visibility = View.GONE
                }
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
