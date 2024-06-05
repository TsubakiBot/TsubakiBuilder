package ani.dantotsu.view

import android.content.Context
import android.util.AttributeSet
import androidx.mediarouter.app.MediaRouteButton
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName

class CustomCastButton : MediaRouteButton {

    private var castCallback: (() -> Unit)? = null

    fun setCastCallback(castCallback: () -> Unit) {
        this.castCallback = castCallback
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun performClick(): Boolean {
        return if (PrefManager.getVal(PrefName.UseInternalCast)) {
            super.performClick()
        } else {
            castCallback?.let { it() }
            true
        }
    }
}