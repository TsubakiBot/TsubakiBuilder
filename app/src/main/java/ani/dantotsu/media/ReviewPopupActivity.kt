package ani.dantotsu.media

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.Refresh
import ani.dantotsu.databinding.ActivityGenreBinding
import ani.dantotsu.initActivity
import ani.dantotsu.padBottomOrRight
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReviewPopupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGenreBinding
    private val scope = lifecycleScope
    private val model: OtherDetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeManager(this).applyTheme()
        binding = ActivityGenreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initActivity(this)

        binding.listBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.genreContainer.padBottomOrRight(resources.configuration)
            .updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin += statusBarHeight
        }

        binding.listTitle.setText(R.string.review_type)

        val type = intent.getStringExtra("type")
        if (type != null) {
            val name ="${type.substring(0, 1)}${type.substring(1).lowercase()}"
            binding.listTitle.text = getString(R.string.review_type, name)
            binding.emptyRecyclerText.text = getString(R.string.reviews_empty, name)

            model.getReviews().observe(this) {
                if (it?.get(type).isNullOrEmpty()) {
                    binding.emptyRecyclerText.visibility = View.VISIBLE
                } else {
                    binding.emptyRecyclerText.visibility = View.GONE
                    binding.mediaInfoGenresProgressBar.visibility = View.GONE
                    val adapter = ReviewAdapter(this, it!![type]!!)
                    binding.mediaInfoGenresRecyclerView.adapter = adapter
                    binding.mediaInfoGenresRecyclerView.layoutManager = LinearLayoutManager(this)
                }
            }

            val live = Refresh.activity.getOrPut(this.hashCode()) { MutableLiveData(true) }
            live.observe(this) {
                if (it) {
                    scope.launch {
                        withContext(Dispatchers.IO) { model.loadReviews(type) }
                        live.postValue(false)
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.genreContainer.padBottomOrRight(newConfig)
    }
}
