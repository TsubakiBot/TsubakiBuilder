package ani.dantotsu.others

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import ani.dantotsu.okHttpClient
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import java.io.InputStream


@GlideModule
class DantotsuGlideApp : AppGlideModule() {
    @SuppressLint("CheckResult")
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)
        val diskCacheSizeBytes = 1024 * 1024 * 192 // 192 MB
        builder.apply {
            setLogLevel(Log.ERROR)
            setDiskCache(InternalCacheDiskCacheFactory(
                context, "img", diskCacheSizeBytes.toLong())
            )
            setDefaultRequestOptions(
                RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            )
        }
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(okHttpClient)
        )
        super.registerComponents(context, glide, registry)
    }

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}