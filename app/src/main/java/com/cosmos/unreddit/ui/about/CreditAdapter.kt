package com.cosmos.unreddit.ui.about

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cosmos.unreddit.data.model.CreditItem
import com.cosmos.unreddit.databinding.ItemContributorBinding
import com.cosmos.unreddit.databinding.ItemCreditBinding
import com.cosmos.unreddit.databinding.ItemSectionBinding

class CreditAdapter(
    private val onClick: (CreditItem) -> Unit
) : ListAdapter<CreditItem, RecyclerView.ViewHolder>(CREDIT_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            Type.CREDIT.value ->
                CreditViewHolder(ItemCreditBinding.inflate(inflater, parent, false))
            Type.SECTION.value ->
                SectionViewHolder(ItemSectionBinding.inflate(inflater, parent, false))
            Type.CONTRIBUTOR.value ->
                ContributorViewHolder(ItemContributorBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Unknown type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            Type.CREDIT.value ->
                (holder as CreditViewHolder).bind(getItem(position) as CreditItem.Credit)
            Type.SECTION.value ->
                (holder as SectionViewHolder).bind(getItem(position) as CreditItem.Section)
            Type.CONTRIBUTOR.value ->
                (holder as ContributorViewHolder).bind(getItem(position) as CreditItem.Contributor)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is CreditItem.Credit -> Type.CREDIT.value
            is CreditItem.Section -> Type.SECTION.value
            is CreditItem.Contributor -> Type.CONTRIBUTOR.value
            else -> throw IllegalArgumentException("Unknown type")
        }
    }

    inner class CreditViewHolder(
        private val binding: ItemCreditBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(credit: CreditItem.Credit) {
            binding.credit = credit
            itemView.setOnClickListener { onClick.invoke(credit) }
        }
    }

    inner class SectionViewHolder(
        private val binding: ItemSectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(section: CreditItem.Section) {
            binding.title.apply {
                text = context.getString(section.title)
            }
        }
    }

    inner class ContributorViewHolder(
        private val binding: ItemContributorBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contributor: CreditItem.Contributor) {
            binding.contributor = contributor
            binding.contributorDescription.run {
                text = context.getString(contributor.description)
            }
            itemView.setOnClickListener { onClick.invoke(contributor) }
        }
    }

    private enum class Type(val value: Int) {
        CREDIT(0), SECTION(1), CONTRIBUTOR(2)
    }

    companion object {
        private val CREDIT_COMPARATOR = object : DiffUtil.ItemCallback<CreditItem>() {

            override fun areItemsTheSame(oldItem: CreditItem, newItem: CreditItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: CreditItem, newItem: CreditItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
