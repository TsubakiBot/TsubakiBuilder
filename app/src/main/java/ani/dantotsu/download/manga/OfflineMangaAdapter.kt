package ani.dantotsu.download.manga

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import ani.dantotsu.R
import bit.himitsu.cardView
import bit.himitsu.imageView
import bit.himitsu.linearLayout
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import bit.himitsu.textView


class OfflineMangaAdapter(
    private val context: Context,
    private var items: List<OfflineMangaModel>,
    private val searchListener: OfflineMangaSearchListener
) : BaseAdapter() {
    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var originalItems: List<OfflineMangaModel> = items
    private var style: Int = PrefManager.getVal(PrefName.OfflineView)

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val view: View = convertView ?: when (style) {
            0 -> inflater.inflate(R.layout.item_media_large, parent, false) // large view
            1 -> inflater.inflate(R.layout.item_media_compact, parent, false) // compact view
            else -> inflater.inflate(R.layout.item_media_compact, parent, false) // compact view
        }

        val item = getItem(position) as OfflineMangaModel

        val imageView = view.imageView(R.id.itemCompactImage)
        val titleTextView = view.textView(R.id.itemCompactTitle)
        val itemScore = view.textView(R.id.itemCompactScore)
        val ongoing = view.cardView(R.id.itemCompactOngoing)
        val totalChapter = view.textView(R.id.itemCompactTotal)
        val typeImage = view.imageView(R.id.itemCompactTypeImage)
        val type = view.textView(R.id.itemCompactRelation)
        val typeView = view.linearLayout(R.id.itemCompactType)

        if (style == 0) {
            val bannerView = view.imageView(R.id.itemCompactBanner) // for large view
            val chapters = view.textView(R.id.itemTotalLabel)
            chapters.text = context.getString(R.string.chapters)
            bannerView.setImageURI(item.banner ?: item.image)
            totalChapter.text = item.totalChapter
        } else if (style == 1) {
            val readChapter = view.textView(R.id.itemCompactUserProgress) // for compact view
            readChapter.text = item.readChapter
            totalChapter.text = context.getString(R.string.total_divider, item.totalChapter)
        }

        // Bind item data to the views
        typeImage.setImageResource(if (item.type == "Novel") R.drawable.ic_round_book_24 else R.drawable.ic_round_import_contacts_24)
        type.text = item.type
        typeView.visibility = View.VISIBLE
        imageView.setImageURI(item.image)
        titleTextView.text = item.title
        itemScore.text = item.score

        if (item.isOngoing) {
            ongoing.visibility = View.VISIBLE
        } else {
            ongoing.visibility = View.GONE
        }
        return view
    }

    fun onSearchQuery(query: String) {
        // Implement the filtering logic here, for example:
        items = if (query.isEmpty()) {
            // Return the original list if the query is empty
            originalItems
        } else {
            // Filter the list based on the query
            originalItems.filter { it.title.contains(query, ignoreCase = true) }
        }
        notifyDataSetChanged() // Notify the adapter that the data set has changed
    }

    fun setItems(items: List<OfflineMangaModel>) {
        this.items = items
        this.originalItems = items
        notifyDataSetChanged()
    }

    fun notifyNewGrid() {
        style = PrefManager.getVal(PrefName.OfflineView)
        notifyDataSetChanged()
    }
}