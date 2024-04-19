package ani.dantotsu.media

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
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
            ContextCompat.startActivity(
                context,
                Intent(context, SearchActivity::class.java)
                    .putExtra("type", MediaType.ANIME.asText().uppercase())
                    .putExtra("query", anime.title)
                    .putExtra("search", true)
                    .putExtra("extension", (parser as AnimeParser).name),
                null
            )
        } ?: source.sManga?.let { manga ->
            ContextCompat.startActivity(
                context,
                Intent(context, SearchActivity::class.java)
                    .putExtra("type", MediaType.MANGA.asText().uppercase())
                    .putExtra("query", manga.title)
                    .putExtra("search", true)
                    .putExtra("extension", (parser as MangaParser).name),
                null
            )
        }
    }
}