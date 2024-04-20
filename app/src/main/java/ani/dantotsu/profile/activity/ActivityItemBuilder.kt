package ani.dantotsu.profile.activity

import ani.dantotsu.R
import ani.dantotsu.Strings.getString
import ani.dantotsu.connections.anilist.api.Notification
import ani.dantotsu.connections.anilist.api.NotificationType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ActivityItemBuilder {
    fun getContent(notification: Notification): String {
        val notificationType: NotificationType =
            NotificationType.valueOf(notification.notificationType)
        return when (notificationType) {
            NotificationType.ACTIVITY_MESSAGE -> {
                getString(R.string.notification_message, notification.user?.name)
            }

            NotificationType.ACTIVITY_REPLY -> {
                getString(R.string.notification_reply, notification.user?.name)
            }

            NotificationType.FOLLOWING -> {
                getString(R.string.notification_followed, notification.user?.name)
            }

            NotificationType.ACTIVITY_MENTION -> {
                getString(R.string.notification_mention, notification.user?.name)
            }

            NotificationType.THREAD_COMMENT_MENTION -> {
                getString(R.string.notification_mention_forum, notification.user?.name)
            }

            NotificationType.THREAD_SUBSCRIBED -> {
                getString(R.string.notification_forum_comment, notification.user?.name)
            }

            NotificationType.THREAD_COMMENT_REPLY -> {
                getString(R.string.notification_reply_forum, notification.user?.name)
            }

            NotificationType.AIRING -> {
                getString(
                    R.string.notification_episode_aired,
                    notification.episode,
                    notification.media?.title?.english ?: notification.media?.title?.romaji
                )
            }

            NotificationType.ACTIVITY_LIKE -> {
                getString(R.string.notification_like, notification.user?.name)
            }

            NotificationType.ACTIVITY_REPLY_LIKE -> {
                getString(R.string.notification_reply_like, notification.user?.name)
            }

            NotificationType.THREAD_LIKE -> {
                getString(R.string.notification_like_thread, notification.user?.name)
            }

            NotificationType.THREAD_COMMENT_LIKE -> {
                getString(R.string.notification_forum_comment_like, notification.user?.name)
            }

            NotificationType.ACTIVITY_REPLY_SUBSCRIBED -> {
                getString(R.string.notification_reply_shared_activity, notification.user?.name)
            }

            NotificationType.RELATED_MEDIA_ADDITION -> {
                getString(
                    R.string.notification_media_added,
                    notification.media?.title?.english ?: notification.media?.title?.romaji
                )
            }

            NotificationType.MEDIA_DATA_CHANGE -> {
                getString(
                    R.string.notification_media_changed,
                    notification.media?.title?.english ?: notification.media?.title?.romaji,
                    notification.reason
                )
            }

            NotificationType.MEDIA_MERGE -> {
                getString(
                    R.string.notification_media_merge,
                    notification.deletedMediaTitles?.joinToString(", "),
                    notification.media?.title?.english ?: notification.media?.title?.romaji
                )
            }

            NotificationType.MEDIA_DELETION -> {
                getString(R.string.notification_media_deleted, notification.deletedMediaTitle)
            }

            NotificationType.COMMENT_REPLY -> {
                notification.context ?: "You should not see this"
            }
        }
    }


    fun getDateTime(timestamp: Int): String {

        val targetDate = Date(timestamp * 1000L)

        if (targetDate < Date(946684800000L)) { // January 1, 2000 (who want dates before that?)
            return ""
        }

        val currentDate = Date()
        val difference = currentDate.time - targetDate.time

        return when (val daysDifference = difference / (1000 * 60 * 60 * 24)) {
            0L -> {
                val hoursDifference = difference / (1000 * 60 * 60)
                val minutesDifference = (difference / (1000 * 60)) % 60

                when {
                    hoursDifference > 0 -> "$hoursDifference hour${if (hoursDifference > 1) "s" else ""} ago"
                    minutesDifference > 0 -> "$minutesDifference minute${if (minutesDifference > 1) "s" else ""} ago"
                    else -> "Just now"
                }
            }

            1L -> "1 day ago"
            in 2..6 -> "$daysDifference days ago"
            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(targetDate)
        }
    }
}