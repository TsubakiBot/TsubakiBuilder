package ani.matagi.widgets.resumable

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
import ani.dantotsu.util.BitmapUtil
import ani.dantotsu.widgets.WidgetSizeProvider
import ani.matagi.collections.Collections.mix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

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
            context.getSharedPreferences(getPrefsName(it), Context.MODE_PRIVATE).edit().clear().apply()
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
        private val continueAnime: ArrayList<Media> = arrayListOf()
        private val continueManga: ArrayList<Media> = arrayListOf()

        fun injectUpdate(context: Context?, anime: ArrayList<Media>?, manga: ArrayList<Media>?) {
            if (null == context) return
            val appWidgetManager = AppWidgetManager.getInstance(context)
            anime?.let { list -> continueAnime.addAll(list) }
            manga?.let { list -> continueManga.addAll(list) }
            appWidgetManager.getAppWidgetIds(ComponentName(context, ResumableWidget::class.java)).forEach {
                appWidgetManager.notifyAppWidgetViewDataChanged(it, R.id.widgetViewFlipper)
            }
        }

        private suspend fun getContinueItems(type: String): MutableList<WidgetItem> {
            val mediaItems = mutableListOf<WidgetItem>()
            val continueMedia = if (type == "MANGA") continueManga else continueAnime
            coroutineScope {
                continueMedia.ifEmpty { Anilist.query.continueMedia(type) }.map { media ->
                    async(Dispatchers.IO) {
                        mediaItems.add(
                            WidgetItem(
                                media.userPreferredName,
                                media.cover ?: "",
                                media.id
                            )
                        )
                    }
                }
            }.awaitAll()
            return mediaItems
        }

        fun fillWidgetItems(prefs: SharedPreferences) : MutableList<WidgetItem> {
            refreshing = true
            runBlocking(Dispatchers.IO) {
                when (prefs.getInt(PREF_WIDGET_TYPE, 2)) {
                    ResumableType.CONTINUE_ANIME.ordinal -> {
                        widgetItems.addAll(getContinueItems("ANIME"))
                    }
                    ResumableType.CONTINUE_MANGA.ordinal -> {
                        widgetItems.addAll(getContinueItems("MANGA"))
                    }
                    else -> {
                        widgetItems.addAll(
                            getContinueItems("ANIME").mix(getContinueItems("MANGA"))
                        )
                    }
                }
                refreshing = false
                continueAnime.clear()
                continueManga.clear()
            }
            return widgetItems
        }

        private fun getPendingSelfIntent(context: Context, appWidgetId: Int, action: String): PendingIntent {
            val intent = Intent(context, ResumableWidget::class.java).setAction(action).apply{
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
                val prefs = context.getSharedPreferences(getPrefsName(appWidgetId), Context.MODE_PRIVATE)
                val titleTextColor = prefs.getInt(PREF_TITLE_TEXT_COLOR, Color.WHITE)
                widgetItems.clear()
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

            val prefs = context.getSharedPreferences(getPrefsName(appWidgetId), Context.MODE_PRIVATE)
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

        fun getPrefsName(appWidgetId: Int): String {
            return "ani.dantotsu.widgets.ResumableWidget.${appWidgetId}"
        }
        const val PREF_BACKGROUND_COLOR = "background_color"
        const val PREF_BACKGROUND_FADE = "background_fade"
        const val PREF_TITLE_TEXT_COLOR = "title_text_color"
        const val PREF_FLIPPER_IMG_COLOR = "flipper_img_color"
        const val PREF_WIDGET_TYPE = "widget_type"

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
