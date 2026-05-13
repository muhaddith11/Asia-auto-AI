package com.car.voicecontrol

enum class CarCommand {
    WINDOW_FRONT_LEFT_OPEN, WINDOW_FRONT_LEFT_CLOSE,
    WINDOW_FRONT_RIGHT_OPEN, WINDOW_FRONT_RIGHT_CLOSE,
    WINDOW_BACK_LEFT_OPEN, WINDOW_BACK_LEFT_CLOSE,
    WINDOW_BACK_RIGHT_OPEN, WINDOW_BACK_RIGHT_CLOSE,
    WINDOW_ALL_OPEN, WINDOW_ALL_CLOSE,
    SUNROOF_OPEN, SUNROOF_CLOSE,
    MUSIC_PLAY, MUSIC_PAUSE, MUSIC_NEXT, MUSIC_PREV,
    VOLUME_UP, VOLUME_DOWN, VOLUME_MUTE,
    AC_ON, AC_OFF, AC_TEMP_UP, AC_TEMP_DOWN,
    HEATER_ON, HEATER_OFF,
    LIGHTS_ON, LIGHTS_OFF, LIGHTS_HIGH, LIGHTS_LOW,
    TRUNK_OPEN, TRUNK_CLOSE,
    HORN_BEEP,
    UNKNOWN
}

data class CommandResult(
    val command: CarCommand,
    val responseUz: String,
    val responseRu: String,
    val responseEn: String,
    val bluetoothCode: String
)

object CommandProcessor {

    private val commandMap = listOf(
        // WINDOWS - ALL
        Triple(
            listOf("barcha oyna", "hamma oyna", "oynalar", "oynaları och"),
            listOf("все окна открыть", "все окна", "открыть все окна"),
            listOf("all windows open", "open all windows", "windows open all")
        ) to CarCommand.WINDOW_ALL_OPEN,

        Triple(
            listOf("barcha oyna yop", "hamma oynani yop", "oynalarni yop"),
            listOf("все окна закрыть", "закрыть все окна"),
            listOf("all windows close", "close all windows")
        ) to CarCommand.WINDOW_ALL_CLOSE,

        // WINDOW FRONT LEFT
        Triple(
            listOf("old chap oyna", "haydovchi oyna", "chap oldingi oyna"),
            listOf("водительское окно", "левое переднее окно открыть"),
            listOf("driver window open", "front left window open")
        ) to CarCommand.WINDOW_FRONT_LEFT_OPEN,

        Triple(
            listOf("old chap oyna yop", "haydovchi oyna yop"),
            listOf("водительское окно закрыть", "левое переднее окно закрыть"),
            listOf("driver window close", "front left window close")
        ) to CarCommand.WINDOW_FRONT_LEFT_CLOSE,

        // WINDOW FRONT RIGHT
        Triple(
            listOf("old ong oyna", "o'ng oldingi oyna", "yo'lovchi oyna"),
            listOf("пассажирское окно", "правое переднее окно открыть"),
            listOf("passenger window open", "front right window open")
        ) to CarCommand.WINDOW_FRONT_RIGHT_OPEN,

        Triple(
            listOf("old ong oyna yop", "yo'lovchi oyna yop"),
            listOf("пассажирское окно закрыть", "правое переднее окно закрыть"),
            listOf("passenger window close", "front right window close")
        ) to CarCommand.WINDOW_FRONT_RIGHT_CLOSE,

        // SUNROOF
        Triple(
            listOf("lyuk och", "lyukni och", "tomi och", "tomni och"),
            listOf("открой люк", "люк открыть", "открыть люк"),
            listOf("open sunroof", "sunroof open")
        ) to CarCommand.SUNROOF_OPEN,

        Triple(
            listOf("lyuk yop", "lyukni yop", "tomi yop"),
            listOf("закрой люк", "люк закрыть", "закрыть люк"),
            listOf("close sunroof", "sunroof close")
        ) to CarCommand.SUNROOF_CLOSE,

        // MUSIC
        Triple(
            listOf("musiqa quy", "musiqa boshla", "musiqa yoq", "qo'shiq yoq"),
            listOf("включи музыку", "музыку", "поставь музыку"),
            listOf("play music", "start music", "music on")
        ) to CarCommand.MUSIC_PLAY,

        Triple(
            listOf("musiqa to'xtat", "musiqa o'chir", "pauza"),
            listOf("стоп музыка", "пауза", "выключи музыку"),
            listOf("pause music", "stop music", "music off")
        ) to CarCommand.MUSIC_PAUSE,

        Triple(
            listOf("keyingi qo'shiq", "keyingisi", "navbatdagi"),
            listOf("следующий", "следующая песня", "вперёд"),
            listOf("next song", "next track", "skip")
        ) to CarCommand.MUSIC_NEXT,

        Triple(
            listOf("oldingi qo'shiq", "oldingisi", "orqaga"),
            listOf("предыдущий", "предыдущая песня", "назад"),
            listOf("previous song", "previous track", "back")
        ) to CarCommand.MUSIC_PREV,

        // VOLUME
        Triple(
            listOf("ovozni oshir", "balandroq", "ovoz oshir"),
            listOf("громче", "увеличить громкость"),
            listOf("volume up", "louder")
        ) to CarCommand.VOLUME_UP,

        Triple(
            listOf("ovozni past", "ovozni kamay", "sekinroq"),
            listOf("тише", "уменьшить громкость"),
            listOf("volume down", "quieter")
        ) to CarCommand.VOLUME_DOWN,

        Triple(
            listOf("jim", "ovozni o'chir", "shovqinsiz"),
            listOf("тихо", "замолчи", "выключить звук"),
            listOf("mute", "silence")
        ) to CarCommand.VOLUME_MUTE,

        // AC / CLIMATE
        Triple(
            listOf("konditsioner yoq", "konditsionerni yoq", "sovutgich yoq", "kond yoq"),
            listOf("включи кондиционер", "кондиционер включить", "кондиционер"),
            listOf("ac on", "air conditioner on", "turn on ac")
        ) to CarCommand.AC_ON,

        Triple(
            listOf("konditsioner o'chir", "konditsionerni o'chir", "sovutgich o'chir"),
            listOf("выключи кондиционер", "кондиционер выключить"),
            listOf("ac off", "air conditioner off", "turn off ac")
        ) to CarCommand.AC_OFF,

        Triple(
            listOf("harorat oshir", "isitish oshir", "ilik qil"),
            listOf("теплее", "повысить температуру"),
            listOf("temperature up", "warmer")
        ) to CarCommand.AC_TEMP_UP,

        Triple(
            listOf("harorat past", "sovutroq qil", "salqin qil"),
            listOf("холоднее", "понизить температуру"),
            listOf("temperature down", "cooler")
        ) to CarCommand.AC_TEMP_DOWN,

        // LIGHTS
        Triple(
            listOf("chiroq yoq", "faralar yoq", "yorug'lik yoq"),
            listOf("включи фары", "фары включить", "свет включить"),
            listOf("lights on", "headlights on", "turn on lights")
        ) to CarCommand.LIGHTS_ON,

        Triple(
            listOf("chiroq o'chir", "faralar o'chir"),
            listOf("выключи фары", "фары выключить", "свет выключить"),
            listOf("lights off", "headlights off", "turn off lights")
        ) to CarCommand.LIGHTS_OFF,

        Triple(
            listOf("uzun yorug", "uzun nur", "katta chiroq"),
            listOf("дальний свет", "дальний"),
            listOf("high beam", "full beam", "brights on")
        ) to CarCommand.LIGHTS_HIGH,

        // TRUNK
        Triple(
            listOf("bagaj och", "kapotyni och", "orqa qopqoq"),
            listOf("открой багажник", "багажник открыть"),
            listOf("open trunk", "trunk open")
        ) to CarCommand.TRUNK_OPEN,

        Triple(
            listOf("bagaj yop", "kapotyni yop"),
            listOf("закрой багажник", "багажник закрыть"),
            listOf("close trunk", "trunk close")
        ) to CarCommand.TRUNK_CLOSE,

        // HORN
        Triple(
            listOf("signal ber", "bib", "guguk"),
            listOf("бибикни", "сигнал", "посигналь"),
            listOf("beep", "horn", "honk")
        ) to CarCommand.HORN_BEEP,
    )

    private val responses = mapOf(
        CarCommand.WINDOW_ALL_OPEN to CommandResult(CarCommand.WINDOW_ALL_OPEN,
            "Barcha oynalar ochilmoqda", "Все окна открываются", "Opening all windows", "WIN_ALL_OPEN"),
        CarCommand.WINDOW_ALL_CLOSE to CommandResult(CarCommand.WINDOW_ALL_CLOSE,
            "Barcha oynalar yopilmoqda", "Все окна закрываются", "Closing all windows", "WIN_ALL_CLOSE"),
        CarCommand.WINDOW_FRONT_LEFT_OPEN to CommandResult(CarCommand.WINDOW_FRONT_LEFT_OPEN,
            "Old chap oyna ochilmoqda", "Водительское окно открывается", "Driver window opening", "WIN_FL_OPEN"),
        CarCommand.WINDOW_FRONT_LEFT_CLOSE to CommandResult(CarCommand.WINDOW_FRONT_LEFT_CLOSE,
            "Old chap oyna yopilmoqda", "Водительское окно закрывается", "Driver window closing", "WIN_FL_CLOSE"),
        CarCommand.WINDOW_FRONT_RIGHT_OPEN to CommandResult(CarCommand.WINDOW_FRONT_RIGHT_OPEN,
            "Old o'ng oyna ochilmoqda", "Пассажирское окно открывается", "Passenger window opening", "WIN_FR_OPEN"),
        CarCommand.WINDOW_FRONT_RIGHT_CLOSE to CommandResult(CarCommand.WINDOW_FRONT_RIGHT_CLOSE,
            "Old o'ng oyna yopilmoqda", "Пассажирское окно закрывается", "Passenger window closing", "WIN_FR_CLOSE"),
        CarCommand.WINDOW_BACK_LEFT_OPEN to CommandResult(CarCommand.WINDOW_BACK_LEFT_OPEN,
            "Orqa chap oyna ochilmoqda", "Заднее левое окно открывается", "Rear left window opening", "WIN_BL_OPEN"),
        CarCommand.WINDOW_BACK_LEFT_CLOSE to CommandResult(CarCommand.WINDOW_BACK_LEFT_CLOSE,
            "Orqa chap oyna yopilmoqda", "Заднее левое окно закрывается", "Rear left window closing", "WIN_BL_CLOSE"),
        CarCommand.WINDOW_BACK_RIGHT_OPEN to CommandResult(CarCommand.WINDOW_BACK_RIGHT_OPEN,
            "Orqa o'ng oyna ochilmoqda", "Заднее правое окно открывается", "Rear right window opening", "WIN_BR_OPEN"),
        CarCommand.WINDOW_BACK_RIGHT_CLOSE to CommandResult(CarCommand.WINDOW_BACK_RIGHT_CLOSE,
            "Orqa o'ng oyna yopilmoqda", "Заднее правое окно закрывается", "Rear right window closing", "WIN_BR_CLOSE"),
        CarCommand.SUNROOF_OPEN to CommandResult(CarCommand.SUNROOF_OPEN,
            "Lyuk ochilmoqda", "Люк открывается", "Opening sunroof", "SUNROOF_OPEN"),
        CarCommand.SUNROOF_CLOSE to CommandResult(CarCommand.SUNROOF_CLOSE,
            "Lyuk yopilmoqda", "Люк закрывается", "Closing sunroof", "SUNROOF_CLOSE"),
        CarCommand.MUSIC_PLAY to CommandResult(CarCommand.MUSIC_PLAY,
            "Musiqa boshlandi", "Музыка включена", "Music playing", "MUSIC_PLAY"),
        CarCommand.MUSIC_PAUSE to CommandResult(CarCommand.MUSIC_PAUSE,
            "Musiqa to'xtatildi", "Музыка на паузе", "Music paused", "MUSIC_PAUSE"),
        CarCommand.MUSIC_NEXT to CommandResult(CarCommand.MUSIC_NEXT,
            "Keyingi qo'shiq", "Следующий трек", "Next track", "MUSIC_NEXT"),
        CarCommand.MUSIC_PREV to CommandResult(CarCommand.MUSIC_PREV,
            "Oldingi qo'shiq", "Предыдущий трек", "Previous track", "MUSIC_PREV"),
        CarCommand.VOLUME_UP to CommandResult(CarCommand.VOLUME_UP,
            "Ovoz oshirildi", "Громкость увеличена", "Volume up", "VOL_UP"),
        CarCommand.VOLUME_DOWN to CommandResult(CarCommand.VOLUME_DOWN,
            "Ovoz pasaytirildi", "Громкость уменьшена", "Volume down", "VOL_DOWN"),
        CarCommand.VOLUME_MUTE to CommandResult(CarCommand.VOLUME_MUTE,
            "Ovoz o'chirildi", "Звук отключён", "Muted", "VOL_MUTE"),
        CarCommand.AC_ON to CommandResult(CarCommand.AC_ON,
            "Konditsioner yoqildi", "Кондиционер включён", "AC turned on", "AC_ON"),
        CarCommand.AC_OFF to CommandResult(CarCommand.AC_OFF,
            "Konditsioner o'chirildi", "Кондиционер выключен", "AC turned off", "AC_OFF"),
        CarCommand.AC_TEMP_UP to CommandResult(CarCommand.AC_TEMP_UP,
            "Harorat oshirildi", "Температура повышена", "Temperature increased", "AC_TEMP_UP"),
        CarCommand.AC_TEMP_DOWN to CommandResult(CarCommand.AC_TEMP_DOWN,
            "Harorat pasaytirildi", "Температура понижена", "Temperature decreased", "AC_TEMP_DOWN"),
        CarCommand.LIGHTS_ON to CommandResult(CarCommand.LIGHTS_ON,
            "Faralar yoqildi", "Фары включены", "Lights on", "LIGHTS_ON"),
        CarCommand.LIGHTS_OFF to CommandResult(CarCommand.LIGHTS_OFF,
            "Faralar o'chirildi", "Фары выключены", "Lights off", "LIGHTS_OFF"),
        CarCommand.LIGHTS_HIGH to CommandResult(CarCommand.LIGHTS_HIGH,
            "Uzun nurlar yoqildi", "Дальний свет включён", "High beam on", "LIGHTS_HIGH"),
        CarCommand.LIGHTS_LOW to CommandResult(CarCommand.LIGHTS_LOW,
            "Qisqa nurlar yoqildi", "Ближний свет включён", "Low beam on", "LIGHTS_LOW"),
        CarCommand.TRUNK_OPEN to CommandResult(CarCommand.TRUNK_OPEN,
            "Bagaj ochilmoqda", "Багажник открывается", "Trunk opening", "TRUNK_OPEN"),
        CarCommand.TRUNK_CLOSE to CommandResult(CarCommand.TRUNK_CLOSE,
            "Bagaj yopilmoqda", "Багажник закрывается", "Trunk closing", "TRUNK_CLOSE"),
        CarCommand.HORN_BEEP to CommandResult(CarCommand.HORN_BEEP,
            "Signal berildi", "Сигнал подан", "Beep!", "HORN_BEEP"),
        CarCommand.UNKNOWN to CommandResult(CarCommand.UNKNOWN,
            "Buyruq tushunilmadi", "Команда не распознана", "Command not recognized", "")
    )

    fun processCommand(command: CarCommand): CommandResult {
        return responses[command] ?: responses[CarCommand.UNKNOWN]!!
    }

    fun process(spokenText: String): CommandResult {
        val lower = spokenText.lowercase().trim()

        for ((keywords, command) in commandMap) {
            val (uzWords, ruWords, enWords) = keywords
            val allKeywords = uzWords + ruWords + enWords
            if (allKeywords.any { keyword -> lower.contains(keyword.lowercase()) }) {
                return responses[command] ?: responses[CarCommand.UNKNOWN]!!
            }
        }
        return responses[CarCommand.UNKNOWN]!!
    }
}
