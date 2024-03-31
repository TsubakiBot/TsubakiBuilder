package ani.dantotsu.media.anime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.BottomSheetDialogFragment
import ani.dantotsu.R
import ani.dantotsu.databinding.BottomSheetSubtitlesBinding
import ani.dantotsu.databinding.ItemSubtitleTextBinding
import com.google.common.collect.ImmutableList


class AudioTrackDialogFragment(
    exoPlayer: ExoPlayer, audioTracks: ArrayList<Tracks.Group>
) : BottomSheetDialogFragment() {
    private var _binding: BottomSheetSubtitlesBinding? = null
    private val binding get() = _binding!!
    private var exoPlayer: ExoPlayer
    private var audioTracks: ArrayList<Tracks.Group>

    init {
        this.exoPlayer = exoPlayer
        this.audioTracks = audioTracks
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

        binding.selectionTitle.text = getString(R.string.audio_tracks)
        binding.subtitlesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.subtitlesRecycler.adapter = AudioTrackAdapter()
    }

    inner class AudioTrackAdapter() : RecyclerView.Adapter<AudioTrackAdapter.StreamViewHolder>() {
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
            binding.subtitleTitle.text = when (audioTracks[position].getTrackFormat(0).language) {
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
                else -> audioTracks[position].getTrackFormat(0).language
            }
            binding.root.setOnClickListener {
                dismiss()
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setOverrideForType(
                        TrackSelectionOverride(audioTracks[position].mediaTrackGroup, 0)
                    )
                    .build()
            }
        }

        override fun getItemCount(): Int = audioTracks.size
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}