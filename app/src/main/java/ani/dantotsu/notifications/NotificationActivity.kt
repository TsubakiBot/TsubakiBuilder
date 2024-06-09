package ani.dantotsu.notifications

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.Notification
import ani.dantotsu.connections.anilist.api.NotificationType
import ani.dantotsu.connections.anilist.api.NotificationType.Companion.fromFormattedString
import ani.dantotsu.connections.anilist.api.User
import ani.dantotsu.connections.anilist.api.UserAvatar
import ani.dantotsu.currContext
import ani.dantotsu.databinding.ActivityFollowBinding
import ani.dantotsu.databinding.CustomDialogLayoutBinding
import ani.dantotsu.initActivity
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.notifications.comment.CommentStore
import ani.dantotsu.notifications.subscription.SubscriptionStore
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.profile.activity.FeedActivity
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.util.Logger
import bit.himitsu.update.MatagiUpdater
import bit.himitsu.updateMargins
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFollowBinding
    private lateinit var commentStore: List<CommentStore>
    private lateinit var subscriptionStore: List<SubscriptionStore>
    private var adapter: GroupieAdapter = GroupieAdapter()
    private var notificationList: List<Notification> = emptyList()
    private val filters = arrayListOf<String>()
    private var currentPage: Int = 1
    private var hasNextPage: Boolean = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        filters.addAll(PrefManager.getVal<Set<String>>(PrefName.NotificationFilters))
        binding = ActivityFollowBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.listTitle.text = getString(R.string.notifications)
        binding.listToolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
        }
        binding.listRecyclerView.adapter = adapter
        binding.listRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.followerGrid.visibility = ViewGroup.GONE
        binding.followerList.visibility = ViewGroup.GONE
        binding.listBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.listProgressBar.visibility = ViewGroup.VISIBLE
        binding.followFilterView.visibility = View.VISIBLE
        commentStore = PrefManager.getNullableVal<List<CommentStore>>(
            PrefName.CommentNotificationStore,
            null
        ) ?: listOf()
        subscriptionStore = PrefManager.getNullableVal<List<SubscriptionStore>>(
            PrefName.SubscriptionNotificationStore,
            null
        ) ?: listOf()

        binding.followFilterButton.setOnClickListener {
            val dialogView = CustomDialogLayoutBinding.inflate(layoutInflater)
            dialogView.dialogHeading.text = getString(R.string.filter)
            fun getToggleImageResource(container: ViewGroup): Int {
                var allChecked = true
                var allUnchecked = true

                for (i in 0 until container.childCount) {
                    val checkBox = container.getChildAt(i) as CheckBox
                    if (!checkBox.isChecked) {
                        allChecked = false
                    } else {
                        allUnchecked = false
                    }
                }
                return when {
                    allChecked -> R.drawable.untick_all_boxes
                    allUnchecked -> R.drawable.tick_all_boxes
                    else -> R.drawable.invert_all_boxes
                }
            }
            NotificationType.entries.forEach { notificationType ->
                val checkBox = CheckBox(currContext())
                checkBox.text = notificationType.toFormattedString()
                checkBox.isChecked = !filters.contains(notificationType.value.fromFormattedString())
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        filters.remove(notificationType.value.fromFormattedString())
                    } else {
                        filters.add(notificationType.value.fromFormattedString())
                    }
                    dialogView.toggleButton.setImageResource(getToggleImageResource(dialogView.checkboxContainer))
                }
                dialogView.checkboxContainer.addView(checkBox)
            }
            dialogView.toggleButton.setImageResource(getToggleImageResource(dialogView.checkboxContainer))
            dialogView.toggleButton.setOnClickListener {
                dialogView.checkboxContainer.children.forEach {
                    val checkBox = it as CheckBox
                    checkBox.isChecked = !checkBox.isChecked
                }
                dialogView.toggleButton.setImageResource(getToggleImageResource(dialogView.checkboxContainer))
            }
            val alertD = AlertDialog.Builder(this, R.style.MyPopup)
            alertD.setView(dialogView.root)
            alertD.setPositiveButton(R.string.ok) { _, _ ->
                PrefManager.setVal(PrefName.NotificationFilters, filters.toSet())
                filterByType(binding.notificationNavBar.selectedTab?.id)
            }
            alertD.setNegativeButton(R.string.cancel) { _, _ -> }
            val dialog = alertD.show()
            dialog.window?.setDimAmount(0.8f)
        }

        val activityId = intent.getIntExtra("activityId", -1)
        lifecycleScope.launch {
            loadPage(activityId) {
                binding.listProgressBar.visibility = ViewGroup.GONE
            }
            withContext(Dispatchers.Main) {
                binding.listProgressBar.visibility = ViewGroup.GONE
                binding.listRecyclerView.setOnTouchListener { _, event ->
                    if (event?.action == MotionEvent.ACTION_UP) {
                        if (hasNextPage
                            && !binding.listRecyclerView.canScrollVertically(1)
                            && !binding.followRefresh.isVisible
                            && adapter.itemCount != 0
                            && (binding.listRecyclerView.layoutManager as LinearLayoutManager)
                                .findLastVisibleItemPosition() == (adapter.itemCount - 1)
                        ) {
                            binding.followRefresh.visibility = ViewGroup.VISIBLE
                            loadPage(-1) {
                                binding.followRefresh.visibility = ViewGroup.GONE
                            }
                        }
                    }
                    false
                }

                binding.followSwipeRefresh.setOnRefreshListener {
                    currentPage = 1
                    hasNextPage = true
                    adapter.clear()
                    notificationList = emptyList()
                    loadPage(-1) {
                        binding.followSwipeRefresh.isRefreshing = false
                    }
                }
                MatagiUpdater.notifyOnUpdate(this@NotificationActivity, binding.appUpdateLayout)
            }
        }

        binding.notificationNavBar.visibility = View.VISIBLE
        binding.notificationNavBar.updateMargins(resources.configuration)
        binding.notificationNavBar.selectTabAt(0)
        binding.notificationNavBar.onTabSelected = { filterByType(it.id) }
    }

    private val media = listOf(
        NotificationType.AIRING.value,
        NotificationType.RELATED_MEDIA_ADDITION.value,
        NotificationType.MEDIA_DATA_CHANGE.value,
        NotificationType.MEDIA_MERGE.value,
        NotificationType.MEDIA_DELETION.value,
        NotificationType.SUBSCRIPTION.value
    )
    private val posts = listOf(
        NotificationType.ACTIVITY_MESSAGE.value,
        NotificationType.ACTIVITY_REPLY.value,
        NotificationType.ACTIVITY_MENTION.value,
        NotificationType.ACTIVITY_REPLY_SUBSCRIBED.value,
        NotificationType.COMMENT_REPLY.value,
        NotificationType.THREAD_COMMENT_MENTION.value,
        NotificationType.THREAD_SUBSCRIBED.value,
        NotificationType.THREAD_COMMENT_REPLY.value
    )
    private val user = listOf(
        NotificationType.FOLLOWING.value,
        NotificationType.ACTIVITY_LIKE.value,
        NotificationType.ACTIVITY_REPLY_LIKE.value,
        NotificationType.THREAD_LIKE.value,
        NotificationType.THREAD_COMMENT_LIKE.value
    )

    private fun getUncategorized(): List<String> {
        val newList = arrayListOf<String>()
        NotificationType.entries.filter { !filters.contains(it.value) }.forEach {
            newList.add(it.value)
        }
        return newList
    }

    private fun enableTabByContent(tabId: Int, items: List<String>) {
        val hasContent = notificationList.any { items.contains(it.notificationType) }
        binding.notificationNavBar.tabs.find { it.id == tabId }?.enabled = hasContent
    }

    private fun filterByType(id: Int?) {
        val newNotifications = when (id) {
            R.id.notificationsMedia -> media
            R.id.notificationsPosts -> posts
            R.id.notificationsUser -> user
            else -> null
        }.let { list ->
            val filter = list?.minus(filters.toSet()) ?: getUncategorized()
            notificationList.filter { notification ->
                filter.contains(notification.notificationType)
            }
        }

        adapter.clear()
        adapter.addAll(newNotifications.map {
            NotificationItem(
                it,
                ::onNotificationClick,
                ::onNotificationLongClick
            )
        })
    }

    private fun loadPage(activityId: Int, onFinish: () -> Unit = {}) {
        lifecycleScope.launch(Dispatchers.IO) {
            val resetNotification = activityId == -1
            val res = Anilist.query.getNotifications(
                Anilist.userid ?: PrefManager.getVal<String>(PrefName.AnilistUserId).toIntOrNull()
                ?: 0, currentPage, resetNotification = resetNotification
            )
            withContext(Dispatchers.Main) {
                val newNotifications: MutableList<Notification> = mutableListOf()
                res?.data?.page?.notifications?.let { notifications ->
                    Logger.log("Notifications: $notifications")
                    newNotifications += if (activityId != -1) {
                        notifications.filter { it.id == activityId }
                    } else {
                        notifications
                    }.toMutableList()
                }
                if (activityId == -1) {
                    val furthestTime = newNotifications.minOfOrNull { it.createdAt } ?: 0
                    commentStore.forEach {
                        if ((it.time > furthestTime * 1000L || !hasNextPage) && notificationList.none { notification ->
                                notification.commentId == it.commentId && notification.createdAt == (it.time / 1000L).toInt()
                            }) {
                            val notification = Notification(
                                it.type.toString(),
                                System.currentTimeMillis().toInt(),
                                commentId = it.commentId,
                                notificationType = it.type.toString(),
                                mediaId = it.mediaId,
                                context = it.title + "\n" + it.content,
                                createdAt = (it.time / 1000L).toInt(),
                            )
                            newNotifications += notification
                        }
                    }
                    subscriptionStore.forEach {
                        if ((it.time > furthestTime * 1000L || !hasNextPage)) {
                            val notification = Notification(
                                it.type,
                                System.currentTimeMillis().toInt(),
                                commentId = it.mediaId,
                                mediaId = it.mediaId,
                                notificationType = it.type,
                                context = it.title + "\n" + it.content,
                                createdAt = (it.time / 1000L).toInt(),
                                user = User(
                                    it.mediaId,
                                    it.title,
                                    UserAvatar(it.cover, it.cover),
                                    it.banner,
                                    null, null, null, null, null
                                ),
                            )
                            newNotifications += notification
                        }
                    }
                    newNotifications.sortByDescending { it.createdAt }
                }

                notificationList += newNotifications
                enableTabByContent(R.id.notificationsMedia, media)
                enableTabByContent(R.id.notificationsPosts, posts)
                enableTabByContent(R.id.notificationsUser, user)
                currentPage = res?.data?.page?.pageInfo?.currentPage?.plus(1) ?: 1
                hasNextPage = res?.data?.page?.pageInfo?.hasNextPage ?: false
                filterByType(binding.notificationNavBar.selectedTab?.id)
                binding.followSwipeRefresh.isRefreshing = false
                onFinish()
            }
        }
    }

    private fun onNotificationLongClick(notification: Notification) {
        when (notification.notificationType) {
            NotificationType.COMMENT_REPLY.value -> {
                val notificationStore = PrefManager.getNullableVal<List<CommentStore>>(
                    PrefName.CommentNotificationStore,
                    null
                ) ?: listOf()
                PrefManager.setVal(
                    PrefName.CommentNotificationStore,
                    notificationStore.minus(notification)
                )
            }
            NotificationType.SUBSCRIPTION.value -> {
                val notificationStore = PrefManager.getNullableVal<List<SubscriptionStore>>(
                    PrefName.SubscriptionNotificationStore,
                    null
                ) ?: listOf()
                PrefManager.setVal(
                    PrefName.SubscriptionNotificationStore,
                    notificationStore.minus(notification)
                )
            }
            else -> {}
        }
        notificationList -= notification
        adapter.clear()
        filterByType(binding.notificationNavBar.selectedTab?.id)
    }

    private fun onNotificationClick(id: Int, optional: Int?, type: NotificationClickType) {
        when (type) {
            NotificationClickType.USER -> {
                ContextCompat.startActivity(
                    this, Intent(this, ProfileActivity::class.java)
                        .putExtra("userId", id), null
                )
            }

            NotificationClickType.MEDIA -> {
                ContextCompat.startActivity(
                    this, Intent(this, MediaDetailsActivity::class.java)
                        .putExtra("mediaId", id), null
                )
            }

            NotificationClickType.ACTIVITY -> {
                ContextCompat.startActivity(
                    this, Intent(this, FeedActivity::class.java)
                        .putExtra("activityId", id), null
                )
            }

            NotificationClickType.COMMENT -> {
                ContextCompat.startActivity(
                    this, Intent(this, MediaDetailsActivity::class.java)
                        .putExtra("FRAGMENT_TO_LOAD", "COMMENTS")
                        .putExtra("mediaId", id)
                        .putExtra("commentId", optional ?: -1),
                    null
                )

            }

            NotificationClickType.UNDEFINED -> {
                // Do nothing
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.notificationNavBar.updateMargins(newConfig)
    }

    companion object {
        enum class NotificationClickType {
            USER, MEDIA, ACTIVITY, COMMENT, UNDEFINED
        }
    }
}