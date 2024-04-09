package ani.dantotsu.widgets.upcoming

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.widget.RemoteViewsService
import ani.dantotsu.util.Logger

class UpcomingRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId: Int = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        return UpcomingRemoteViewsFactory(applicationContext, appWidgetId)
    }
}
