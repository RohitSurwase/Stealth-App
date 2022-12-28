package com.cosmos.unreddit.ui.privacyenhancer

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import com.cosmos.unreddit.databinding.ItemInstanceBinding
import com.cosmos.unreddit.util.extension.titlecase

class PrivacyEnhancerInstanceAdapter(
    private val instances: Map<String, String>
) : BaseAdapter(), Filterable {

    private var _instances: Map<String, String> = instances

    override fun getCount(): Int {
        return _instances.size
    }

    override fun getItem(position: Int): Any {
        return _instances.keys.elementAt(position)
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    @SuppressLint("ViewHolder")
    @Suppress("UNCHECKED_CAST")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val layoutInflater = LayoutInflater.from(parent.context)

        val entry = _instances.entries.elementAt(position)

        val binding = ItemInstanceBinding.inflate(layoutInflater, parent, false)
            .apply {
                instance.text = entry.key
                service.text = entry.value.titlecase
            }

        return binding.root
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()

                if (constraint == null || constraint.isEmpty()) {
                    results.run {
                        values = instances
                        count = instances.size
                    }
                } else {
                    val filteredMap = instances.filter {
                        it.key.contains(constraint) || it.value.contains(constraint)
                    }
                    results.run {
                        values = filteredMap
                        count = filteredMap.size
                    }
                }

                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                _instances = results.values as? Map<String, String> ?: instances

                if (results.count > 0) {
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }
}
