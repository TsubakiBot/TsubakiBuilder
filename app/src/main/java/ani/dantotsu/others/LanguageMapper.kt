package ani.dantotsu.others

import ani.dantotsu.R
import ani.dantotsu.Strings.getString
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
        if (code == "all") return getString(R.string.multi)
        val locale = getLocalFromCode(code)
        return locale?.displayName ?: code
    }

    fun mapLanguageNameToCode(name: String): String? {
        return when (name.lowercase()) {
            "العربية" -> Language.ARABIC.code
            "中文, 汉语, 漢語" -> Language.CHINESE.code
            "english" -> Language.ENGLISH.code
            "français" -> Language.FRENCH.code
            "bahasa indonesia" -> Language.INDONESIAN.code
            "日本語" -> Language.JAPANESE.code
            "조선말, 한국어" -> Language.KOREAN.code
            "polski" -> Language.POLISH.code
            "português" -> Language.PORTUGUESE_BRAZIL.code
            "pусский" -> Language.RUSSIAN.code
            "español" -> Language.SPANISH.code
            "ไทย" -> Language.THAI.code
            "türkçe" -> Language.TURKISH.code
            "Українська" -> Language.UKRAINIAN.code
            "tiếng việt" -> Language.VIETNAMESE.code
            else -> null
        }
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
        return getLocalFromCode(code)?.let { locale ->
            if (locale.language == locale.displayName) {
                mapLanguageNameToCode(code)?.let { getLanguageItem(it) }
            } else {
                "[${locale.language}] ${locale.displayName}"
            }
        }
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
        CHINESE_SIMPLIFIED("zh-Hans");
    }
}

