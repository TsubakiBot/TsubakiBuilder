package eu.kanade.tachiyomi.extension.api

import ani.dantotsu.others.LanguageMapper
import ani.dantotsu.parsers.novel.AvailableNovelSources
import ani.dantotsu.parsers.novel.NovelExtension
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.util.Logger
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import eu.kanade.tachiyomi.extension.anime.model.AvailableAnimeSources
import eu.kanade.tachiyomi.extension.manga.model.AvailableMangaSources
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tachiyomi.core.util.lang.withIOContext
import uy.kohesive.injekt.injectLazy

internal class ExtensionGithubApi {
    private val networkService: NetworkHelper by injectLazy()
    private val json: Json by injectLazy()

    private fun List<ExtensionSourceJsonObject>.toAnimeExtensionSources(): List<AvailableAnimeSources> {
        return this.map {
            AvailableAnimeSources(
                id = it.id,
                lang = it.lang,
                name = it.name,
                baseUrl = it.baseUrl,
            )
        }
    }

    private fun List<ExtensionJsonObject>.toAnimeExtensions(repository: String): List<AnimeExtension.Available> {
        return this
            .filter {
                val libVersion = it.extractLibVersion()
                libVersion >= ExtensionLoader.ANIME_LIB_VERSION_MIN && libVersion <= ExtensionLoader.ANIME_LIB_VERSION_MAX
            }
            .map {
                AnimeExtension.Available(
                    name = it.name.substringAfter("Aniyomi: "),
                    pkgName = it.pkg,
                    versionName = it.version,
                    versionCode = it.code,
                    libVersion = it.extractLibVersion(),
                    lang = it.lang,
                    isNsfw = it.nsfw == 1,
                    hasReadme = it.hasReadme == 1,
                    hasChangelog = it.hasChangelog == 1,
                    sources = it.sources?.toAnimeExtensionSources().orEmpty(),
                    apkName = it.apk,
                    repository = repository,
                    iconUrl = "${repository}/icon/${it.pkg}.png",
                )
            }
    }

    suspend fun findAnimeExtensions(): List<AnimeExtension.Available> {
        return withIOContext {

            val extensions: ArrayList<AnimeExtension.Available> = arrayListOf()

            val repos =
                PrefManager.getVal<Set<String>>(PrefName.AnimeExtensionRepos).toMutableList()

            repos.map {
                async {
                    try {
                        val githubResponse = try {
                            networkService.client
                                .newCall(GET("${it}/index.min.json"))
                                .awaitSuccess()
                        } catch (e: Throwable) {
                            Logger.log("Failed to get repo: $it")
                            Logger.log(e)
                            null
                        }

                        val response = githubResponse ?: run {
                            networkService.client
                                .newCall(GET("${fallbackRepoUrl(it)}/index.min.json"))
                                .awaitSuccess()
                        }

                        val repoExtensions = with(json) {
                            response
                                .parseAs<List<ExtensionJsonObject>>()
                                .toAnimeExtensions(it)
                        }

                        // Sanity check - a small number of extensions probably means something broke
                        // with the repo generator
//                        if (repoExtensions.size < 10) {
//                            throw Exception()
//                        }

                        extensions.addAll(repoExtensions)
                    } catch (e: Throwable) {
                        Logger.log("Failed to get extensions from GitHub")
                        Logger.log(e)
                    }
                }
            }.awaitAll()
            extensions
        }
    }

    fun getAnimeApkUrl(extension: AnimeExtension.Available): String {
        return "${extension.repository}/apk/${extension.apkName}"
    }

    private fun List<ExtensionSourceJsonObject>.toMangaExtensionSources(): List<AvailableMangaSources> {
        return this.map {
            AvailableMangaSources(
                id = it.id,
                lang = it.lang,
                name = it.name,
                baseUrl = it.baseUrl,
            )
        }
    }

    private fun List<ExtensionJsonObject>.toMangaExtensions(repository: String): List<MangaExtension.Available> {
        return this
            .filter {
                val libVersion = it.extractLibVersion()
                libVersion >= ExtensionLoader.MANGA_LIB_VERSION_MIN && libVersion <= ExtensionLoader.MANGA_LIB_VERSION_MAX
            }
            .map {
                MangaExtension.Available(
                    name = it.name.substringAfter("Tachiyomi: "),
                    pkgName = it.pkg,
                    versionName = it.version,
                    versionCode = it.code,
                    libVersion = it.extractLibVersion(),
                    lang = it.lang,
                    isNsfw = it.nsfw == 1,
                    hasReadme = it.hasReadme == 1,
                    hasChangelog = it.hasChangelog == 1,
                    sources = it.sources?.toMangaExtensionSources().orEmpty(),
                    apkName = it.apk,
                    repository = repository,
                    iconUrl = "${repository}/icon/${it.pkg}.png",
                )
            }
    }

    suspend fun findMangaExtensions(): List<MangaExtension.Available> {
        return withIOContext {

            val extensions: ArrayList<MangaExtension.Available> = arrayListOf()

            val repos =
                PrefManager.getVal<Set<String>>(PrefName.MangaExtensionRepos).toMutableList()

            repos.map {
                async {
                    try {
                        val githubResponse = try {
                            networkService.client
                                .newCall(GET("${it}/index.min.json"))
                                .awaitSuccess()
                        } catch (e: Throwable) {
                            Logger.log("Failed to get repo: $it")
                            Logger.log(e)
                            null
                        }

                        val response = githubResponse ?: run {
                            networkService.client
                                .newCall(GET("${fallbackRepoUrl(it)}/index.min.json"))
                                .awaitSuccess()
                        }

                        val repoExtensions = with(json) {
                            response
                                .parseAs<List<ExtensionJsonObject>>()
                                .toMangaExtensions(it)
                        }

                        // Sanity check - a small number of extensions probably means something broke
                        // with the repo generator
//                        if (repoExtensions.size < 10) {
//                            throw Exception()
//                        }

                        extensions.addAll(repoExtensions)
                    } catch (e: Throwable) {
                        Logger.log("Failed to get extensions from GitHub")
                        Logger.log(e)
                    }
                }
            }.awaitAll()
            extensions
        }
    }

    fun getMangaApkUrl(extension: MangaExtension.Available): String {
        return "${extension.repository}/apk/${extension.apkName}"
    }

    private fun List<ExtensionSourceJsonObject>.toNovelSources(): List<AvailableNovelSources> {
        return map { source ->
            AvailableNovelSources(
                source.id,
                source.lang,
                source.name,
                source.baseUrl,
            )
        }
    }

    private fun List<ExtensionJsonObject>.toNovelExtensions(repository: String): List<NovelExtension.Available> {
        return mapNotNull { extension ->
            val sources = extension.sources?.map { source ->
                ExtensionSourceJsonObject(
                    source.id,
                    source.lang,
                    source.name,
                    source.baseUrl,
                )
            }
            NovelExtension.Available(
                extension.name,
                extension.pkg,
                extension.apk,
                extension.code,
                sources?.toNovelSources() ?: emptyList(),
                repository = repository,
                iconUrl = "${repository}/icon/${extension.pkg}.png",
            )
        }
    }

    suspend fun findNovelExtensions(): List<NovelExtension.Available> {
        return withIOContext {

            val extensions: ArrayList<NovelExtension.Available> = arrayListOf()

            val repos =
                PrefManager.getVal<Set<String>>(PrefName.NovelExtensionRepos)
                    .toMutableList().filter { it.endsWith("index.min.json") }

            repos.map {
                async {
                    try {
                        val githubResponse = try {
                            networkService.client
                                .newCall(GET("${it}/index.min.json"))
                                .awaitSuccess()
                        } catch (e: Throwable) {
                            Logger.log("Failed to get repo: $it")
                            Logger.log(e)
                            null
                        }

                        val response = githubResponse ?: run {
                            networkService.client
                                .newCall(GET("${fallbackRepoUrl(it)}/index.min.json"))
                                .awaitSuccess()
                        }

                        val repoExtensions = with(json) {
                            response
                                .parseAs<List<ExtensionJsonObject>>()
                                .toNovelExtensions(it)
                        }

                        extensions.addAll(repoExtensions)
                    } catch (e: Throwable) {
                        Logger.log("Failed to get extension. Possibly a plugin.")
                        Logger.log(e)
                    }
                }
            }.awaitAll()
            extensions
        }
    }

    // https://github.com/LNReader/lnreader-plugins/blob/master/docs/plugin-template.ts
    private fun List<PluginJsonObject>.toNovelPlugins(repository: String): List<NovelExtension.Plugin> {
        return this
            .filter {
                it.url.endsWith("[madara].js") || it.iconUrl.contains("/madara/")
            }
            .map { extension ->
                val lang = LanguageMapper.mapNativeNameToCode(extension.lang) ?: LanguageMapper.Language.ALL.code
                val sources =
                    listOf(
                        ExtensionSourceJsonObject(
                            extension.id.hashCode().toLong(),
                            lang,
                            extension.name,
                            extension.site,
                        ),
                        ExtensionSourceJsonObject(
                            extension.id.hashCode().toLong(),
                            lang,
                            extension.name,
                            extension.url,
                        )
                    )
                NovelExtension.Plugin(
                    extension.name,
                    "plugin:${extension.id}",
                    extension.version,
                    extension.version.replace(".", "").toLong(),
                    lang,
                    sources.toNovelSources(),
                    repository = repository,
                    iconUrl = extension.iconUrl,
                )
            }
    }

    suspend fun findNovelPlugins(): List<NovelExtension.Plugin> {
        return withIOContext {

            val plugins: ArrayList<NovelExtension.Plugin> = arrayListOf()

            val repos =
                PrefManager.getVal<Set<String>>(PrefName.NovelExtensionRepos)
                    .toMutableList().filter { it.endsWith("plugins.min.json") }

            repos.forEach {
                    try {
                        val response = networkService.client
                            .newCall(GET("${it}/plugins.min.json"))
                            .awaitSuccess()

                        val repoExtensions = with(json) {
                            response
                                .parseAs<List<PluginJsonObject>>()
                                .toNovelPlugins(it)
                        }

                        plugins.addAll(repoExtensions)
                    } catch (ex: Throwable) {
                        Logger.log("Failed to get plugin. Possibly an extension.")
                        Logger.log(ex)
                    }
                }
            plugins
        }
    }



    fun getNovelApkUrl(extension: NovelExtension.Available): String {
        return "${extension.repository}/apk/${extension.pkgName}.apk"
    }


    private fun fallbackRepoUrl(repoUrl: String): String? {
        var fallbackRepoUrl = "https://gcore.jsdelivr.net/gh/"
        val strippedRepoUrl =
            repoUrl.removePrefix("https://").removePrefix("http://").removeSuffix("/")
        val repoUrlParts = strippedRepoUrl.split("/")
        if (repoUrlParts.size < 3) {
            return null
        }
        val repoOwner = repoUrlParts[1]
        val repoName = repoUrlParts[2]
        fallbackRepoUrl += "$repoOwner/$repoName"
        val repoBranch = if (repoUrlParts.size > 3) {
            repoUrlParts[3]
        } else {
            "main"
        }
        fallbackRepoUrl += "@$repoBranch"
        return fallbackRepoUrl
    }
}

@Serializable
private data class ExtensionJsonObject(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String,
    val code: Long,
    val version: String,
    val nsfw: Int,
    val hasReadme: Int = 0,
    val hasChangelog: Int = 0,
    val sources: List<ExtensionSourceJsonObject>?,
)

@Serializable
private data class PluginJsonObject(
    val id: String,
    val name: String,
    val site: String,
    val lang: String,
    val version: String,
    val url: String,
    val iconUrl: String,
)

@Serializable
private data class ExtensionSourceJsonObject(
    val id: Long,
    val lang: String,
    val name: String,
    val baseUrl: String,
)

private fun ExtensionJsonObject.extractLibVersion(): Double {
    return version.substringBeforeLast('.').toDouble()
}
