package com.cosmos.unreddit.ui.common.adapter

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class FragmentAdapter(
    fragment: Fragment,
    private val fragments: List<Page>
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment =
        fragments[position].fragment.newInstance()

    data class Page(@StringRes val title: Int, val fragment: Class<out Fragment>)
}
