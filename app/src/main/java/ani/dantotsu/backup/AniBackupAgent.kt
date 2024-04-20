package ani.dantotsu.backup

import android.app.backup.BackupAgentHelper
import android.app.backup.SharedPreferencesBackupHelper
import android.content.Context
import ani.dantotsu.settings.saving.PrefName

class AniBackupAgent : BackupAgentHelper() {

    companion object {
        private const val PREFS_GENERAL = "ani.dantotsu.general"
        private const val PREFS_UI = "ani.dantotsu.ui"
        private const val PREFS_PLAYER = "ani.dantotsu.player"
        private const val PREFS_READER = "ani.dantotsu.reader"
        private const val PREFS_NOVEL_READER = "ani.dantotsu.novelReader"
        private const val PREFS_IRRELEVANT = "ani.dantotsu.irrelevant"
        private const val PREFS_ANIME_DOWNLOADS = "animeDownloads"
        private const val PREFS_PROTECTED = "ani.dantotsu.protected"
        private const val PREFS_BACKUP_KEY = "aniBackup"
    }

    override fun onCreate() {
        SharedPreferencesBackupHelper(this, PREFS_GENERAL).also {
            addHelper(PREFS_BACKUP_KEY, it)
        }
        SharedPreferencesBackupHelper(this, PREFS_UI).also {
            addHelper(PREFS_BACKUP_KEY, it)
        }

        SharedPreferencesBackupHelper(this, PREFS_PLAYER).also {
            addHelper(PREFS_BACKUP_KEY, it)
        }

        SharedPreferencesBackupHelper(this, PREFS_READER).also {
            addHelper(PREFS_BACKUP_KEY, it)
        }

        SharedPreferencesBackupHelper(this, PREFS_NOVEL_READER).also {
            addHelper(PREFS_BACKUP_KEY, it)
        }

//        SharedPreferencesBackupHelper(this, PREFS_IRRELEVANT).also {
//            addHelper(PREFS_BACKUP_KEY, it)
//        }
        val irrelevant: Map<String, *> =
            getSharedPreferences(PREFS_IRRELEVANT, Context.MODE_PRIVATE).all
        val keys = irrelevant.filterKeys {
            it != PrefName.OfflineMode.name &&
                    it != PrefName.DiscordStatus.name &&
                    it != PrefName.DownloadsKeys.name &&
                    it != PrefName.NovelLastExtCheck.name &&
                    it != PrefName.ImageUrl.name &&
                    it != PrefName.AllowOpeningLinks.name &&
                    it != PrefName.HasUpdatedPrefs.name &&
                    it != PrefName.MakeDefault.name &&
                    it != PrefName.FirstComment.name &&
                    it != PrefName.CommentAuthResponse.name &&
                    it != PrefName.CommentTokenExpiry.name &&
                    it != PrefName.RecentGlobalNotification.name &&
                    it != PrefName.CommentNotificationStore.name &&
                    it != PrefName.UnreadCommentNotifications.name
        }.keys.toTypedArray()

        SharedPreferencesBackupHelper(this, *keys).also {
            addHelper(PREFS_BACKUP_KEY, it)
        }


        SharedPreferencesBackupHelper(this, PREFS_ANIME_DOWNLOADS).also {
            addHelper(PREFS_BACKUP_KEY, it)
        }

        SharedPreferencesBackupHelper(this, PREFS_PROTECTED).also {
            addHelper(PREFS_BACKUP_KEY, it)
        }
    }

}
