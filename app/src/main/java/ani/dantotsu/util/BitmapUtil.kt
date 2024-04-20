package ani.dantotsu.util

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.widget.ImageView
import androidx.collection.LruCache
import ani.dantotsu.R
import ani.dantotsu.geUrlOrTrolled
import ani.dantotsu.loadImage
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.matagi.io.Memory
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
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
        var bitmap: Bitmap?
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

    fun blurImage(imageView: ImageView, banner: String?) {
        if (banner != null) {
            val radius = PrefManager.getVal<Float>(PrefName.BlurRadius).toInt()
            val sampling = PrefManager.getVal<Float>(PrefName.BlurSampling).toInt()
            if (PrefManager.getVal(PrefName.BlurBanners)) {
                val context = imageView.context
                if (!(context as Activity).isDestroyed) {
                    val url = geUrlOrTrolled(banner)
                    Glide.with(context as Context)
                        .load(
                            if (banner.startsWith("http")) GlideUrl(url) else if (banner.startsWith(
                                    "content://"
                                )
                            ) Uri.parse(
                                url
                            ) else File(url)
                        )
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE).override(400)
                        .apply(
                            if (PrefManager.getVal<String>(PrefName.ImageUrl).isEmpty()) {
                                RequestOptions.noTransformation()
                            } else {
                                RequestOptions.bitmapTransform(BlurTransformation(radius, sampling))
                            }
                        )
                        .into(imageView)
                }
            } else {
                imageView.loadImage(banner)
            }
        } else {
            imageView.setImageResource(R.drawable.linear_gradient_bg)
        }
    }
}
