package ani.dantotsu.settings.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.databinding.FragmentExtensionsBinding
import ani.dantotsu.others.webview.PluginBottomDialog
import ani.dantotsu.parsers.novel.NovelExtension
import ani.dantotsu.parsers.novel.NovelExtensionManager
import ani.dantotsu.settings.SearchQueryHandler
import ani.dantotsu.settings.paging.NovelPluginsAdapter
import ani.dantotsu.settings.paging.NovelPluginsViewModel
import ani.dantotsu.settings.paging.NovelPluginssViewModelFactory
import ani.dantotsu.settings.paging.OnNovelViewClickListener
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class NovelPluginsFragment : Fragment(),
    SearchQueryHandler, OnNovelViewClickListener {
    private var _binding: FragmentExtensionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NovelPluginsViewModel by viewModels {
        NovelPluginssViewModelFactory(novelExtensionManager)
    }

    private val adapter by lazy {
        NovelPluginsAdapter(this)
    }

    private val novelExtensionManager: NovelExtensionManager = Injekt.get()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExtensionsBinding.inflate(inflater, container, false)

        binding.allExtensionsRecyclerView.isNestedScrollingEnabled = false
        binding.allExtensionsRecyclerView.adapter = adapter
        binding.allExtensionsRecyclerView.layoutManager = LinearLayoutManager(context)
        (binding.allExtensionsRecyclerView.layoutManager as LinearLayoutManager).isItemPrefetchEnabled =
            true

        lifecycleScope.launch {
            viewModel.pagerFlow.collectLatest { pagingData ->
                adapter.submitData(pagingData)
            }
        }


        viewModel.invalidatePager() // Force a refresh of the pager
        return binding.root
    }

    override fun updateContentBasedOnQuery(query: String?) {
        viewModel.setSearchQuery(query ?: "")
    }

    override fun notifyDataChanged() {
        viewModel.invalidatePager()
    }

    override fun onViewClick(plugin: NovelExtension.Plugin) {
        if (isAdded) {  // Check if the fragment is currently added to its activity
            if (plugin.pkgName.startsWith("plugin:")) {
                parentFragmentManager.let {
                    PluginBottomDialog.newInstance(plugin.sources[0].baseUrl).apply {
                        show(it, "dialog")
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView();_binding = null
    }
}