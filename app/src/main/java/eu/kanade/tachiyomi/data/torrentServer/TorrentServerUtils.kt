package eu.kanade.tachiyomi.data.torrentServer

import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.utf8
import eu.kanade.tachiyomi.data.torrentServer.model.FileStat
import eu.kanade.tachiyomi.data.torrentServer.model.Torrent
import uy.kohesive.injekt.injectLazy
import java.io.File

object TorrentServerUtils {
    private val preferences: TorrentServerPreferences by injectLazy()
    val hostUrl = "http://127.0.0.1:${port}"

    private val animeTrackers = preferences.trackers().get().split("\n").joinToString(",\n")

    fun setTrackersList() {
        server.Server.addTrackers(animeTrackers)
    }

    var port
        get() = PrefManager.getVal<String>(PrefName.TorrServerPort)
        set(value) = PrefManager.setVal(PrefName.TorrServerPort, value)

    fun getTorrentPlayLink(torr: Torrent, index: Int): String {
        val file = findFile(torr, index)
        val name = file?.let { File(it.path).name } ?: torr.title
        return "$hostUrl/stream/${name.utf8}?link=${torr.hash}&index=$index&play"
    }

    private fun findFile(torrent: Torrent, index: Int): FileStat? {
        torrent.file_stats?.forEach {
            if (it.id == index) {
                return it
            }
        }
        return null
    }
}
