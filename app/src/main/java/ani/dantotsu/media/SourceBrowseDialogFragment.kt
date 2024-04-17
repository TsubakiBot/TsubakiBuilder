package ani.dantotsu.media

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.math.MathUtils.clamp
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.databinding.BottomSheetSourceSearchBinding
import ani.dantotsu.media.anime.AnimeSourceAdapter
import ani.dantotsu.media.manga.MangaSourceAdapter
import ani.dantotsu.navBarHeight
import ani.dantotsu.others.BottomSheetDialogFragment
import ani.dantotsu.parsers.AnimeSources
import ani.dantotsu.parsers.DynamicAnimeParser
import ani.dantotsu.parsers.DynamicMangaParser
import ani.dantotsu.parsers.HAnimeSources
import ani.dantotsu.parsers.HMangaSources
import ani.dantotsu.parsers.MangaSources
import ani.dantotsu.parsers.novel.DynamicNovelParser
import ani.dantotsu.parsers.novel.NovelExtension
import ani.dantotsu.toPx
import ani.dantotsu.tryWithSuspend
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SourceBrowseDialogFragment() : BottomSheetDialogFragment() {
    constructor(extension: AnimeExtension.Installed) : this() {
        mediaType = MediaType.ANIME
        animeExtesnion = extension
    }
    constructor(extension: MangaExtension.Installed) : this() {
        mediaType = MediaType.MANGA
        mangaExtension = extension
    }
    constructor(extension: NovelExtension.Installed) : this() {
        mediaType = MediaType.NOVEL
        novelExtension = extension
    }

    private lateinit var mediaType: MediaType
    private lateinit var animeExtesnion: AnimeExtension.Installed
    private lateinit var mangaExtension: MangaExtension.Installed
    private lateinit var novelExtension: NovelExtension.Installed

    private var _binding: BottomSheetSourceSearchBinding? = null
    private val binding get() = _binding!!
    val model: MediaDetailsViewModel by viewModels()
    private var searched = false
    var anime = true
    var i: Int? = null
    var id: Int? = null
    var media: Media? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSourceSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.mediaListContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin += navBarHeight }

        val scope = requireActivity().lifecycleScope
        val imm =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        CoroutineScope(Dispatchers.IO).launch {
            when (mediaType) {
                MediaType.ANIME -> {
                    Anilist.query.getMedia(16498)?.let { model.loadSelected(it, false) }
                }
                MediaType.MANGA -> {
                    Anilist.query.getMedia(105778)?.let { model.loadSelected(it, false) }
                }
                MediaType.NOVEL -> {

                }
            }
        }

        model.getMedia().observe(viewLifecycleOwner) {
            media = it

            val source = when (mediaType) {
                MediaType.ANIME -> {
                    DynamicAnimeParser(animeExtesnion)
                }
                MediaType.MANGA -> {
                    DynamicMangaParser(mangaExtension)
                }
                MediaType.NOVEL -> {
                    DynamicNovelParser(novelExtension)
                }
            }

            binding.mediaListProgressBar.visibility = View.GONE
            binding.mediaListLayout.visibility = View.VISIBLE

            binding.searchRecyclerView.visibility = View.GONE
            binding.searchProgress.visibility = View.VISIBLE

            fun search() {
                binding.searchBarText.clearFocus()
                imm.hideSoftInputFromWindow(binding.searchBarText.windowToken, 0)
                scope.launch {
                    model.responses.postValue(
                        withContext(Dispatchers.IO) {
                            tryWithSuspend {
                                source.search(binding.searchBarText.text.toString())
                            }
                        }
                    )
                }
            }
            binding.searchSourceTitle.text = source.name
            binding.searchBarText.setText(media?.mangaName() ?: "")
            binding.searchBarText.setOnEditorActionListener { _, actionId, _ ->
                return@setOnEditorActionListener when (actionId) {
                    EditorInfo.IME_ACTION_SEARCH -> {
                        search()
                        true
                    }

                    else -> false
                }
            }
            binding.searchBar.setEndIconOnClickListener { search() }
            if (!searched) search()
            searched = true
            model.responses.observe(viewLifecycleOwner) { j ->
                if (j != null) {
                    binding.searchRecyclerView.visibility = View.VISIBLE
                    binding.searchProgress.visibility = View.GONE
                    binding.searchRecyclerView.adapter = SourceBrowseAdapter(j, this, scope)
                    binding.searchRecyclerView.layoutManager = GridLayoutManager(
                        requireActivity(),
                        clamp(
                            requireActivity().resources.displayMetrics.widthPixels / 124.toPx,
                            1,
                            4
                        )
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun dismiss() {
        model.responses.value = null
        super.dismiss()
    }
}