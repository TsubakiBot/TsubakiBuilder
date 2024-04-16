package ani.dantotsu.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.collection.LruCache
import ani.matagi.io.Memory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min

object BitmapUtil {
    private fun roundCorners(bitmap: Bitmap, cornerRadius: Float = 20f): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        return output
    }

    private val cacheSize = (Memory.maxMemory() / 1024 / 12).toInt()
    private val bitmapCache = LruCache<String, Bitmap>(cacheSize)

    fun downloadImageAsBitmap(imageUrl: String): Bitmap? {
        var bitmap: Bitmap? = null

        runBlocking(Dispatchers.IO) {
            val cacheName = imageUrl.substringAfterLast("/")
            bitmap = bitmapCache[cacheName]
            if (bitmap != null) return@runBlocking
            var inputStream: InputStream? = null
            var urlConnection: HttpURLConnection? = null
            try {
                val url = URL(imageUrl)
                urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "GET"
                urlConnection.connect()

                if (urlConnection.responseCode == HttpURLConnection.HTTP_OK) {
                    inputStream = urlConnection.inputStream
                    bitmap = BitmapFactory.decodeStream(inputStream)
                    bitmap?.let { bitmapCache.put(cacheName, it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                inputStream?.close()
                urlConnection?.disconnect()
            }
        }
        return bitmap?.let { roundCorners(it) }
    }

    fun Bitmap.toSquare(): Bitmap {
        val side = min(width, height)
        val xOffset = (width - side) / 2
        // Slight offset for the y, since a lil bit under the top is usually the focus of covers
        val yOffset = ((height - side) / 2 * 0.25).toInt()
        return Bitmap.createBitmap(this, xOffset, yOffset, side, side)
    }
}
