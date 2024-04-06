package ani.dantotsu.widgets.resumable

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import ani.dantotsu.R
import ani.dantotsu.util.BitmapUtil
import ani.dantotsu.util.BitmapUtil.Companion.roundCorners
import ani.dantotsu.util.Logger
import ani.dantotsu.widgets.resumable.ResumableWidget.Companion.widgetItems
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class ResumableRemoteViewsFactory(private val context: Context, appWidgetId: Int) :
    RemoteViewsService.RemoteViewsFactory {
    private var refreshing = false

    private val prefs =
        context.getSharedPreferences(ResumableWidget.getPrefsName(appWidgetId), Context.MODE_PRIVATE)

    override fun onCreate() {
        Logger.log("ResumableRemoteViewsFactory onCreate")
        ResumableWidget.fillWidgetItems(prefs)
    }

    override fun onDataSetChanged() {
        if (refreshing) return
        Logger.log("ResumableRemoteViewsFactory onDataSetChanged")
        widgetItems.clear()
        ResumableWidget.fillWidgetItems(prefs)
    }

    override fun onDestroy() {
        widgetItems.clear()
    }

    override fun getCount(): Int {
        return widgetItems.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        val item = widgetItems[position]
        val titleTextColor = prefs.getInt(ResumableWidget.PREF_TITLE_TEXT_COLOR, Color.WHITE)
        val rv = RemoteViews(context.packageName, R.layout.item_resumable_widget).apply {
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

        return rv
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.item_resumable_widget)
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(id: Int): Long {
        return id.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }
}

data class WidgetItem(val title: String, val image: String, val id: Int)

enum class ResumableType(val type: String) {
    CONTINUE_ANIME("Continue Watching"),
    CONTINUE_MANGA("Continue Reading"),
    CONTINUE_MEDIA("Continue Anything");
}