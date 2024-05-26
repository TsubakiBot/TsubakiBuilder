package ani.dantotsu.settings.fragment

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.AnilistHomeViewModel
import ani.dantotsu.connections.anilist.api.NotificationType
import ani.dantotsu.databinding.ActivitySettingsNotificationsBinding
import ani.dantotsu.media.Media
import ani.dantotsu.notifications.TaskScheduler
import ani.dantotsu.notifications.anilist.AnilistNotificationWorker
import ani.dantotsu.notifications.comment.CommentNotificationWorker
import ani.dantotsu.notifications.subscription.SubscriptionHelper
import ani.dantotsu.notifications.subscription.SubscriptionHelper.Companion.saveSubscription
import ani.dantotsu.notifications.subscription.SubscriptionNotificationWorker
import ani.dantotsu.openSettings
import ani.dantotsu.settings.Settings
import ani.dantotsu.settings.SettingsActivity
import ani.dantotsu.settings.SettingsAdapter
import ani.dantotsu.settings.SettingsView
import ani.dantotsu.settings.SubscriptionsBottomDialog
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsNotificationFragment : Fragment() {
    private lateinit var binding: ActivitySettingsNotificationsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivitySettingsNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val settings = requireActivity() as SettingsActivity

        binding.apply {
            notificationSettingsBack.setOnClickListener {
                settings.backToMenu()
            }

            var curTime = PrefManager.getVal<Int>(PrefName.SubscriptionNotificationInterval)
            val timeNames = SubscriptionNotificationWorker.checkIntervals.map {
                val mins = it % 60
                val hours = it / 60
                if (it > 0) "${if (hours > 0) "$hours hrs " else ""}${if (mins > 0) "$mins mins" else ""}"
                else getString(R.string.do_not_update)
            }.toTypedArray()

            val aTimeNames = AnilistNotificationWorker.checkIntervals.map { it.toInt() }
            val aItems = aTimeNames.map {
                val mins = it % 60
                val hours = it / 60
                if (it > 0) "${if (hours > 0) "$hours hrs " else ""}${if (mins > 0) "$mins mins" else ""}"
                else getString(R.string.do_not_update)
            }

            val cTimeNames = CommentNotificationWorker.checkIntervals.map { it.toInt() }
            val cItems = cTimeNames.map {
                val mins = it % 60
                val hours = it / 60
                if (it > 0) "${if (hours > 0) "$hours hrs " else ""}${if (mins > 0) "$mins mins" else ""}"
                else getString(R.string.do_not_update)
            }

            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.subscribe_lists),
                        desc = getString(R.string.subscribe_lists_desc),
                        icon = R.drawable.ic_round_notifications_active_24,
                        onClick = {
                            subscribeCurrentLists()
                        }
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(
                            R.string.subscriptions_checking_time_s,
                            timeNames[curTime]
                        ),
                        desc = getString(R.string.subscriptions_info),
                        icon = R.drawable.ic_round_notifications_none_24,
                        onClick = {
                            val speedDialog = AlertDialog.Builder(settings, R.style.MyPopup)
                                .setTitle(R.string.subscriptions_checking_time)
                            val dialog =
                                speedDialog.setSingleChoiceItems(timeNames, curTime) { dialog, i ->
                                    curTime = i
                                    it.settingsTitle.text =
                                        getString(
                                            R.string.subscriptions_checking_time_s,
                                            timeNames[i]
                                        )
                                    PrefManager.setVal(
                                        PrefName.SubscriptionNotificationInterval,
                                        curTime
                                    )
                                    dialog.dismiss()
                                    TaskScheduler.create(
                                        settings, PrefManager.getVal(PrefName.UseAlarmManager)
                                    ).scheduleAllTasks(settings)
                                }.show()
                            dialog.window?.setDimAmount(0.8f)
                        },
                        onLongClick = {
                            TaskScheduler.create(
                                settings, PrefManager.getVal(PrefName.UseAlarmManager)
                            ).scheduleAllTasks(settings)
                        }
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.view_subscriptions),
                        desc = getString(R.string.view_subscriptions_desc),
                        icon = R.drawable.ic_round_search_24,
                        onClick = {
                            val subscriptions = SubscriptionHelper.getSubscriptions()
                            SubscriptionsBottomDialog.newInstance(subscriptions).show(
                                settings.supportFragmentManager,
                                "subscriptions"
                            )
                        }
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.anilist_notification_filters),
                        desc = getString(R.string.anilist_notification_filters_desc),
                        icon = R.drawable.ic_anilist,
                        onClick = {
                            val types = NotificationType.entries.map { it.name }
                            val filteredTypes =
                                PrefManager.getVal<Set<String>>(PrefName.AnilistFilteredTypes)
                                    .toMutableSet()
                            val selected = types.map { filteredTypes.contains(it) }.toBooleanArray()
                            val dialog = AlertDialog.Builder(settings, R.style.MyPopup)
                                .setTitle(R.string.anilist_notification_filters)
                                .setMultiChoiceItems(
                                    types.toTypedArray(),
                                    selected
                                ) { _, which, isChecked ->
                                    val type = types[which]
                                    if (isChecked) {
                                        filteredTypes.add(type)
                                    } else {
                                        filteredTypes.remove(type)
                                    }
                                    PrefManager.setVal(PrefName.AnilistFilteredTypes, filteredTypes)
                                }.create()
                            dialog.window?.setDimAmount(0.8f)
                            dialog.show()
                        }

                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(
                            R.string.anilist_notifications_checking_time,
                            aItems[PrefManager.getVal(PrefName.AnilistNotificationInterval)]
                        ),
                        desc = getString(R.string.anilist_notifications_checking_time_desc),
                        icon = R.drawable.ic_round_notifications_none_24,
                        onClick = {
                            val selected =
                                PrefManager.getVal<Int>(PrefName.AnilistNotificationInterval)
                            val dialog = AlertDialog.Builder(settings, R.style.MyPopup)
                                .setTitle(R.string.subscriptions_checking_time)
                                .setSingleChoiceItems(
                                    aItems.toTypedArray(),
                                    selected
                                ) { dialog, i ->
                                    PrefManager.setVal(PrefName.AnilistNotificationInterval, i)
                                    it.settingsTitle.text =
                                        getString(
                                            R.string.anilist_notifications_checking_time,
                                            aItems[i]
                                        )
                                    dialog.dismiss()
                                    TaskScheduler.create(
                                        settings, PrefManager.getVal(PrefName.UseAlarmManager)
                                    ).scheduleAllTasks(settings)
                                }.create()
                            dialog.window?.setDimAmount(0.8f)
                            dialog.show()
                        }
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(
                            R.string.comment_notification_checking_time,
                            cItems[PrefManager.getVal(PrefName.CommentNotificationInterval)]
                        ),
                        desc = getString(R.string.comment_notification_checking_time_desc),
                        icon = R.drawable.ic_round_notifications_none_24,
                        onClick = {
                            val selected =
                                PrefManager.getVal<Int>(PrefName.CommentNotificationInterval)
                            val dialog = AlertDialog.Builder(settings, R.style.MyPopup)
                                .setTitle(R.string.subscriptions_checking_time)
                                .setSingleChoiceItems(
                                    cItems.toTypedArray(),
                                    selected
                                ) { dialog, i ->
                                    PrefManager.setVal(PrefName.CommentNotificationInterval, i)
                                    it.settingsTitle.text =
                                        getString(
                                            R.string.comment_notification_checking_time,
                                            cItems[i]
                                        )
                                    dialog.dismiss()
                                    TaskScheduler.create(
                                        settings, PrefManager.getVal(PrefName.UseAlarmManager)
                                    ).scheduleAllTasks(settings)
                                }.create()
                            dialog.window?.setDimAmount(0.8f)
                            dialog.show()
                        }
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.notification_for_checking_subscriptions),
                        desc = getString(R.string.notification_for_checking_subscriptions_desc),
                        icon = R.drawable.ic_round_smart_button_24,
                        isChecked = PrefManager.getVal(PrefName.SubscriptionCheckingNotifications),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(
                                PrefName.SubscriptionCheckingNotifications,
                                isChecked
                            )
                        },
                        onLongClick = {
                            openSettings(settings, null)
                        }
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.use_alarm_manager_reliable),
                        desc = getString(R.string.use_alarm_manager_reliable_desc),
                        icon = R.drawable.ic_anilist,
                        isChecked = PrefManager.getVal(PrefName.UseAlarmManager),
                        switch = { isChecked, view ->
                            if (isChecked) {
                                val alertDialog = AlertDialog.Builder(settings, R.style.MyPopup)
                                    .setTitle(R.string.use_alarm_manager)
                                    .setMessage(R.string.use_alarm_manager_confirm)
                                    .setPositiveButton(R.string.use) { dialog, _ ->
                                        PrefManager.setVal(PrefName.UseAlarmManager, true)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            if (!(settings.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()) {
                                                val intent =
                                                    Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM")
                                                startActivity(intent)
                                                view.settingsButton.isChecked = true
                                            }
                                        }
                                        dialog.dismiss()
                                    }.setNegativeButton(R.string.cancel) { dialog, _ ->
                                        view.settingsButton.isChecked = false
                                        PrefManager.setVal(PrefName.UseAlarmManager, false)

                                        dialog.dismiss()
                                    }.create()
                                alertDialog.window?.setDimAmount(0.8f)
                                alertDialog.show()
                            } else {
                                PrefManager.setVal(PrefName.UseAlarmManager, false)
                                TaskScheduler.create(settings, true).cancelAllTasks()
                                TaskScheduler.create(settings, false)
                                    .scheduleAllTasks(settings)
                            }
                        },
                    ),
                )
            )
            settingsRecyclerView.apply {
                layoutManager = LinearLayoutManager(settings, LinearLayoutManager.VERTICAL, false)
                setHasFixedSize(true)
            }
        }
    }

    val model: AnilistHomeViewModel by viewModels()

    private fun subscribeCurrentLists() {
        CoroutineScope(Dispatchers.IO).launch {
            val userList = arrayListOf<Any>()
            Anilist.query.initHomePage().let { list ->
                list["currentAnime"]?.let { userList.addAll(it) }
                list["plannedAnime"]?.let { userList.addAll(it) }
                list["currentManga"]?.let { userList.addAll(it) }
                list["plannedManga"]?.let { userList.addAll(it) }
                // list["hiddenAnime"]?.let { userList.addAll(it) }
                // list["hiddenManga"]?.let { userList.addAll(it) }
            }

            if (userList.isEmpty()) {
                snackString(R.string.no_current_items)
            } else {
                userList.forEach {
                    saveSubscription(it as Media, true)
                }
                snackString(R.string.current_subscribed)
            }
        }
    }
}