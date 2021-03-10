package com.cosmos.unreddit.user

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.cosmos.unreddit.R
import com.cosmos.unreddit.databinding.ItemUserCommentBinding
import com.cosmos.unreddit.post.Comment
import com.cosmos.unreddit.post.CommentEntity
import com.cosmos.unreddit.util.DateUtil
import com.cosmos.unreddit.util.PostUtil
import com.cosmos.unreddit.util.applyGradient
import com.cosmos.unreddit.util.blurText
import com.cosmos.unreddit.view.RedditView

class UserCommentsAdapter(
    context: Context,
    private val onLinkClickListener: RedditView.OnLinkClickListener? = null,
    private val onCommentClick: (CommentEntity) -> Unit
) : PagingDataAdapter<Comment, UserCommentsAdapter.CommentViewHolder>(COMMENT_COMPARATOR) {

    private val colorPrimary by lazy {
        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return CommentViewHolder(ItemUserCommentBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = getItem(position) as? CommentEntity ?: return
        holder.bind(comment)
    }

    inner class CommentViewHolder(
        private val binding: ItemUserCommentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: CommentEntity) {
            binding.comment = comment
            binding.includeItemComment.comment = comment

            with(comment) {
                with(binding.includeItemComment.commentAuthor) {
                    applyGradient(
                        comment.author,
                        PostUtil.getAuthorGradientColor(
                            context,
                            R.color.regular_gradient_start,
                            R.color.regular_gradient_end
                        )
                    )
                }

                binding.includeItemComment.commentScore.blurText(scoreHidden)

                with(binding.includeItemComment.commentDate) {
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

                with(binding.includeItemComment.commentColorIndicator) {
                    visibility = View.VISIBLE
                    backgroundTintList = colorPrimary
                }

                with(binding.includeItemComment.commentFlair) {
                    if (!flair.isEmpty()) {
                        visibility = View.VISIBLE

                        setFlair(flair)
                    } else {
                        visibility = View.GONE
                    }
                }

                with(binding.includeItemComment.commentAwards) {
                    if (awards.isNotEmpty()) {
                        visibility = View.VISIBLE

                        setAwards(awards, totalAwards)
                    } else {
                        visibility = View.GONE
                    }
                }

            }

            with(binding.includeItemComment.commentBody) {
                setText(comment.body)
                setOnLinkClickListener(onLinkClickListener)
                setOnClickListener { onCommentClick(comment) }
            }

            itemView.setOnClickListener { onCommentClick(comment) }
        }
    }

    companion object {
        private val COMMENT_COMPARATOR = object : DiffUtil.ItemCallback<Comment>() {

            override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
                return (oldItem as? CommentEntity)?.name == (newItem as? CommentEntity)?.name
            }

            override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
                return oldItem as? CommentEntity == newItem as? CommentEntity
            }
        }
    }
}
