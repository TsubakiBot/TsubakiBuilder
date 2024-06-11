package ani.dantotsu.media.anime

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.app.PictureInPictureParams
import android.app.PictureInPictureUiState
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.AudioManager.AUDIOFOCUS_GAIN
import android.media.AudioManager.AUDIOFOCUS_LOSS
import android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
import android.media.AudioManager.STREAM_MUSIC
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings.System
import android.util.AttributeSet
import android.util.Rational
import android.util.TypedValue
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.KeyEvent.ACTION_UP
import android.view.KeyEvent.KEYCODE_B
import android.view.KeyEvent.KEYCODE_DPAD_LEFT
import android.view.KeyEvent.KEYCODE_DPAD_RIGHT
import android.view.KeyEvent.KEYCODE_N
import android.view.KeyEvent.KEYCODE_SPACE
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.math.MathUtils.clamp
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.C
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE
import androidx.media3.common.C.TRACK_TYPE_AUDIO
import androidx.media3.common.C.TRACK_TYPE_TEXT
import androidx.media3.common.C.TRACK_TYPE_VIDEO
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.session.MediaSession
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_DEPRESSED
import androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
import androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_NONE
import androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import ani.dantotsu.GesturesListener
import ani.dantotsu.NoPaddingArrayAdapter
import ani.dantotsu.R
import ani.dantotsu.brightnessConverter
import ani.dantotsu.circularReveal
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.discord.Discord
import ani.dantotsu.connections.discord.DiscordService
import ani.dantotsu.connections.discord.DiscordServiceSingleton
import ani.dantotsu.connections.discord.RPC
import ani.dantotsu.connections.updateProgress
import ani.dantotsu.databinding.ActivityExoplayerBinding
import ani.dantotsu.defaultHeaders
import ani.dantotsu.download.DownloadsManager.Companion.getSubDirectory
import ani.dantotsu.download.video.Helper
import ani.dantotsu.getCurrentBrightnessValue
import ani.dantotsu.hideSystemBars
import ani.dantotsu.hideSystemBarsExtendView
import ani.dantotsu.isOnline
import ani.dantotsu.logError
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaDetailsViewModel
import ani.dantotsu.media.MediaNameAdapter
import ani.dantotsu.media.MediaType
import ani.dantotsu.media.SubtitleDownloader
import ani.dantotsu.okHttpClient
import ani.dantotsu.others.AniSkip
import ani.dantotsu.others.AniSkip.getType
import ani.dantotsu.others.LanguageMapper
import ani.dantotsu.others.ResettableTimer
import ani.dantotsu.others.getSerialized
import ani.dantotsu.parsers.AnimeSources
import ani.dantotsu.parsers.HAnimeSources
import ani.dantotsu.parsers.Subtitle
import ani.dantotsu.parsers.SubtitleType
import ani.dantotsu.parsers.Video
import ani.dantotsu.parsers.VideoExtractor
import ani.dantotsu.parsers.VideoType
import ani.dantotsu.refresh
import ani.dantotsu.settings.PlayerSettingsActivity
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toDp
import ani.dantotsu.toPx
import ani.dantotsu.toast
import ani.dantotsu.tryWithSuspend
import ani.dantotsu.util.Logger
import ani.dantotsu.view.CustomCastButton
import bit.himitsu.TorrManager.removeTorrent
import bit.himitsu.os.Version
import com.anggrayudi.storage.file.extension
import com.bumptech.glide.Glide
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.slider.Slider
import com.lagradost.nicehttp.ignoreAllSSLErrors
import eu.kanade.tachiyomi.data.torrentServer.model.Torrent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.set
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


@UnstableApi
@SuppressLint("ClickableViewAccessibility")
class ExoplayerView : AppCompatActivity(), Player.Listener, SessionAvailabilityListener, SensorEventListener {

    private val resumeWindow = "resumeWindow"
    private val resumePosition = "resumePosition"
    private val playerFullscreen = "playerFullscreen"
    private val playerOnPlay = "playerOnPlay"
    private var disappeared: Boolean = false
    private var functionstarted: Boolean = false

    private lateinit var exoPlayer: ExoPlayer
    private var castPlayer: CastPlayer? = null
    private var castContext: CastContext? = null
    private var isCastApiAvailable = false
    private lateinit var trackSelector: DefaultTrackSelector
    private lateinit var cacheFactory: CacheDataSource.Factory
    private lateinit var playbackParameters: PlaybackParameters
    private lateinit var mediaItem: MediaItem
    private var userSubtitles: MutableList<MediaItem.SubtitleConfiguration> = mutableListOf()
    private lateinit var mediaSource: MergingMediaSource
    private var mediaSession: MediaSession? = null

    private lateinit var binding: ActivityExoplayerBinding
    private lateinit var playerView: PlayerView
    private lateinit var exoPlay: ImageButton
    private lateinit var exoSource: ImageButton
    private lateinit var exoSettings: ImageButton
    private lateinit var exoSubtitle: ImageButton
    private lateinit var exoSubtitleView: SubtitleView
    private lateinit var exoAudioTrack: ImageButton
    private lateinit var exoRotate: ImageButton
    private lateinit var exoSpeed: ImageButton
    private lateinit var exoScreen: ImageButton
    private lateinit var exoNext: ImageButton
    private lateinit var exoPrev: ImageButton
    private lateinit var exoSkipOpEd: ImageButton
    private lateinit var exoPip: ImageButton
    private lateinit var exoBrightness: Slider
    private lateinit var exoBrightnessIcon: ImageView
    private lateinit var exoVolume: Slider
    private lateinit var exoVolumeIcon: ImageView
    private lateinit var exoBrightnessCont: View
    private lateinit var exoVolumeCont: View
    private lateinit var exoSkip: View
    private lateinit var skipTimeButton: View
    private lateinit var skipTimeText: TextView
    private lateinit var timeStampText: TextView
    private lateinit var animeTitle: TextView
    private lateinit var videoInfo: TextView
    private lateinit var episodeTitle: Spinner

    private var orientationListener: OrientationEventListener? = null

    private var downloadId: String? = null
    private var hasExtSubtitles = false

    companion object {
        var initialized = false
        lateinit var media: Media
        var torrent: Torrent? = null

        private const val DEFAULT_MIN_BUFFER_MS = 600000
        private const val DEFAULT_MAX_BUFFER_MS = 600000
        private const val BUFFER_FOR_PLAYBACK_MS = 2500
        private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5000
    }

    private lateinit var episode: Episode
    private lateinit var episodes: MutableMap<String, Episode>
    private lateinit var episodeArr: List<String>
    private lateinit var episodeTitleArr: ArrayList<String>
    private var currentEpisodeIndex = 0
    private var epChanging = false

    private var extractor: VideoExtractor? = null
    private var video: Video? = null
    private var subtitle: Subtitle? = null
    var audioLanguages = mutableListOf<String>()

    private var notchHeight: Int = 0
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var episodeLength: Float = 0f
    private var isFullscreen: Int = 0
    private var isInitialized = false
    private var isPlayerPlaying = true
    private var changingServer = false
    private var interacted = false

    private var pipEnabled = false
    private var aspectRatio = Rational(16, 9)

    private val handler = Handler(Looper.getMainLooper())
    val model: MediaDetailsViewModel by viewModels()

    private var isTimeStampsLoaded = false
    private var isSeeking = false
    private var isFastForwarding = false

    var rotation = 0

    private val isTorrent: Boolean get() = torrent != null
    private val subsEmbedded: Boolean get() = isTorrent || !hasExtSubtitles

    private var sensorManager: SensorManager? = null
    private var proximity: Sensor? = null

    override fun onAttachedToWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val displayCutout = window.decorView.rootWindowInsets.displayCutout
            if (displayCutout != null) {
                if (displayCutout.boundingRects.size > 0) {
                    notchHeight = min(
                        displayCutout.boundingRects[0].width(),
                        displayCutout.boundingRects[0].height()
                    )
                    checkNotch()
                }
            }
        }
        super.onAttachedToWindow()
    }

    private fun checkNotch() {
        if (notchHeight != 0) {
            val orientation = resources.configuration.orientation
            playerView.findViewById<View>(R.id.exo_controller_margin)
                .updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        marginStart = notchHeight
                        marginEnd = notchHeight
                        topMargin = 0
                    } else {
                        topMargin = notchHeight
                        marginStart = 0
                        marginEnd = 0
                    }
                }
            playerView.findViewById<View>(androidx.media3.ui.R.id.exo_buffering).translationY =
                (if (orientation == Configuration.ORIENTATION_LANDSCAPE) 0 else (notchHeight + 8.toPx)).toDp
            exoBrightnessCont.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginEnd =
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) notchHeight else 0
            }
            exoVolumeCont.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginStart =
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) notchHeight else 0
            }
        }
    }

    private fun setupSubFormatting(playerView: PlayerView) {
        val primaryColor = when (PrefManager.getVal<Int>(PrefName.PrimaryColor)) {
            0 -> Color.BLACK
            1 -> Color.DKGRAY
            2 -> Color.GRAY
            3 -> Color.LTGRAY
            4 -> Color.WHITE
            5 -> Color.RED
            6 -> Color.YELLOW
            7 -> Color.GREEN
            8 -> Color.CYAN
            9 -> Color.BLUE
            10 -> Color.MAGENTA
            11 -> Color.TRANSPARENT
            else -> Color.WHITE
        }
        val secondaryColor = when (PrefManager.getVal<Int>(PrefName.SecondaryColor)) {
            0 -> Color.BLACK
            1 -> Color.DKGRAY
            2 -> Color.GRAY
            3 -> Color.LTGRAY
            4 -> Color.WHITE
            5 -> Color.RED
            6 -> Color.YELLOW
            7 -> Color.GREEN
            8 -> Color.CYAN
            9 -> Color.BLUE
            10 -> Color.MAGENTA
            11 -> Color.TRANSPARENT
            else -> Color.BLACK
        }
        val outline = when (PrefManager.getVal<Int>(PrefName.Outline)) {
            0 -> EDGE_TYPE_OUTLINE // Normal
            1 -> EDGE_TYPE_DEPRESSED // Shine
            2 -> EDGE_TYPE_DROP_SHADOW // Drop shadow
            3 -> EDGE_TYPE_NONE // No outline
            else -> EDGE_TYPE_OUTLINE // Normal
        }
        val subBackground = when (PrefManager.getVal<Int>(PrefName.SubBackground)) {
            0 -> Color.TRANSPARENT
            1 -> Color.BLACK
            2 -> Color.DKGRAY
            3 -> Color.GRAY
            4 -> Color.LTGRAY
            5 -> Color.WHITE
            6 -> Color.RED
            7 -> Color.YELLOW
            8 -> Color.GREEN
            9 -> Color.CYAN
            10 -> Color.BLUE
            11 -> Color.MAGENTA
            else -> Color.TRANSPARENT
        }
        val subWindow = when (PrefManager.getVal<Int>(PrefName.SubWindow)) {
            0 -> Color.TRANSPARENT
            1 -> Color.BLACK
            2 -> Color.DKGRAY
            3 -> Color.GRAY
            4 -> Color.LTGRAY
            5 -> Color.WHITE
            6 -> Color.RED
            7 -> Color.YELLOW
            8 -> Color.GREEN
            9 -> Color.CYAN
            10 -> Color.BLUE
            11 -> Color.MAGENTA
            else -> Color.TRANSPARENT
        }
        val font = when (PrefManager.getVal<Int>(PrefName.Font)) {
            0 -> ResourcesCompat.getFont(this, R.font.poppins_semi_bold)
            1 -> ResourcesCompat.getFont(this, R.font.poppins_bold)
            2 -> ResourcesCompat.getFont(this, R.font.poppins)
            3 -> ResourcesCompat.getFont(this, R.font.poppins_thin)
            4 -> ResourcesCompat.getFont(this, R.font.century_gothic_regular)
            5 -> ResourcesCompat.getFont(this, R.font.levenim_mt_bold)
            6 -> ResourcesCompat.getFont(this, R.font.blocky)
            else -> ResourcesCompat.getFont(this, R.font.poppins_semi_bold)
        }
        val fontSize = PrefManager.getVal<Int>(PrefName.FontSize).toFloat()

        playerView.subtitleView?.let { subtitles ->
            subtitles.setApplyEmbeddedStyles(false)
            subtitles.setApplyEmbeddedFontSizes(false)

            subtitles.setStyle(
                CaptionStyleCompat(
                    primaryColor,
                    subBackground,
                    subWindow,
                    outline,
                    secondaryColor,
                    font
                )
            )

            subtitles.alpha = PrefManager.getVal(PrefName.SubAlpha)
            subtitles.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
        }
    }

    /**
     * Updating the layout depending on type and state of device
     */
    private fun updateCurrentLayout(newLayoutInfo: WindowLayoutInfo) {
        if (!PrefManager.getVal<Boolean>(PrefName.UseFoldable)) return
        val controller = playerView.findViewById<FrameLayout>(R.id.exo_controller)
        val isFolding = (newLayoutInfo.displayFeatures.find {
            it is FoldingFeature
        } as? FoldingFeature)?.let {
            if (it.isSeparating) {
                if (it.orientation == FoldingFeature.Orientation.HORIZONTAL) {
                    playerView.layoutParams.height = it.bounds.top - 24.toPx // Crease
                    controller.layoutParams.height = it.bounds.bottom - 24.toPx // Crease
                }
            }
            it.isSeparating
        } ?: false
        if (!isFolding) {
            playerView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            controller.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        binding.root.requestLayout()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        binding = ActivityExoplayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        proximity = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        //Initialize
        isCastApiAvailable = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS
        try {
            castContext =
                CastContext.getSharedInstance(this, Executors.newSingleThreadExecutor()).result
            castPlayer = CastPlayer(castContext!!)
            castPlayer!!.setSessionAvailabilityListener(this)
        } catch (e: Exception) {
            isCastApiAvailable = false
        }

        hideSystemBarsExtendView()

        lifecycleScope.launch(Dispatchers.Main) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                WindowInfoTracker.getOrCreate(this@ExoplayerView)
                    .windowLayoutInfo(this@ExoplayerView)
                    .collect { updateCurrentLayout(it) }
            }
        }

        onBackPressedDispatcher.addCallback(this) { playerTeardown() }

        playerView = findViewById(R.id.player_view)
        exoPlay = playerView.findViewById(androidx.media3.ui.R.id.exo_play)
        exoSource = playerView.findViewById(R.id.exo_source)
        exoSettings = playerView.findViewById(R.id.exo_settings)
        exoSubtitle = playerView.findViewById(R.id.exo_sub)
        exoAudioTrack = playerView.findViewById(R.id.exo_audio)
        exoSubtitleView = playerView.findViewById(androidx.media3.ui.R.id.exo_subtitles)
        exoRotate = playerView.findViewById(R.id.exo_rotate)
        exoSpeed = playerView.findViewById(androidx.media3.ui.R.id.exo_playback_speed)
        exoScreen = playerView.findViewById(R.id.exo_screen)
        exoBrightness = playerView.findViewById(R.id.exo_brightness)
        exoBrightnessIcon = playerView.findViewById(R.id.exo_brightness_icon)
        exoVolume = playerView.findViewById(R.id.exo_volume)
        exoVolumeIcon = playerView.findViewById(R.id.exo_volume_icon)
        exoBrightnessCont = playerView.findViewById(R.id.exo_brightness_cont)
        exoVolumeCont = playerView.findViewById(R.id.exo_volume_cont)
        exoPip = playerView.findViewById(R.id.exo_pip)
        exoSkipOpEd = playerView.findViewById(R.id.exo_skip_op_ed)
        exoSkip = playerView.findViewById(R.id.exo_skip)
        skipTimeButton = playerView.findViewById(R.id.exo_skip_timestamp)
        skipTimeText = skipTimeButton.findViewById(R.id.exo_skip_timestamp_text)
        timeStampText = playerView.findViewById(R.id.exo_time_stamp_text)

        animeTitle = playerView.findViewById(R.id.exo_anime_title)
        episodeTitle = playerView.findViewById(R.id.exo_ep_sel)

        playerView.controllerShowTimeoutMs = 5000

        val audioManager = applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager

        @Suppress("DEPRECATION")
        audioManager.requestAudioFocus({ focus ->
            when (focus) {
                AUDIOFOCUS_LOSS_TRANSIENT, AUDIOFOCUS_LOSS -> if (isInitialized) exoPlayer.pause()
            }
        }, AUDIO_CONTENT_TYPE_MOVIE, AUDIOFOCUS_GAIN)

        if (System.getInt(contentResolver, System.ACCELEROMETER_ROTATION, 0) != 1) {
            if (PrefManager.getVal(PrefName.RotationPlayer)) {
                orientationListener =
                    object : OrientationEventListener(this, SensorManager.SENSOR_DELAY_UI) {
                        override fun onOrientationChanged(orientation: Int) {
                            when (orientation) {
                                in 45..135 -> {
                                    if (rotation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                                        exoRotate.visibility = View.VISIBLE
                                    }
                                    rotation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                                }

                                in 225..315 -> {
                                    if (rotation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                        exoRotate.visibility = View.VISIBLE
                                    }
                                    rotation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                }

                                in 315..360, in 0..45 -> {
                                    if (rotation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                                        exoRotate.visibility = View.VISIBLE
                                    }
                                    rotation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                }
                            }
                        }
                    }
                orientationListener?.enable()
            }

            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            exoRotate.setOnClickListener {
                requestedOrientation = rotation
                it.visibility = View.GONE
            }
        }
        setupSubFormatting(playerView)

        if (savedInstanceState != null) {
            currentWindow = savedInstanceState.getInt(resumeWindow)
            playbackPosition = savedInstanceState.getLong(resumePosition)
            isFullscreen = savedInstanceState.getInt(playerFullscreen)
            isPlayerPlaying = savedInstanceState.getBoolean(playerOnPlay)
        }

        //BackButton
        playerView.findViewById<ImageButton>(R.id.exo_back).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        //TimeStamps
        model.timeStamps.observe(this) { it ->
            isTimeStampsLoaded = true
            exoSkipOpEd.visibility = if (it != null) {
                val adGroups = it.flatMap {
                    listOf(
                        it.interval.startTime.toLong() * 1000,
                        it.interval.endTime.toLong() * 1000
                    )
                }.toLongArray()
                val playedAdGroups = it.flatMap {
                    listOf(false, false)
                }.toBooleanArray()
                playerView.setExtraAdGroupMarkers(adGroups, playedAdGroups)
                View.VISIBLE
            } else View.GONE
        }

        exoSkipOpEd.alpha = if (PrefManager.getVal(PrefName.AutoSkipOPED)) 1f else 0.3f
        exoSkipOpEd.setOnClickListener {
            if (PrefManager.getVal(PrefName.AutoSkipOPED)) {
                snackString(getString(R.string.disabled_auto_skip))
                PrefManager.setVal(PrefName.AutoSkipOPED, false)
            } else {
                snackString(getString(R.string.auto_skip))
                PrefManager.setVal(PrefName.AutoSkipOPED, true)
            }
            exoSkipOpEd.alpha = if (PrefManager.getVal(PrefName.AutoSkipOPED)) 1f else 0.3f
        }

        //Play Pause
        exoPlay.setOnClickListener {
            if (isInitialized) {
                isPlayerPlaying = exoPlayer.isPlaying
                (exoPlay.drawable as Animatable?)?.start()
                if (isPlayerPlaying || castPlayer?.isPlaying == true) {
                    Glide.with(this).load(R.drawable.anim_play_to_pause).into(exoPlay)
                    exoPlayer.pause()
                    castPlayer?.pause()
                } else {
                    if (castPlayer?.isPlaying == false && castPlayer?.currentMediaItem != null) {
                        Glide.with(this).load(R.drawable.anim_pause_to_play).into(exoPlay)
                        castPlayer?.play()
                    } else if (!isPlayerPlaying) {
                        Glide.with(this).load(R.drawable.anim_pause_to_play).into(exoPlay)
                        exoPlayer.play()
                    }
                }
            }
        }

        // Picture-in-picture
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pipEnabled = packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                    && PrefManager.getVal(PrefName.Pip)
            if (pipEnabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setPictureInPictureParams(getPictureInPictureBuilder().build())
                }
                exoPip.visibility = View.VISIBLE
                exoPip.setOnClickListener {
                    enterPipMode()
                }
            } else exoPip.visibility = View.GONE
        }

        //Lock Button
        var locked = false
        val container = playerView.findViewById<View>(R.id.exo_controller_cont)
        val screen = playerView.findViewById<View>(R.id.exo_black_screen)
        val lockButton = playerView.findViewById<ImageButton>(R.id.exo_unlock)
        val timeline =
            playerView.findViewById<ExtendedTimeBar>(androidx.media3.ui.R.id.exo_progress)
        playerView.findViewById<ImageButton>(R.id.exo_lock).setOnClickListener {
            locked = true
            screen.visibility = View.GONE
            container.visibility = View.GONE
            lockButton.visibility = View.VISIBLE
            timeline.setForceDisabled(true)
        }
        lockButton.setOnClickListener {
            locked = false
            screen.visibility = View.VISIBLE
            container.visibility = View.VISIBLE
            it.visibility = View.GONE
            timeline.setForceDisabled(false)
        }

        //Skip Time Button
        var skipTime = PrefManager.getVal<Int>(PrefName.SkipTime)
        if (skipTime > 0) {
            exoSkip.findViewById<TextView>(R.id.exo_skip_time).text = skipTime.toString()
            exoSkip.setOnClickListener {
                if (isInitialized)
                    exoPlayer.seekTo(exoPlayer.currentPosition + skipTime * 1000)
            }
            exoSkip.setOnLongClickListener {
                val dialog = Dialog(this, R.style.MyPopup)
                dialog.setContentView(R.layout.item_seekbar_dialog)
                dialog.setCancelable(true)
                dialog.setCanceledOnTouchOutside(true)
                dialog.window?.setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                if (skipTime <= 120) {
                    dialog.findViewById<Slider>(R.id.seekbar).value = skipTime.toFloat()
                } else {
                    dialog.findViewById<Slider>(R.id.seekbar).value = 120f
                }
                dialog.findViewById<Slider>(R.id.seekbar).addOnChangeListener { _, value, _ ->
                    skipTime = value.toInt()
                    //saveData(player, settings)
                    PrefManager.setVal(PrefName.SkipTime, skipTime)
                    playerView.findViewById<TextView>(R.id.exo_skip_time).text =
                        skipTime.toString()
                    dialog.findViewById<TextView>(R.id.seekbar_value).text =
                        skipTime.toString()
                }
                dialog.findViewById<Slider>(R.id.seekbar)
                    .addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                        override fun onStartTrackingTouch(slider: Slider) {}
                        override fun onStopTrackingTouch(slider: Slider) {
                            dialog.dismiss()
                        }
                    })
                dialog.findViewById<TextView>(R.id.seekbar_title).text =
                    getString(R.string.skip_time)
                dialog.findViewById<TextView>(R.id.seekbar_value).text =
                    skipTime.toString()
                @Suppress("DEPRECATION")
                dialog.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                dialog.show()
                true
            }
        } else {
            exoSkip.visibility = View.GONE
        }

        val gestureSpeed = (300 * PrefManager.getVal<Float>(PrefName.AnimationSpeed)).toLong()
        //Player UI Visibility Handler
        val brightnessRunnable = Runnable {
            if (exoBrightnessCont.alpha == 1f)
                lifecycleScope.launch {
                    ObjectAnimator.ofFloat(exoBrightnessCont, "alpha", 1f, 0f)
                        .setDuration(gestureSpeed).start()
                    delay(gestureSpeed)
                    exoBrightnessCont.visibility = View.GONE
                    checkNotch()
                }
        }
        val volumeRunnable = Runnable {
            if (exoVolumeCont.alpha == 1f)
                lifecycleScope.launch {
                    ObjectAnimator.ofFloat(exoVolumeCont, "alpha", 1f, 0f).setDuration(gestureSpeed)
                        .start()
                    delay(gestureSpeed)
                    exoVolumeCont.visibility = View.GONE
                    checkNotch()
                }
        }
        playerView.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
            if (visibility == View.GONE) {
                hideSystemBars()
                brightnessRunnable.run()
                volumeRunnable.run()
            }
        })
        val overshoot = AnimationUtils.loadInterpolator(this, R.anim.over_shoot)
        val controllerDuration = (300 * PrefManager.getVal<Float>(PrefName.AnimationSpeed)).toLong()
        fun handleController() {
            if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) !isInPictureInPictureMode else true) {
                if (playerView.isControllerFullyVisible) {
                    ObjectAnimator.ofFloat(
                        playerView.findViewById(R.id.exo_controller),
                        "alpha",
                        1f,
                        0f
                    )
                        .setDuration(controllerDuration).start()
                    ObjectAnimator.ofFloat(
                        playerView.findViewById(R.id.exo_bottom_cont),
                        "translationY",
                        0f,
                        128f
                    )
                        .apply { interpolator = overshoot;duration = controllerDuration;start() }
                    ObjectAnimator.ofFloat(
                        playerView.findViewById(R.id.exo_timeline_cont),
                        "translationY",
                        0f,
                        128f
                    )
                        .apply { interpolator = overshoot;duration = controllerDuration;start() }
                    ObjectAnimator.ofFloat(
                        playerView.findViewById(R.id.exo_top_cont),
                        "translationY",
                        0f,
                        -128f
                    )
                        .apply { interpolator = overshoot;duration = controllerDuration;start() }
                    playerView.postDelayed({ playerView.hideController() }, controllerDuration)
                } else {
                    checkNotch()
                    playerView.showController()
                    ObjectAnimator.ofFloat(
                        playerView.findViewById(R.id.exo_controller),
                        "alpha",
                        0f,
                        1f
                    )
                        .setDuration(controllerDuration).start()
                    ObjectAnimator.ofFloat(
                        playerView.findViewById(R.id.exo_bottom_cont),
                        "translationY",
                        128f,
                        0f
                    )
                        .apply { interpolator = overshoot;duration = controllerDuration;start() }
                    ObjectAnimator.ofFloat(
                        playerView.findViewById(R.id.exo_timeline_cont),
                        "translationY",
                        128f,
                        0f
                    )
                        .apply { interpolator = overshoot;duration = controllerDuration;start() }
                    ObjectAnimator.ofFloat(
                        playerView.findViewById(R.id.exo_top_cont),
                        "translationY",
                        -128f,
                        0f
                    )
                        .apply { interpolator = overshoot;duration = controllerDuration;start() }
                }
            }
        }

        playerView.findViewById<View>(R.id.exo_full_area).setOnClickListener {
            handleController()
        }

        val rewindText = playerView.findViewById<TextView>(R.id.exo_fast_rewind_anim)
        val forwardText = playerView.findViewById<TextView>(R.id.exo_fast_forward_anim)
        val fastForwardCard = playerView.findViewById<View>(R.id.exo_fast_forward)
        val fastRewindCard = playerView.findViewById<View>(R.id.exo_fast_rewind)

        //Seeking
        val seekTimerF = ResettableTimer()
        val seekTimerR = ResettableTimer()
        var seekTimesF = 0
        var seekTimesR = 0

        fun seek(forward: Boolean, event: MotionEvent? = null) {
            val seekTime = PrefManager.getVal<Int>(PrefName.SeekTime)
            val (card, text) = if (forward) {
                val text = "+${seekTime * ++seekTimesF}"
                forwardText.text = text
                handler.post { exoPlayer.seekTo(exoPlayer.currentPosition + seekTime * 1000) }
                fastForwardCard to forwardText
            } else {
                val text = "-${seekTime * ++seekTimesR}"
                rewindText.text = text
                handler.post { exoPlayer.seekTo(exoPlayer.currentPosition - seekTime * 1000) }
                fastRewindCard to rewindText
            }

            //region Double Tap Animation
            val showCardAnim = ObjectAnimator.ofFloat(card, "alpha", 0f, 1f).setDuration(300)
            val showTextAnim = ObjectAnimator.ofFloat(text, "alpha", 0f, 1f).setDuration(150)

            fun startAnim() {
                showTextAnim.start()

                (text.compoundDrawables[1] as Animatable).apply {
                    if (!isRunning) start()
                }

                if (!isSeeking && event != null) {
                    playerView.hideController()
                    card.circularReveal(event.x.toInt(), event.y.toInt(), !forward, 800)
                    showCardAnim.start()
                }
            }

            fun stopAnim() {
                handler.post {
                    showCardAnim.cancel()
                    showTextAnim.cancel()
                    ObjectAnimator.ofFloat(card, "alpha", card.alpha, 0f).setDuration(150).start()
                    ObjectAnimator.ofFloat(text, "alpha", 1f, 0f).setDuration(150).start()
                }
            }
            //endregion

            startAnim()

            isSeeking = true

            if (forward) {
                seekTimerR.reset(object : TimerTask() {
                    override fun run() {
                        isSeeking = false
                        stopAnim()
                        seekTimesF = 0
                    }
                }, 850)
            } else {
                seekTimerF.reset(object : TimerTask() {
                    override fun run() {
                        isSeeking = false
                        stopAnim()
                        seekTimesR = 0
                    }
                }, 850)
            }
        }

        if (!PrefManager.getVal<Boolean>(PrefName.DoubleTap)) {
            playerView.findViewById<View>(R.id.exo_fast_forward_button_cont).visibility =
                View.VISIBLE
            playerView.findViewById<View>(R.id.exo_fast_rewind_button_cont).visibility =
                View.VISIBLE
            playerView.findViewById<ImageButton>(R.id.exo_fast_forward_button).setOnClickListener {
                if (isInitialized) {
                    seek(true)
                }
            }
            playerView.findViewById<ImageButton>(R.id.exo_fast_rewind_button).setOnClickListener {
                if (isInitialized) {
                    seek(false)
                }
            }
        }

        keyMap[KEYCODE_DPAD_RIGHT] = { seek(true) }
        keyMap[KEYCODE_DPAD_LEFT] = { seek(false) }

        //Screen Gestures
        if (PrefManager.getVal(PrefName.Gestures) || PrefManager.getVal(PrefName.DoubleTap)) {

            fun doubleTap(forward: Boolean, event: MotionEvent) {
                if (!locked && isInitialized && PrefManager.getVal(PrefName.DoubleTap)) {
                    seek(forward, event)
                }
            }

            fun setBrightnessIconByValue(brightness: Float) {
                when (brightness) {
                    in 0F..3F -> {
                        exoBrightnessIcon.setImageResource(R.drawable.ic_brightness_low_24)
                    }
                    in 3.000001F..7F -> {
                        exoBrightnessIcon.setImageResource(R.drawable.ic_brightness_medium_24)
                    }
                    else -> {
                        exoBrightnessIcon.setImageResource(R.drawable.ic_brightness_high_24)
                    }
                }
            }

            //Brightness
            var brightnessTimer = Timer()
            exoBrightnessCont.visibility = View.GONE

            fun brightnessHide() {
                brightnessTimer.cancel()
                brightnessTimer.purge()
                val timerTask: TimerTask = object : TimerTask() {
                    override fun run() {
                        handler.post(brightnessRunnable)
                    }
                }
                brightnessTimer = Timer()
                brightnessTimer.schedule(timerTask, 3000)
            }
            exoBrightness.value = (getCurrentBrightnessValue(this) * 10f)
            setBrightnessIconByValue(exoBrightness.value)

            exoBrightness.addOnChangeListener { _, value, _ ->
                val lp = window.attributes
                lp.screenBrightness =
                    brightnessConverter((value.takeIf { !it.isNaN() } ?: 0f) / 10, false)
                setBrightnessIconByValue(value)
                window.attributes = lp
                brightnessHide()
            }

            fun setAudioIconByVolume(volume: Float) {
                when (volume) {
                    0F -> {
                        exoVolumeIcon.setImageResource(R.drawable.ic_volume_off_24)
                    }
                    in 0.000001F..3F -> {
                        exoVolumeIcon.setImageResource(R.drawable.ic_volume_low_24)
                    }
                    in 3.000001F..<7F -> {
                        exoVolumeIcon.setImageResource(R.drawable.ic_volume_medium_24)
                    }
                    else -> {
                        exoVolumeIcon.setImageResource(R.drawable.ic_volume_high_24)
                    }
                }
            }

            //Volume
            var volumeTimer = Timer()
            exoVolumeCont.visibility = View.GONE

            val volumeMax = audioManager.getStreamMaxVolume(STREAM_MUSIC)
            exoVolume.value = audioManager.getStreamVolume(STREAM_MUSIC).toFloat() / volumeMax * 10
            setAudioIconByVolume(exoVolume.value)
            fun volumeHide() {
                volumeTimer.cancel()
                volumeTimer.purge()
                val timerTask: TimerTask = object : TimerTask() {
                    override fun run() {
                        handler.post(volumeRunnable)
                    }
                }
                volumeTimer = Timer()
                volumeTimer.schedule(timerTask, 3000)
            }
            exoVolume.addOnChangeListener { _, value, _ ->
                val volume = ((value.takeIf { !it.isNaN() } ?: 0f) / 10 * volumeMax).roundToInt()
                setAudioIconByVolume(value)
                audioManager.setStreamVolume(STREAM_MUSIC, volume, 0)
                volumeHide()
            }
            val fastForward = playerView.findViewById<TextView>(R.id.exo_fast_forward_text)
            fun fastForward() {
                isFastForwarding = true
                exoPlayer.setPlaybackSpeed(exoPlayer.playbackParameters.speed * 2)
                fastForward.visibility = View.VISIBLE
                val speedText = "${exoPlayer.playbackParameters.speed}x"
                fastForward.text = speedText
            }

            fun stopFastForward() {
                if (isFastForwarding) {
                    isFastForwarding = false
                    exoPlayer.setPlaybackSpeed(exoPlayer.playbackParameters.speed / 2)
                    fastForward.visibility = View.GONE
                }
            }

            //FastRewind (Left Panel)
            val fastRewindDetector = GestureDetector(this, object : GesturesListener() {
                override fun onLongClick(event: MotionEvent) {
                    if (PrefManager.getVal(PrefName.FastForward)) fastForward()
                }

                override fun onDoubleClick(event: MotionEvent) {
                    doubleTap(false, event)
                }

                override fun onScrollYClick(y: Float) {
                    if (!locked && PrefManager.getVal(PrefName.Gestures)) {
                        if (!playerView.isControllerFullyVisible) return
                        exoBrightness.value = clamp(exoBrightness.value + y / 100, 0f, 10f)
                        if (exoBrightnessCont.visibility != View.VISIBLE) {
                            exoBrightnessCont.visibility = View.VISIBLE
                        }
                        exoBrightnessCont.alpha = 1f
                    }
                }

                override fun onSingleClick(event: MotionEvent) =
                    if (isSeeking) doubleTap(false, event) else handleController()
            })
            val rewindArea = playerView.findViewById<View>(R.id.exo_rewind_area)
            rewindArea.isClickable = true
            rewindArea.setOnTouchListener { v, event ->
                fastRewindDetector.onTouchEvent(event)
                if (event.action == MotionEvent.ACTION_UP) stopFastForward()
                v.performClick()
                true
            }

            //FastForward (Right Panel)
            val fastForwardDetector = GestureDetector(this, object : GesturesListener() {
                override fun onLongClick(event: MotionEvent) {
                    if (PrefManager.getVal(PrefName.FastForward)) fastForward()
                }

                override fun onDoubleClick(event: MotionEvent) {
                    doubleTap(true, event)
                }

                override fun onScrollYClick(y: Float) {
                    if (!locked && PrefManager.getVal(PrefName.Gestures)) {
                        if (!playerView.isControllerFullyVisible) return
                        exoVolume.value = clamp(exoVolume.value + y / 100, 0f, 10f)
                        if (exoVolumeCont.visibility != View.VISIBLE) {
                            exoVolumeCont.visibility = View.VISIBLE
                        }
                        exoVolumeCont.alpha = 1f
                    }
                }

                override fun onSingleClick(event: MotionEvent) =
                    if (isSeeking) doubleTap(true, event) else handleController()
            })
            val forwardArea = playerView.findViewById<View>(R.id.exo_forward_area)
            forwardArea.isClickable = true
            forwardArea.setOnTouchListener { v, event ->
                fastForwardDetector.onTouchEvent(event)
                if (event.action == MotionEvent.ACTION_UP) stopFastForward()
                v.performClick()
                true
            }
        }

        // Handle Media
        if (!initialized) return refresh()
        model.setMedia(media)
        title = media.userPreferredName
        episodes = media.anime?.episodes ?: return refresh()

        videoInfo = playerView.findViewById(R.id.exo_video_info)

        model.watchSources = if (media.isAdult) HAnimeSources else AnimeSources

        model.epChanged.observe(this) {
            epChanging = !it
        }

        //Anime Title
        animeTitle.text = media.userPreferredName

        episodeArr = episodes.keys.toList()
        currentEpisodeIndex = episodeArr.indexOf(media.anime!!.selectedEpisode!!)

        episodeTitleArr = arrayListOf()
        episodes.forEach {
            val episode = it.value
            val cleanedTitle = MediaNameAdapter.removeEpisodeNumberCompletely(episode.title ?: "")
            episodeTitleArr.add("Episode ${episode.number}${if (episode.filler) " [Filler]" else ""}${if (cleanedTitle.isNotBlank() && cleanedTitle != "null") ": $cleanedTitle" else ""}")
        }

        //Episode Change
        fun change(index: Int) {
            if (isInitialized) {
                changingServer = false
                PrefManager.setCustomVal(getEpisodeNumber(), exoPlayer.currentPosition)
                exoPlayer.seekTo(0)
                val prev = episodeArr[currentEpisodeIndex]
                isTimeStampsLoaded = false
                episodeLength = 0f
                media.anime!!.selectedEpisode = episodeArr[index]
                model.setMedia(media)
                model.epChanged.postValue(false)
                model.setEpisode(episodes[media.anime!!.selectedEpisode!!]!!, "change")
                model.onEpisodeClick(
                    media, media.anime!!.selectedEpisode!!, this.supportFragmentManager,
                    false,
                    prev
                )
            }
        }

        //EpisodeSelector
        episodeTitle.adapter = NoPaddingArrayAdapter(this, R.layout.item_dropdown, episodeTitleArr)
        episodeTitle.setSelection(currentEpisodeIndex)
        episodeTitle.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                if (position != currentEpisodeIndex) {
                    disappeared = false
                    functionstarted = false
                    change(position)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        //Next Episode
        exoNext = playerView.findViewById(R.id.exo_next_ep)
        exoNext.setOnClickListener {
            if (isInitialized) {
                nextEpisode { i ->
                    updateAniProgress()
                    disappeared = false
                    functionstarted = false
                    change(currentEpisodeIndex + i)
                }
            }
        }
        //Prev Episode
        exoPrev = playerView.findViewById(R.id.exo_prev_ep)
        exoPrev.setOnClickListener {
            if (currentEpisodeIndex > 0) {
                disappeared = false
                change(currentEpisodeIndex - 1)
            } else
                snackString(getString(R.string.first_episode))
        }

        model.getEpisode().observe(this) { ep ->
            hideSystemBars()
            if (ep != null && !epChanging) {
                episode = ep
                media.selected = model.loadSelected(media)
                model.setMedia(media)
                currentEpisodeIndex = episodeArr.indexOf(ep.number)
                episodeTitle.setSelection(currentEpisodeIndex)
                if (isInitialized) releasePlayer()
                playbackPosition = PrefManager.getCustomVal(
                    "${media.id}_${ep.number}",
                    0
                )
                initPlayer()
                preloading = false
                setDiscordStatus()
                updateProgress()
            }
        }

        //FullScreen
        isFullscreen = PrefManager.getCustomVal("${media.id}_fullscreenInt", isFullscreen)
        playerView.resizeMode = when (isFullscreen) {
            0 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            1 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            2 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }

        exoScreen.setOnClickListener {
            if (isFullscreen < 2) isFullscreen += 1 else isFullscreen = 0
            playerView.resizeMode = when (isFullscreen) {
                0 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                1 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                2 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
            snackString(
                when (isFullscreen) {
                    0 -> "Original"
                    1 -> "Zoom"
                    2 -> "Stretch"
                    else -> "Original"
                }
            )
            PrefManager.setCustomVal("${media.id}_fullscreenInt", isFullscreen)
        }

        //Cast
        if (PrefManager.getVal(PrefName.Cast)) {
            playerView.findViewById<CustomCastButton>(R.id.exo_cast).apply {
                visibility = View.VISIBLE
                if (PrefManager.getVal(PrefName.UseInternalCast)) {
                    try {
                        CastButtonFactory.setUpMediaRouteButton(context, this)
                        dialogFactory = CustomCastThemeFactory()
                    } catch (e: Exception) {
                        isCastApiAvailable = false
                    }
                } else {
                    setCastCallback { cast() }
                }
            }
        }

        //Settings
        exoSettings.setOnClickListener {
            PrefManager.setCustomVal(getEpisodeNumber(), exoPlayer.currentPosition)
            val intent = Intent(this, PlayerSettingsActivity::class.java).apply {
                putExtra("subtitle", subtitle)
            }
            exoPlayer.pause()
            onChangeSettings.launch(intent)
        }

        //Speed
        val speeds =
            if (PrefManager.getVal(PrefName.CursedSpeeds))
                arrayOf(1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 4f, 5f, 10f, 25f, 50f)
            else
                arrayOf(
                    0.25f,
                    0.33f,
                    0.5f,
                    0.66f,
                    0.75f,
                    1f,
                    1.15f,
                    1.25f,
                    1.33f,
                    1.5f,
                    1.66f,
                    1.75f,
                    2f
                )

        val speedsName = speeds.map { "${it}x" }.toTypedArray()
        //var curSpeed = loadData("${media.id}_speed", this) ?: settings.defaultSpeed
        var curSpeed = PrefManager.getCustomVal(
            "${media.id}_speed",
            PrefManager.getVal<Int>(PrefName.DefaultSpeed)
        )

        playbackParameters = PlaybackParameters(speeds[curSpeed])
        var speed: Float
        val speedDialog =
            AlertDialog.Builder(this, R.style.MyPopup).setTitle(getString(R.string.speed))
        exoSpeed.setOnClickListener {
            val dialog = speedDialog.setSingleChoiceItems(speedsName, curSpeed) { dialog, i ->
                if (isInitialized) {
                    PrefManager.setCustomVal("${media.id}_speed", i)
                    speed = speeds[i]
                    curSpeed = i
                    playbackParameters = PlaybackParameters(speed)
                    exoPlayer.playbackParameters = playbackParameters
                    dialog.dismiss()
                    hideSystemBars()
                }
            }.show()
            dialog.window?.setDimAmount(0.8f)
        }
        speedDialog.setOnCancelListener { hideSystemBars() }

        if (PrefManager.getVal(PrefName.AutoPlay)) {
            var touchTimer = Timer()
            fun touched() {
                interacted = true
                touchTimer.apply {
                    cancel()
                    purge()
                }
                touchTimer = Timer()
                touchTimer.schedule(object : TimerTask() {
                    override fun run() {
                        interacted = false
                    }
                }, 1000 * 60 * 60)
            }
            playerView.findViewById<View>(R.id.exo_touch_view).setOnTouchListener { _, _ ->
                touched()
                false
            }
        }

        isFullscreen = PrefManager.getVal(PrefName.Resize)
        playerView.resizeMode = when (isFullscreen) {
            0 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            1 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            2 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }

        preloading = false
        val incognito: Boolean = PrefManager.getVal(PrefName.Incognito)
        val showProgressDialog =
            if (PrefManager.getVal(PrefName.AskIndividualPlayer)) PrefManager.getCustomVal(
                "${media.id}_ProgressDialog",
                true
            ) else false
        if (!incognito && showProgressDialog && Anilist.userid != null && if (media.isAdult) PrefManager.getVal(
                PrefName.UpdateForHPlayer
            ) else true
        ) {
            AlertDialog.Builder(this, R.style.MyPopup)
                .setTitle(getString(R.string.auto_update, media.userPreferredName))
                .apply {
                    setOnCancelListener { hideSystemBars() }
                    setCancelable(false)
                    setPositiveButton(getString(R.string.yes)) { dialog, _ ->
                        PrefManager.setCustomVal(
                            "${media.id}_ProgressDialog",
                            false
                        )
                        PrefManager.setCustomVal(
                            "${media.id}_save_progress",
                            true
                        )
                        dialog.dismiss()
                        model.setEpisode(episodes[media.anime!!.selectedEpisode!!]!!, "invoke")
                    }
                    setNegativeButton(getString(R.string.no)) { dialog, _ ->
                        PrefManager.setCustomVal(
                            "${media.id}_ProgressDialog",
                            false
                        )
                        PrefManager.setCustomVal(
                            "${media.id}_save_progress",
                            false
                        )
                        toast(getString(R.string.reset_auto_update))
                        dialog.dismiss()
                        model.setEpisode(episodes[media.anime!!.selectedEpisode!!]!!, "invoke")
                    }
                    show()
                }
        } else model.setEpisode(episodes[media.anime!!.selectedEpisode!!]!!, "invoke")

        //Start the recursive Fun
        if (PrefManager.getVal(PrefName.TimeStampsEnabled))
            updateTimeStamp()
    }

    private fun setDiscordStatus() {
        if (DiscordService.isRunning()) return
        val context = this
        val offline: Boolean = PrefManager.getVal(PrefName.OfflineMode)
        val incognito: Boolean = PrefManager.getVal(PrefName.Incognito)
        if ((isOnline(context) && !offline) && Discord.token != null && !incognito) {
            lifecycleScope.launch {
                val discordMode = PrefManager.getCustomVal("discord_mode", "dantotsu")
                val buttons = when (discordMode) {
                    "nothing" -> mutableListOf(
                        RPC.Link(getString(R.string.rpc_anime), media.shareLink ?: ""),
                    )

                    "dantotsu" -> mutableListOf(
                        RPC.Link(getString(R.string.rpc_anime), media.shareLink ?: ""),
                        RPC.Link(getString(R.string.rpc_watch), getString(R.string.dantotsu))
                    )

                    "anilist" -> {
                        val userId = PrefManager.getVal<String>(PrefName.AnilistUserId)
                        val anilistLink = "https://anilist.co/user/$userId/"
                        mutableListOf(
                            RPC.Link(getString(R.string.rpc_anime), media.shareLink ?: ""),
                            RPC.Link(getString(R.string.rpc_anilist), anilistLink)
                        )
                    }

                    else -> mutableListOf()
                }
                val presence = RPC.createPresence(
                    RPC.Companion.RPCData(
                        applicationId = Discord.application_Id,
                        type = RPC.Type.WATCHING,
                        activityName = media.userPreferredName,
                        details = episode.title?.takeIf { it.isNotEmpty() } ?: getString(
                            R.string.episode_num,
                            episode.number
                        ),
                        state = "Episode : ${episode.number}/${media.anime?.totalEpisodes ?: "??"}",
                        largeImage = media.cover?.let {
                            RPC.Link(
                                media.userPreferredName,
                                it
                            )
                        },
                        smallImage = RPC.Link(getString(R.string.app_name), Discord.small_Image),
                        buttons = buttons
                    )
                )
                val intent = Intent(context, DiscordService::class.java).apply {
                    putExtra("presence", presence)
                }
                DiscordServiceSingleton.isEnabled = true
                startService(intent)
            }
        }
    }

    private fun stopDiscordService() {
        DiscordServiceSingleton.isEnabled = false
        if (DiscordService.isRunning()) {
            stopService(Intent(this, DiscordService::class.java))
        }
    }

    private fun initPlayer() {
        checkNotch()

        PrefManager.setCustomVal(
            "${media.id}_current_ep",
            media.anime!!.selectedEpisode!!
        )

        @Suppress("UNCHECKED_CAST")
        val list = (PrefManager.getNullableCustomVal(
            "continueAnimeList",
            listOf<Int>(),
            List::class.java
        ) as List<Int>).toMutableList()
        if (list.contains(media.id)) list.remove(media.id)
        list.add(media.id)
        PrefManager.setCustomVal("continueAnimeList", list)

        lifecycleScope.launch(Dispatchers.IO) {
            extractor?.onVideoStopped(video)
        }

        val ext = episode.extractors?.find { it.server.name == episode.selectedExtractor } ?: return
        extractor = ext
        video = ext.videos.getOrNull(episode.selectedVideo) ?: return

        val languagePref = LanguageMapper.codeMap.entries.filterIndexed { index, _ ->
            index == PrefManager.getVal(PrefName.SubLanguage)
        }.first()
        subtitle = intent.getSerialized("subtitle")
            ?: when (val subLang: String? =
                PrefManager.getNullableCustomVal("subLang_${media.id}", null, String::class.java)) {
                null -> {
                    when (episode.selectedSubtitle) {
                        null -> null
                        -1 -> ext.subtitles.find { it.language.contains(languagePref.key, ignoreCase = true)
                                || it.language.contains(languagePref.value, ignoreCase = true) }
                        else -> ext.subtitles.getOrNull(episode.selectedSubtitle!!)
                    }
                }

                "None", "none" -> ext.subtitles.let { null }
                else -> ext.subtitles.find { it.language == subLang }
            }

        //Subtitles
        hasExtSubtitles = ext.subtitles.isNotEmpty()
        val sub: MutableList<MediaItem.SubtitleConfiguration> =
            emptyList<MediaItem.SubtitleConfiguration>().toMutableList()
        ext.subtitles.forEach { subtitle ->
            val subtitleUrl = if (subsEmbedded) video!!.file.url else subtitle.file.url
            //var localFile: String? = null
            if (subtitle.type == SubtitleType.UNKNOWN) {
                runBlocking(Dispatchers.IO) {
                    val type = SubtitleDownloader.loadSubtitleType(subtitleUrl)
                    val fileUri = Uri.parse(subtitleUrl)
                    sub += MediaItem.SubtitleConfiguration
                        .Builder(fileUri)
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .setMimeType(SubtitleType.toMimeType(type))
                        .setId("Extractor")
                        .setLanguage(subtitle.language)
                        .build()
                }
                println("Unknown Subtitle: $sub")
            } else {
                sub += MediaItem.SubtitleConfiguration
                    .Builder(Uri.parse(subtitleUrl))
                    .setSelectionFlags(C.SELECTION_FLAG_FORCED)
                    .setMimeType(SubtitleType.toMimeType(subtitle.type))
                    .setId("Extractor")
                    .setLanguage(subtitle.language)
                    .build()
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            ext.onVideoPlayed(video)
        }

        cacheFactory = CacheDataSource.Factory().apply {
            setCache(VideoCache.getInstance(this@ExoplayerView))
            if (ext.server.offline) {
                setUpstreamDataSourceFactory(DefaultDataSource.Factory(this@ExoplayerView))
            } else {
                val httpClient = okHttpClient.newBuilder().apply {
                    ignoreAllSSLErrors()
                    followRedirects(true)
                    followSslRedirects(true)
                }.build()
                val dataSourceFactory = DataSource.Factory {
                    val dataSource: HttpDataSource =
                        OkHttpDataSource.Factory(httpClient).createDataSource()
                    defaultHeaders.forEach {
                        dataSource.setRequestProperty(it.key, it.value)
                    }
                    video?.file?.headers?.forEach {
                        dataSource.setRequestProperty(it.key, it.value)
                    }
                    dataSource
                }
                setUpstreamDataSourceFactory(dataSourceFactory)
            }
            setCacheWriteDataSinkFactory(null)
            setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        }

        val mimeType = when (video?.format) {
            VideoType.M3U8 -> MimeTypes.APPLICATION_M3U8
            VideoType.DASH -> MimeTypes.APPLICATION_MPD
            else -> MimeTypes.APPLICATION_MP4
        }

        val downloadedMediaItem = if (ext.server.offline) {
            val titleName = ext.server.name.split("/").first()
            val episodeName = ext.server.name.split("/").last()
            downloadId = PrefManager.getAnimeDownloadPreferences()
                .getString("$titleName - $episodeName", null) ?:
                    PrefManager.getAnimeDownloadPreferences()
                        .getString(ext.server.name, null)
            val exoItem = if (downloadId != null) {
                Helper.downloadManager(this)
                    .downloadIndex.getDownload(downloadId!!)?.request?.toMediaItem()
            } else null
            if (exoItem != null) {
                exoItem
            } else {
                val directory =
                    getSubDirectory(this, MediaType.ANIME, false, titleName, episodeName)
                if (directory != null) {
                    val files = directory.listFiles()
                    println(files)
                    val docFile = directory.listFiles().firstOrNull {
                        it.name?.endsWith(".mp4") == true || it.name?.endsWith(".mkv") == true
                    }
                    if (docFile != null) {
                        val uri = docFile.uri
                        val downloadedMimeType = when (docFile.extension) {
                            "mp4" -> MimeTypes.APPLICATION_MP4
                            "mkv" -> MimeTypes.APPLICATION_MATROSKA
                            else -> MimeTypes.APPLICATION_MP4
                        }
                        MediaItem.Builder().setUri(uri).setMimeType(downloadedMimeType).build()
                    } else {
                        snackString(getString(R.string.file_not_found))
                        null
                    }
                } else {
                    snackString(getString(R.string.directory_not_found))
                    null
                }
            }
        } else null

        mediaItem = if (downloadedMediaItem == null) {
            val builder = MediaItem.Builder().setUri(video!!.file.url).setMimeType(mimeType)
            Logger.log("Video URL: ${video!!.file.url}")
            Logger.log("Video MimeType: $mimeType")
            builder.setSubtitleConfigurations(sub)
            builder.build()
        } else {
            if (sub.isNotEmpty()) {
                val addedSubsDownloadedMediaItem = downloadedMediaItem.buildUpon()
                val addLanguage = sub[0].buildUpon().setLanguage(Locale.getDefault().language).build()
                addedSubsDownloadedMediaItem.setSubtitleConfigurations(listOf(addLanguage))
                episode.selectedSubtitle = 0
                addedSubsDownloadedMediaItem.build()
            } else {
                downloadedMediaItem
            }
        }

        val audioMediaItem = mutableListOf<MediaItem>()
        audioLanguages.clear()
        ext.audioTracks.forEach { track ->
            audioLanguages.add(LanguageMapper.mapNativeToCode(track.lang) ?: track.lang)
            audioMediaItem.add(
                MediaItem.Builder()
                    .setUri(track.url)
                    .setMimeType(MimeTypes.AUDIO_UNKNOWN)
                    .setTag(track.lang)
                    .setSubtitleConfigurations(sub)
                    .build()
            )
        }

        val audioSources = audioMediaItem.map { mediaItem ->
            if (video?.format == VideoType.M3U8) {
                HlsMediaSource.Factory(cacheFactory).apply {
                    setAllowChunklessPreparation(false)
                }.createMediaSource(mediaItem)
            } else {
                DefaultMediaSourceFactory(cacheFactory).createMediaSource(mediaItem)
            }
        }.toTypedArray()
        val videoMediaSource = DefaultMediaSourceFactory(cacheFactory)
            .createMediaSource(mediaItem)
        mediaSource = MergingMediaSource(videoMediaSource, *audioSources)

        PrefManager.getCustomVal<Set<String>>(
            "${media.id}_${episode.number}_subtitles", setOf()
        ).forEach { userSubtitles.add(importSubtitle(Uri.parse(it), true))  }
        if (userSubtitles.isNotEmpty()) {
            mediaSource = MergingMediaSource(DefaultMediaSourceFactory(this).createMediaSource(
                mediaItem.buildUpon().setSubtitleConfigurations(userSubtitles).build()
            ))
        }

        //Source
        exoSource.setOnClickListener {
            sourceClick()
        }

        //Quality Track
        trackSelector = DefaultTrackSelector(this)
        val parameters = trackSelector.buildUponParameters()
            .setAllowVideoMixedMimeTypeAdaptiveness(true)
            .setAllowVideoNonSeamlessAdaptiveness(true)
            .setSelectUndeterminedTextLanguage(true)
            .setAllowAudioMixedMimeTypeAdaptiveness(true)
            .setAllowMultipleAdaptiveSelections(true)
            .setPreferredTextLanguage(subtitle?.language ?: Locale.getDefault().language)
            .setPreferredTextRoleFlags(C.ROLE_FLAG_SUBTITLE)
            .setRendererDisabled(TRACK_TYPE_VIDEO, false)
            .setRendererDisabled(TRACK_TYPE_AUDIO, false)
            .setRendererDisabled(TRACK_TYPE_TEXT, false)
            .setMaxVideoSize(1, 1)
        // .setOverrideForType(TrackSelectionOverride(trackSelector, TRACK_TYPE_VIDEO))
        if (PrefManager.getVal(PrefName.SettingsPreferDub))
            parameters.setPreferredAudioLanguage(Locale.getDefault().language)
        if (PrefManager.getVal(PrefName.SettingsExceedCap))
            parameters.setExceedRendererCapabilitiesIfNecessary(true)
        trackSelector.setParameters(parameters)

        if (playbackPosition != 0L && !changingServer && !PrefManager.getVal<Boolean>(PrefName.AlwaysContinue)) {
            val time = String.format(
                "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(playbackPosition),
                TimeUnit.MILLISECONDS.toMinutes(playbackPosition) - TimeUnit.HOURS.toMinutes(
                    TimeUnit.MILLISECONDS.toHours(
                        playbackPosition
                    )
                ),
                TimeUnit.MILLISECONDS.toSeconds(playbackPosition) - TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(
                        playbackPosition
                    )
                )
            )
            val dialog = AlertDialog.Builder(this, R.style.MyPopup)
                .setTitle(getString(R.string.continue_from, time)).apply {
                    setCancelable(false)
                    setPositiveButton(getString(R.string.yes)) { d, _ ->
                        buildExoplayer()
                        d.dismiss()
                    }
                    setNegativeButton(getString(R.string.no)) { d, _ ->
                        playbackPosition = 0L
                        buildExoplayer()
                        d.dismiss()
                    }
                }.show()
            dialog.window?.setDimAmount(0.8f)
        } else buildExoplayer()

        torrent?.active_peers?.let {
            if (it < 2) toast(R.string.peer_count_low)
        }
    }

    private fun buildExoplayer() {
        exoSubtitle.isEnabled = false
        //Player
        val loadControl = DefaultLoadControl.Builder()
            .setBackBuffer(1000 * 60 * 2, true)
            .setBufferDurationsMs(
                DEFAULT_MIN_BUFFER_MS,
                DEFAULT_MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .build()

        hideSystemBars()
        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheFactory))
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build().apply {
                playWhenReady = true
                this.playbackParameters = this@ExoplayerView.playbackParameters
                setMediaSource(mediaSource)
                prepare()
                PrefManager.getCustomVal(
                    "${media.id}_${media.anime!!.selectedEpisode}_max",
                    Long.MAX_VALUE
                )
                    .takeIf { it != Long.MAX_VALUE }
                    ?.let { if (it <= playbackPosition) playbackPosition = max(0, it - 5) }
                seekTo(playbackPosition)
            }
        playerView.player = exoPlayer

        try {
            val rightNow = Calendar.getInstance()
            mediaSession = MediaSession.Builder(this, exoPlayer)
                .setId(rightNow.timeInMillis.toString())
                .build()
        } catch (e: Exception) {
            toast(e.toString())
        }

        exoPlayer.addListener(this)
        exoPlayer.addAnalyticsListener(EventLogger())
        isInitialized = true
        exoSubtitle.isEnabled = true

        if (!PrefManager.getVal<Boolean>(PrefName.Subtitles)) {
            onSetTrackGroupOverride(dummyTrack, TRACK_TYPE_TEXT)
        }
    }

    private fun releasePlayer() {
        isPlayerPlaying = exoPlayer.playWhenReady
        playbackPosition = exoPlayer.currentPosition
        isInitialized = false
        disappeared = false
        functionstarted = false
        exoPlayer.release()
        VideoCache.release()
        mediaSession?.release()
        stopDiscordService()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (isInitialized) {
            outState.putInt(resumeWindow, exoPlayer.currentMediaItemIndex)
            outState.putLong(resumePosition, exoPlayer.currentPosition)
        }
        outState.putInt(playerFullscreen, isFullscreen)
        outState.putBoolean(playerOnPlay, isPlayerPlaying)
        super.onSaveInstanceState(outState)
    }

    private fun sourceClick() {
        changingServer = true

        media.selected!!.server = null
        PrefManager.setCustomVal(getEpisodeNumber(), exoPlayer.currentPosition)
        model.saveSelected(media.id, media.selected!!)
        model.onEpisodeClick(
            media, episode.number, this.supportFragmentManager,
            launch = false
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
        orientationListener?.disable()
        if (isInitialized) {
            if (castPlayer?.isPlaying == false) {
                playerView.player?.pause()
            }
            if (exoPlayer.currentPosition > 5000) {
                PrefManager.setCustomVal(
                    getEpisodeNumber(),
                    exoPlayer.currentPosition
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL)
        orientationListener?.enable()
        hideSystemBars()
        if (isInitialized) {
            playerView.onResume()
            playerView.useController = true
        }
    }

    override fun onStop() {
        super.onStop()
        if (castPlayer?.isPlaying == false) {
            playerView.player?.pause()
        }
        stopDiscordService()
    }

    override fun onStart() {
        super.onStart()
        if (this::episode.isInitialized) setDiscordStatus()
    }

    private var wasPlaying = false
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        binding.playerView.isInvisible = PrefManager.getVal(PrefName.SecureLock)
                && !hasFocus && (Version.isNougat && !isInPictureInPictureMode)
        if (PrefManager.getVal(PrefName.FocusPause) && !epChanging) {
            if (isInitialized && !hasFocus) wasPlaying = exoPlayer.isPlaying
            if (hasFocus) {
                if (isInitialized && wasPlaying) exoPlayer.play()
            } else {
                if (isInitialized) exoPlayer.pause()

            }
        }
        super.onWindowFocusChanged(hasFocus)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (!isBuffering) {
            isPlayerPlaying = isPlaying
            playerView.keepScreenOn = isPlaying
            (exoPlay.drawable as Animatable?)?.start()
            if (!this.isDestroyed) Glide.with(this)
                .load(if (isPlaying) R.drawable.anim_play_to_pause else R.drawable.anim_pause_to_play)
                .into(exoPlay)
        }
    }

    override fun onRenderedFirstFrame() {
        super.onRenderedFirstFrame()
        PrefManager.setCustomVal(
            "${media.id}_${media.anime!!.selectedEpisode}_max",
            exoPlayer.duration
        )
        val height = (exoPlayer.videoFormat ?: return).height
        val width = (exoPlayer.videoFormat ?: return).width

        aspectRatio = Rational(width, height)

        videoInfo.text = getString(R.string.video_quality, height)

        if (exoPlayer.duration < playbackPosition)
            exoPlayer.seekTo(0)

        //if playbackPosition is within 92% of the episode length, reset it to 0
        if (playbackPosition > exoPlayer.duration.toFloat() * 0.92) {
            playbackPosition = 0
            exoPlayer.seekTo(0)
        }

        if (!isTimeStampsLoaded && PrefManager.getVal(PrefName.TimeStampsEnabled)) {
            val dur = exoPlayer.duration
            lifecycleScope.launch(Dispatchers.IO) {
                model.loadTimeStamps(
                    media.idMAL,
                    media.anime?.selectedEpisode?.trim()?.toIntOrNull(),
                    dur / 1000,
                    PrefManager.getVal(PrefName.UseProxyForTimeStamps)
                )
            }
        }
    }

    //Link Preloading
    private var preloading = false
    private fun updateProgress() {
        if (isInitialized) {
            if (exoPlayer.currentPosition.toFloat() / exoPlayer.duration > PrefManager.getVal<Float>(
                    PrefName.WatchPercentage
                )
            ) {
                preloading = true
                nextEpisode(false) { i ->
                    val ep = episodes[episodeArr[currentEpisodeIndex + i]] ?: return@nextEpisode
                    val selected = media.selected ?: return@nextEpisode
                    lifecycleScope.launch(Dispatchers.IO) {
                        if (media.selected!!.server != null)
                            model.loadEpisodeSingleVideo(ep, selected, false)
                        else
                            model.loadEpisodeVideos(ep, selected.sourceIndex, false)
                    }
                }
            }
        }
        if (!preloading) handler.postDelayed({
            updateProgress()
        }, 2500)
    }

    //TimeStamp Updating
    private var currentTimeStamp: AniSkip.Stamp? = null
    private var skippedTimeStamps: MutableList<AniSkip.Stamp> = mutableListOf()
    private fun updateTimeStamp() {
        if (isInitialized) {
            val playerCurrentTime = exoPlayer.currentPosition / 1000
            currentTimeStamp = model.timeStamps.value?.find { timestamp ->
                timestamp.interval.startTime < playerCurrentTime
                        && playerCurrentTime < (timestamp.interval.endTime - 1)
            }

            val new = currentTimeStamp
            var timer: CountDownTimer? = null
            fun cancelTimer() {
                timer?.cancel()
                timer = null
                return
            }
            timer = object : CountDownTimer(5000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    if (new == null) {
                        skipTimeButton.visibility = View.GONE
                        exoSkip.isVisible = PrefManager.getVal<Int>(PrefName.SkipTime) > 0
                        disappeared = false
                        functionstarted = false
                        cancelTimer()
                    }
                }

                override fun onFinish() {
                    skipTimeButton.visibility = View.GONE
                    exoSkip.isVisible = PrefManager.getVal<Int>(PrefName.SkipTime) > 0
                    disappeared = true
                    functionstarted = false
                    cancelTimer()
                }
            }
            timeStampText.text = if (new != null) {
                fun disappearSkip() {
                    functionstarted = true
                    skipTimeButton.visibility = View.VISIBLE
                    exoSkip.visibility = View.GONE
                    skipTimeText.text = new.skipType.getType()
                    skipTimeButton.setOnClickListener {
                        exoPlayer.seekTo((new.interval.endTime * 1000).toLong())
                    }
                    timer?.start()

                }
                if (PrefManager.getVal(PrefName.ShowTimeStampButton)) {

                    if (!functionstarted && !disappeared && PrefManager.getVal(PrefName.AutoHideTimeStamps)) {
                        disappearSkip()
                    } else if (!PrefManager.getVal<Boolean>(PrefName.AutoHideTimeStamps)) {
                        skipTimeButton.visibility = View.VISIBLE
                        exoSkip.visibility = View.GONE
                        skipTimeText.text = new.skipType.getType()
                        skipTimeButton.setOnClickListener {
                            exoPlayer.seekTo((new.interval.endTime * 1000).toLong())
                        }
                    }

                }
                if (PrefManager.getVal(PrefName.AutoSkipOPED)
                    && (new.skipType == "op" || new.skipType == "ed")
                    && !skippedTimeStamps.contains(new)
                ) {
                    exoPlayer.seekTo((new.interval.endTime * 1000).toLong())
                    skippedTimeStamps.add(new)
                }
                if (PrefManager.getVal(PrefName.AutoSkipRecap)
                    && new.skipType == "recap"
                    && !skippedTimeStamps.contains(new)
                ) {
                    exoPlayer.seekTo((new.interval.endTime * 1000).toLong())
                    skippedTimeStamps.add(new)
                }
                new.skipType.getType()
            } else {
                disappeared = false
                functionstarted = false
                skipTimeButton.visibility = View.GONE
                exoSkip.isVisible = PrefManager.getVal<Int>(PrefName.SkipTime) > 0
                ""
            }
        }
        handler.postDelayed({
            updateTimeStamp()
        }, 500)
    }

    fun onSetTrackGroupOverride(trackGroup: Tracks.Group, type: @C.TrackType Int, index: Int = 0) {
        val format = trackGroup.mediaTrackGroup.getFormat(0)
        if (format.language == "load") {
            showSubtitleDialog()
            return
        }
        PrefManager.setCustomVal("subLang_${media.id}", format.language)
        val isDisabled = format.language == "none"
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(TRACK_TYPE_TEXT, isDisabled)
            .setOverrideForType(
                TrackSelectionOverride(trackGroup.mediaTrackGroup, index)
            )
            .setSelectUndeterminedTextLanguage(format.containerMimeType == getString(R.string.mimetype_cea))
            .build()
        if (type == TRACK_TYPE_TEXT) setupSubFormatting(playerView)
    }

    private val dummyTrack = Tracks.Group(
        TrackGroup("Dummy Track", Format.Builder().apply { setLanguage("none") }.build()),
        true,
        intArrayOf(1),
        booleanArrayOf(false)
    )

    private val loaderTrack = Tracks.Group(
        TrackGroup("Sideload", Format.Builder().apply { setLanguage("load") }.build()),
        true,
        intArrayOf(1),
        booleanArrayOf(false)
    )

    override fun onTracksChanged(tracks: Tracks) {
        val audioTracks: ArrayList<Tracks.Group> = arrayListOf()
        val subTracks: ArrayList<Tracks.Group> = arrayListOf(dummyTrack)
        tracks.groups.forEach {
            when (it.mediaTrackGroup.type) {
                TRACK_TYPE_AUDIO -> {
                    if (it.isSupported(true)) audioTracks.add(it)
                }

                TRACK_TYPE_TEXT -> {
                    if (it.isSupported(true)) subTracks.add(it)
                }
            }
        }
        subTracks.add(loaderTrack)
        exoAudioTrack.isVisible = audioTracks.size > 1
        exoAudioTrack.setOnClickListener {
            TrackGroupDialogFragment(this, audioTracks, TRACK_TYPE_AUDIO)
                .show(supportFragmentManager, "dialog")
        }
        exoSubtitle.setOnClickListener {
            TrackGroupDialogFragment(this, subTracks, TRACK_TYPE_TEXT)
                .show(supportFragmentManager, "dialog")
        }
    }

    private fun extensionMimeType(extension: String): String {
        return when (extension) {
            "vtt" -> MimeTypes.TEXT_VTT
            "ttml" -> MimeTypes.APPLICATION_TTML
            "srt" -> MimeTypes.APPLICATION_SUBRIP
            "ass" -> MimeTypes.TEXT_SSA
            else -> MimeTypes.TEXT_UNKNOWN
        }
    }

    private fun importSubtitle(uri: Uri, isFile: Boolean): MediaItem.SubtitleConfiguration {
        val file = if (isFile) DocumentFile.fromSingleUri(this, uri) else null
        val mimeType = if (isFile) {
            Logger.log("Sideload MimeType: ${contentResolver.getType(uri)}")
            when (contentResolver.getType(uri)) {
                MimeTypes.TEXT_VTT -> MimeTypes.TEXT_VTT
                MimeTypes.APPLICATION_TTML -> MimeTypes.APPLICATION_TTML
                MimeTypes.APPLICATION_SUBRIP -> MimeTypes.APPLICATION_SUBRIP
                MimeTypes.TEXT_SSA -> MimeTypes.TEXT_SSA
                getString(R.string.mimetype_binary) -> MimeTypes.TEXT_SSA
                else ->
                    file?.let { doc ->
                        Logger.log("Sideload Extension: ${doc.extension.lowercase()}")
                        extensionMimeType(doc.extension.lowercase())
                    } ?: MimeTypes.TEXT_UNKNOWN
            }
        } else {
            Logger.log("Sideload Extension: ${uri.toString().substringAfterLast(".")}")
            extensionMimeType(uri.toString().substringAfterLast("."))
        }
        return MediaItem.SubtitleConfiguration
            .Builder(uri)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .setMimeType(mimeType)
            .setLanguage("file")
            .setId(uri.toString())
            .build()
    }

    private fun buildSubtitleSource(mediaItem: MediaItem) {
        mediaSource = if (userSubtitles.isNotEmpty()) {
            MergingMediaSource(DefaultMediaSourceFactory(this).createMediaSource(mediaItem))
        } else {
            MergingMediaSource(DefaultMediaSourceFactory(cacheFactory).createMediaSource(mediaItem))
        }
        buildExoplayer()
    }

    fun onRemoveSubtitleFile(uriString: String?) {
        AlertDialog.Builder(this@ExoplayerView, R.style.MyPopup).apply {
            setTitle(R.string.remove_subtitle)
            setMessage(DocumentFile.fromSingleUri(this@ExoplayerView, Uri.parse(uriString))?.name)
            setPositiveButton(R.string.delete) { _, _ ->
                val subtitlePref = "${media.id}_${episode.number}_subtitles"
                val subs = PrefManager.getCustomVal<Set<String>>(
                    subtitlePref, setOf()
                ).minus(uriString)
                PrefManager.setCustomVal(subtitlePref, subs)
                userSubtitles.remove(userSubtitles.find { it.uri == Uri.parse(uriString) })
                if (exoPlayer.currentPosition > 5000) {
                    PrefManager.setCustomVal(
                        getEpisodeNumber(),
                        exoPlayer.currentPosition
                    )
                }
                model.setEpisode(episode, "Subtitle")
            }
            setNegativeButton(R.string.cancel) { _, _ -> }
            show()
            window?.setDimAmount(0.8f)
        }
    }

    private val onImportSubtitle = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { documents: List<Uri> ->
        userSubtitles.forEach {
            it.buildUpon().setSelectionFlags(C.SELECTION_FLAG_FORCED).build()
        }
        val subtitlePref = "${media.id}_${episode.number}_subtitles"
        documents.forEach { uri ->
            if (userSubtitles.any { it.uri == uri }) {
                snackString(R.string.duplicate_sub)
                return@forEach
            }
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val subs = PrefManager.getCustomVal<Set<String>>(
                subtitlePref, setOf()
            ).plus(uri.toString())
            PrefManager.setCustomVal(subtitlePref, subs)
            userSubtitles.add(importSubtitle(uri, true))
        }
        if (userSubtitles.isNotEmpty()) {
            buildSubtitleSource(
                mediaItem.buildUpon().setSubtitleConfigurations(userSubtitles).build()
            )
        }
    }

    private fun showSubtitleDialog() {
//        val dialogView = DialogUserAgentBinding.inflate(layoutInflater)
//        val editText = dialogView.userAgentTextBox.apply {
//            hint = getString(R.string.subtitle_url)
//        }
//        customAlertDialog().apply {
//            setCancellable(true)
//            setTitle(R.string.subtitle_url)
//            setCustomView(dialogView.root)
//            setPosButton(R.string.save) {
//                if (!editText.text.isNullOrBlank()) {
//                    val subs = mediaItem.localConfiguration?.subtitleConfigurations?.toMutableList()
//                        ?: mutableListOf<MediaItem.SubtitleConfiguration>()
//                    val uri = Uri.parse(editText.text.toString())
//                    if (subs.any { it.uri == uri }) {
//                        snackString(R.string.duplicate_sub)
//                        return@setPosButton
//                    }
//                    subs.forEach {
//                        it.buildUpon().setSelectionFlags(C.SELECTION_FLAG_FORCED).build()
//                    }
//                    subs.add(importSubtitle(uri, false))
//                    buildSubtitleSource(
//                        mediaItem.buildUpon().setSubtitleConfigurations(subs).build()
//                    )
//                }
//            }
//            setNeutralButton(R.string.import_file) {
//                try {
                    onImportSubtitle.launch(
                        arrayOf(
                            MimeTypes.TEXT_VTT,
                            MimeTypes.APPLICATION_TTML,
                            MimeTypes.APPLICATION_SUBRIP,
                            MimeTypes.TEXT_SSA,
                            getString(R.string.mimetype_binary)
                        )
                    )
//                } catch (ignored: ActivityNotFoundException) { }
//            }
//            setNegButton(R.string.cancel)
//            show()
//        }
    }

    private val onChangeSettings = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _: ActivityResult ->
        onSetTrackGroupOverride(dummyTrack, TRACK_TYPE_TEXT)
        if (PrefManager.getVal(PrefName.Subtitles)) {
            exoPlayer.currentTracks.groups.filter {
                it.mediaTrackGroup.type == TRACK_TYPE_TEXT
            } .forEach { trackGroup ->
                if (trackGroup.isSelected) {
                    onSetTrackGroupOverride(trackGroup, TRACK_TYPE_TEXT)
                }
            }
        }
        if (isInitialized) exoPlayer.play()
    }

    override fun onPlayerError(error: PlaybackException) {
        when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> {
                toast(getString(R.string.exo_source_exception, error.message))
                isPlayerPlaying = true
                sourceClick()
            }

            PlaybackException.ERROR_CODE_DECODING_FAILED -> {
                toast(getString(R.string.exo_decoding_failed, error.message))
                sourceClick()
            }

            else -> {
                toast(
                    getString(
                        R.string.exo_player_error,
                        error.errorCode,
                        error.errorCodeName,
                        error.message
                    )
                )
                Logger.log(error)
            }
        }
    }

    private var isBuffering = true
    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == ExoPlayer.STATE_READY) {
            exoPlayer.play()
            if (episodeLength == 0f) {
                episodeLength = exoPlayer.duration.toFloat()
            }
        }
        isBuffering = playbackState == Player.STATE_BUFFERING
        if (playbackState == Player.STATE_ENDED && PrefManager.getVal(PrefName.AutoPlay)) {
            if (interacted) exoNext.performClick()
            else toast(getString(R.string.autoplay_cancelled))
        }
        super.onPlaybackStateChanged(playbackState)
    }

    private fun updateAniProgress() {
        val incognito: Boolean = PrefManager.getVal(PrefName.Incognito)
        val episodeEnd = exoPlayer.currentPosition / episodeLength > PrefManager.getVal<Float>(
            PrefName.WatchPercentage
        )
        val episode0 = currentEpisodeIndex == 0 && PrefManager.getVal(PrefName.ChapterZeroPlayer)
        if (!incognito && (episodeEnd || episode0) && Anilist.userid != null
        )
            if (PrefManager.getCustomVal(
                    "${media.id}_save_progress",
                    true
                ) && (if (media.isAdult) PrefManager.getVal(PrefName.UpdateForHPlayer) else true)
            ) {
                if (episode0) {
                    updateProgress(media, "0")
                } else {
                    media.anime!!.selectedEpisode?.apply {
                        updateProgress(media, this)
                    }
                }
            }
    }

    private fun nextEpisode(toast: Boolean = true, runnable: ((Int) -> Unit)) {
        var isFiller = true
        var i = 1
        while (isFiller) {
            if (episodeArr.size > currentEpisodeIndex + i) {
                isFiller =
                    if (PrefManager.getVal(PrefName.AutoSkipFiller)) episodes[episodeArr[currentEpisodeIndex + i]]?.filler
                        ?: false else false
                if (!isFiller) runnable.invoke(i)
                i++
            } else {
                if (toast)
                    toast(getString(R.string.no_next_episode))
                isFiller = false
            }
        }
    }

    @SuppressLint("UnsafeIntentLaunch")
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        finishAndRemoveTask()
        startActivity(intent)
    }

    private fun playerTeardown() {
        if (isInitialized) {
            PrefManager.setCustomVal(getEpisodeNumber(), exoPlayer.currentPosition)
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        CoroutineScope(Dispatchers.IO).launch {
            tryWithSuspend(true) {
                extractor?.onVideoStopped(video)
            }
        }

        if (isInitialized) {
            updateAniProgress()
            exoPlayer.stop()
            releasePlayer()
        }

        torrent?.hash?.let {
            runBlocking(Dispatchers.IO) { removeTorrent(it) }
            torrent = null
        }

        finishAndRemoveTask()
    }

    override fun onDestroy() {
        playerTeardown()
        super.onDestroy()
    }

    private fun getEpisodeNumber(): String {
        return if (this::episode.isInitialized) {
            "${media.id}_${episode.number}"
        } else {
            media.anime?.let { "${media.id}_${it.selectedEpisode}" }
                ?: "${media.id}_${episodeArr[currentEpisodeIndex]}"
        }
    }

    // Enter PiP Mode
    @Suppress("DEPRECATION")
    private fun enterPipMode() {
        wasPlaying = isPlayerPlaying
        if (!pipEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                enterPictureInPictureMode(getPictureInPictureBuilder().build())
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                enterPictureInPictureMode()
            }
        } catch (e: Exception) {
            logError(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getPictureInPictureBuilder(): PictureInPictureParams.Builder {
        return PictureInPictureParams.Builder().also {
            setPictureInPictureParams(it.setAspectRatio(aspectRatio).build())
        }
    }

    private fun onPiPChanged(isInPictureInPictureMode: Boolean) {
        playerView.useController = !isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            orientationListener?.disable()
        } else {
            orientationListener?.enable()
        }
        if (lifecycle.currentState == Lifecycle.State.CREATED && !isInPictureInPictureMode) {
            playerTeardown()
        } else {
            if (wasPlaying) exoPlayer.play()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        onPiPChanged(isInPictureInPictureMode)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onPictureInPictureUiStateChanged(pipState: PictureInPictureUiState) {
        super.onPictureInPictureUiStateChanged(pipState)
        onPiPChanged(isInPictureInPictureMode)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        onPiPChanged(isInPictureInPictureMode)
    }

    private val keyMap: MutableMap<Int, (() -> Unit)?> = mutableMapOf(
        KEYCODE_DPAD_RIGHT to null,
        KEYCODE_DPAD_LEFT to null,
        KEYCODE_SPACE to { exoPlay.performClick() },
        KEYCODE_N to { exoNext.performClick() },
        KEYCODE_B to { exoPrev.performClick() }
    )

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return if (keyMap.containsKey(event.keyCode)) {
            (event.action == ACTION_UP).also {
                if (isInitialized && it) keyMap[event.keyCode]?.invoke()
            }
        } else {
            super.dispatchKeyEvent(event)
        }
    }

    // Cast
    private fun cast() {
        val videoURL = video?.file?.url ?: return
        val subtitleUrl = if (subsEmbedded) video!!.file.url else subtitle!!.file.url
        val shareVideo = Intent(Intent.ACTION_VIEW)
        shareVideo.setDataAndType(Uri.parse(videoURL), "video/*")
        shareVideo.setPackage("com.instantbits.cast.webvideo")
        if (subtitle != null) shareVideo.putExtra("subtitle", subtitleUrl)
        shareVideo.putExtra(
            "title",
            media.userPreferredName + " : Ep " + episodeTitleArr[currentEpisodeIndex]
        )
        shareVideo.putExtra("poster", episode.thumb?.url ?: media.cover)
        val headers = Bundle()
        defaultHeaders.forEach {
            headers.putString(it.key, it.value)
        }
        video?.file?.headers?.forEach {
            headers.putString(it.key, it.value)
        }
        shareVideo.putExtra("android.media.intent.extra.HTTP_HEADERS", headers)
        shareVideo.putExtra("secure_uri", true)
        try {
            startActivity(shareVideo)
        } catch (ex: ActivityNotFoundException) {
            val intent = Intent(Intent.ACTION_VIEW)
            val uriString = "market://details?id=com.instantbits.cast.webvideo"
            intent.data = Uri.parse(uriString)
            startActivity(intent)
        }
    }

    private fun startCastPlayer() {
        if (!isCastApiAvailable) {
            snackString("Cast API not available")
            return
        }
        //make sure mediaItem is initialized and castPlayer is not null
        if (!this::mediaItem.isInitialized || castPlayer == null) return
        castPlayer?.setMediaItem(mediaItem)
        castPlayer?.prepare()
        playerView.player = castPlayer
        exoPlayer.stop()
        castPlayer?.addListener(object : Player.Listener {
            //if the player is paused changed, we want to update the UI
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                super.onPlayWhenReadyChanged(playWhenReady, reason)
                if (playWhenReady) {
                    (exoPlay.drawable as Animatable?)?.start()
                    Glide.with(this@ExoplayerView)
                        .load(R.drawable.anim_play_to_pause)
                        .into(exoPlay)
                } else {
                    (exoPlay.drawable as Animatable?)?.start()
                    Glide.with(this@ExoplayerView)
                        .load(R.drawable.anim_pause_to_play)
                        .into(exoPlay)
                }
            }
        })
    }

    private fun startExoPlayer() {
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        playerView.player = exoPlayer
        castPlayer?.stop()
    }

    override fun onCastSessionAvailable() {
        if (isCastApiAvailable && !this.isDestroyed) {
            startCastPlayer()
        }
    }

    override fun onCastSessionUnavailable() {
        startExoPlayer()
    }

    @SuppressLint("ViewConstructor")
    class ExtendedTimeBar(
        context: Context,
        attrs: AttributeSet?
    ) : DefaultTimeBar(context, attrs) {
        private var enabled = false
        private var forceDisabled = false
        override fun setEnabled(enabled: Boolean) {
            this.enabled = enabled
            super.setEnabled(!forceDisabled && this.enabled)
        }

        fun setForceDisabled(forceDisabled: Boolean) {
            this.forceDisabled = forceDisabled
            isEnabled = enabled
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        proximity?.let {
            if (event.sensor.type == Sensor.TYPE_PROXIMITY && event.values[0] < it.maximumRange)
                playerView.keepScreenOn = exoPlayer.isPlaying
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}