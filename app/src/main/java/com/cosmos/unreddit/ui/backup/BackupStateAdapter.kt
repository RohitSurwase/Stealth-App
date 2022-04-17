package com.cosmos.unreddit.ui.backup

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.cosmos.unreddit.ui.backup.BackupFragment.Operation.BACKUP
import com.cosmos.unreddit.ui.backup.BackupFragment.Operation.RESTORE
import com.cosmos.unreddit.ui.base.BaseFragment

class BackupStateAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    var operation: BackupFragment.Operation? = null
        set(value) {
            if (field != value && value != null) {
                updateFragments(value)
            }
            field = value
        }

    private val backupSteps: Array<Class<out BaseFragment>> = arrayOf(
        BackupOperationFragment::class.java,
        BackupLocationFragment::class.java,
        BackupLoadingFragment::class.java
    )

    private val restoreSteps: Array<Class<out BaseFragment>> = arrayOf(
        BackupOperationFragment::class.java,
        BackupChoiceFragment::class.java,
        BackupLocationFragment::class.java,
        BackupLoadingFragment::class.java
    )

    private val fragments: MutableList<Class<out BaseFragment>> = mutableListOf(
        BackupOperationFragment::class.java
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position].newInstance()

    private fun updateFragments(operation: BackupFragment.Operation) {
        if (fragments.size > 1) {
            // Remove all other fragments except the first one
            val indices = 1..fragments.lastIndex
            fragments.subList(indices.first, indices.last + 1).clear()
            notifyItemRangeRemoved(indices.first, indices.count())
        }

        val count: Int

        when (operation) {
            BACKUP -> {
                backupSteps
                    .filterIndexed { index, _ -> index > 0 }
                    .let { steps ->
                        fragments.addAll(steps)
                        count = steps.size
                    }
            }
            RESTORE -> {
                restoreSteps
                    .filterIndexed { index, _ -> index > 0 }
                    .let { steps ->
                        fragments.addAll(steps)
                        count = steps.size
                    }
            }
        }

        notifyItemRangeChanged(1, count)
    }

    fun getStep(position: Int): Class<out BaseFragment>? = fragments.getOrNull(position)
}
