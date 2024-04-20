package ani.dantotsu.media

interface Type {
    fun asText(): String

    companion object {
        fun fromText(string : String): Type? {
            return when (string.lowercase()) {
                "anime" -> MediaType.ANIME
                "manga" -> MediaType.MANGA
                "novel" -> MediaType.NOVEL
                "torrent" -> AddonType.TORRENT
                "download" -> AddonType.DOWNLOAD
                else -> { null }
            }
        }
    }
}

enum class MediaType: Type {
    ANIME,
    MANGA,
    NOVEL;

    override fun asText(): String {
        return when (this) {
            ANIME -> "Anime"
            MANGA -> "Manga"
            NOVEL -> "Novel"
        }
    }
}

enum class AddonType: Type {
    TORRENT,
    DOWNLOAD;

    override fun asText(): String {
        return when (this) {
            TORRENT -> "Torrent"
            DOWNLOAD -> "Download"
        }
    }
}
