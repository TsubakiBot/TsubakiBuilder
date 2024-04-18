package ani.dantotsu.media

import androidx.annotation.OptIn
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.util.UnstableApi
import ani.dantotsu.media.anime.Episode
import ani.dantotsu.media.manga.MangaChapter
import ani.dantotsu.parsers.AnimeParser
import ani.dantotsu.parsers.BaseParser
import ani.dantotsu.parsers.MangaParser
import ani.dantotsu.parsers.ShowResponse
import ani.dantotsu.util.Logger
import kotlinx.coroutines.CoroutineScope

class GenericSourceAdapter(
    sources: List<ShowResponse>,
    val parser: BaseParser,
    val model: MediaDetailsViewModel,
    fragment: SourceBrowseDialogFragment,
    scope: CoroutineScope
) : SourceBrowseAdapter(sources, fragment, scope) {

    @OptIn(UnstableApi::class)
    override suspend fun onItemClick(source: ShowResponse) {
        source.sAnime?.let { anime ->
            val map = mutableMapOf<String, Episode>()
            val animeParser = parser as AnimeParser
            animeParser.loadEpisodes(source.link, source.extra, anime).forEach {
                map[it.number] = Episode(
                    it.number,
                    it.link,
                    it.title,
                    it.description,
                    it.thumbnail,
                    it.isFiller,
                    extra = it.extra,
                    sEpisode = it.sEpisode
                )
            }
            map.forEach {
                Logger.log(it.key)
            }
        } ?: source.sManga?.let { manga ->
            val map = mutableMapOf<String, MangaChapter>()
            val mangaParser = parser as MangaParser
            mangaParser.loadChapters(source.link, source.extra, manga).forEach {
                map[it.number] = MangaChapter(it)
            }
            map.forEach {
                Logger.log(it.key)
            }
        }
    }
}