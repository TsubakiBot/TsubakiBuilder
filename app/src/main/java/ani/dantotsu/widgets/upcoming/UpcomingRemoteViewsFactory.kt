package ani.dantotsu.widgets.upcoming

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.media.Media
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.util.BitmapUtil
import ani.dantotsu.util.Logger
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SAnimeImpl
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.SEpisodeImpl
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SChapterImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

class UpcomingRemoteViewsFactory(private val context: Context, appWidgetId: Int) :
    RemoteViewsService.RemoteViewsFactory {
    private var widgetItems = mutableListOf<WidgetItem>()
    private var refreshing = false
    private val prefs = context.getSharedPreferences(
        UpcomingWidget.getPrefsName(appWidgetId), Context.MODE_PRIVATE
    )

    override fun onCreate() {
        Logger.log("UpcomingRemoteViewsFactory onCreate")
        fillWidgetItems()
    }

    private fun timeUntil(timeUntil: Long): String {
        val days = timeUntil / (1000 * 60 * 60 * 24)
        val hours = (timeUntil % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)
        val minutes = ((timeUntil % (1000 * 60 * 60 * 24)) % (1000 * 60 * 60)) / (1000 * 60)
        return "$days days $hours hours $minutes minutes"
    }

    override fun onDataSetChanged() {
        if (refreshing) return
        Logger.log("UpcomingRemoteViewsFactory onDataSetChanged")
        widgetItems.clear()
        fillWidgetItems()

    }

    private fun fillWidgetItems() {
        refreshing = true
        val userId = PrefManager.getVal<String>(PrefName.AnilistUserId)
        val lastUpdated = prefs.getLong(UpcomingWidget.LAST_UPDATE, 0)
        val serializedMedia = prefs.getString(UpcomingWidget.PREF_SERIALIZED_MEDIA, null)
        val mediaList =
            if (System.currentTimeMillis() - lastUpdated > 1000 * 60 * 60 * 4 || serializedMedia.isNullOrEmpty()) {
                prefs.edit().putLong(UpcomingWidget.LAST_UPDATE, 0).apply()
                prefs.edit().remove(UpcomingWidget.PREF_SERIALIZED_MEDIA).apply()
                listOf()
            } else {
                deserializeMedia(serializedMedia)
            }
        runBlocking(Dispatchers.IO) {
            val upcoming = mediaList.ifEmpty {
                prefs.edit().putLong(UpcomingWidget.LAST_UPDATE, System.currentTimeMillis()).apply()
                Anilist.query.getUpcomingAnime(userId)
            }
            upcoming.map {
                async(Dispatchers.IO) {
                    widgetItems.add(
                        WidgetItem(
                            it.userPreferredName,
                            timeUntil(it.timeUntilAiring ?: 0),
                            it.cover ?: "",
                            it.banner ?: "",
                            it.id
                        )
                    )
                }
            }.awaitAll()
            serializeMedia(upcoming)?.let {
                prefs.edit().putString(UpcomingWidget.PREF_SERIALIZED_MEDIA, it).apply()
            } ?: prefs.edit().remove(UpcomingWidget.PREF_SERIALIZED_MEDIA).apply()
            refreshing = false
        }
    }

    private val upcomingGson = GsonBuilder()
        .registerTypeAdapter(SChapter::class.java, InstanceCreator<SChapter> {
            SChapterImpl() // Provide an instance of SChapterImpl
        })
        .registerTypeAdapter(SAnime::class.java, InstanceCreator<SAnime> {
            SAnimeImpl() // Provide an instance of SAnimeImpl
        })
        .registerTypeAdapter(SEpisode::class.java, InstanceCreator<SEpisode> {
            SEpisodeImpl() // Provide an instance of SEpisodeImpl
        })
        .create()

    private fun serializeMedia(media: List<Media>): String? {
        return try {
            val json = upcomingGson.toJson(media)
            json
        } catch (e: Exception) {
            Logger.log("Error serializing media: $e")
            Logger.log(e)
            null
        }
    }

    private fun deserializeMedia(json: String): List<Media> {
        return try {
            val media = upcomingGson.fromJson(json, Array<Media>::class.java).toList()
            media
        } catch (e: Exception) {
            Logger.log("Error deserializing media: $e")
            Logger.log(e)
            listOf()
        }
    }

    override fun onDestroy() {
        widgetItems.clear()
    }

    override fun getCount(): Int {
        return widgetItems.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        Logger.log("UpcomingRemoteViewsFactory getViewAt")
        val item = widgetItems[position]
        val titleTextColor = prefs.getInt(UpcomingWidget.PREF_TITLE_TEXT_COLOR, Color.WHITE)
        val countdownTextColor =
            prefs.getInt(UpcomingWidget.PREF_COUNTDOWN_TEXT_COLOR, Color.WHITE)
        val rv = RemoteViews(context.packageName, R.layout.item_upcoming_widget).apply {
            setTextViewText(R.id.text_show_title, item.title)
            setTextViewText(R.id.text_show_countdown, item.countdown)
            setTextColor(R.id.text_show_title, titleTextColor)
            setTextColor(R.id.text_show_countdown, countdownTextColor)
            val bitmap = BitmapUtil.downloadImageAsBitmap(item.image)
            setImageViewBitmap(R.id.image_show_icon, bitmap)
            val banner = BitmapUtil.downloadImageAsBitmap(item.banner)
            setImageViewBitmap(R.id.image_show_banner, banner)
            val fillInIntent = Intent().apply {
                putExtra("mediaId", item.id)
            }
            setOnClickFillInIntent(R.id.widget_item, fillInIntent)
        }

        return rv
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.item_upcoming_widget)
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }
}

data class WidgetItem(val title: String, val countdown: String, val image: String, val banner: String, val id: Int)
