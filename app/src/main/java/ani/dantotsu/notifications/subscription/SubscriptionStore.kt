package ani.dantotsu.notifications.subscription

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionStore(
    val title: String,
    val content: String,
    val mediaId: Int,
    val cover: String?,
    val banner: String?,
    val type: String = "SUBSCRIPTION",
    val time: Long = System.currentTimeMillis(),
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID = 2L
    }
}