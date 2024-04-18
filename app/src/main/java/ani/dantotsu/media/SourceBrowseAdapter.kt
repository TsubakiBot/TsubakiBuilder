package ani.dantotsu.media

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.databinding.ItemCharacterBinding
import ani.dantotsu.loadImage
import ani.dantotsu.parsers.ShowResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SourceBrowseAdapter(
    private val sources: List<ShowResponse>,
    private val mediaType: MediaType,
    private val dialogFragment: SourceBrowseDialogFragment,
    private val scope: CoroutineScope
) : RecyclerView.Adapter<SourceBrowseAdapter.SourceViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SourceViewHolder {
        val binding =
            ItemCharacterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SourceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SourceViewHolder, position: Int) {
        val binding = holder.binding
        val character = sources[position]
        binding.itemCompactImage.loadImage(character.coverUrl, 200)
        binding.itemCompactTitle.isSelected = true
        binding.itemCompactTitle.text = character.name
    }

    override fun getItemCount(): Int = sources.size

    inner class SourceViewHolder(val binding: ItemCharacterBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                dialogFragment.dismiss()
                scope.launch(Dispatchers.IO) {
                    ContextCompat.startActivity(
                        it.context,
                        Intent(it.context, SearchActivity::class.java)
                            .putExtra("type", mediaType.asText().uppercase())
                            .putExtra("query", binding.itemCompactTitle.text)
                            .putExtra("search", true),
                        null
                    )
                }
            }
            var a = true
            itemView.setOnLongClickListener {
                a = !a
                binding.itemCompactTitle.isSingleLine = a
                true
            }
        }
    }
}