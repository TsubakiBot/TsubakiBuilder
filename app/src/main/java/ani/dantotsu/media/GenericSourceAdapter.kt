package ani.dantotsu.media

import ani.dantotsu.media.MediaDetailsViewModel
import ani.dantotsu.media.SourceAdapter
import ani.dantotsu.media.SourceSearchDialogFragment
import ani.dantotsu.parsers.ShowResponse
import kotlinx.coroutines.CoroutineScope

class GenericSourceAdapter(
    sources: List<ShowResponse>,
    val model: MediaDetailsViewModel,
    private val mediaType: MediaType,
    fragment: SourceBrowseDialogFragment,
    scope: CoroutineScope
) : SourceBrowseAdapter(sources, fragment, scope) {
    override suspend fun onItemClick(source: ShowResponse) {
        when (mediaType) {
            MediaType.ANIME -> {

            }
            MediaType.MANGA -> {

            }
            MediaType.NOVEL -> {

            }
        }
    }
}