package ani.dantotsu.connections.github

import ani.dantotsu.Mapper
import ani.dantotsu.R
import ani.dantotsu.client
import ani.dantotsu.settings.Developer
import bit.himitsu.Strings.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import java.util.Collections

class Contributors {

    fun getContributors(): Array<Developer> {
        val contributors = arrayListOf<Developer>()
        runBlocking(Dispatchers.IO) {
            val repo = getString(R.string.repo_gh)
            val res = client.get("https://api.github.com/repos/$repo/contributors")
                .parsed<JsonArray>().map {
                    Mapper.json.decodeFromJsonElement<GithubResponse>(it)
                }
            val owner = res.first { it.login == "rebelonion" }
            Collections.swap(res, res.indexOf(owner), 0)
            res.filter { it.login != "SunglassJerry" }.forEach {
                contributors.add(
                    Developer(
                        it.login,
                        it.avatarUrl,
                        when (it.login) {
                            "rebelonion" -> "Dantotsu Maintainer"
                            "AbandonedCart" -> "${getString(R.string.contributor)} ${getString(R.string.himitsu)}"
                            "sneazy-ibo" -> "${getString(R.string.contributor)} & ${getString(R.string.comment_mod)}"
                            "WaiWhat" -> "Icon Designer"
                            else -> getString(R.string.contributor)
                        },
                        it.htmlUrl
                    )
                )
            }

            contributors.addAll(
                arrayOf(
                    Developer(
                        "MarshMeadow",
                        "https://avatars.githubusercontent.com/u/88599122?v=4",
                        "Beta Icon Designer & Website Maintainer",
                        "https://github.com/MarshMeadow?tab=repositories"
                    ),
                    Developer(
                        "Zaxx69",
                        "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b6342562-kxE8m4i7KUMK.png",
                        "Telegram Admin",
                        "https://anilist.co/user/6342562"
                    ),
                    Developer(
                        "Arif Alam",
                        "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b6011177-2n994qtayiR9.jpg",
                        "Discord & ${getString(R.string.comment_mod)}",
                        "https://anilist.co/user/6011177"
                    ),
                    Developer(
                        "SunglassJeery",
                        "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b5804776-FEKfP5wbz2xv.png",
                        "Head Discord & ${getString(R.string.comment_mod)}",
                        "https://anilist.co/user/5804776"
                    ),
                    Developer(
                        "Excited",
                        "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b6131921-toSoGWmKbRA1.png",
                        getString(R.string.comment_mod),
                        "https://anilist.co/user/6131921"
                    ),
                    Developer(
                        "Gurjshan",
                        "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b6363228-rWQ3Pl3WuxzL.png",
                        getString(R.string.comment_mod),
                        "https://anilist.co/user/6363228"
                    ),
                    Developer(
                        "NekoMimi",
                        "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b6244220-HOpImMGMQAxW.jpg",
                        getString(R.string.comment_mod),
                        "https://anilist.co/user/6244220"
                    ),
                    Developer(
                        "Ziadsenior",
                        "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b6049773-8cjYeUOFUguv.jpg",
                        "${getString(R.string.comment_mod)} & Arabic Translator",
                        "https://anilist.co/user/6049773"
                    ),
                    Developer(
                        "hastsu",
                        "https://cdn.discordapp.com/avatars/602422545077108749/20b4a6efa4314550e4ed51cdbe4fef3d.webp?size=160",
                        "${getString(R.string.comment_mod)} & Arabic Translator",
                        "https://anilist.co/user/6183359"
                    ),
                )
            )
        }
        return contributors.toTypedArray()
    }


    @Serializable
    data class GithubResponse(
        @SerialName("login")
        val login: String,
        @SerialName("avatar_url")
        val avatarUrl: String,
        @SerialName("html_url")
        val htmlUrl: String
    )
}