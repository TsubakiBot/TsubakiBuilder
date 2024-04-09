package ani.dantotsu.others

import java.util.Locale

object LanguageMapper {
    private fun getLocalFromCode(code: String): Locale? {
        return if (code.contains("-")) {
            val parts = code.split("-")
            try {
                Locale(parts[0], parts[1])
            } catch (ignored: Exception) { null }
        } else {
            try {
                Locale(code)
            } catch (ignored: Exception) { null }
        }
    }

    fun mapLanguageCodeToName(code: String): String {
        if (code == "all" ) return "Multi"
        val locale = getLocalFromCode(code)
        return locale?.displayName ?: code
    }

    fun getLanguageListItem(code: String): String? {
        val locale = getLocalFromCode(code)
        return locale?.let { "[${it.language}] ${it.displayName}" }
    }

    enum class Language(val code: String) {
        ALL("all"),
        ARABIC("ar"),
        GERMAN("de"),
        ENGLISH("en"),
        SPANISH("es"),
        FRENCH("fr"),
        INDONESIAN("id"),
        ITALIAN("it"),
        JAPANESE("ja"),
        KOREAN("ko"),
        POLISH("pl"),
        PORTUGUESE_BRAZIL("pt-BR"),
        RUSSIAN("ru"),
        THAI("th"),
        TURKISH("tr"),
        UKRAINIAN("uk"),
        VIETNAMESE("vi"),
        CHINESE("zh"),
        CHINESE_SIMPLIFIED("zh-Hans")
    }
}

