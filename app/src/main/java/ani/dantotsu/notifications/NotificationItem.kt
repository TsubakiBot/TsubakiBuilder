package ani.dantotsu.notifications

import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import ani.dantotsu.R
import ani.dantotsu.blurImage
import ani.dantotsu.connections.anilist.api.Notification
import ani.dantotsu.connections.anilist.api.NotificationType
import ani.dantotsu.databinding.ItemNotificationBinding
import ani.dantotsu.loadCover
import ani.dantotsu.loadImage
import ani.dantotsu.notifications.NotificationActivity.Companion.NotificationClickType
import ani.dantotsu.profile.activity.ActivityItemBuilder
import ani.dantotsu.setAnimation
import ani.dantotsu.toPx
import com.xwray.groupie.viewbinding.BindableItem

class NotificationItem(
    private val notification: Notification,
    val clickCallback: (Int, Int?, NotificationClickType) -> Unit,
    val longClickCallback: (notification: Notification) -> Unit
) : BindableItem<ItemNotificationBinding>() {
    private lateinit var binding: ItemNotificationBinding
    override fun bind(viewBinding: ItemNotificationBinding, position: Int) {
        binding = viewBinding
        setAnimation(binding.root.context, binding.root)
        setBinding()
    }

    override fun getLayout(): Int {
        return R.layout.item_notification
    }

    override fun initializeViewBinding(view: View): ItemNotificationBinding {
        return ItemNotificationBinding.bind(view)
    }

    private fun image(
        user: Boolean = false, commentNotification: Boolean = false, subscription : Boolean = false
    ) {
        val cover = when {
            user -> notification.user?.bannerImage ?: notification.user?.avatar?.medium
            subscription -> notification.user?.bannerImage ?: notification.user?.avatar?.large
            else -> notification.media?.bannerImage ?: notification.media?.coverImage?.large
        }
        binding.notificationBannerImage.blurImage(cover)
        val userHeight = 90.toPx
        when {
            user -> {
                binding.notificationCover.visibility = View.GONE
                binding.notificationCoverUserContainer.visibility = View.VISIBLE
                binding.notificationTitle.visibility = View.GONE
                if (commentNotification) {
                    binding.notificationCoverUser.setImageResource(R.drawable.ic_dantotsu_round)
                    binding.notificationCoverUser.scaleX = 1.4f
                    binding.notificationCoverUser.scaleY = 1.4f
                } else {
                    binding.notificationCoverUser.loadImage(notification.user?.avatar?.large)
                }
                binding.notificationBannerImage.layoutParams.height = userHeight
                binding.notificationGradiant.layoutParams.height = userHeight
                (binding.notificationTextContainer.layoutParams
                        as ViewGroup.MarginLayoutParams).marginStart = userHeight
            }
            subscription -> {
                binding.notificationCover.visibility = View.VISIBLE
                binding.notificationCoverUserContainer.visibility = View.GONE
                binding.notificationTitle.visibility = View.VISIBLE
                binding.notificationCover.layoutParams.height = 120.toPx
                binding.notificationCover.layoutParams.width = 81.toPx
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                binding.notificationTitle.text = notification.media?.title?.userPreferred
                binding.notificationBannerImage.layoutParams.height = 128.toPx
                binding.notificationGradiant.layoutParams.height = 128.toPx
                (binding.notificationTextContainer.layoutParams
                        as ViewGroup.MarginLayoutParams).marginStart = 118.toPx
            }
            else -> {
                binding.notificationCover.visibility = View.VISIBLE
                binding.notificationCoverUserContainer.visibility = View.GONE
                binding.notificationTitle.visibility = View.VISIBLE
                binding.notificationCover.loadCover(notification.media?.coverImage)
                binding.notificationTitle.text = notification.media?.title?.userPreferred
                binding.notificationBannerImage.layoutParams.height = 153.toPx
                binding.notificationGradiant.layoutParams.height = 153.toPx
                (binding.notificationTextContainer.layoutParams
                        as ViewGroup.MarginLayoutParams).marginStart = 125.toPx
            }
        }
    }

    private fun setBinding() {
        val notificationType: NotificationType =
            NotificationType.valueOf(notification.notificationType)
        binding.notificationText.text = ActivityItemBuilder.getContent(notification)
        binding.notificationDate.text = ActivityItemBuilder.getDateTime(notification.createdAt)

        binding.notificationCoverUser.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            longClickCallback(notification)
            true
        }
        binding.notificationBannerImage.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            longClickCallback(notification)
            true
        }


        when (notificationType) {
            NotificationType.ACTIVITY_MESSAGE -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.activityId ?: 0, null, NotificationClickType.ACTIVITY
                    )
                }
            }

            NotificationType.ACTIVITY_REPLY -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.activityId ?: 0, null, NotificationClickType.ACTIVITY
                    )
                }
            }

            NotificationType.FOLLOWING -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.userId ?: 0, null, NotificationClickType.USER
                    )
                }
            }

            NotificationType.ACTIVITY_MENTION -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.activityId ?: 0, null, NotificationClickType.ACTIVITY
                    )
                }
            }

            NotificationType.THREAD_COMMENT_MENTION -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
            }

            NotificationType.THREAD_SUBSCRIBED -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
            }

            NotificationType.THREAD_COMMENT_REPLY -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
            }

            NotificationType.AIRING -> {
                binding.notificationCover.loadCover(notification.media?.coverImage)
                image()
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.media?.id ?: 0, null, NotificationClickType.MEDIA
                    )
                }
            }

            NotificationType.ACTIVITY_LIKE -> {
                image(true)
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.activityId ?: 0, null, NotificationClickType.ACTIVITY
                    )
                }
            }

            NotificationType.ACTIVITY_REPLY_LIKE -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.activityId ?: 0, null, NotificationClickType.ACTIVITY
                    )
                }
            }

            NotificationType.THREAD_LIKE -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
            }

            NotificationType.THREAD_COMMENT_LIKE -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
            }

            NotificationType.ACTIVITY_REPLY_SUBSCRIBED -> {
                binding.notificationCover.loadImage(notification.user?.avatar?.large)
                image(true)
                binding.notificationCoverUser.setOnClickListener {
                    clickCallback(
                        notification.user?.id ?: 0, null, NotificationClickType.USER
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.activityId ?: 0, null, NotificationClickType.ACTIVITY
                    )
                }
            }

            NotificationType.RELATED_MEDIA_ADDITION -> {
                binding.notificationCover.loadCover(notification.media?.coverImage)
                image()
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.media?.id ?: 0, null, NotificationClickType.MEDIA
                    )
                }
            }

            NotificationType.MEDIA_DATA_CHANGE -> {
                binding.notificationCover.loadCover(notification.media?.coverImage)
                image()
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.media?.id ?: 0, null, NotificationClickType.MEDIA
                    )
                }
            }

            NotificationType.MEDIA_MERGE -> {
                binding.notificationCover.loadCover(notification.media?.coverImage)
                image()
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.media?.id ?: 0, null, NotificationClickType.MEDIA
                    )
                }
            }

            NotificationType.MEDIA_DELETION -> {
                binding.notificationCover.visibility = View.GONE
            }

            NotificationType.COMMENT_REPLY -> {
                image(user = true, commentNotification = true)
                if (notification.commentId != null && notification.mediaId != null) {
                    binding.notificationBannerImage.setOnClickListener {
                        clickCallback(
                            notification.mediaId,
                            notification.commentId,
                            NotificationClickType.COMMENT
                        )
                    }
                }
            }

            NotificationType.COMMENT_WARNING -> {
                image(user = true, commentNotification = true)
                if (notification.commentId != null && notification.mediaId != null) {
                    binding.notificationBannerImage.setOnClickListener {
                        clickCallback(
                            notification.mediaId,
                            notification.commentId,
                            NotificationClickType.COMMENT
                        )
                    }
                }
            }

            NotificationType.DANTOTSU_UPDATE -> {
                image(user = true)
            }

            NotificationType.SUBSCRIPTION -> {
                image(subscription = true)
                binding.notificationCover.setOnClickListener {
                    clickCallback(
                        notification.mediaId ?: 0, null, NotificationClickType.MEDIA
                    )
                }
                binding.notificationBannerImage.setOnClickListener {
                    clickCallback(
                        notification.mediaId ?: 0, null, NotificationClickType.MEDIA
                    )
                }
            }
        }
    }

}