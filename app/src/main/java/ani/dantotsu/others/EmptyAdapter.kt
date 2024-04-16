package ani.dantotsu.others

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class EmptyAdapter(private val count: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return EmptyViewHolder(View(parent.context))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}

    override fun getItemCount(): Int = count

    inner class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}