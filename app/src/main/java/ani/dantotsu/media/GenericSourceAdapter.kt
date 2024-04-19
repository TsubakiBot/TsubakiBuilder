package ani.dantotsu.media

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import ani.dantotsu.media.anime.Episode
import ani.dantotsu.media.manga.MangaChapter
import ani.dantotsu.parsers.AnimeParser
import ani.dantotsu.parsers.BaseParser
import ani.dantotsu.parsers.MangaParser
import ani.dantotsu.parsers.ShowResponse
import kotlinx.coroutines.CoroutineScope

class GenericSourceAdapter(
    sources: List<ShowResponse>,
    val parser: BaseParser,
    val model: MediaDetailsViewModel,
    fragment: SourceBrowseDialogFragment,
    scope: CoroutineScope
) : SourceBrowseAdapter(sources, fragment, scope) {

    @OptIn(UnstableApi::class)
    override suspend fun onItemClick(context: Context, source: ShowResponse) {
        source.sAnime?.let { anime ->
            val map = mutableMapOf<String, Episode>()
            val extension = with (parser as AnimeParser) {
                loadEpisodes(source.link, source.extra, anime).forEach {
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
                name
            }
            ContextCompat.startActivity(
                context,
                Intent(context, SearchActivity::class.java)
                    .putExtra("type", MediaType.ANIME.asText().uppercase())
                    .putExtra("query", anime.title)
                    .putExtra("search", true)
                    .putExtra("extension", extension),
                null
            )
        } ?: source.sManga?.let { manga ->
            val map = mutableMapOf<String, MangaChapter>()
            val extension = with (parser as MangaParser) {
                loadChapters(source.link, source.extra, manga).forEach {
                    map[it.number] = MangaChapter(it)
                }
                name
            }
            ContextCompat.startActivity(
                context,
                Intent(context, SearchActivity::class.java)
                    .putExtra("type", MediaType.MANGA.asText().uppercase())
                    .putExtra("query", manga.title)
                    .putExtra("search", true)
                    .putExtra("extension", extension),
                null
            )
        }
    }
}