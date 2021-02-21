package com.cosmos.unreddit.user

import android.graphics.BlurMaskFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.cosmos.unreddit.databinding.ItemUserCommentBinding
import com.cosmos.unreddit.post.Comment
import com.cosmos.unreddit.post.CommentEntity

class UserCommentsAdapter
    : PagingDataAdapter<Comment, UserCommentsAdapter.CommentViewHolder>(COMMENT_COMPARATOR) {

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

            with(comment) {
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
            }
            // TODO: Text
        }
    }

    companion object {
        private val COMMENT_COMPARATOR = object : DiffUtil.ItemCallback<Comment>() {

            override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
                return (oldItem as? CommentEntity)?.id == (newItem as? CommentEntity)?.id
            }

            override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
                val oldComment = oldItem as? CommentEntity
                val newComment = (newItem as? CommentEntity)
                return oldComment?.score == newComment?.score &&
                    oldComment?.edited == newComment?.edited &&
                    oldComment?.stickied == newComment?.stickied
            }
        }
    }
}
