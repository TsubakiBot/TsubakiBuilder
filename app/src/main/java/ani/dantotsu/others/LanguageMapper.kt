package ani.dantotsu.others

import ani.dantotsu.R
import bit.himitsu.Strings.getString
import bit.himitsu.flagEmoji
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.MangaSource
import java.util.Locale

object LanguageMapper {
    private fun getLocalFromCode(code: String): Locale? {
        return when {
            code.contains("-") -> {
                val parts = code.split("-")
                try {
                    Locale(parts[0].lowercase(), parts[1].uppercase())
                } catch (ignored: Exception) {
                    null
                }
            }
            code.contains("_") -> {
                val parts = code.split("_")
                try {
                    Locale(parts[0].lowercase(), parts[1].uppercase())
                } catch (ignored: Exception) {
                    null
                }
            }
            else -> {
                try {
                    Locale(code.lowercase())
                } catch (ignored: Exception) {
                    null
                }
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

    private fun findByDisplayName(locale: Locale): String? {
        val language = locale.getDisplayName(Locale.US).substringBefore(" ")
        return codeMap.filterValues { it == language }.keys.firstOrNull()
    }

    fun getTrackItem(code: String): String? {
        return getLocalFromCode(code)?.let { locale ->
            if (locale.language == locale.displayName) {
                mapNativeToCode(code)?.let { getTrackItem(it) }
                    ?: findByDisplayName(locale)?.let { getTrackItem(it) }
            } else {
                "[${locale.flagEmoji ?: locale.language}] ${locale.displayName}"
            }
        }
    }

    fun mapNativeToCode(name: String): String? {
        return when (name.lowercase()) {
            "العربية" -> "ar"
            "中文, 汉语, 漢語" -> "zh"
            "english" -> "en"
            "français" -> "fr"
            "bahasa indonesia" -> "id"
            "日本語" -> "ja"
            "조선말, 한국어" -> "ko"
            "polski" -> "pl"
            "português" -> "pt"
            "pусский" -> "ru"
            "español" -> "es"
            "ไทย" -> "th"
            "türkçe" -> "tr"
            "Українська" -> "uk"
            "tiếng việt" -> "vi"
            else -> null
        }
    }

    val codeMap: Map<String, String> = mapOf(
        "all" to "Multi",
        "af" to "Afrikaans",
        "am" to "Amharic",
        "ar" to "Arabic",
        "as" to "Assamese",
        "az" to "Azerbaijani",
        "be" to "Belarusian",
        "bg" to "Bulgarian",
        "bn" to "Bengali",
        "bs" to "Bosnian",
        "ca" to "Catalan",
        "ceb" to "Cebuano",
        "cs" to "Czech",
        "da" to "Danish",
        "de" to "German",
        "el" to "Greek",
        "en" to "English",
        "en-Us" to "English (United States)",
        "eo" to "Esperanto",
        "es" to "Spanish",
        "es-419" to "Spanish (Latin America)",
        "es-ES" to "Spanish (Spain)",
        "et" to "Estonian",
        "eu" to "Basque",
        "fa" to "Persian",
        "fi" to "Finnish",
        "fil" to "Filipino",
        "fo" to "Faroese",
        "fr" to "French",
        "ga" to "Irish",
        "gn" to "Guarani",
        "gu" to "Gujarati",
        "ha" to "Hausa",
        "he" to "Hebrew",
        "hi" to "Hindi",
        "hr" to "Croatian",
        "ht" to "Haitian Creole",
        "hu" to "Hungarian",
        "hy" to "Armenian",
        "id" to "Indonesian",
        "ig" to "Igbo",
        "is" to "Icelandic",
        "it" to "Italian",
        "ja" to "Japanese",
        "jv" to "Javanese",
        "ka" to "Georgian",
        "kk" to "Kazakh",
        "km" to "Khmer",
        "kn" to "Kannada",
        "ko" to "Korean",
        "ku" to "Kurdish",
        "ky" to "Kyrgyz",
        "la" to "Latin",
        "lb" to "Luxembourgish",
        "lo" to "Lao",
        "lt" to "Lithuanian",
        "lv" to "Latvian",
        "mg" to "Malagasy",
        "mi" to "Maori",
        "mk" to "Macedonian",
        "ml" to "Malayalam",
        "mn" to "Mongolian",
        "mo" to "Moldovan",
        "mr" to "Marathi",
        "ms" to "Malay",
        "mt" to "Maltese",
        "my" to "Burmese",
        "ne" to "Nepali",
        "nl" to "Dutch",
        "no" to "Norwegian",
        "ny" to "Chichewa",
        "pl" to "Polish",
        "pt" to "Portuguese",
        "pt-BR" to "Portuguese (Brazil)",
        "pt-PT" to "Portuguese (Portugal)",
        "ps" to "Pashto",
        "ro" to "Romanian",
        "rm" to "Romansh",
        "ru" to "Russian",
        "sd" to "Sindhi",
        "sh" to "Serbo-Croatian",
        "si" to "Sinhala",
        "sk" to "Slovak",
        "sl" to "Slovenian",
        "sm" to "Samoan",
        "sn" to "Shona",
        "so" to "Somali",
        "sq" to "Albanian",
        "sr" to "Serbian",
        "st" to "Southern Sotho",
        "sv" to "Swedish",
        "sw" to "Swahili",
        "ta" to "Tamil",
        "te" to "Telugu",
        "tg" to "Tajik",
        "th" to "Thai",
        "ti" to "Tigrinya",
        "tk" to "Turkmen",
        "tl" to "Tagalog",
        "to" to "Tongan",
        "tr" to "Turkish",
        "uk" to "Ukrainian",
        "ur" to "Urdu",
        "uz" to "Uzbek",
        "vi" to "Vietnamese",
        "yo" to "Yoruba",
        "zh" to "Chinese",
        "zh-Hans" to "Chinese (Simplified)",
        "zh-Hant" to "Chinese (Traditional)",
        "zh-Habt" to "Chinese (Hakka)",
        "zu" to "Zulu"
    )
}

