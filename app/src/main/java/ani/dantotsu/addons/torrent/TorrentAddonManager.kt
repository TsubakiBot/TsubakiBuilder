package ani.dantotsu.addons.torrent

import android.content.Context
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ani.dantotsu.R
import ani.dantotsu.addons.AddonDownloader.Companion.hasUpdate
import ani.dantotsu.addons.AddonLoader
import ani.dantotsu.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TorrentAddonManager(
    private val context: Context
) {

    var result: TorrentLoadResult? = null
        private set
    var extension: TorrentAddonApi? = null
        private set
    var torrentHash: String? = null
    var hasUpdate: Boolean = false
        private set

    private val _isInitialized = MutableLiveData<Boolean>().apply { value = false }
    val isInitialized: LiveData<Boolean> = _isInitialized

    private var error: String? = null

    suspend fun init() {
        result = null
        extension = null
        error = null
        hasUpdate = false
        withContext(Dispatchers.Main) {
            _isInitialized.value = false
        }
        if (Build.VERSION.SDK_INT < 23) {
            Logger.log("Torrent extension is not supported on this device.")
            error = context.getString(R.string.torrent_extension_not_supported)
            return
        }
        try {
            result = AddonLoader.loadExtension(
                context,
                TORRENT_PACKAGE,
                TORRENT_CLASS,
                AddonLoader.Companion.AddonType.TORRENT
            ) as TorrentLoadResult
            result?.let {
                if (it is TorrentLoadResult.Success) {
                    extension = it.extension.extension
                    hasUpdate = hasUpdate(REPO, it.extension.versionName)
                }
            }
            withContext(Dispatchers.Main) {
                _isInitialized.value = true
            }
        } catch (e: Exception) {
            Logger.log("Error initializing torrent extension")
            Logger.log(e)
            error = e.message
        }
    }

    fun isAvailable(): Boolean {
        return extension != null
    }

    fun getVersion(): String? {
        return result?.let {
            if (it is TorrentLoadResult.Success) it.extension.versionName else null
        }
    }

    fun hadError(context: Context): String? {
        return if (isInitialized.value == true) {
            error ?: context.getString(R.string.loaded_successfully)
        } else {
            null
        }
    }

    companion object {

        private const val TORRENT_PACKAGE = "dantotsu.torrentAddon"
        private const val TORRENT_CLASS = "ani.dantotsu.torrentAddon.TorrentAddon"
        const val REPO = "rebelonion/Dantotsu-Torrent-Addon"
    }
}