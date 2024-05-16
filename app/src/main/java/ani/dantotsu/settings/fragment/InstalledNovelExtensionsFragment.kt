package ani.dantotsu.settings.fragment

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.databinding.FragmentExtensionsBinding
import ani.dantotsu.databinding.ItemExtensionBinding
import ani.dantotsu.others.LanguageMapper
import ani.dantotsu.parsers.NovelSources
import ani.dantotsu.parsers.novel.NovelExtension
import ani.dantotsu.parsers.novel.NovelExtensionManager
import ani.dantotsu.settings.SearchQueryHandler
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.util.Logger
import ani.himitsu.extension.ReverseSearchDialogFragment
import eu.kanade.tachiyomi.data.notification.Notifications
import kotlinx.coroutines.launch
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Collections

class InstalledNovelExtensionsFragment : Fragment(), SearchQueryHandler {
    private var _binding: FragmentExtensionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var extensionsRecyclerView: RecyclerView
    private val skipIcons: Boolean = PrefManager.getVal(PrefName.SkipExtensionIcons)
    private val novelExtensionManager: NovelExtensionManager = Injekt.get()
    private val extensionsAdapter = NovelExtensionsAdapter(
        { _ ->
            Toast.makeText(requireContext(), "Source is not configurable", Toast.LENGTH_SHORT)
                .show()
        },
        { pkg, forceDelete ->
            if (isAdded) {  // Check if the fragment is currently added to its activity
                val context = requireContext()  // Store context in a variable
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager  // Initialize NotificationManager once

                if (pkg.hasUpdate && !forceDelete) {
                    novelExtensionManager.updateExtension(pkg)
                        .observeOn(AndroidSchedulers.mainThread())  // Observe on main thread
                        .subscribe(
                            { installStep ->
                                val builder = NotificationCompat.Builder(
                                    context,
                                    Notifications.CHANNEL_DOWNLOADER_PROGRESS
                                )
                                    .setSmallIcon(R.drawable.ic_round_sync_24)
                                    .setContentTitle("Updating extension")
                                    .setContentText("Step: $installStep")
                                    .setPriority(NotificationCompat.PRIORITY_LOW)
                                notificationManager.notify(1, builder.build())
                            },
                            { error ->
                                Logger.log(error)  // Log the error
                                val builder = NotificationCompat.Builder(
                                    context,
                                    Notifications.CHANNEL_DOWNLOADER_ERROR
                                )
                                    .setSmallIcon(R.drawable.ic_round_info_24)
                                    .setContentTitle("Update failed: ${error.message}")
                                    .setContentText("Error: ${error.message}")
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                notificationManager.notify(1, builder.build())
                                snackString("Update failed: ${error.message}")
                            },
                            {
                                val builder = NotificationCompat.Builder(
                                    context,
                                    Notifications.CHANNEL_DOWNLOADER_PROGRESS
                                )
                                    .setSmallIcon(R.drawable.ic_check)
                                    .setContentTitle("Update complete")
                                    .setContentText("The extension has been successfully updated.")
                                    .setPriority(NotificationCompat.PRIORITY_LOW)
                                notificationManager.notify(1, builder.build())
                                snackString("Extension updated")
                            }
                        )
                } else {
                    novelExtensionManager.uninstallExtension(pkg.pkgName)
                    snackString("Extension uninstalled")
                }
            }
        },
        { extension ->
            ReverseSearchDialogFragment(extension).show(
                requireActivity().supportFragmentManager, null
            )
        },
        skipIcons
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExtensionsBinding.inflate(inflater, container, false)

        extensionsRecyclerView = binding.allExtensionsRecyclerView
        extensionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        extensionsRecyclerView.adapter = extensionsAdapter

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val newList = extensionsAdapter.currentList.toMutableList()
                val fromPosition = viewHolder.absoluteAdapterPosition
                val toPosition = target.absoluteAdapterPosition
                if (fromPosition < toPosition) { //probably need to switch to a recyclerview adapter
                    for (i in fromPosition until toPosition) {
                        Collections.swap(newList, i, i + 1)
                    }
                } else {
                    for (i in fromPosition downTo toPosition + 1) {
                        Collections.swap(newList, i, i - 1)
                    }
                }
                extensionsAdapter.submitList(newList)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.elevation = 8f
                    viewHolder?.itemView?.translationZ = 8f
                }
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                extensionsAdapter.updatePref()
                viewHolder.itemView.elevation = 0f
                viewHolder.itemView.translationZ = 0f
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(extensionsRecyclerView)


        lifecycleScope.launch {
            novelExtensionManager.installedExtensionsFlow.collect { extensions ->
                extensionsAdapter.updateData(sortToNovelSourcesList(extensions))
            }
        }
        return binding.root
    }

    private fun sortToNovelSourcesList(inpt: List<NovelExtension.Installed>): List<NovelExtension.Installed> {
        val sourcesMap = inpt.associateBy { it.name }
        val orderedSources = NovelSources.pinnedNovelSources.mapNotNull { name ->
            sourcesMap[name]
        }
        return orderedSources + inpt.filter { !NovelSources.pinnedNovelSources.contains(it.name) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun updateContentBasedOnQuery(query: String?) {
        extensionsAdapter.filter(
            query,
            sortToNovelSourcesList(novelExtensionManager.installedExtensionsFlow.value)
        )
    }

    override fun notifyDataChanged() {}

    private class NovelExtensionsAdapter(
        private val onSettingsClicked: (NovelExtension.Installed) -> Unit,
        private val onUninstallClicked: (NovelExtension.Installed, Boolean) -> Unit,
        private val onSearchClicked: (NovelExtension.Installed) -> Unit,
        val skipIcons: Boolean
    ) : ListAdapter<NovelExtension.Installed, NovelExtensionsAdapter.ViewHolder>(
        DIFF_CALLBACK_INSTALLED
    ) {

        fun updateData(newExtensions: List<NovelExtension.Installed>) {
            submitList(newExtensions)
        }

        fun updatePref() {
            val map = currentList.map { it.name }
            PrefManager.setVal(PrefName.NovelSourcesOrder, map)
            NovelSources.pinnedNovelSources = map
            NovelSources.performReorderNovelSources()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemExtensionBinding.inflate(
                LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding.root)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val extension = getItem(position)  // Use getItem() from ListAdapter
            val nsfw = ""
            val lang = LanguageMapper.mapLanguageCodeToName("all")
            holder.extensionNameTextView.text = extension.name
            val versionText = "$lang ${extension.versionName} $nsfw"
            holder.extensionVersionTextView.text = versionText
            if (!skipIcons) {
                holder.extensionIconImageView.setImageDrawable(extension.icon)
            }
            if (extension.hasUpdate) {
                holder.closeTextView.setImageResource(R.drawable.ic_round_sync_24)
            } else {
                holder.closeTextView.setImageResource(R.drawable.ic_round_delete_24)
            }
            holder.closeTextView.setOnClickListener {
                onUninstallClicked(extension, false)
            }
            holder.settingsImageView.setOnClickListener {
                onSettingsClicked(extension)
            }
            holder.closeTextView.setOnLongClickListener {
                onUninstallClicked(extension, true)
                false
            }
            holder.searchImageView.setOnClickListener {
                onSearchClicked(extension)
            }
        }

        fun filter(query: String?, currentList: List<NovelExtension.Installed>) {
            val filteredList = if (!query.isNullOrBlank()) {
                currentList.filter { it.name.lowercase().contains(query.lowercase()) }
            } else { currentList }
            if (filteredList != currentList) submitList(filteredList)
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val extensionNameTextView: TextView = view.findViewById(R.id.extensionNameTextView)
            val extensionVersionTextView: TextView =
                view.findViewById(R.id.extensionVersionTextView)
            val settingsImageView: ImageView = view.findViewById(R.id.settingsImageView)
            val extensionIconImageView: ImageView = view.findViewById(R.id.extensionIconImageView)
            val closeTextView: ImageView = view.findViewById(R.id.closeTextView)
            val searchImageView: ImageView = view.findViewById(R.id.searchImageView)
        }

        companion object {
            val DIFF_CALLBACK_INSTALLED =
                object : DiffUtil.ItemCallback<NovelExtension.Installed>() {
                override fun areItemsTheSame(
                    oldItem: NovelExtension.Installed,
                    newItem: NovelExtension.Installed
                ): Boolean {
                    return oldItem.pkgName == newItem.pkgName
                }

                override fun areContentsTheSame(
                    oldItem: NovelExtension.Installed,
                    newItem: NovelExtension.Installed
                ): Boolean {
                    return oldItem == newItem
                }
            }
        }
    }
}