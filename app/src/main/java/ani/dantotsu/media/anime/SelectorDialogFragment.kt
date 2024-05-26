package ani.dantotsu.media.anime

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.addons.download.DownloadAddonManager
import ani.dantotsu.copyToClipboard
import ani.dantotsu.currActivity
import ani.dantotsu.currContext
import ani.dantotsu.databinding.BottomSheetSelectorBinding
import ani.dantotsu.databinding.ItemStreamBinding
import ani.dantotsu.databinding.ItemUrlBinding
import ani.dantotsu.download.DownloadedType
import ani.dantotsu.download.video.Helper
import ani.dantotsu.hideSystemBars
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaDetailsViewModel
import ani.dantotsu.media.MediaType
import ani.dantotsu.media.SubtitleDownloader
import ani.dantotsu.navBarHeight
import ani.dantotsu.openInGooglePlay
import ani.dantotsu.others.Download.download
import ani.dantotsu.parsers.Subtitle
import ani.dantotsu.parsers.Video
import ani.dantotsu.parsers.VideoExtractor
import ani.dantotsu.parsers.VideoType
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.settings.SettingsAddonFragment
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.toast
import ani.dantotsu.tryWith
import ani.dantotsu.util.Logger
import ani.dantotsu.util.customAlertDialog
import ani.dantotsu.view.dialog.BottomSheetDialogFragment
import bit.himitsu.TorrManager
import bit.himitsu.TorrManager.getPlayLink
import bit.himitsu.torrServerStart
import eu.kanade.tachiyomi.util.system.getThemeColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.DecimalFormat

class SelectorDialogFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetSelectorBinding? = null
    private val binding by lazy { _binding!! }
    val model: MediaDetailsViewModel by activityViewModels()
    private var scope: CoroutineScope = lifecycleScope
    private var media: Media? = null
    private var episode: Episode? = null
    private var prevEpisode: String? = null
    private var makeDefault = false
    private var selected: String? = null
    private var launch: Boolean? = null
    private var isDownloadMenu: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selected = it.getString("server")
            launch = it.getBoolean("launch", true)
            prevEpisode = it.getString("prev")
            isDownloadMenu = it.getBoolean("isDownload")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSelectorBinding.inflate(inflater, container, false)
        val window = dialog?.window
        window?.statusBarColor = Color.TRANSPARENT
        window?.navigationBarColor = requireContext().getThemeColor(
            com.google.android.material.R.attr.colorSurface
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        var loaded = false
        model.getMedia().observe(viewLifecycleOwner) { m ->
            media = m
            if (media != null && !loaded) {
                loaded = true
                val ep = media?.anime?.episodes?.get(media?.anime?.selectedEpisode)
                episode = ep
                if (ep != null) {
                    if (isDownloadMenu == true) {
                        binding.selectorMakeDefault.visibility = View.GONE
                    }

                    if (selected != null && isDownloadMenu == false) {
                        binding.selectorListContainer.visibility = View.GONE
                        binding.selectorAutoListContainer.visibility = View.VISIBLE
                        binding.selectorAutoText.text = selected
                        binding.selectorCancel.setOnClickListener {
                            media!!.selected!!.server = null
                            model.saveSelected(media!!.id, media!!.selected!!)
                            tryWith {
                                dismiss()
                            }
                        }
                        fun fail() {
                            snackString(getString(R.string.auto_select_server_error))
                            binding.selectorCancel.performClick()
                        }

                        fun load() {
                            val size =
                                if (model.watchSources!!.isDownloadedSource(media!!.selected!!.sourceIndex)) {
                                    ep.extractors?.firstOrNull()?.videos?.size
                                } else {
                                    ep.extractors?.find { it.server.name == selected }?.videos?.size
                                }

                            if (size != null && size >= media!!.selected!!.video) {
                                media!!.anime!!.episodes?.get(media!!.anime!!.selectedEpisode!!)?.selectedExtractor =
                                    selected
                                media!!.anime!!.episodes?.get(media!!.anime!!.selectedEpisode!!)?.selectedVideo =
                                    media!!.selected!!.video
                                startExoplayer(media!!)
                            } else fail()
                        }

                        if (ep.extractors.isNullOrEmpty()) {
                            model.getEpisode().observe(this) {
                                if (it != null) {
                                    episode = it
                                    load()
                                }
                            }
                            scope.launch {
                                if (withContext(Dispatchers.IO) {
                                        !model.loadEpisodeSingleVideo(
                                            ep,
                                            media!!.selected!!
                                        )
                                    }) fail()
                            }
                        } else load()
                    } else {
                        binding.selectorRecyclerView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                            bottomMargin = navBarHeight
                        }
                        binding.selectorRecyclerView.adapter = null
                        binding.selectorProgressBar.visibility = View.VISIBLE
                        makeDefault = PrefManager.getVal(PrefName.MakeDefault)
                        binding.selectorMakeDefault.isChecked = makeDefault
                        binding.selectorMakeDefault.setOnClickListener {
                            makeDefault = binding.selectorMakeDefault.isChecked
                            PrefManager.setVal(PrefName.MakeDefault, makeDefault)
                        }
                        binding.selectorRecyclerView.layoutManager =
                            LinearLayoutManager(
                                requireActivity(),
                                LinearLayoutManager.VERTICAL,
                                false
                            )
                        val adapter = ExtractorAdapter()
                        binding.selectorRecyclerView.adapter = adapter
                        if (!ep.allStreams) {
                            ep.extractorCallback = {
                                scope.launch {
                                    adapter.add(it)
                                    if (model.watchSources!!.isDownloadedSource(media?.selected!!.sourceIndex)) {
                                        adapter.performClick(0)
                                    }
                                }
                            }
                            model.getEpisode().observe(this) {
                                if (it != null) {
                                    media!!.anime?.episodes?.set(
                                        media!!.anime?.selectedEpisode!!,
                                        ep
                                    )
                                }
                            }
                            scope.launch(Dispatchers.IO) {
                                model.loadEpisodeVideos(ep, media!!.selected!!.sourceIndex)
                                withContext(Dispatchers.Main) {
                                    binding.selectorProgressBar.visibility = View.GONE
                                    if (adapter.itemCount == 0) {
                                        snackString(getString(R.string.stream_selection_empty))
                                        tryWith {
                                            dismiss()
                                        }
                                    }
                                }
                            }
                        } else {
                            media!!.anime?.episodes?.set(media!!.anime?.selectedEpisode!!, ep)
                            adapter.addAll(ep.extractors)
                            if (ep.extractors?.size == 0) {
                                snackString(getString(R.string.stream_selection_empty))
                                tryWith {
                                    dismiss()
                                }
                            }
                            if (model.watchSources!!.isDownloadedSource(media?.selected!!.sourceIndex)) {
                                adapter.performClick(0)
                            }
                            binding.selectorProgressBar.visibility = View.GONE
                        }
                    }
                }
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    private fun exportMagnetIntent(episode: Episode, video: Video): Intent {
        val amnis = "com.amnis"
        return Intent(Intent.ACTION_VIEW).apply {
            component = ComponentName(amnis, "$amnis.gui.player.PlayerActivity")
            data = Uri.parse(video.file.url)
            putExtra("title", "${media?.name} - ${episode.title}")
            putExtra("position", 0)
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra("secure_uri", true)
            val headersArray = arrayOf<String>()
            video.file.headers.forEach {
                headersArray.plus(arrayOf(it.key, it.value))
            }
            putExtra("headers", headersArray)
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private suspend fun launchWithTorrentServer(video: Video) = withContext(Dispatchers.IO) {
        ExoplayerView.torrent?.hash?.let {
            runBlocking(Dispatchers.IO) { TorrManager.removeTorrent(it) }
        }
        val index = if (video.file.url.contains("index=")) {
            try {
                video.file.url.substringAfter("index=").toInt()
            } catch (ignored: NumberFormatException) {
                0
            }
        } else {
            0
        }
        ExoplayerView.torrent = TorrManager.addTorrent(
            video.file.url, video.quality.toString(), "", "", false
        ).apply {
            video.file.url = this.getPlayLink(index)
        }
    }

    private fun parseMagnetLink(video: Video, ep: Episode, videoUrl: String): Boolean {
        if (videoUrl.startsWith("magnet:") || videoUrl.endsWith(".torrent")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!PrefManager.getVal<Boolean>(PrefName.TorrServerEnabled)
                    && !TorrManager.isServiceRunning()
                ) {
                    val dialog = AlertDialog.Builder(requireContext(), R.style.MyPopup)
                        .setTitle(R.string.server_disabled)
                        .setMessage(R.string.enable_server_temp)
                        .setPositiveButton(R.string.yes) { dialog, _ ->
                            torrServerStart()
                            toast(R.string.server_enabled)
                            dialog.dismiss()
                        }
                        .setNegativeButton(R.string.no) { dialog, _ ->
                            dialog.dismiss()
                            dismiss()
                        }
                        .create()
                    dialog.window?.setDimAmount(0.8f)
                    dialog.show()
                    return true
                }
                runBlocking(Dispatchers.IO) {
                    launchWithTorrentServer(video)
                }
            } else {
                dismiss()
                try {
                    startActivity(exportMagnetIntent(ep, video))
                } catch (e: ActivityNotFoundException) {
                    openInGooglePlay("com.amnis")
                }
                return true
            }
        }
        return false
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun startExoplayer(media: Media) {
        prevEpisode = null

        episode?.let { ep ->
            val video = ep.extractors?.find {
                it.server.name == ep.selectedExtractor
            }?.videos?.getOrNull(ep.selectedVideo)
            val isExportedOrUnavailable = video?.file?.url?.let { videoUrl ->
                parseMagnetLink(video, ep, videoUrl)
            } ?: false
            if (isExportedOrUnavailable) return
        }
        dismiss()
        if (launch!!) {
            stopAddingToList()
            val intent = Intent(activity, ExoplayerView::class.java)
            ExoplayerView.media = media
            ExoplayerView.initialized = true
            startActivity(intent)
        } else {
            model.setEpisode(
                media.anime!!.episodes!![media.anime.selectedEpisode!!]!!,
                "startExo no launch"
            )
        }
    }

    private fun stopAddingToList() {
        episode?.extractorCallback = null
        episode?.also {
            it.extractors = it.extractors?.toMutableList()
        }
    }

    private inner class ExtractorAdapter :
        RecyclerView.Adapter<ExtractorAdapter.StreamViewHolder>() {
        val links = mutableListOf<VideoExtractor>()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamViewHolder =
            StreamViewHolder(
                ItemStreamBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: StreamViewHolder, position: Int) {
            val extractor = links[position]
            holder.binding.streamName.text = ""//extractor.server.name
            holder.binding.streamName.visibility = View.GONE

            holder.binding.streamRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            holder.binding.streamRecyclerView.adapter = VideoAdapter(extractor)

        }

        override fun getItemCount(): Int = links.size

        fun add(videoExtractor: VideoExtractor) {
            if (videoExtractor.videos.isNotEmpty()) {
                links.add(videoExtractor)
                notifyItemInserted(links.size - 1)
            }
        }

        fun addAll(extractors: List<VideoExtractor>?) {
            links.addAll(extractors ?: return)
            notifyItemRangeInserted(0, extractors.size)
        }

        fun performClick(position: Int) {
            try {
                val extractor = links[position]
                media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]?.selectedExtractor =
                    extractor.server.name
                media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]?.selectedVideo = 0
                startExoplayer(media!!)
            } catch (e: Exception) {
                Logger.log(e)
            }
        }

        private inner class StreamViewHolder(val binding: ItemStreamBinding) :
            RecyclerView.ViewHolder(binding.root)
    }

    private inner class VideoAdapter(private val extractor: VideoExtractor) :
        RecyclerView.Adapter<VideoAdapter.UrlViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UrlViewHolder {
            return UrlViewHolder(
                ItemUrlBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: UrlViewHolder, position: Int) {
            val binding = holder.binding
            val video = extractor.videos[position]
            binding.urlDownload.isVisible = isDownloadMenu == true
            val subtitles = extractor.subtitles
            if (subtitles.isNotEmpty()) {
                binding.urlSub.visibility = View.VISIBLE
                binding.urlSub.setOnClickListener {
                    val subtitleNames = subtitles.map { it.language }
                    var subtitleToDownload: Subtitle? = null
                    val alertDialog = AlertDialog.Builder(context, R.style.MyPopup)
                        .setTitle(R.string.download_subtitle)
                        .setSingleChoiceItems(
                            subtitleNames.toTypedArray(),
                            -1
                        ) { _, which ->
                            subtitleToDownload = subtitles[which]
                        }
                        .setPositiveButton(R.string.download) { dialog, _ ->
                            if (subtitleToDownload != null) {
                                scope.launch {
                                    SubtitleDownloader.downloadSubtitle(
                                        requireContext(),
                                        subtitleToDownload!!.file.url,
                                        DownloadedType(
                                            media!!.mainName(),
                                            media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!.number,
                                            MediaType.ANIME
                                        )
                                    )
                                }
                            } else {
                                snackString(R.string.no_subs_available)
                            }
                            dialog.dismiss()
                        }
                        .setNegativeButton(R.string.cancel) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                    alertDialog.window?.setDimAmount(0.8f)
                }
            } else {
                binding.urlSub.visibility = View.GONE
            }
            binding.urlDownload.setSafeOnClickListener {
                media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!.selectedExtractor =
                    extractor.server.name
                media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!.selectedVideo =
                    position
                if ((PrefManager.getVal(PrefName.DownloadManager) as Int) != 0) {
                    download(
                        requireActivity(),
                        media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!,
                        media!!.userPreferredName
                    )
                } else {
                    val downloadAddonManager: DownloadAddonManager = Injekt.get()
                    if (!downloadAddonManager.isAvailable()){
                        currContext().customAlertDialog().apply {
                            setTitle(R.string.download_addon_not_installed)
                            setMessage(R.string.would_you_like_to_install)
                            setPosButton(R.string.yes) {
                                ContextCompat.startActivity(
                                    currContext(),
                                    Intent(currContext(), SettingsAddonFragment::class.java),
                                    null
                                )
                            }
                            setNegButton(R.string.no) {
                                return@setNegButton
                            }
                            show()
                        }
                        dismiss()
                        return@setSafeOnClickListener
                    }
                    val episode =
                        media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!
                    val selectedVideo =
                        if (extractor.videos.size > episode.selectedVideo) extractor.videos[episode.selectedVideo] else null
                    val subtitleNames = subtitles.map { it.language }
                    var selectedSubtitles: MutableList<Pair<String, String>> = mutableListOf()
                    var selectedAudioTracks: MutableList<Pair<String, String>> = mutableListOf()
                    val activity = currActivity() ?: requireActivity()
                    val isExportedOrUnavailable = selectedVideo?.file?.url?.let { videoUrl ->
                        parseMagnetLink(video, episode, videoUrl)
                    } ?: false
                    if (isExportedOrUnavailable) return@setSafeOnClickListener
                    fun go() {
                        if (selectedVideo != null) {
                            Helper.startAnimeDownloadService(
                                activity,
                                media!!.mainName(),
                                episode.number,
                                selectedVideo,
                                selectedSubtitles,
                                selectedAudioTracks,
                                media,
                                episode.thumb?.url ?: media!!.banner ?: media!!.cover
                            )
                            broadcastDownloadStarted(episode.number, activity)
                        } else {
                            snackString(R.string.no_video_selected)
                        }
                    }
                    fun checkAudioTracks() {
                        val audioTracks = extractor.audioTracks.map { it.lang }
                        if (audioTracks.isNotEmpty()) {
                            val audioNamesArray = audioTracks.toTypedArray()
                            val checkedItems = BooleanArray(audioNamesArray.size) { false }
                            val alertDialog = AlertDialog.Builder(currContext(), R.style.MyPopup)
                                .setTitle(R.string.download_audio_tracks)
                                .setMultiChoiceItems(audioNamesArray, checkedItems) { _, which, isChecked ->
                                    val audioPair = Pair(extractor.audioTracks[which].url, extractor.audioTracks[which].lang)
                                    if (isChecked) {
                                        selectedAudioTracks.add(audioPair)
                                    } else {
                                        selectedAudioTracks.remove(audioPair)
                                    }
                                }
                                .setPositiveButton(R.string.download) { _, _ ->
                                    dialog?.dismiss()
                                    go()
                                }
                                .setNegativeButton(R.string.skip) { dialog, _ ->
                                    selectedAudioTracks = mutableListOf()
                                    go()
                                    dialog.dismiss()
                                }
                                .setNeutralButton(R.string.cancel) { dialog, _ ->
                                    selectedAudioTracks = mutableListOf()
                                    dialog.dismiss()
                                }
                                .show()
                            alertDialog.window?.setDimAmount(0.8f)
                        } else {
                            go()
                        }
                    }
                    if (subtitles.isNotEmpty()) {
                        val subtitleNamesArray = subtitleNames.toTypedArray()
                        val checkedItems = BooleanArray(subtitleNamesArray.size) { false }

                        val alertDialog = AlertDialog.Builder(currContext(), R.style.MyPopup)
                            .setTitle(R.string.download_subtitle)
                            .setMultiChoiceItems(subtitleNamesArray, checkedItems) { _, which, isChecked ->
                                val subtitlePair = Pair(subtitles[which].file.url, subtitles[which].language)
                                if (isChecked) {
                                    selectedSubtitles.add(subtitlePair)
                                } else {
                                    selectedSubtitles.remove(subtitlePair)
                                }
                            }
                            .setPositiveButton(R.string.download) { _, _ ->
                                dialog?.dismiss()
                                checkAudioTracks()
                            }
                            .setNegativeButton(R.string.skip) { dialog, _ ->
                                selectedSubtitles = mutableListOf()
                                checkAudioTracks()
                                dialog.dismiss()
                            }
                            .setNeutralButton(R.string.cancel) { dialog, _ ->
                                selectedSubtitles = mutableListOf()
                                dialog.dismiss()
                            }
                            .show()
                        alertDialog.window?.setDimAmount(0.8f)

                    } else {
                        checkAudioTracks()
                    }
                }
                dismiss()
            }
            if (video.format == VideoType.CONTAINER) {
                binding.urlSize.isVisible = video.size != null
                // if video size is null or 0, show "Unknown Size" else show the size in MB
                val sizeText = getString(
                    R.string.mb_size, "${if (video.extraNote != null) " : " else ""}${
                        if (video.size == 0.0) getString(R.string.size_unknown) else DecimalFormat("#.##").format(
                            video.size ?: 0
                        )
                    }"
                )
                binding.urlSize.text = sizeText
            }
            binding.urlNote.visibility = View.VISIBLE
            binding.urlNote.text = video.format.name
            binding.urlQuality.text = extractor.server.name
        }

        private fun broadcastDownloadStarted(episodeNumber: String, activity: Activity) {
            val intent = Intent(AnimeWatchFragment.ACTION_DOWNLOAD_STARTED).apply {
                putExtra(AnimeWatchFragment.EXTRA_EPISODE_NUMBER, episodeNumber)
            }
            activity.sendBroadcast(intent)
        }

        override fun getItemCount(): Int = extractor.videos.size

        private inner class UrlViewHolder(val binding: ItemUrlBinding) :
            RecyclerView.ViewHolder(binding.root) {
            init {
                itemView.setSafeOnClickListener {
                    if (isDownloadMenu == true) {
                        binding.urlDownload.performClick()
                        return@setSafeOnClickListener
                    }
                    tryWith(true) {
                        media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]?.selectedExtractor =
                            extractor.server.name
                        media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]?.selectedVideo =
                            bindingAdapterPosition
                        if (makeDefault) {
                            media!!.selected!!.server = extractor.server.name
                            media!!.selected!!.video = bindingAdapterPosition
                            model.saveSelected(media!!.id, media!!.selected!!)
                        }
                        startExoplayer(media!!)
                    }
                }
                itemView.setOnLongClickListener {
                    val video = extractor.videos[bindingAdapterPosition]
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(video.file.url), "video/*")
                    }
                    copyToClipboard(video.file.url, true)
                    dismiss()
                    startActivity(Intent.createChooser(intent, "Open Video in :"))
                    true
                }
            }
        }
    }

    companion object {
        fun newInstance(
            server: String? = null,
            launch: Boolean = true,
            prev: String? = null,
            isDownload: Boolean
        ): SelectorDialogFragment =
            SelectorDialogFragment().apply {
                arguments = Bundle().apply {
                    putString("server", server)
                    putBoolean("launch", launch)
                    putString("prev", prev)
                    putBoolean("isDownload", isDownload)
                }
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {}

    override fun onDismiss(dialog: DialogInterface) {
        if (launch == false) {
            activity?.hideSystemBars()
            model.epChanged.postValue(true)
            if (prevEpisode != null) {
                media?.anime?.selectedEpisode = prevEpisode
                model.setEpisode(media?.anime?.episodes?.get(prevEpisode) ?: return, "prevEp")
            }
        }
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
