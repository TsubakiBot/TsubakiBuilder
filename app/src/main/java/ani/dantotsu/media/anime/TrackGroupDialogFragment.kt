package ani.dantotsu.media.anime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.C.TRACK_TYPE_AUDIO
import androidx.media3.common.C.TrackType
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.BottomSheetDialogFragment
import ani.dantotsu.R
import ani.dantotsu.databinding.BottomSheetSubtitlesBinding
import ani.dantotsu.databinding.ItemSubtitleTextBinding

@OptIn(UnstableApi::class)
class TrackGroupDialogFragment(
    instance: ExoplayerView, trackGroups: ArrayList<Tracks.Group>, type : @TrackType Int
) : BottomSheetDialogFragment() {
    private var _binding: BottomSheetSubtitlesBinding? = null
    private val binding get() = _binding!!
    private var instance: ExoplayerView
    private var trackGroups: ArrayList<Tracks.Group>
    private var type: @TrackType Int

    init {
        this.instance = instance
        this.trackGroups = trackGroups
        this.type = type
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSubtitlesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (type == TRACK_TYPE_AUDIO) binding.selectionTitle.text = getString(R.string.audio_tracks)
        binding.subtitlesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.subtitlesRecycler.adapter = TrackGroupAdapter()
    }

    inner class TrackGroupAdapter : RecyclerView.Adapter<TrackGroupAdapter.StreamViewHolder>() {
        inner class StreamViewHolder(val binding: ItemSubtitleTextBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamViewHolder =
            StreamViewHolder(
                ItemSubtitleTextBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        @OptIn(UnstableApi::class)
        override fun onBindViewHolder(holder: StreamViewHolder, position: Int) {
            val binding = holder.binding
            trackGroups[position].let { trackGroup ->
                binding.subtitleTitle.text = when (trackGroup.getTrackFormat(0).language) {
                    "ja" -> "[ja] Japanese"
                    "en" -> "[en] English"
                    "de" -> "[de] German"
                    "es" -> "[es] Spanish"
                    "fr" -> "[fr] French"
                    "it" -> "[it] Italian"
                    "pt" -> "[pt] Portuguese"
                    "ru" -> "[ru] Russian"
                    "zh" -> "[zh] Chinese (Simplified)"
                    "tr" -> "[tr] Turkish"
                    "ar" -> "[ar] Arabic"
                    "uk" -> "[uk] Ukrainian"
                    "he" -> "[he] Hebrew"
                    "pl" -> "[pl] Polish"
                    "ro" -> "[ro] Romanian"
                    "sv" -> "[sv] Swedish"
                    "und" -> "[??] Unknown"
                    else -> trackGroup.getTrackFormat(0).language
                }
                binding.root.setOnClickListener {
                    dismiss()
                    instance.onSetTrackGroupOverride(trackGroup, type)
                }
            }
        }

        override fun getItemCount(): Int = trackGroups.size
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}
