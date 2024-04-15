package ani.matagi.widgets.resumable

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.widget.RemoteViewsService

class ResumableRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId: Int = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        return ResumableRemoteViewsFactory(applicationContext, appWidgetId)
    }
}
