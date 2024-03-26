package ani.dantotsu.widgets.resumable

import android.content.Intent
import android.widget.RemoteViewsService
import ani.dantotsu.util.Logger

class ResumableRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        Logger.log("ResumableRemoteViewsFactory onGetViewFactory")
        return ResumableRemoteViewsFactory(applicationContext)
    }
}
