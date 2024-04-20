package ani.dantotsu.others

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.MangaSource
import java.util.Locale

object LanguageMapper {
    private fun getLocalFromCode(code: String): Locale? {
        return if (code.contains("-")) {
            val parts = code.split("-")
            try {
                Locale(parts[0].lowercase(), parts[1].uppercase())
            } catch (ignored: Exception) {
                null
            }
        } else {
            try {
                Locale(code.lowercase())
            } catch (ignored: Exception) {
                null
            }
        }
    }

    fun mapLanguageCodeToName(code: String): String {
        if (code == "all") return "Multi"
        val locale = getLocalFromCode(code)
        return locale?.displayName ?: code
    }

    fun getExtensionItem(source: ConfigurableAnimeSource): String {
        return "${mapLanguageCodeToName(source.lang)}: ${source.name}"
    }

    fun getExtensionItem(source: AnimeSource): String {
        return "${mapLanguageCodeToName(source.lang)}: ${source.name}"
    }

    fun getExtensionItem(source: ConfigurableSource): String {
        return "${mapLanguageCodeToName(source.lang)}: ${source.name}"
    }

    fun getExtensionItem(source: MangaSource): String {
        return "${mapLanguageCodeToName(source.lang)}: ${source.name}"
    }

    fun getLanguageItem(code: String): String? {
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

