package ani.dantotsu.settings.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.databinding.FragmentExtensionsBinding
import ani.dantotsu.databinding.ItemExtensionBinding
import ani.dantotsu.others.LanguageMapper
import ani.dantotsu.others.webview.PluginBottomDialog
import ani.dantotsu.parsers.novel.NovelExtension
import ani.dantotsu.parsers.novel.NovelExtensionManager
import ani.dantotsu.settings.SearchQueryHandler
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class NovelPluginsFragment : Fragment(), SearchQueryHandler {
    private var _binding: FragmentExtensionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var pluginsRecyclerView: RecyclerView
    private val skipIcons: Boolean = PrefManager.getVal(PrefName.SkipExtensionIcons)
    private val novelExtensionManager: NovelExtensionManager = Injekt.get()
    private val pluginsAdapter = NovelPluginsAdapter(
        { plugin ->
            if (isAdded) {  // Check if the fragment is currently added to its activity
                if (plugin.pkgName.startsWith("plugin:")) {
                    parentFragmentManager.let {
                        PluginBottomDialog.newInstance(plugin.sources[0].baseUrl).apply {
                            show(it, "dialog")
                        }
                    }
                }
            }
        },
        skipIcons
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExtensionsBinding.inflate(inflater, container, false)

        pluginsRecyclerView = binding.allExtensionsRecyclerView
        pluginsRecyclerView.isNestedScrollingEnabled = false
        pluginsRecyclerView.adapter = pluginsAdapter
        pluginsRecyclerView.layoutManager = LinearLayoutManager(context)

        lifecycleScope.launch {
            novelExtensionManager.availablePluginsFlow.collect { plugins ->
                pluginsAdapter.updateData(plugins)
            }
        }

        return binding.root
    }

    override fun updateContentBasedOnQuery(query: String?) {
        pluginsAdapter.filter(
            query,
            novelExtensionManager.availablePluginsFlow.value
        )
    }

    override fun notifyDataChanged() {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class NovelPluginsAdapter(
        private val onViewerClicked: (NovelExtension.Plugin) -> Unit,
        val skipIcons: Boolean
    ) : ListAdapter<NovelExtension.Plugin, NovelPluginsAdapter.ViewHolder>(
        DIFF_CALLBACK
    ) {

        fun updateData(newPlugins: List<NovelExtension.Plugin>) {
            submitList(newPlugins)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemExtensionBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ).apply {
                extensionPinImageView.isGone = true
                searchImageView.isGone = true
                settingsImageView.isGone = true
                closeTextView.setImageResource(R.drawable.ic_globe_24)
            }
            return ViewHolder(binding.root)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val plugin = getItem(position)
            if (plugin != null) {
                val lang = LanguageMapper.mapLanguageCodeToName(plugin.lang)
                holder.extensionNameTextView.text = plugin.name
                val text = "$lang ${plugin.versionName}"
                holder.extensionVersionTextView.text = text
                if (!skipIcons) {
                    Glide.with(holder.itemView.context)
                        .load(plugin.iconUrl)
                        .into(holder.extensionIconImageView)
                }
                holder.closeTextView.setOnClickListener {
                    onViewerClicked(plugin)
                }
            }
        }

        fun filter(query: String?, currentList: List<NovelExtension.Plugin>) {
            val filteredList = if (!query.isNullOrBlank()) {
                currentList.filter { it.name.lowercase().contains(query.lowercase()) }
            } else { currentList }
            if (filteredList != currentList) submitList(filteredList)
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val extensionNameTextView: TextView = view.findViewById(R.id.extensionNameTextView)
            val extensionVersionTextView: TextView =
                view.findViewById(R.id.extensionVersionTextView)
            val extensionIconImageView: ImageView = view.findViewById(R.id.extensionIconImageView)
            val closeTextView: ImageView = view.findViewById(R.id.closeTextView)
        }

        companion object {
            val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NovelExtension.Plugin>() {
                override fun areItemsTheSame(
                    oldItem: NovelExtension.Plugin,
                    newItem: NovelExtension.Plugin
                ): Boolean {
                    return oldItem.pkgName == newItem.pkgName
                }

                override fun areContentsTheSame(
                    oldItem: NovelExtension.Plugin,
                    newItem: NovelExtension.Plugin
                ): Boolean {
                    return oldItem == newItem
                }
            }
        }
    }
}