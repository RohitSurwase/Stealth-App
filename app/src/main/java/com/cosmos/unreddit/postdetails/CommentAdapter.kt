package com.cosmos.unreddit.postdetails

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.BlurMaskFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.cosmos.unreddit.R
import com.cosmos.unreddit.database.CommentMapper
import com.cosmos.unreddit.databinding.ItemCommentBinding
import com.cosmos.unreddit.databinding.ItemMoreBinding
import com.cosmos.unreddit.model.PosterType
import com.cosmos.unreddit.parser.ClickableMovementMethod
import com.cosmos.unreddit.post.Comment
import com.cosmos.unreddit.post.CommentEntity
import com.cosmos.unreddit.post.MoreEntity
import com.cosmos.unreddit.postlist.PostListRepository
import com.cosmos.unreddit.util.DateUtil
import com.cosmos.unreddit.util.applyGradient
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class CommentAdapter(
    context: Context,
    private val clickableMovementMethod: ClickableMovementMethod,
    private val repository: PostListRepository,
    private val viewLifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var linkId: String? = null
    private val visibleComments = mutableListOf<Comment>()

    private val commentOffset by lazy {
        context.resources.getDimension(R.dimen.comment_offset)
    }
    private val popInAnimation by lazy {
        AnimationUtils.loadAnimation(context, R.anim.pop_in)
    }
    private val popOutAnimation by lazy {
        AnimationUtils.loadAnimation(context, R.anim.pop_out)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            Type.COMMENT.value ->
                CommentViewHolder(ItemCommentBinding.inflate(inflater, parent, false))
            Type.MORE.value ->
                MoreViewHolder(ItemMoreBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Unknown type $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (visibleComments[position]) {
            is CommentEntity -> Type.COMMENT.value
            is MoreEntity -> Type.MORE.value
            else -> throw IllegalArgumentException("Unknown type")
        }
    }

    override fun getItemCount(): Int {
        return visibleComments.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            Type.COMMENT.value ->
                (holder as CommentViewHolder).bind(visibleComments[position] as CommentEntity)
            Type.MORE.value -> {
                (holder as MoreViewHolder).bind(visibleComments[position] as MoreEntity)
            }
            else -> throw IllegalArgumentException("Unknown type")
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val comment = visibleComments[position]
            if (holder is CommentViewHolder && comment is CommentEntity) {
                holder.bindCommentHiddenIndicator(comment, true)
            }
        }
    }

    private fun onCommentClick(position: Int) {
        when (val comment = visibleComments[position]) {
            is CommentEntity -> {
                if (!comment.hasReplies) return

                val startIndex = position + 1

                if (!comment.isExpanded) {
                    comment.isExpanded = true

                    val replies = getExpandedReplies(comment.replies)
                    visibleComments.addAll(startIndex, replies)

                    notifyItemRangeInserted(startIndex, replies.size)
                } else {
                    comment.isExpanded = false

                    val replyCount = getReplyCount(startIndex, comment.depth)
                    comment.visibleReplyCount = replyCount

                    val endIndex = startIndex + replyCount
                    visibleComments.subList(startIndex, endIndex).clear()

                    notifyItemRangeRemoved(startIndex, replyCount)
                }

                notifyItemChanged(position, comment)
            }
            is MoreEntity -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    val link = linkId ?: return@launch

                    val containsMoreComments = comment.more.size > LOAD_MORE_LIMIT

                    val children = comment.more.take(LOAD_MORE_LIMIT).joinToString(",")
                    if (containsMoreComments) {
                        // Remove first 100 comments from list
                        with(comment) {
                            more.subList(0, LOAD_MORE_LIMIT).clear()
                            count = if (count > LOAD_MORE_LIMIT) count - LOAD_MORE_LIMIT else 0
                        }
                    }

                    repository.getMoreChildren(children, link).map {
                        CommentMapper.dataToEntities(it.json.data.things)
                    }.map {
                        it.filter { comment -> comment.depth < COMMENT_DEPTH_LIMIT }
                    }.map {
                        restoreCommentHierarchy(it, comment.depth)
                    }.collectLatest { comments ->
                        if (comment.depth > 0) {
                            val parentComment = visibleComments.find { it.name == comment.parent }

                            if (parentComment != null &&
                                parentComment is CommentEntity &&
                                parentComment.isExpanded
                            ) {
                                parentComment.replies.removeLastOrNull()
                                parentComment.replies.addAll(comments)
                                if (containsMoreComments) {
                                    parentComment.replies.add(comment)
                                }
                            } else {
                                return@collectLatest
                            }
                        }

                        visibleComments.removeAt(position)
                        notifyItemRemoved(position)
                        visibleComments.addAll(position, comments)

                        var itemCount = comments.size
                        if (containsMoreComments) {
                            visibleComments.add(position + itemCount, comment)
                            itemCount++
                        }

                        notifyItemRangeInserted(position, itemCount)
                    }
                }
            }
        }
    }

    private fun getExpandedReplies(comments: List<Comment>): List<Comment> {
        val replies = mutableListOf<Comment>()

        for (comment in comments) {
            when (comment) {
                is CommentEntity -> {
                    replies.add(comment)
                    if (comment.isExpanded) {
                        replies.addAll(getExpandedReplies(comment.replies))
                    }
                }
                is MoreEntity -> replies.add(comment)
            }
        }

        return replies
    }

    private fun getReplyCount(index: Int, depth: Int): Int {
        var count = 0

        for (i in index until visibleComments.size) {
            if (visibleComments[i].depth > depth) {
                count++
            } else {
                break
            }
        }

        return count
    }

    private fun restoreCommentHierarchy(comments: List<Comment>, depth: Int): List<Comment> {
        val restored = mutableListOf<Comment>()

        for (i in comments.indices) {
            val comment = comments[i]

            if (comment.depth > depth) {
                continue
            } else if (comment.depth < depth) {
                break
            }

            val nextComment = comments.getOrNull(i + 1)

            if (comment is CommentEntity && nextComment != null && nextComment.depth > depth) {
                comment.replies.addAll(
                    restoreCommentHierarchy(comments.subList(i + 1, comments.lastIndex), depth + 1)
                )
                comment.visibleReplyCount = comment.replies.size
            }

            restored.add(comment)
        }

        return restored
    }

    fun submitData(comments: List<Comment>) {
        val result = DiffUtil.calculateDiff(CommentDiffCallback(this.visibleComments, comments))

        this.visibleComments.clear()
        this.visibleComments.addAll(comments)

        result.dispatchUpdatesTo(this)
    }

    fun setLinkId(linkId: String) {
        if (this.linkId != linkId) {
            this.linkId = linkId
        }
    }

    private inner class CommentViewHolder(
        private val binding: ItemCommentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: CommentEntity) {
            binding.comment = comment

            with(comment) {
                with(binding.commentAuthor) {
                    applyGradient(
                        comment.author,
                        PosterType.getGradientColors(context, comment.posterType)
                    )
                }

                with(binding.commentScore) {
                    // Blur score when hidden
                    if (scoreHidden) {
                        val radius = textSize / 3
                        val blurMaskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
                        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                        paint.maskFilter = blurMaskFilter
                    } else {
                        paint.maskFilter = null
                    }
                }

                with(binding.commentDate) {
                    val timeDifference = DateUtil.getTimeDifference(context, created)
                    text = if (edited > -1) {
                        val editedTimeDifference = DateUtil.getTimeDifference(
                            context,
                            edited,
                            false
                        )
                        context.getString(
                            R.string.comment_date_edited,
                            timeDifference,
                            editedTimeDifference
                        )
                    } else {
                        timeDifference
                    }
                }

                with(binding.commentColorIndicator) {
                    if (depth == 0) {
                        visibility = View.GONE
                    } else {
                        visibility = View.VISIBLE
                        getIndicatorColor(context)?.let {
                            backgroundTintList = ColorStateList.valueOf(it)
                        }
                        val params = ConstraintLayout.LayoutParams(
                            layoutParams as ConstraintLayout.LayoutParams
                        ).apply {
                            marginStart = (commentOffset * (depth - 1)).toInt()
                        }
                        layoutParams = params
                    }
                }

                bindCommentHiddenIndicator(comment, false)

                with(binding.commentFlair) {
                    if (!flair.isEmpty()) {
                        visibility = View.VISIBLE

                        setFlair(flair)
                    } else {
                        visibility = View.GONE
                    }
                }

                with(binding.commentAwards) {
                    if (awards.isNotEmpty()) {
                        visibility = View.VISIBLE

                        setAwards(awards, totalAwards)
                    } else {
                        visibility = View.GONE
                    }
                }

                binding.commentOpIcon.visibility = if (isSubmitter) View.VISIBLE else View.GONE

                itemView.setOnClickListener {
                    onCommentClick(bindingAdapterPosition)
                }
            }

            with(binding.commentBody) {
                setText(comment.body, clickableMovementMethod)
            }
        }

        fun bindCommentHiddenIndicator(comment: CommentEntity, showAnimation: Boolean) {
            with(comment) {
                with(binding.commentHiddenIndicator) {
                    if (!hasReplies || isExpanded) {
                        visibility = View.GONE
                        if (showAnimation) startAnimation(popOutAnimation)
                    } else if (hasReplies && !isExpanded) {
                        visibility = View.VISIBLE
                        text = visibleReplyCount.toString()
                        if (showAnimation) startAnimation(popInAnimation)
                    }
                }
            }
        }
    }

    private inner class MoreViewHolder(
        private val binding: ItemMoreBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(more: MoreEntity) {
            binding.more = more

            binding.progress.visibility = View.GONE

            with(binding.commentBody) {
                val params = ConstraintLayout.LayoutParams(
                    layoutParams as ConstraintLayout.LayoutParams
                ).apply {
                    marginStart = if (more.depth > 0) {
                        (commentOffset * more.depth).toInt()
                    } else {
                        0
                    }
                }
                layoutParams = params
            }

            itemView.setOnClickListener {
                binding.progress.visibility = View.VISIBLE
                onCommentClick(bindingAdapterPosition)
            }
        }
    }

    private inner class CommentDiffCallback(
        private val oldComments: List<Comment>,
        private val newComments: List<Comment>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldComments.size
        }

        override fun getNewListSize(): Int {
            return newComments.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldComment = oldComments[oldItemPosition]
            val newComment = newComments[newItemPosition]

            return if (oldComment is CommentEntity && newComment is CommentEntity) {
                oldComment.name == newComment.name
            } else if (oldComment is MoreEntity && newComment is MoreEntity) {
                oldComment.id == newComment.id
            } else {
                false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldComments[oldItemPosition] == newComments[newItemPosition]
        }
    }

    private enum class Type(val value: Int) {
        COMMENT(0), MORE(1)
    }

    companion object {
        private const val LOAD_MORE_LIMIT = 100
        private const val COMMENT_DEPTH_LIMIT = 10
    }
}
