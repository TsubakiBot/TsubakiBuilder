package ani.dantotsu.widgets.resumable

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import ani.dantotsu.MainActivity
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaType
import ani.dantotsu.util.BitmapUtil
import ani.dantotsu.util.Logger
import ani.dantotsu.widgets.WidgetSizeProvider
import ani.dantotsu.widgets.upcoming.UpcomingWidget
import ani.himitsu.collections.Collections.mix
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SAnimeImpl
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.SEpisodeImpl
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SChapterImpl
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [ResumableWidgetConfigure]
 */
class ResumableWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val rv = updateAppWidget(context, appWidgetId)
            appWidgetManager.updateAppWidget(appWidgetId, rv)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach {
            context.getSharedPreferences(getPrefsName(it), Context.MODE_PRIVATE).edit().clear()
                .apply()
        }
        super.onDeleted(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetId: Int = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val views = RemoteViews(context.packageName, R.layout.resumable_widget).apply {
            if (VIEWFLIPPER_NEXT == intent.action) {
                @Suppress("DEPRECATION") showNext(R.id.widgetViewFlipper)
            }
            if (VIEWFLIPPER_PREV == intent.action) {
                @Suppress("DEPRECATION") showPrevious(R.id.widgetViewFlipper)
            }
        }
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        if (context != null && appWidgetManager != null) {
            val views = updateAppWidget(context, appWidgetId)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    companion object {
        var widgetItems = mutableListOf<WidgetItem>()
        var refreshing = false

        suspend fun injectUpdate(
            context: Context?, anime: ArrayList<Media>?, manga: ArrayList<Media>?
        ) = withContext(Dispatchers.IO) {
            if (null == context) return@withContext
            val appWidgetManager = AppWidgetManager.getInstance(context)

            val serializedAnime = anime?.let { list -> serializeAnime(list) }
            val serializedManga = manga?.let { list -> serializeManga(list) }
            appWidgetManager.getAppWidgetIds(ComponentName(context, ResumableWidget::class.java))
                .forEach {
                    val prefs = context.getSharedPreferences(getPrefsName(it), Context.MODE_PRIVATE)
                    serializedAnime?.let { list ->
                        prefs.edit().putString(PREF_SERIALIZED_ANIME, list).apply()
                    }
                        ?: prefs.edit().remove(PREF_SERIALIZED_ANIME).apply()
                    serializedManga?.let { list ->
                        prefs.edit().putString(PREF_SERIALIZED_MANGA, list).apply()
                    }
                        ?: prefs.edit().remove(PREF_SERIALIZED_MANGA).apply()
                    prefs.edit().putLong(UpcomingWidget.LAST_UPDATE, System.currentTimeMillis())
                        .apply()
                    appWidgetManager.notifyAppWidgetViewDataChanged(it, R.id.widgetViewFlipper)
                }
        }

        private suspend fun getContinueItems(
            prefs: SharedPreferences,
            type: MediaType?
        ): MutableList<WidgetItem> {
            val mediaItems = mutableListOf<WidgetItem>()

            val expired = System.currentTimeMillis() - prefs.getLong(LAST_UPDATE, 0) > 28800000
            val serializedAnime = prefs.getString(PREF_SERIALIZED_ANIME, null)
            val serializedManga = prefs.getString(PREF_SERIALIZED_MANGA, null)

            val continueAnime: ArrayList<Media> = arrayListOf()
            val continueManga: ArrayList<Media> = arrayListOf()

            when (type) {
                MediaType.ANIME -> {
                    continueAnime.addAll(
                        if (expired || serializedAnime.isNullOrEmpty()) {
                            prefs.edit().putLong(LAST_UPDATE, 0).apply()
                            prefs.edit().remove(PREF_SERIALIZED_ANIME).apply()
                            listOf()
                        } else {
                            deserializeAnime(serializedAnime)
                        }
                    )
                }

                MediaType.MANGA -> {
                    continueManga.addAll(
                        if (expired || serializedManga.isNullOrEmpty()) {
                            prefs.edit().putLong(LAST_UPDATE, 0).apply()
                            prefs.edit().remove(PREF_SERIALIZED_MANGA).apply()
                            listOf()
                        } else {
                            deserializeManga(serializedManga)
                        }
                    )
                }

                else -> {
                    continueAnime.addAll(
                        if (expired || serializedAnime.isNullOrEmpty()) {
                            prefs.edit().putLong(LAST_UPDATE, 0).apply()
                            listOf()
                        } else {
                            deserializeAnime(serializedAnime)
                        }
                    )
                    continueManga.addAll(
                        if (expired || serializedManga.isNullOrEmpty()) {
                            prefs.edit().putLong(LAST_UPDATE, 0).apply()
                            listOf()
                        } else {
                            deserializeManga(serializedManga)
                        }
                    )
                }
            }
            val resumableAnime = if (type == null || type == MediaType.ANIME)
                continueAnime.ifEmpty {
                    prefs.edit().putLong(LAST_UPDATE, System.currentTimeMillis()).apply()
                    Anilist.query.initResumable(MediaType.ANIME)
                }
            else listOf()
            val resumableManga = if (type == null || type == MediaType.MANGA)
                continueManga.ifEmpty {
                    prefs.edit().putLong(LAST_UPDATE, System.currentTimeMillis()).apply()
                    Anilist.query.initResumable(MediaType.MANGA)
                }
            else listOf()
            val resumable = when (type) {
                MediaType.ANIME -> {
                    resumableAnime
                }

                MediaType.MANGA -> {
                    resumableManga
                }

                else -> {
                    resumableAnime.mix(resumableManga)
                }
            }
            coroutineScope {
                resumable.map { media ->
                    async(Dispatchers.IO) {
                        mediaItems.add(
                            WidgetItem(
                                media.userPreferredName,
                                media.cover ?: "",
                                media.id
                            )
                        )
                    }
                }.awaitAll()
            }
            when (type) {
                MediaType.ANIME -> {
                    serializeAnime(resumableAnime)?.let {
                        prefs.edit().putString(UpcomingWidget.PREF_SERIALIZED_MEDIA, it).apply()
                    } ?: prefs.edit().remove(UpcomingWidget.PREF_SERIALIZED_MEDIA).apply()
                }

                MediaType.MANGA -> {
                    serializeManga(resumableManga)?.let {
                        prefs.edit().putString(UpcomingWidget.PREF_SERIALIZED_MEDIA, it).apply()
                    } ?: prefs.edit().remove(UpcomingWidget.PREF_SERIALIZED_MEDIA).apply()
                }

                else -> {
                    serializeAnime(resumableAnime)?.let {
                        prefs.edit().putString(UpcomingWidget.PREF_SERIALIZED_MEDIA, it).apply()
                    } ?: prefs.edit().remove(UpcomingWidget.PREF_SERIALIZED_MEDIA).apply()
                    serializeManga(resumableManga)?.let {
                        prefs.edit().putString(UpcomingWidget.PREF_SERIALIZED_MEDIA, it).apply()
                    } ?: prefs.edit().remove(UpcomingWidget.PREF_SERIALIZED_MEDIA).apply()
                }
            }

            return mediaItems
        }

        fun fillWidgetItems(prefs: SharedPreferences): MutableList<WidgetItem> {
            refreshing = true
            widgetItems.clear()
            runBlocking(Dispatchers.IO) {
                when (prefs.getInt(PREF_WIDGET_TYPE, 2)) {
                    ResumableType.CONTINUE_ANIME.ordinal -> {
                        widgetItems.addAll(getContinueItems(prefs, MediaType.ANIME))
                    }

                    ResumableType.CONTINUE_MANGA.ordinal -> {
                        widgetItems.addAll(getContinueItems(prefs, MediaType.MANGA))
                    }

                    else -> {
                        widgetItems.addAll(getContinueItems(prefs, null))
                    }
                }
                refreshing = false
            }
            return widgetItems
        }

        private fun getPendingSelfIntent(
            context: Context,
            appWidgetId: Int,
            action: String
        ): PendingIntent {
            val intent = Intent(context, ResumableWidget::class.java).setAction(action).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            return PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        }

        private fun RemoteViews.setLocalAdapter(context: Context, appWidgetId: Int, view: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val builder = RemoteViews.RemoteCollectionItems.Builder()
                val prefs =
                    context.getSharedPreferences(getPrefsName(appWidgetId), Context.MODE_PRIVATE)
                val titleTextColor = prefs.getInt(PREF_TITLE_TEXT_COLOR, Color.WHITE)
                fillWidgetItems(prefs).forEach { item ->
                    val rv =
                        RemoteViews(context.packageName, R.layout.item_resumable_widget).apply {
                            setTextViewText(R.id.text_show_title, item.title)
                            setTextColor(R.id.text_show_title, titleTextColor)
                            val bitmap = BitmapUtil.downloadImageAsBitmap(item.image)
                            setImageViewBitmap(R.id.image_show_icon, bitmap)
                            val fillInIntent = Intent().apply {
                                putExtra("mediaId", item.id)
                                putExtra("continue", true)
                            }
                            setOnClickFillInIntent(R.id.image_show_icon, fillInIntent)
                        }
                    builder.addItem(item.id.toLong(), rv)
                }
                setRemoteAdapter(view, builder.setViewTypeCount(1).build())
            } else {
                val intent = Intent(context, ResumableRemoteViewsService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                }
                setRemoteAdapter(view, intent)
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetId: Int,
        ): RemoteViews {
            val intentTemplate = Intent(context, MainActivity::class.java)
            intentTemplate.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intentTemplate.putExtra("fromWidget", true)

            val prefs =
                context.getSharedPreferences(getPrefsName(appWidgetId), Context.MODE_PRIVATE)
            val backgroundColor = prefs.getInt(PREF_BACKGROUND_COLOR, Color.parseColor("#80000000"))
            val backgroundFade = prefs.getInt(PREF_BACKGROUND_FADE, Color.parseColor("#00000000"))
            val titleTextColor = prefs.getInt(PREF_TITLE_TEXT_COLOR, Color.WHITE)
            val flipperImgColor = prefs.getInt(PREF_FLIPPER_IMG_COLOR, Color.WHITE)

            val gradientDrawable = ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.linear_gradient_black,
                null
            ) as GradientDrawable
            gradientDrawable.colors = intArrayOf(backgroundColor, backgroundFade)
            val widgetSizeProvider = WidgetSizeProvider(context)
            var (width, height) = widgetSizeProvider.getWidgetsSize(appWidgetId)
            if (width > 0 && height > 0) {
                gradientDrawable.cornerRadius = 64f
            } else {
                width = 300
                height = 300
            }

            val flipperDrawable = ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ic_round_arrow_back_ios_new_24,
                null
            ) as Drawable
            flipperDrawable.setTint(flipperImgColor)

            val views = RemoteViews(context.packageName, R.layout.resumable_widget).apply {
                setImageViewBitmap(R.id.backgroundView, gradientDrawable.toBitmap(width, height))
                setTextColor(R.id.widgetTitle, titleTextColor)
                setTextColor(R.id.text_show_title, titleTextColor)
                setTextColor(R.id.empty_view, titleTextColor)
                setImageViewBitmap(R.id.leftFlipper, flipperDrawable.toBitmap())
                setImageViewBitmap(R.id.rightFlipper, flipperDrawable.toBitmap())

                setPendingIntentTemplate(
                    R.id.widgetViewFlipper,
                    PendingIntent.getActivity(
                        context,
                        0,
                        intentTemplate,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    )
                )
                if (refreshing) return@apply
                setLocalAdapter(context, appWidgetId, R.id.widgetViewFlipper)
                setEmptyView(R.id.widgetViewFlipper, R.id.empty_view)

                setOnClickPendingIntent(
                    R.id.leftFlipper, getPendingSelfIntent(context, appWidgetId, VIEWFLIPPER_PREV)
                )
                setOnClickPendingIntent(
                    R.id.rightFlipper, getPendingSelfIntent(context, appWidgetId, VIEWFLIPPER_NEXT)
                )

                setOnClickPendingIntent(
                    R.id.widgetTitle,
                    PendingIntent.getActivity(
                        context,
                        1,
                        Intent(context, ResumableWidgetConfigure::class.java).apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
            return views
        }

        suspend fun notifyDataSetChanged(context: Context) = withContext(Dispatchers.IO) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            appWidgetManager.getAppWidgetIds(ComponentName(context, ResumableWidget::class.java))
                .forEach {
                    appWidgetManager.notifyAppWidgetViewDataChanged(it, R.id.widgetViewFlipper)
                }
        }

        private val animeGson = GsonBuilder()
            .registerTypeAdapter(SAnime::class.java, InstanceCreator<SAnime> {
                SAnimeImpl() // Provide an instance of SAnimeImpl
            })
            .registerTypeAdapter(SEpisode::class.java, InstanceCreator<SEpisode> {
                SEpisodeImpl() // Provide an instance of SEpisodeImpl
            })
            .create()

        private fun serializeAnime(media: List<Media>): String? {
            return try {
                val json = animeGson.toJson(media)
                json
            } catch (e: Exception) {
                Logger.log(e)
                null
            }
        }

        private fun deserializeAnime(json: String): List<Media> {
            return try {
                val media = animeGson.fromJson(json, Array<Media>::class.java).toList()
                media
            } catch (e: Exception) {
                Logger.log(e)
                listOf()
            }
        }

        private val mangaGson = GsonBuilder()
            .registerTypeAdapter(SManga::class.java, InstanceCreator<SManga> {
                SMangaImpl() // Provide an instance of SMangaImpl
            })
            .registerTypeAdapter(SChapter::class.java, InstanceCreator<SChapter> {
                SChapterImpl() // Provide an instance of SChapterImpl
            })
            .create()

        private fun serializeManga(media: List<Media>): String? {
            return try {
                val json = mangaGson.toJson(media)
                json
            } catch (e: Exception) {
                Logger.log(e)
                null
            }
        }

        private fun deserializeManga(json: String): List<Media> {
            return try {
                val media = mangaGson.fromJson(json, Array<Media>::class.java).toList()
                media
            } catch (e: Exception) {
                Logger.log(e)
                listOf()
            }
        }


        fun getPrefsName(appWidgetId: Int): String {
            return "ani.dantotsu.widgets.ResumableWidget.${appWidgetId}"
        }

        const val PREF_BACKGROUND_COLOR = "background_color"
        const val PREF_BACKGROUND_FADE = "background_fade"
        const val PREF_TITLE_TEXT_COLOR = "title_text_color"
        const val PREF_FLIPPER_IMG_COLOR = "flipper_img_color"
        const val PREF_WIDGET_TYPE = "widget_type"

        private const val PREF_SERIALIZED_ANIME = "serialized_anime"
        private const val PREF_SERIALIZED_MANGA = "serialized_manga"
        private const val LAST_UPDATE = "last_update"

        const val VIEWFLIPPER_NEXT = "viewflipper_next"
        const val VIEWFLIPPER_PREV = "viewflipper_prev"
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = ResumableWidget.updateAppWidget(context, appWidgetId)
    appWidgetManager.updateAppWidget(appWidgetId, views)
    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetViewFlipper)
}
