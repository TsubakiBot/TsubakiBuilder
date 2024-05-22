package ani.dantotsu.others

import ani.dantotsu.R
import bit.himitsu.Strings.getString
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

    fun mapNativeNameToCode(name: String): String? {
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


    fun getLanguageItem(code: String): String? {
        return getLocalFromCode(code)?.let { locale ->
            if (locale.language == locale.displayName) {
                mapNativeNameToCode(code)?.let { getLanguageItem(it) }
            } else {
                "[${locale.language}] ${locale.displayName}"
            }
        }
    }

    val subLanguages = arrayOf(
        "Albanian",
        "Arabic",
        "Bosnian",
        "Bulgarian",
        "Chinese",
        "Croatian",
        "Czech",
        "Danish",
        "Dutch",
        "English",
        "Estonian",
        "Finnish",
        "French",
        "Georgian",
        "German",
        "Greek",
        "Hebrew",
        "Hindi",
        "Indonesian",
        "Irish",
        "Italian",
        "Japanese",
        "Korean",
        "Lithuanian",
        "Luxembourgish",
        "Macedonian",
        "Mongolian",
        "Norwegian",
        "Polish",
        "Portuguese",
        "Punjabi",
        "Romanian",
        "Russian",
        "Serbian",
        "Slovak",
        "Slovenian",
        "Spanish",
        "Turkish",
        "Ukrainian",
        "Urdu",
        "Vietnamese",
    )

    fun getLanguageCode(language: String): CharSequence {
        Locale.getAvailableLocales().forEach { locale ->
            if (locale.displayLanguage.equals(language, ignoreCase = true)) {
                return locale.language

            }
        }
        return "null"
    }

    fun getLanguageName(language: String): String? {
        Locale.getAvailableLocales().forEach { locale ->
            if (locale.language.equals(language, ignoreCase = true)) {
                return locale.displayLanguage
            }
        }
        return null
    }
}

