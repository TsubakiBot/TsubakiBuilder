package ani.dantotsu

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.widget.ImageView
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.BlurTransformation
import java.io.File

fun ImageView.loadImage(url: String?, size: Int = 0) {
    if (!url.isNullOrEmpty()) {
        val localFile = File(url)
        if (localFile.exists()) loadLocalImage(localFile, size) else loadImage(FileUrl(url), size)
    }
}

fun geUrlOrTrolled(url: String?): String {
    return if (PrefManager.getVal(PrefName.DisableMitM)) url ?: "" else
        PrefManager.getVal<String>(PrefName.ImageUrl).ifEmpty { url ?: "" }
}

fun ImageView.loadImage(file: FileUrl?, size: Int = 0) {
    file?.url = geUrlOrTrolled(file?.url)
    if (file?.url?.isNotEmpty() == true) {
        tryWith {
            if (file.url.startsWith("content://")) {
                Glide.with(this.context).load(Uri.parse(file.url)).transition(
                    DrawableTransitionOptions.withCrossFade()
                )
                    .override(size).into(this)
            } else {
                val glideUrl = GlideUrl(file.url) { file.headers }
                Glide.with(this.context).load(glideUrl).transition(DrawableTransitionOptions.withCrossFade()).override(size)
                    .into(this)
            }
        }
    }
}

fun ImageView.loadImage(file: FileUrl?, width: Int = 0, height: Int = 0) {
    file?.url = PrefManager.getVal<String>(PrefName.ImageUrl).ifEmpty { file?.url ?: "" }
    if (file?.url?.isNotEmpty() == true) {
        tryWith {
            if (file.url.startsWith("content://")) {
                Glide.with(this.context).load(Uri.parse(file.url)).transition(
                    DrawableTransitionOptions.withCrossFade()
                )
                    .override(width, height).into(this)
            } else {
                val glideUrl = GlideUrl(file.url) { file.headers }
                Glide.with(this.context).load(glideUrl).transition(DrawableTransitionOptions.withCrossFade()).override(width, height)
                    .into(this)
            }
        }
    }
}


fun ImageView.loadLocalImage(file: File?, size: Int = 0) {
    if (file?.exists() == true) {
        tryWith {
            Glide.with(this.context).load(file).transition(DrawableTransitionOptions.withCrossFade()).override(size)
                .into(this)
        }
    }
}

fun ImageView.blurImage(banner: String?) {
    if (banner != null) {
        val radius = PrefManager.getVal<Float>(PrefName.BlurRadius).toInt()
        val sampling = PrefManager.getVal<Float>(PrefName.BlurSampling).toInt()
        if (PrefManager.getVal(PrefName.BlurBanners)) {
            val context = context
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
                    .into(this)
            }
        } else {
            loadImage(banner)
        }
    } else {
        setImageResource(R.drawable.linear_gradient_bg)
    }
}