package com.car.voicecontrol

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

object ModelManager {

    private const val MODEL_DIR = "vosk_model"

    // Vosk modellar - Uzbek katta model (aniqroq), Rus va Ingliz kichik
    private val MODEL_URLS = mapOf(
        "ru" to "https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip",
        "en" to "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip",
        "uz" to "https://alphacephei.com/vosk/models/vosk-model-uz-0.22.zip"  // Katta model ~40MB
    )

    fun getModelPath(context: Context, lang: String): String {
        return File(context.filesDir, "$MODEL_DIR/$lang").absolutePath
    }

    fun isModelReady(context: Context, lang: String): Boolean {
        val dir = File(context.filesDir, "$MODEL_DIR/$lang")
        return dir.exists() && dir.isDirectory && (dir.listFiles()?.isNotEmpty() == true)
    }

    suspend fun downloadModel(
        context: Context,
        lang: String,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = MODEL_URLS[lang] ?: return@withContext false
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connect()

            val totalBytes = connection.contentLength.toLong()
            val zipFile = File(context.cacheDir, "vosk_$lang.zip")

            // Yuklab olish
            connection.inputStream.use { input ->
                FileOutputStream(zipFile).use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        downloaded += bytes
                        if (totalBytes > 0) {
                            onProgress((downloaded * 100 / totalBytes).toInt())
                        }
                    }
                }
            }

            // Zip ochish
            onProgress(95)
            unzip(zipFile, context, lang)
            zipFile.delete()
            onProgress(100)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun unzip(zipFile: File, context: Context, lang: String) {
        val outputDir = File(context.filesDir, "$MODEL_DIR/$lang")
        outputDir.mkdirs()

        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                // Zip ichidagi birinchi papka nomini olib tashlaymiz
                val name = entry.name.substringAfter("/")
                if (name.isNotBlank()) {
                    val file = File(outputDir, name)
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { out ->
                            zis.copyTo(out)
                        }
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    fun deleteModel(context: Context, lang: String) {
        File(context.filesDir, "$MODEL_DIR/$lang").deleteRecursively()
    }
}
