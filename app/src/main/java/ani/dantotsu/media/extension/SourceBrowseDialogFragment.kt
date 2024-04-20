package ani.dantotsu.media.extension

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.math.MathUtils.clamp
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import ani.dantotsu.R
import ani.dantotsu.databinding.BottomSheetSourceSearchBinding
import ani.dantotsu.media.MediaDetailsViewModel
import ani.dantotsu.media.MediaType
import ani.dantotsu.navBarHeight
import ani.dantotsu.others.BottomSheetDialogFragment
import ani.dantotsu.parsers.AnimeParser
import ani.dantotsu.parsers.AnimeSources
import ani.dantotsu.parsers.BaseParser
import ani.dantotsu.parsers.DynamicAnimeParser
import ani.dantotsu.parsers.DynamicMangaParser
import ani.dantotsu.parsers.MangaParser
import ani.dantotsu.parsers.MangaSources
import ani.dantotsu.parsers.ShowResponse
import ani.dantotsu.parsers.novel.DynamicNovelParser
import ani.dantotsu.parsers.novel.NovelExtension
import ani.dantotsu.toPx
import ani.dantotsu.tryWithSuspend
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    constructor(search: String) : this() {
        incomingQuery = search
    }

    private var incomingQuery = ""
    private lateinit var mediaType: MediaType
    private lateinit var animeExtesnion: AnimeExtension.Installed
    private lateinit var mangaExtension: MangaExtension.Installed
    private lateinit var novelExtension: NovelExtension.Installed

    private var _binding: BottomSheetSourceSearchBinding? = null
    private val binding get() = _binding!!
    val model: MediaDetailsViewModel by viewModels()
    private var searched = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSourceSearchBinding.inflate(inflater, container, false)
        dialog?.window?.statusBarColor = Color.TRANSPARENT
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.mediaListContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin += navBarHeight }

        val scope = requireActivity().lifecycleScope
        val imm =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        model.getMedia().observe(viewLifecycleOwner) {

            binding.mediaListProgressBar.visibility = View.GONE
            binding.mediaListLayout.visibility = View.VISIBLE

            binding.searchRecyclerView.visibility = View.GONE
            binding.searchProgress.visibility = View.VISIBLE

            if (incomingQuery.isNotBlank()) {
                binding.searchSourceTitle.text = getString(R.string.extension_search)
                val allResults = hashMapOf<BaseParser, List<ShowResponse>?>()
                binding.searchBar.isVisible = false
                scope.launch {
                    withContext(Dispatchers.IO) {
                        AnimeSources.list.take(AnimeSources.names.size - 1).map {
                            async {
                                val animeParser = it.get.value as AnimeParser
                                tryWithSuspend {
                                    allResults.put(animeParser, animeParser.search(incomingQuery))
                                }
                            }
                        }.awaitAll()
                        MangaSources.list.take(MangaSources.names.size - 1).map {
                            async {
                                val mangaParser = it.get.value as MangaParser
                                tryWithSuspend {
                                    allResults.put(mangaParser, mangaParser.search(incomingQuery))
                                }
                            }
                        }.awaitAll()
                        withContext(scope.coroutineContext) {
                            model.collections.postValue(allResults)
                        }
                    }
                }

                model.collections.observe(viewLifecycleOwner) { collections ->
                    collections?.let {
                        val adapter = ConcatAdapter().apply {
                            it.forEach {
                                it.value?.let { results ->
                                    addAdapter(
                                        SourceBrowserAdapter(
                                            results,
                                            it.key,
                                            model,
                                            this@SourceBrowseDialogFragment,
                                            scope
                                        )
                                    )
                                }
                            }
                        }

                        binding.searchRecyclerView.visibility = View.VISIBLE
                        binding.searchProgress.visibility = View.GONE
                        binding.searchRecyclerView.adapter = adapter
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
            } else {
                val parser = when (mediaType) {
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

                fun search(query: String? = null) {
                    binding.searchBarText.clearFocus()
                    imm.hideSoftInputFromWindow(binding.searchBarText.windowToken, 0)
                    scope.launch {
                        model.responses.postValue(
                            withContext(Dispatchers.IO) {
                                tryWithSuspend {
                                    parser.search(query ?: binding.searchBarText.text.toString())
                                }
                            }
                        )
                    }
                }

                binding.searchSourceTitle.text = parser.name
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
                model.responses.observe(viewLifecycleOwner) { results ->
                    results?.let {
                        binding.searchRecyclerView.visibility = View.VISIBLE
                        binding.searchProgress.visibility = View.GONE
                        binding.searchRecyclerView.adapter =
                            SourceBrowserAdapter(it, parser, model, this, scope)
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
