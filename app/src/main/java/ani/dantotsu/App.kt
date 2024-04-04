package ani.dantotsu

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import ani.dantotsu.aniyomi.anime.custom.AppModule
import ani.dantotsu.aniyomi.anime.custom.PreferenceModule
import ani.dantotsu.connections.comments.CommentsAPI
import ani.dantotsu.notifications.TaskScheduler
import ani.dantotsu.parsers.AnimeSources
import ani.dantotsu.parsers.MangaSources
import ani.dantotsu.parsers.NovelSources
import ani.dantotsu.parsers.novel.NovelExtensionManager
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.util.FinalExceptionHandler
import ani.dantotsu.util.Logger
import com.eightbit.io.Debug
import com.google.android.material.color.DynamicColors
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess


@SuppressLint("StaticFieldLeak")
class App : MultiDexApplication() {
    private lateinit var animeExtensionManager: AnimeExtensionManager
    private lateinit var mangaExtensionManager: MangaExtensionManager
    private lateinit var novelExtensionManager: NovelExtensionManager
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    init {
        instance = this
    }

    val mFTActivityLifecycleCallbacks = FTActivityLifecycleCallbacks()

    override fun onCreate() {
        super.onCreate()

        PrefManager.init(this)
        Injekt.importModule(AppModule(this))
        Injekt.importModule(PreferenceModule(this))

        val useMaterialYou: Boolean = PrefManager.getVal(PrefName.UseMaterialYou)
        if (useMaterialYou) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
        registerActivityLifecycleCallbacks(mFTActivityLifecycleCallbacks)

        Logger.init(this)
        Thread.setDefaultUncaughtExceptionHandler { _: Thread?, error: Throwable ->
            Logger.log(error)
            val exception = StringWriter().apply {
                error.printStackTrace(PrintWriter(this))
            }
            try {
                Debug.clipException(this, exception.toString())
            } catch (ignored: Exception) { }
        }

        initializeNetwork()

        setupNotificationChannels()
        if (!LogcatLogger.isInstalled) {
            LogcatLogger.install(AndroidLogcatLogger(LogPriority.VERBOSE))
        }

        animeExtensionManager = Injekt.get()
        mangaExtensionManager = Injekt.get()
        novelExtensionManager = Injekt.get()

        CoroutineScope(Dispatchers.IO).launch {
            loadAnimeExtensions()
            loadMangaExtensions()
            loadNovelExtensions()
        }
        if (PrefManager.getVal(PrefName.CommentsOptIn)) {
            CoroutineScope(Dispatchers.Default).launch { CommentsAPI.fetchAuthToken() }
        }

        val useAlarmManager = PrefManager.getVal<Boolean>(PrefName.UseAlarmManager)
        val scheduler = TaskScheduler.create(this, useAlarmManager)
        scheduler.scheduleAllTasks(this)
        scheduler.scheduleSingleWork(this)
    }

    private suspend fun loadAnimeExtensions() = withContext(Dispatchers.IO) {
        animeExtensionManager.findAvailableExtensions()
        Logger.log("Anime Extensions: ${animeExtensionManager.installedExtensionsFlow.first()}")
        AnimeSources.init(animeExtensionManager.installedExtensionsFlow)
    }

    private suspend fun loadMangaExtensions() = withContext(Dispatchers.IO) {
        mangaExtensionManager.findAvailableExtensions()
        Logger.log("Manga Extensions: ${mangaExtensionManager.installedExtensionsFlow.first()}")
        MangaSources.init(mangaExtensionManager.installedExtensionsFlow)
    }

    private suspend fun loadNovelExtensions() = withContext(Dispatchers.IO) {
        novelExtensionManager.findAvailableExtensions()
        Logger.log("Novel Extensions: ${novelExtensionManager.installedExtensionsFlow.first()}")
        NovelSources.init(novelExtensionManager.installedExtensionsFlow)
    }

    private fun setupNotificationChannels() {
        try {
            Notifications.createChannels(this)
        } catch (e: Exception) {
            Logger.log("Failed to modify notification channels")
            Logger.log(e)
        }
    }

    inner class FTActivityLifecycleCallbacks : ActivityLifecycleCallbacks {
        var currentActivity: Activity? = null
        override fun onActivityCreated(p0: Activity, p1: Bundle?) {}
        override fun onActivityStarted(p0: Activity) {
            currentActivity = p0
        }

        override fun onActivityResumed(p0: Activity) {
            currentActivity = p0
        }

        override fun onActivityPaused(p0: Activity) {}
        override fun onActivityStopped(p0: Activity) {}
        override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}
        override fun onActivityDestroyed(p0: Activity) {}
    }

    companion object {
        private var instance: App? = null

        /** Reference to the application context.
         *
         * USE WITH EXTREME CAUTION!**/
        var context: Context? = null
        fun currentContext(): Context? {
            return instance?.mFTActivityLifecycleCallbacks?.currentActivity ?: context
        }

        fun currentActivity(): Activity? {
            return instance?.mFTActivityLifecycleCallbacks?.currentActivity
        }
    }
}