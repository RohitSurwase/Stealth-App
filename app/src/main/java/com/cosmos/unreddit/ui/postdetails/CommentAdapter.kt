package com.cosmos.unreddit.ui.postdetails

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.local.mapper.CommentMapper2
import com.cosmos.unreddit.data.model.Comment
import com.cosmos.unreddit.data.model.Comment.CommentEntity
import com.cosmos.unreddit.data.model.Comment.MoreEntity
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.repository.PostListRepository
import com.cosmos.unreddit.databinding.ItemCommentBinding
import com.cosmos.unreddit.databinding.ItemMoreBinding
import com.cosmos.unreddit.ui.common.widget.RedditView
import com.cosmos.unreddit.util.extension.blurText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentAdapter(
    context: Context,
    mainImmediateDispatcher: CoroutineDispatcher,
    private val defaultDispatcher: CoroutineDispatcher,
    private val repository: PostListRepository,
    private val commentMapper: CommentMapper2,
    private val onLinkClickListener: RedditView.OnLinkClickListener? = null,
    private val onCommentLongClick: (CommentEntity) -> Unit
) : ListAdapter<Comment, RecyclerView.ViewHolder>(COMMENT_COMPARATOR) {

    var postEntity: PostEntity? = null

    var savedIds: List<String> = emptyList()

    private val scope = CoroutineScope(Job() + mainImmediateDispatcher)

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
        return when (getItem(position)) {
            is CommentEntity -> Type.COMMENT.value
            is MoreEntity -> Type.MORE.value
            else -> throw IllegalArgumentException("Unknown type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            Type.COMMENT.value ->
                (holder as CommentViewHolder).bind(getItem(position) as CommentEntity)
            Type.MORE.value -> {
                (holder as MoreViewHolder).bind(getItem(position) as MoreEntity)
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
            val comment = getItem(position)
            if (holder is CommentViewHolder && comment is CommentEntity) {
                holder.bindCommentHiddenIndicator(comment, true)
            }
        }
    }

    private fun onCommentClick(position: Int) {
        val newList = currentList.toMutableList()
        when (val comment = newList[position]) {
            is CommentEntity -> {
                scope.launch { onCommentClick(position, newList, comment) }
            }
            is MoreEntity -> {
                scope.launch { onMoreClick(position, newList, comment) }
            }
        }
    }

    private suspend fun onCommentClick(
        position: Int,
        newList: MutableList<Comment>,
        comment: CommentEntity
    ) {
        if (!comment.hasReplies) return

        val startIndex = position + 1

        if (!comment.isExpanded) {
            comment.isExpanded = true

            val replies = getExpandedReplies(comment.replies)
            newList.addAll(startIndex, replies)
        } else {
            comment.isExpanded = false

            val replyCount = getReplyCount(startIndex, comment.depth)
            comment.visibleReplyCount = replyCount

            val endIndex = startIndex + replyCount
            newList.subList(startIndex, endIndex).clear()
        }

        notifyItemChanged(position, comment)
        submitList(newList)
    }

    private suspend fun onMoreClick(
        position: Int,
        newList: MutableList<Comment>,
        comment: MoreEntity
    ) {
        val post = postEntity ?: return

        val containsMoreComments = comment.more.size > LOAD_MORE_LIMIT

        val children = comment.more.take(LOAD_MORE_LIMIT).joinToString(",")
        if (containsMoreComments) {
            // Remove first 100 comments from list
            with(comment) {
                more.subList(0, LOAD_MORE_LIMIT).clear()
                count = if (count > LOAD_MORE_LIMIT) count - LOAD_MORE_LIMIT else 0
            }
        }

        repository.getMoreChildren(children, post.id)
            .map {
                commentMapper.dataToEntities(it.json.data.things, null)
            }
            .map {
                it.filter { comment -> comment.depth < COMMENT_DEPTH_LIMIT }
            }
            .map {
                restoreCommentHierarchy(it, comment.depth)
            }
            .map { comments ->
                comment.apply { isLoading = false; isError = false }

                comments.forEach { comment ->
                    (comment as? CommentEntity)?.run {
                        linkTitle = linkTitle ?: post.title
                        linkPermalink = linkPermalink ?: post.permalink
                        linkAuthor = linkAuthor ?: post.author
                        saved = savedIds.contains(comment.name)
                    }
                }

                if (comment.depth > 0) {
                    val parentComment = newList.find { it.name == comment.parent }

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
                        return@map newList
                    }
                }

                newList.removeAt(position)
                newList.addAll(position, comments)

                if (containsMoreComments) {
                    newList.add(position + comments.size, comment)
                }

                return@map newList
            }
            .flowOn(defaultDispatcher)
            .onStart {
                comment.apply { isLoading = true; isError = false }
                notifyItemChanged(position)
            }
            .catch {
                comment.apply { isLoading = false; isError = true }
                notifyItemChanged(position)
            }
            .collect { comments ->
                submitList(comments)
            }
    }

    private fun onCommentLongClick(position: Int) {
        val comment = getItem(position)
        if (comment is CommentEntity) {
            comment.run {
                saved = savedIds.contains(name)
                onCommentLongClick.invoke(this)
            }
        }
    }

    private suspend fun getExpandedReplies(
        comments: List<Comment>
    ): List<Comment> = withContext(defaultDispatcher) {
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

        return@withContext replies
    }

    private suspend fun getReplyCount(
        index: Int,
        depth: Int
    ): Int = withContext(defaultDispatcher) {
        var count = 0

        for (i in index until itemCount) {
            if (getItem(i).depth > depth) {
                count++
            } else {
                break
            }
        }

        return@withContext count
    }

    private suspend fun restoreCommentHierarchy(
        comments: List<Comment>,
        depth: Int
    ): List<Comment> = withContext(defaultDispatcher) {
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

        return@withContext restored
    }

    fun cleanUp() {
        scope.cancel()
    }

    private inner class CommentViewHolder(
        private val binding: ItemCommentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: CommentEntity) {
            binding.comment = comment

            binding.commentAuthor.apply {
                setTextColor(ContextCompat.getColor(context, comment.posterType.color))
            }

            binding.commentScore.blurText(comment.scoreHidden)

            binding.commentColorIndicator.setCommentColor(comment)

            bindCommentHiddenIndicator(comment, false)

            binding.commentFlair.apply {
                if (!comment.flair.isEmpty()) {
                    visibility = View.VISIBLE

                    setFlair(comment.flair)
                } else {
                    visibility = View.GONE
                }
            }

            binding.commentAwards.apply {
                if (comment.awards.isNotEmpty()) {
                    visibility = View.VISIBLE

                    setAwards(comment.awards, comment.totalAwards)
                } else {
                    visibility = View.GONE
                }
            }

            binding.commentOpText.visibility = if (comment.isSubmitter) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                onCommentClick(bindingAdapterPosition)
            }

            itemView.setOnLongClickListener {
                onCommentLongClick(bindingAdapterPosition)
                true
            }

            binding.commentBody.apply {
                setText(comment.body)
                setOnLinkClickListener(onLinkClickListener)
                setOnClickListener {
                    onCommentClick(bindingAdapterPosition)
                }
                setOnLongClickListener {
                    onCommentLongClick(bindingAdapterPosition)
                    true
                }
            }
        }

        fun bindCommentHiddenIndicator(comment: CommentEntity, showAnimation: Boolean) {
            binding.commentHiddenIndicator.apply {
                if (comment.hasReplies && !comment.isExpanded) {
                    visibility = View.VISIBLE
                    text = comment.visibleReplyCount.toString()
                    if (showAnimation) startAnimation(popInAnimation)
                } else {
                    visibility = View.GONE
                    if (showAnimation) startAnimation(popOutAnimation)
                }
            }
        }
    }

    private inner class MoreViewHolder(
        private val binding: ItemMoreBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(more: MoreEntity) {
            binding.more = more

            binding.progress.isVisible = more.isLoading
            binding.textError.isVisible = more.isError

            binding.commentColorIndicator.setCommentColor(more)

            itemView.setOnClickListener {
                onCommentClick(bindingAdapterPosition)
            }
        }
    }

    private fun ImageView.setCommentColor(comment: Comment) {
        this.apply {
            if (comment.depth == 0) {
                visibility = View.GONE
            } else {
                visibility = View.VISIBLE
                comment.commentIndicator?.let {
                    backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(context, it)
                    )
                }
                val params = ConstraintLayout.LayoutParams(
                    layoutParams as ConstraintLayout.LayoutParams
                ).apply {
                    marginStart = (commentOffset * (comment.depth - 1)).toInt()
                }
                layoutParams = params
            }
        }
    }

    private enum class Type(val value: Int) {
        COMMENT(0), MORE(1)
    }

    companion object {
        const val LOAD_MORE_LIMIT = 100
        const val COMMENT_DEPTH_LIMIT = 10

        private val COMMENT_COMPARATOR = object : DiffUtil.ItemCallback<Comment>() {
            override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
                return when {
                    oldItem is CommentEntity && newItem is CommentEntity -> {
                        oldItem.name == newItem.name
                    }
                    oldItem is MoreEntity && newItem is MoreEntity -> {
                        oldItem.id == newItem.id
                    }
                    else -> {
                        false
                    }
                }
            }

            override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
                return oldItem == newItem
            }
        }
    }
}
