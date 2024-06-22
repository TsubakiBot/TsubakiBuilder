package ani.dantotsu.connections

import ani.dantotsu.R
import ani.dantotsu.Refresh
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.mal.MAL
import ani.dantotsu.currContext
import ani.dantotsu.media.cereal.Media
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.toast
import bit.himitsu.nio.Strings.getString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun updateProgress(media: Media, number: String) {
    val incognito: Boolean = PrefManager.getVal(PrefName.Incognito)
    if (!incognito) {
        if (Anilist.userid != null) {
            val a = number.toFloatOrNull()?.toInt() ?: 0
            if (a > (media.userProgress ?: -1)) {
                CoroutineScope(Dispatchers.IO).launch {
                    Anilist.mutation.editList(
                        media.id,
                        a,
                        status = if (media.userStatus == "REPEATING") media.userStatus else "CURRENT"
                    )
                    media.userProgress = a
                    Refresh.all()
                }
                CoroutineScope(Dispatchers.IO).launch {
                    MAL.query.editList(
                        media.idMAL,
                        media.anime != null,
                        a, null,
                        if (media.userStatus == "REPEATING") media.userStatus!! else "CURRENT"
                    )
                }
                toast(
                    getString(R.string.setting_progress, media.userPreferredName, a)
                )
            }
        } else {
            toast(R.string.login_anilist_account)
        }
    } else {
        toast("Sneaky sneaky :3")
    }
}
