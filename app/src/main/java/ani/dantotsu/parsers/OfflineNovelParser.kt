package ani.dantotsu.parsers

import android.os.Environment
import ani.dantotsu.currContext
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.media.MediaNameAdapter
import ani.dantotsu.util.Logger
import me.xdrop.fuzzywuzzy.FuzzySearch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

class OfflineNovelParser : NovelParser() {
    private val downloadManager = Injekt.get<DownloadsManager>()

    override val hostUrl: String = "Offline"
    override val name: String = "Offline"
    override val saveName: String = "Offline"

    override val volumeRegex =
        Regex("vol\\.? (\\d+(\\.\\d+)?)|volume (\\d+(\\.\\d+)?)", RegexOption.IGNORE_CASE)

    override suspend fun loadBook(link: String, extra: Map<String, String>?): Book {
        //link should be a directory
        val directory = File(
            currContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "Dantotsu/Novel/$link"
        )
        val chapters = mutableListOf<Book>()
        if (directory.exists()) {
            directory.listFiles()?.forEach {
                if (it.isDirectory) {
                    val chapter = Book(
                        it.name,
                        it.absolutePath + "/cover.jpg",
                        null,
                        listOf(it.absolutePath + "/0.epub")
                    )
                    chapters.add(chapter)
                }
            }
            chapters.sortBy { MediaNameAdapter.findChapterNumber(it.name) }
            return chapters.first()
        }
        return Book(
            "error",
            "",
            null,
            listOf("error")
        )
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val titles = downloadManager.novelDownloadedTypes.map { it.titleName }.distinct()
        val returnTitlesPair: MutableList<Pair<String, Int>> = mutableListOf()
        for (title in titles) {
            Logger.log("Comparing $title to $query")
            val score = FuzzySearch.ratio(title.lowercase(), query.lowercase())
            if (score > 80) {
                returnTitlesPair.add(Pair(title, score))
            }
        }
        val returnTitles = returnTitlesPair.sortedByDescending { it.second }.map { it.first }
        val returnList: MutableList<ShowResponse> = mutableListOf()
        for (title in returnTitles) {
            //need to search the subdirectories for the ShowResponses
            val directory = File(
                currContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "Dantotsu/Novel/$title"
            )
            val names = mutableListOf<String>()
            if (directory.exists()) {
                directory.listFiles()?.forEach {
                    if (it.isDirectory) {
                        names.add(it.name)
                    }
                }
            }
            val cover =
                currContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath + "/Dantotsu/Novel/$title/cover.jpg"
            names.forEach {
                returnList.add(ShowResponse(it, query, cover))
            }
        }
        return returnList
    }

}