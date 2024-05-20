package ani.dantotsu.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivityFaqBinding
import ani.dantotsu.initActivity
import ani.dantotsu.themes.ThemeManager

class FAQActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFaqBinding

    private val faqs by lazy {
        listOf(

            Triple(
                R.drawable.ic_round_help_24,
                getString(R.string.question_1),
                getString(R.string.answer_1)
            ),
            Triple(
                R.drawable.ic_round_auto_awesome_24,
                getString(R.string.question_2),
                getString(R.string.answer_2)
            ),
            Triple(
                R.drawable.ic_round_auto_awesome_24,
                getString(R.string.question_17),
                getString(R.string.answer_17)
            ),
            Triple(
                R.drawable.ic_download_24,
                getString(R.string.question_3),
                getString(R.string.answer_3)
            ),
            Triple(
                R.drawable.ic_round_help_24,
                getString(R.string.question_16),
                getString(R.string.answer_16)
            ),
            Triple(
                R.drawable.ic_round_dns_24,
                getString(R.string.question_4),
                getString(R.string.answer_4)
            ),
            Triple(
                R.drawable.ic_baseline_screen_lock_portrait_24,
                getString(R.string.question_5),
                getString(R.string.answer_5)
            ),
            Triple(
                R.drawable.ic_admin_panel_settings_24,
                getString(R.string.question_src),
                getString(R.string.answer_src)
            ),
            Triple(
                R.drawable.ic_anilist,
                getString(R.string.question_6),
                getString(R.string.answer_6)
            ),
            Triple(
                R.drawable.ic_round_movie_filter_24,
                getString(R.string.question_7),
                getString(R.string.answer_7)
            ),
            Triple(
                R.drawable.ic_round_lock_open_24,
                getString(R.string.question_9),
                getString(R.string.answer_9)
            ),
            Triple(
                R.drawable.ic_round_smart_button_24,
                getString(R.string.question_10),
                getString(R.string.answer_10)
            ),
            Triple(
                R.drawable.ic_round_smart_button_24,
                getString(R.string.question_11),
                getString(R.string.answer_11)
            ),
            Triple(
                R.drawable.ic_round_info_24,
                getString(R.string.question_12),
                getString(R.string.answer_12)
            ),
            Triple(
                R.drawable.ic_round_help_24,
                getString(R.string.question_13),
                getString(R.string.answer_13)
            ),
            Triple(
                R.drawable.ic_round_art_track_24,
                getString(R.string.question_14),
                getString(R.string.answer_14)
            ),
            Triple(
                R.drawable.ic_round_video_settings_24,
                getString(R.string.question_15),
                getString(R.string.answer_15)
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeManager(this).applyTheme()
        binding = ActivityFaqBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivity(this)

        binding.devsTitle2.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.devsRecyclerView.adapter = FAQAdapter(faqs, supportFragmentManager)
        binding.devsRecyclerView.layoutManager = LinearLayoutManager(this)
    }
}
