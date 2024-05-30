package ani.dantotsu

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.TRANSPORT_BLUETOOTH
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkCapabilities.TRANSPORT_ETHERNET
import android.net.NetworkCapabilities.TRANSPORT_LOWPAN
import android.net.NetworkCapabilities.TRANSPORT_USB
import android.net.NetworkCapabilities.TRANSPORT_VPN
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import android.net.NetworkCapabilities.TRANSPORT_WIFI_AWARE
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.InputFilter
import android.text.Spanned
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.OvershootInterpolator
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.DatePicker
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.FileProvider
import androidx.core.math.MathUtils.clamp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.viewpager2.widget.ViewPager2
import ani.dantotsu.BuildConfig.APPLICATION_ID
import bit.himitsu.nio.Strings.getString
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.Genre
import ani.dantotsu.connections.anilist.api.FuzzyDate
import ani.dantotsu.connections.anilist.getUserId
import bit.himitsu.bakaupdates.MangaUpdates
import ani.dantotsu.databinding.ItemCountDownBinding
import ani.dantotsu.media.Media
import ani.dantotsu.notifications.IncognitoNotificationClickReceiver
import ani.dantotsu.others.SpoilerPlugin
import ani.dantotsu.parsers.ShowResponse
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.settings.saving.internal.PreferenceKeystore
import ani.dantotsu.settings.saving.internal.PreferenceKeystore.Companion.generateSalt
import ani.dantotsu.util.CountUpTimer
import ani.dantotsu.util.Logger
import ani.dantotsu.view.dialog.CustomBottomDialog
import bit.himitsu.os.Version
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.android.material.snackbar.Snackbar
import eu.kanade.tachiyomi.data.notification.Notifications
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.html.TagHandlerNoOp
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.glide.GlideImagesPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joery.animatedbottombar.AnimatedBottomBar
import java.io.File
import java.io.FileOutputStream
import java.lang.reflect.Field
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.Timer
import java.util.TimerTask
import kotlin.collections.set
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow


var statusBarHeight = 0
var navBarHeight = 0

val Number.toPx
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics
    ).toInt()

val Number.toDp
    get() = if (Version.isUpsideDownCake) {
        TypedValue.deriveDimension(
            TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics
        )
    } else {
        this.toFloat() / (Resources.getSystem().displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
    }

val Number.dpToColumns: Int
    get() {
        val columns = currContext().run {
            val metrics = DisplayMetrics()
            with(getSystemService(Context.WINDOW_SERVICE) as WindowManager) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val bounds: Rect = currentWindowMetrics.bounds
                    bounds.width() / this@dpToColumns.toPx
                } else @Suppress("deprecation") {
                    defaultDisplay.getRealMetrics(metrics)
                    metrics.widthPixels / this@dpToColumns.toPx
                }
            }
        }
        return columns
    }

lateinit var bottomBar: AnimatedBottomBar
var selectedOption = 1

object Refresh {
    fun all() {
        for (i in activity) {
            activity[i.key]!!.postValue(true)
        }
    }

    val activity = mutableMapOf<Int, MutableLiveData<Boolean>>()
}

fun currContext(): Context {
    return Himitsu.currentContext()
}

fun currActivity(): Activity? {
    return Himitsu.currentActivity()
}

var loadMedia: Int? = null
var loadIsMAL = false

fun initActivity(a: Activity) {
    val window = a.window
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val darkMode = PrefManager.getVal<Int>(PrefName.DarkMode)
    val immersiveMode: Boolean = PrefManager.getVal(PrefName.ImmersiveMode)
    darkMode.apply {
        AppCompatDelegate.setDefaultNightMode(
            when (this) {
                2 -> AppCompatDelegate.MODE_NIGHT_YES
                1 -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
    if (immersiveMode) {
        if (navBarHeight == 0) {
            ViewCompat.getRootWindowInsets(window.decorView)
                ?.apply {
                    navBarHeight = if (a.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                        this.getInsets(WindowInsetsCompat.Type.navigationBars()).right
                    else
                        this.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) navBarHeight += 48.toPx
                }
        }
        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).hide(WindowInsetsCompat.Type.statusBars())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && statusBarHeight == 0
            && a.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        ) {
            window.decorView.rootWindowInsets?.displayCutout?.apply {
                if (boundingRects.size > 0) {
                    statusBarHeight = min(boundingRects[0].width(), boundingRects[0].height())
                }
            }
        }
    } else
        if (statusBarHeight == 0) {
            val windowInsets =
                ViewCompat.getRootWindowInsets(window.decorView)
            if (windowInsets != null) {
                statusBarHeight = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                navBarHeight = if (a.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                    windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).right
                else
                    windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) navBarHeight += 48.toPx
            }
        }
    if (a !is MainActivity) a.setNavigationTheme()
}

fun AnimatedBottomBar.updateLayoutParams(orientation: Int) {
    val navBarRightMargin = if (orientation == Configuration.ORIENTATION_LANDSCAPE) navBarHeight else 0
    val navBarBottomMargin = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 0 else navBarHeight
    this.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        rightMargin = navBarRightMargin
        bottomMargin = navBarBottomMargin
    }
}

fun AnimatedBottomBar.updateMargins(orientation: Int) {
    val rightMargin = if (orientation == Configuration.ORIENTATION_LANDSCAPE) navBarHeight else 0
    val bottomMargin = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 0 else navBarHeight
    val params: ViewGroup.MarginLayoutParams = layoutParams as ViewGroup.MarginLayoutParams
    params.updateMargins(right = rightMargin, bottom = bottomMargin)
}

fun Activity.hideSystemBars() {
    WindowInsetsControllerCompat(window, window.decorView).let { controller ->
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

fun Activity.hideSystemBarsExtendView() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    hideSystemBars()
}

fun Activity.showSystemBars() {
    WindowInsetsControllerCompat(window, window.decorView).let { controller ->
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        controller.show(WindowInsetsCompat.Type.systemBars())
    }
}

fun Activity.showSystemBarsRetractView() {
    WindowCompat.setDecorFitsSystemWindows(window, true)
    showSystemBars()
}

fun Activity.setNavigationTheme() {
    val tv = TypedValue()
    theme.resolveAttribute(android.R.attr.colorBackground, tv, true)
    if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && tv.isColorType)
        || (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT)
    ) {
        window.navigationBarColor = tv.data
    }
}

fun Window.setNavigationTheme(context: Context) {
    val tv = TypedValue()
    context.theme.resolveAttribute(android.R.attr.colorBackground, tv, true)
    if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && tv.isColorType)
        || (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT)
    ) {
        navigationBarColor = tv.data
    }
}

/**
 * Finish the calling activity and launch it again within the same lifecycle scope
 */
fun Activity.reloadActivity() {
    finish()
    startActivity(Intent(this, this::class.java))
}

/**
 * Restarts the application from the launch intent and redirects to the calling activity
 */
fun Activity.restartApp() {
    val mainIntent = Intent.makeRestartActivityTask(
        packageManager.getLaunchIntentForPackage(this.packageName)!!.component
    )
    val component =
        ComponentName(this@restartApp.packageName, this@restartApp::class.qualifiedName!!)
    try {
        startActivity(Intent().setComponent(component))
    } catch (anything: Exception) {
        startActivity(mainIntent)
    }
    finishAndRemoveTask()
}

suspend fun serverDownDialog(activity: FragmentActivity?) = withContext(Dispatchers.Main) {
    activity?.let {
        CustomBottomDialog.newInstance().apply {
            title = it.getString(R.string.anilist_broken_title)
            addView(TextView(activity).apply {
                text = it.getString(R.string.anilist_broken)
            })

            setNegativeButton(it.getString(R.string.cancel)) {
                dismiss()
            }

            setPositiveButton(it.getString(R.string.close)) {
                it.finishAffinity()
            }
            show(it.supportFragmentManager, "dialog")
        }
    }
}

suspend fun loadFragment(activity: FragmentActivity, response: () -> Unit) = withContext(Dispatchers.IO) {
    Anilist.userid = PrefManager.getNullableVal<String>(PrefName.AnilistUserId, null)
        ?.toIntOrNull()
    try {
        if (Anilist.userid == null) {
            getUserId(activity) { response.invoke() }
        } else {
            getUserId(activity) { response.invoke() }
        }
    } catch (ignored: Exception) {
        serverDownDialog(activity)
    }
}

fun isOnline(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return tryWith {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cap = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            return@tryWith if (cap != null) {
                when {
                    cap.hasTransport(TRANSPORT_BLUETOOTH) ||
                            cap.hasTransport(TRANSPORT_CELLULAR) ||
                            cap.hasTransport(TRANSPORT_ETHERNET) ||
                            cap.hasTransport(TRANSPORT_LOWPAN) ||
                            cap.hasTransport(TRANSPORT_USB) ||
                            cap.hasTransport(TRANSPORT_VPN) ||
                            cap.hasTransport(TRANSPORT_WIFI) ||
                            cap.hasTransport(TRANSPORT_WIFI_AWARE) -> true

                    else -> false
                }
            } else false
        } else {
            @Suppress("DEPRECATION")
            return@tryWith connectivityManager.activeNetworkInfo?.run {
                type == ConnectivityManager.TYPE_BLUETOOTH ||
                        type == ConnectivityManager.TYPE_ETHERNET ||
                        type == ConnectivityManager.TYPE_MOBILE ||
                        type == ConnectivityManager.TYPE_MOBILE_DUN ||
                        type == ConnectivityManager.TYPE_MOBILE_HIPRI ||
                        type == ConnectivityManager.TYPE_WIFI ||
                        type == ConnectivityManager.TYPE_WIMAX ||
                        type == ConnectivityManager.TYPE_VPN
            } ?: false
        }
    } ?: false
}

fun startMainActivity(activity: Activity, bundle: Bundle? = null) {
    activity.finishAffinity()
    activity.startActivity(
        Intent(activity, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (bundle != null) putExtras(bundle)
        }
    )
}

class DatePickerFragment(activity: Activity, var date: FuzzyDate = FuzzyDate().getToday()) :
    DialogFragment(),
    DatePickerDialog.OnDateSetListener {
    var dialog: DatePickerDialog

    init {
        val c = Calendar.getInstance()
        val year = date.year ?: c.get(Calendar.YEAR)
        val month = if (date.month != null) date.month!! - 1 else c.get(Calendar.MONTH)
        val day = date.day ?: c.get(Calendar.DAY_OF_MONTH)
        dialog = DatePickerDialog(activity, this, year, month, day)
        dialog.setButton(
            DialogInterface.BUTTON_NEUTRAL,
            activity.getString(R.string.remove)
        ) { _, which ->
            if (which == DialogInterface.BUTTON_NEUTRAL) {
                date = FuzzyDate()
            }
        }
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        date = FuzzyDate(year, month + 1, day)
    }
}

class InputFilterMinMax(
    private val min: Double,
    private val max: Double,
    private val status: AutoCompleteTextView? = null
) :
    InputFilter {
    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        try {
            val input = (dest.toString() + source.toString()).toDouble()
            if (isInRange(min, max, input)) return null
        } catch (nfe: NumberFormatException) {
            Logger.log(nfe)
        }
        return ""
    }

    private fun isInRange(a: Double, b: Double, c: Double): Boolean {
        val statusStrings = currContext().resources.getStringArray(R.array.status_manga)[2]

        if (c == b) {
            status?.setText(statusStrings, false)
            status?.parent?.requestLayout()
        }
        return if (b > a) c in a..b else c in b..a
    }
}


class ZoomOutPageTransformer :
    ViewPager2.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        if (position == 0.0f && PrefManager.getVal(PrefName.LayoutAnimations)) {
            setAnimation(
                view.context,
                view,
                300,
                floatArrayOf(1.3f, 1f, 1.3f, 1f),
                0.5f to 0f
            )
            ObjectAnimator.ofFloat(view, "alpha", 0f, 1.0f)
                .setDuration((200 * (PrefManager.getVal(PrefName.AnimationSpeed) as Float)).toLong())
                .start()
        }
    }
}

fun setAnimation(
    context: Context,
    viewToAnimate: View,
    duration: Long = 150,
    list: FloatArray = floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f),
    pivot: Pair<Float, Float> = 0.5f to 0.5f
) {
    if (PrefManager.getVal(PrefName.LayoutAnimations)) {
        val anim = ScaleAnimation(
            list[0],
            list[1],
            list[2],
            list[3],
            Animation.RELATIVE_TO_SELF,
            pivot.first,
            Animation.RELATIVE_TO_SELF,
            pivot.second
        )
        anim.duration = (duration * (PrefManager.getVal(PrefName.AnimationSpeed) as Float)).toLong()
        anim.setInterpolator(context, R.anim.over_shoot)
        viewToAnimate.startAnimation(anim)
    }
}

fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
    if (lhs == rhs) {
        return 0
    }
    if (lhs.isEmpty()) {
        return rhs.length
    }
    if (rhs.isEmpty()) {
        return lhs.length
    }

    val lhsLength = lhs.length + 1
    val rhsLength = rhs.length + 1

    var cost = Array(lhsLength) { it }
    var newCost = Array(lhsLength) { 0 }

    for (i in 1 until rhsLength) {
        newCost[0] = i

        for (j in 1 until lhsLength) {
            val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1

            val costReplace = cost[j - 1] + match
            val costInsert = cost[j] + 1
            val costDelete = newCost[j - 1] + 1

            newCost[j] = min(min(costInsert, costDelete), costReplace)
        }

        val swap = cost
        cost = newCost
        newCost = swap
    }

    return cost[lhsLength - 1]
}

fun List<ShowResponse>.sortByTitle(string: String): List<ShowResponse> {
    val list = this.toMutableList()
    list.sortByTitle(string)
    return list
}

fun MutableList<ShowResponse>.sortByTitle(string: String) {
    val temp: MutableMap<Int, Int> = mutableMapOf()
    for (i in 0 until this.size) {
        temp[i] = levenshtein(string.lowercase(), this[i].name.lowercase())
    }
    val c = temp.toList().sortedBy { (_, value) -> value }.toMap()
    val a = ArrayList(c.keys.toList().subList(0, min(this.size, 25)))
    val b = c.values.toList().subList(0, min(this.size, 25))
    for (i in b.indices.reversed()) {
        if (b[i] > 18 && i < a.size) a.removeAt(i)
    }
    val temp2 = this.toMutableList()
    this.clear()
    for (i in a.indices) {
        this.add(temp2[a[i]])
    }
}

class SafeClickListener(
    private var defaultInterval: Int = 1000,
    private val onSafeCLick: (View) -> Unit
) : View.OnClickListener {

    private var lastTimeClicked: Long = 0

    override fun onClick(v: View) {
        if (SystemClock.elapsedRealtime() - lastTimeClicked < defaultInterval) {
            return
        }
        lastTimeClicked = SystemClock.elapsedRealtime()
        onSafeCLick(v)
    }
}

fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
    val safeClickListener = SafeClickListener {
        onSafeClick(it)
    }
    setOnClickListener(safeClickListener)
}

suspend fun getSize(file: FileUrl): Double? {
    return tryWithSuspend {
        client.head(file.url, file.headers, timeout = 1000).size?.toDouble()?.div(1024 * 1024)
    }
}

suspend fun getSize(file: String): Double? {
    return getSize(FileUrl(file))
}


abstract class GesturesListener : GestureDetector.SimpleOnGestureListener() {
    private var timer: Timer? = null //at class level;
    private val delay: Long = 200

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        processSingleClickEvent(e)
        return super.onSingleTapUp(e)
    }

    override fun onLongPress(e: MotionEvent) {
        processLongClickEvent(e)
        super.onLongPress(e)
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        processDoubleClickEvent(e)
        return super.onDoubleTap(e)
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        onScrollYClick(distanceY)
        onScrollXClick(distanceX)
        return super.onScroll(e1, e2, distanceX, distanceY)
    }

    private fun processSingleClickEvent(e: MotionEvent) {
        val handler = Handler(Looper.getMainLooper())
        val mRunnable = Runnable {
            onSingleClick(e)
        }
        timer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    handler.post(mRunnable)
                }
            }, delay)
        }
    }

    private fun processDoubleClickEvent(e: MotionEvent) {
        timer?.apply {
            cancel()
            purge()
        }
        onDoubleClick(e)
    }

    private fun processLongClickEvent(e: MotionEvent) {
        timer?.apply {
            cancel()
            purge()
        }
        onLongClick(e)
    }

    open fun onSingleClick(event: MotionEvent) {}
    open fun onDoubleClick(event: MotionEvent) {}
    open fun onScrollYClick(y: Float) {}
    open fun onScrollXClick(y: Float) {}
    open fun onLongClick(event: MotionEvent) {}
}

fun View.circularReveal(ex: Int, ey: Int, subX: Boolean, time: Long) {
    ViewAnimationUtils.createCircularReveal(
        this,
        if (subX) (ex - x.toInt()) else ex,
        ey - y.toInt(),
        0f,
        max(height, width).toFloat()
    ).setDuration(time).start()
}

fun openLinkInBrowser(link: String?) {
    link?.let {
        try {
            val emptyBrowserIntent = Intent(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                data = Uri.fromParts("http", "", null)
            }
            val sendIntent = Intent().apply {
                action = Intent.ACTION_VIEW
                addCategory(Intent.CATEGORY_BROWSABLE)
                data = Uri.parse(link)
                selector = emptyBrowserIntent
            }
            currContext().startActivity(sendIntent)
        } catch (e: ActivityNotFoundException) {
            snackString("No browser found")
        } catch (e: Exception) {
            Logger.log(e)
        }
    }
}

fun openLinkInYouTube(link: String?) {
    link?.let {
        try {
            val videoIntent = Intent(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                data = Uri.parse(link)
                setPackage("com.google.android.youtube")
            }
            currContext().startActivity(videoIntent)
        } catch (e: ActivityNotFoundException) {
            openLinkInBrowser(link)
        } catch (e: Exception) {
            Logger.log(e)
        }
    }
}

fun openInGooglePlay(packageName: String) {
    try {
        currContext().startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$packageName")
            )
        )
    } catch (e: ActivityNotFoundException) {
        currContext().startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            )
        )
    }
}

fun saveImageToDownloads(title: String, bitmap: Bitmap, context: Activity) {
    FileProvider.getUriForFile(
        context,
        "$APPLICATION_ID.provider",
        saveImage(
            bitmap,
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
            title
        ) ?: return
    )
}

fun savePrefsToDownloads(
    title: String,
    serialized: String,
    context: Activity,
    password: CharArray? = null
) {
    FileProvider.getUriForFile(
        context,
        "$APPLICATION_ID.provider",
        if (password != null && Version.isMarshmallow) {
            savePrefs(
                serialized,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
                title,
                context,
                password
            ) ?: return
        } else {
            savePrefs(
                serialized,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
                title,
                context
            ) ?: return
        }
    )
}

fun savePrefs(serialized: String, path: String, title: String, context: Context): File? {
    var file = File(path, "$title.ani")
    var counter = 1
    while (file.exists()) {
        file = File(path, "${title}_${counter}.ani")
        counter++
    }

    return try {
        file.writeText(serialized)
        scanFile(file.absolutePath, context)
        toast(String.format(context.getString(R.string.saved_to_path, file.absolutePath)))
        file
    } catch (e: Exception) {
        snackString("Failed to save settings: ${e.localizedMessage}")
        null
    }
}

@RequiresApi(Build.VERSION_CODES.M)
fun savePrefs(
    serialized: String,
    path: String,
    title: String,
    context: Context,
    password: CharArray
): File? {
    var file = File(path, "$title.sani")
    var counter = 1
    while (file.exists()) {
        file = File(path, "${title}_${counter}.sani")
        counter++
    }

    val salt = generateSalt()

    return try {
        val encryptedData = PreferenceKeystore.encryptWithPassword(password, serialized, salt)

        // Combine salt and encrypted data
        val dataToSave = salt + encryptedData

        file.writeBytes(dataToSave)
        scanFile(file.absolutePath, context)
        toast(String.format(context.getString(R.string.saved_to_path, file.absolutePath)))
        file
    } catch (e: Exception) {
        snackString("Failed to save settings: ${e.localizedMessage}")
        null
    }
}

fun shareImage(title: String, bitmap: Bitmap, context: Context) {

    val contentUri = FileProvider.getUriForFile(
        context,
        "$APPLICATION_ID.provider",
        saveImage(bitmap, context.cacheDir.absolutePath, title) ?: return
    )

    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "image/png"
    intent.putExtra(Intent.EXTRA_TEXT, title)
    intent.putExtra(Intent.EXTRA_STREAM, contentUri)
    context.startActivity(Intent.createChooser(intent, "Share $title"))
}

fun saveImage(image: Bitmap, path: String, imageFileName: String): File? {
    val imageFile = File(path, "$imageFileName.png")
    return try {
        FileOutputStream(imageFile).use {
            image.compress(Bitmap.CompressFormat.PNG, 0, it)
        }
        scanFile(imageFile.absolutePath, currContext())
        toast(String.format(currContext().getString(R.string.saved_to_path, path)))
        imageFile
    } catch (e: Exception) {
        snackString("Failed to save image: ${e.localizedMessage}")
        null
    }
}

private fun scanFile(path: String, context: Context) {
    MediaScannerConnection.scanFile(context, arrayOf(path), null) { _, _ -> }
}

class MediaPageTransformer : ViewPager2.PageTransformer {
    private fun parallax(view: View, position: Float) {
        if (position > -1 && position < 1) {
            val width = view.width.toFloat()
            view.translationX = -(position * width * 0.8f)
        }
    }

    override fun transformPage(view: View, position: Float) {

        val bannerContainer = view.findViewById<View>(R.id.itemCompactBanner)
        parallax(bannerContainer, position)
    }
}

class NoGestureSubsamplingImageView(context: Context?, attr: AttributeSet?) :
    SubsamplingScaleImageView(context, attr) {
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return false
    }
}

fun copyToClipboard(string: String, toast: Boolean = true) {
    val context = currContext()
    val clipboard = getSystemService(context, ClipboardManager::class.java)
    val clip = ClipData.newPlainText("label", string)
    clipboard?.setPrimaryClip(clip)
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        if (toast) snackString(context.getString(R.string.copied_text, string))
    }
}

fun countDown(media: Media, view: ViewGroup) {
    if (media.anime?.nextAiringEpisode != null && media.anime.nextAiringEpisodeTime != null
        && (media.anime.nextAiringEpisodeTime!! - System.currentTimeMillis() / 1000) <= 86400 * 28.toLong()
    ) {
        val v = ItemCountDownBinding.inflate(LayoutInflater.from(view.context), view, false)
        view.addView(v.root, 0)
        v.mediaCountdownText.text =
            getString(
                R.string.episode_release_countdown,
                media.anime.nextAiringEpisode!!
            )

        object : CountDownTimer(
            (media.anime.nextAiringEpisodeTime!! + 10000) * 1000 - System.currentTimeMillis(),
            1000
        ) {
            override fun onTick(millisUntilFinished: Long) {
                val a = millisUntilFinished / 1000
                v.mediaCountdown.text = currActivity()?.getString(
                    R.string.time_format,
                    a / 86400,
                    a % 86400 / 3600,
                    a % 86400 % 3600 / 60,
                    a % 86400 % 3600 % 60
                )
            }

            override fun onFinish() {
                v.mediaCountdownContainer.visibility = View.GONE
                snackString(R.string.congrats_vro)
            }
        }.start()
    }
}

fun sinceWhen(media: Media, view: ViewGroup) {
    if (media.status != "RELEASING" && media.status != "HIATUS") return
    CoroutineScope(Dispatchers.IO).launch {
        with(MangaUpdates()) {
            findLatestRelease(media)?.let {
                var timestamp: Long = it.metadata.series.lastUpdated!!.timestamp

                val latestChapter = getSeries(it)?.let { series ->
                    timestamp = series.lastUpdated?.timestamp ?: timestamp
                    currActivity()?.getString(R.string.chapter_number, series.latestChapter)
                } ?: {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                    timestamp = dateFormat.parse(it.record.releaseDate)?.time ?: timestamp
                    getLatestChapter(view.context, it)
                }
                val predicted = if (media.status == "RELEASING")
                    predictRelease(media, it.record.title, timestamp * 1000)
                else null
                val timeSince = (System.currentTimeMillis() - (timestamp * 1000)) / 1000

                withContext(Dispatchers.Main) {
                    val v = ItemCountDownBinding.inflate(
                        LayoutInflater.from(view.context), view, false
                    )
                    view.addView(v.root, 0)
                    v.mediaCountdownText.text =
                        currActivity()?.getString(R.string.chapter_release_timeout, latestChapter)

                    predicted?.let { time ->
                        v.mediaPredication.text = if (System.currentTimeMillis() > time) {
                            currActivity()?.getString(R.string.chapter_delayed)
                        } else {
                            currActivity()?.getString(
                                R.string.chapter_predication,
                                SimpleDateFormat.getDateTimeInstance().format(time)
                                    .substringBeforeLast(' ').substringBeforeLast(' ')
                                // SimpleDateFormat parses MMMM to MO5 for May. This is a workaround
                            )
                        }
                        v.mediaPredication.isVisible = true
                    }

                    object : CountUpTimer(86400000) {
                        override fun onTick(second: Int) {
                            val a = second + timeSince
                            v.mediaCountdown.text = currActivity()?.getString(
                                R.string.time_format,
                                a / 86400,
                                a % 86400 / 3600,
                                a % 86400 % 3600 / 60,
                                a % 86400 % 3600 % 60
                            )
                        }

                        override fun onFinish() {
                            // The legend will never die.
                        }
                    }.start()
                }
            }
        }
    }
}

fun displayTimer(media: Media, view: ViewGroup) {
    when {
        media.anime != null -> countDown(media, view)
        media.format == "MANGA" || media.format == "ONE_SHOT" -> sinceWhen(media, view)
        else -> {} // No timer yet
    }
}

fun MutableMap<String, Genre>.checkId(id: Int): Boolean {
    this.forEach {
        if (it.value.id == id) {
            return false
        }
    }
    return true
}

fun MutableMap<String, Genre>.checkGenreTime(genre: String): Boolean {
    if (containsKey(genre))
        return (System.currentTimeMillis() - get(genre)!!.time) >= (1000 * 60 * 60 * 24 * 7)
    return true
}

fun setSlideIn() = AnimationSet(false).apply {
    if (PrefManager.getVal(PrefName.LayoutAnimations)) {
        var animation: Animation = AlphaAnimation(0.0f, 1.0f)
        val animationSpeed: Float = PrefManager.getVal(PrefName.AnimationSpeed)
        animation.duration = (500 * animationSpeed).toLong()
        animation.interpolator = AccelerateDecelerateInterpolator()
        addAnimation(animation)

        animation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 1.0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0f
        )

        animation.duration = (750 * animationSpeed).toLong()
        animation.interpolator = OvershootInterpolator(1.1f)
        addAnimation(animation)
    }
}

fun setSlideUp() = AnimationSet(false).apply {
    if (PrefManager.getVal(PrefName.LayoutAnimations)) {
        var animation: Animation = AlphaAnimation(0.0f, 1.0f)
        val animationSpeed: Float = PrefManager.getVal(PrefName.AnimationSpeed)
        animation.duration = (500 * animationSpeed).toLong()
        animation.interpolator = AccelerateDecelerateInterpolator()
        addAnimation(animation)

        animation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 1.0f,
            Animation.RELATIVE_TO_SELF, 0f
        )

        animation.duration = (750 * animationSpeed).toLong()
        animation.interpolator = OvershootInterpolator(1.1f)
        addAnimation(animation)
    }
}

open class NoPaddingArrayAdapter<T>(context: Context, layoutId: Int, items: List<T>) :
    ArrayAdapter<T>(context, layoutId, items) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        view.setPadding(0, view.paddingTop, view.paddingRight, view.paddingBottom)
        (view as TextView).setTextColor(Color.WHITE)
        return view
    }
}

fun getCurrentBrightnessValue(context: Context): Float {
    fun getMax(): Int {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        val fields: Array<Field> = powerManager.javaClass.declaredFields
        for (field in fields) {
            if (field.name.equals("BRIGHTNESS_ON")) {
                field.isAccessible = true
                return try {
                    field.get(powerManager)?.toString()?.toInt() ?: 255
                } catch (e: IllegalAccessException) {
                    255
                }
            }
        }
        return 255
    }

    fun getCur(): Float {
        return Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            127
        ).toFloat()
    }

    return brightnessConverter(getCur() / getMax(), true)
}

fun brightnessConverter(it: Float, fromLog: Boolean) =
    clamp(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            if (fromLog) log2((it * 256f)) * 12.5f / 100f else 2f.pow(it * 100f / 12.5f) / 256f
        else it, 0.001f, 1f
    )


fun checkCountry(context: Context): Boolean {
    val telMgr = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    return when (telMgr.simState) {
        TelephonyManager.SIM_STATE_ABSENT -> {
            val tz = TimeZone.getDefault().id
            tz.equals("Asia/Kolkata", ignoreCase = true)
        }

        TelephonyManager.SIM_STATE_READY -> {
            val countryCodeValue = telMgr.networkCountryIso
            countryCodeValue.equals("in", ignoreCase = true)
        }

        else -> false
    }
}

const val INCOGNITO_CHANNEL_ID = 26

@SuppressLint("LaunchActivityFromNotification")
fun incognitoNotification(context: Context) {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val incognito: Boolean = PrefManager.getVal(PrefName.Incognito)
    if (incognito) {
        val intent = Intent(context, IncognitoNotificationClickReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, Notifications.CHANNEL_INCOGNITO_MODE)
            .setSmallIcon(R.drawable.ic_incognito_24)
            .setContentTitle("Incognito Mode")
            .setContentText("Disable Incognito Mode")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
        notificationManager.notify(INCOGNITO_CHANNEL_ID, builder.build())
    } else {
        notificationManager.cancel(INCOGNITO_CHANNEL_ID)
    }
}

fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

fun openSettings(context: Context, channelId: String?): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val intent = Intent(
            if (channelId != null) Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
            else Settings.ACTION_APP_NOTIFICATION_SETTINGS
        ).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
        }
        context.startActivity(intent)
        true
    } else false
}

suspend fun View.pop() {
    withContext(Dispatchers.Main) {
        ObjectAnimator.ofFloat(this@pop, "scaleX", 1f, 1.25f).setDuration(120).start()
        ObjectAnimator.ofFloat(this@pop, "scaleY", 1f, 1.25f).setDuration(120).start()
    }
    delay(120)
    withContext(Dispatchers.Main) {
        ObjectAnimator.ofFloat(this@pop, "scaleX", 1.25f, 1f).setDuration(100).start()
        ObjectAnimator.ofFloat(this@pop, "scaleY", 1.25f, 1f).setDuration(100).start()
    }
    delay(100)
}

/**
 * Builds the markwon instance with all the plugins
 * @return the markwon instance
 */
fun buildMarkwon(
    activity: Context,
    userInputContent: Boolean = true,
    fragment: Fragment? = null,
    anilist: Boolean = false
): Markwon {
    val glideContext = fragment?.let { Glide.with(it) } ?: Glide.with(activity)
    val markwon = Markwon.builder(activity)
        .usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                builder.linkResolver { _, link ->
                    copyToClipboard(link, true)
                }
            }
        })

        .usePlugin(SoftBreakAddsNewLinePlugin.create())
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TablePlugin.create(activity))
        .usePlugin(TaskListPlugin.create(activity))
        .usePlugin(SpoilerPlugin(anilist))
        .usePlugin(HtmlPlugin.create { plugin ->
            if (userInputContent) {
                plugin.addHandler(
                    TagHandlerNoOp.create("h1", "h2", "h3", "h4", "h5", "h6", "hr", "pre", "a")
                )
            }
        })
        .usePlugin(GlideImagesPlugin.create(object : GlideImagesPlugin.GlideStore {

            private val requestManager: RequestManager = glideContext.apply {
                addDefaultRequestListener(object : RequestListener<Any> {
                    override fun onResourceReady(
                        resource: Any,
                        model: Any,
                        target: Target<Any>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (resource is GifDrawable) {
                            resource.start()
                        }
                        return false
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Any>,
                        isFirstResource: Boolean
                    ): Boolean {
                        Logger.log("Image failed to load: $model")
                        Logger.log(e as Exception)
                        return false
                    }
                })
            }

            override fun load(drawable: AsyncDrawable): RequestBuilder<Drawable> {
                Logger.log("Loading image: ${drawable.destination}")
                return requestManager.load(drawable.destination)
            }

            override fun cancel(target: Target<*>) {
                Logger.log("Cancelling image load")
                requestManager.clear(target)
            }
        }))
        .build()
    return markwon
}

fun String.findBetween(a: String, b: String): String? {
    val string = substringAfter(a, "").substringBefore(b, "")
    return string.ifEmpty { null }
}

fun toast(string: String?) {
    if (string != null) {
        Logger.log(string)
        MainScope().launch {
            Toast.makeText(currActivity()?.application ?: return@launch, string, Toast.LENGTH_SHORT)
                .show()
        }
    }
}

fun toast(res: Int) {
    toast(getString(res))
}

fun snackString(s: String?, activity: Activity? = null, clipboard: String? = null): Snackbar? {
    try { //I have no idea why this sometimes crashes for some people...
        if (s != null) {
            (activity ?: currActivity())?.apply {
                val snackBar = Snackbar.make(
                    window.decorView.findViewById(android.R.id.content),
                    s,
                    Snackbar.LENGTH_SHORT
                )
                runOnUiThread {
                    snackBar.view.apply {
                        updateLayoutParams<FrameLayout.LayoutParams> {
                            gravity = (Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM)
                            width = ViewGroup.LayoutParams.WRAP_CONTENT
                        }
                        translationY = -(navBarHeight.toDp + 32f)
                        translationZ = 32f
                        updatePadding(16f.toPx, right = 16f.toPx)
                        setOnClickListener {
                            snackBar.dismiss()
                        }
                        setOnLongClickListener {
                            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            copyToClipboard(clipboard ?: s, false)
                            true
                        }
                    }
                    snackBar.show()
                }
                return snackBar
            }
            Logger.log(s)
        }
    } catch (e: Exception) {
        Logger.log(e)
    }
    return null
}

fun snackString(r: Int, activity: Activity? = null, clipboard: String? = null): Snackbar? {
    return snackString(getString(r), activity, clipboard)
}
