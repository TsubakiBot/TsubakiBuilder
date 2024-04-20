package ani.dantotsu.media.extension

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import ani.dantotsu.media.MediaDetailsViewModel
import ani.dantotsu.media.SearchActivity
import ani.dantotsu.parsers.BaseParser
import ani.dantotsu.parsers.ShowResponse
import kotlinx.coroutines.CoroutineScope

class SourceBrowserAdapter(
    sources: List<ShowResponse>,
    val parser: BaseParser,
    val model: MediaDetailsViewModel,
    fragment: SourceBrowseDialogFragment,
    scope: CoroutineScope
) : GenericSourceBrowseAdapter(sources, fragment, scope) {

    @OptIn(UnstableApi::class)
    override suspend fun onItemClick(context: Context, source: ShowResponse) {
        source.sAnime?.let { anime ->
            ContextCompat.startActivity(
                context,
                Intent(context, SearchActivity::class.java)
                    .putExtra("type", "ANIME")
                    .putExtra("query", anime.title)
                    .putExtra("search", true)
                    .putExtra("extension", parser.name),
                null
            )
        } ?: source.sManga?.let { manga ->
            ContextCompat.startActivity(
                context,
                Intent(context, SearchActivity::class.java)
                    .putExtra("type", "MANGA")
                    .putExtra("query", manga.title)
                    .putExtra("search", true)
                    .putExtra("extension", parser.name),
                null
            )
        }
    }
}
