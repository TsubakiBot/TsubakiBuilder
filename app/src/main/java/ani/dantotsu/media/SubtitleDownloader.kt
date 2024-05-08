package ani.dantotsu.media

import android.content.Context
import androidx.core.util.toAndroidPair
import androidx.media3.common.MimeTypes
import ani.dantotsu.download.DownloadedType
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.parsers.SubtitleType
import ani.dantotsu.snackString
import ani.dantotsu.util.Logger
import com.anggrayudi.storage.file.openOutputStream
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SubtitleDownloader {
        suspend fun loadSubtitleType(url: String): SubtitleType =
            withContext(Dispatchers.IO) {
                return@withContext try {
                    // Initialize the NetworkHelper instance. Replace this line based on how you usually initialize it
                    val networkHelper = Injekt.get<NetworkHelper>()
                    val request = Request.Builder()
                        .url(url)
                        .build()

                    val response = networkHelper.client.newCall(request).execute()

                    // Check if response is successful
                    if (response.isSuccessful) {
                        val responseBody = response.body.string()

                        val formats = arrayOf(
                            MimeTypes.TEXT_VTT,
                            MimeTypes.APPLICATION_TTML,
                            MimeTypes.APPLICATION_SUBRIP,
                            MimeTypes.TEXT_SSA
                        )

                        response.headers.find {
                            it.first == "Content-Type" && formats.contains(it.second)
                        }?.let {
                            when (it.second) {
                                MimeTypes.TEXT_VTT -> SubtitleType.VTT
                                MimeTypes.APPLICATION_TTML ->  SubtitleType.TTML
                                MimeTypes.APPLICATION_SUBRIP ->  SubtitleType.SRT
                                MimeTypes.TEXT_SSA ->  SubtitleType.ASS
                                else -> SubtitleType.SRT
                            }
                        } ?: when {
                            responseBody.contains("[Script Info]") -> SubtitleType.ASS
                            responseBody.contains("WEBVTT") -> SubtitleType.VTT
                            else -> SubtitleType.SRT
                        }
                    } else {
                        SubtitleType.UNKNOWN
                    }
                } catch (e: Exception) {
                    Logger.log(e)
                    SubtitleType.UNKNOWN
                }
            }

        suspend fun downloadSubtitle(
            context: Context,
            url: String,
            downloadedType: DownloadedType
        ) = withContext(Dispatchers.IO) {
            try {
                val directory = DownloadsManager.getSubDirectory(
                    context,
                    downloadedType.type,
                    false,
                    downloadedType.titleName,
                    downloadedType.chapterName
                ) ?: throw Exception("Could not create directory")
                val type = loadSubtitleType(url)
                directory.findFile("subtitle.${type}")?.delete()
                val subtitleFile = directory.createFile("*/*", "subtitle.${type}")
                    ?: throw Exception("Could not create subtitle file")

                val client = Injekt.get<NetworkHelper>().client
                val request = Request.Builder().url(url).build()
                val reponse = client.newCall(request).execute()

                if (!reponse.isSuccessful) {
                    snackString("Failed to download subtitle")
                    return@withContext
                }

                context.contentResolver.openOutputStream(subtitleFile.uri).use { output ->
                    output?.write(reponse.body.bytes())
                        ?: throw Exception("Could not open output stream")
                }
            } catch (e: Exception) {
                snackString("Failed to download subtitle")
                Logger.log(e)
                return@withContext
            }

        }
    }
