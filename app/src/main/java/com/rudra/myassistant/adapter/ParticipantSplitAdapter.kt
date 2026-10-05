package com.rudra.myassistant.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rudra.myassistant.R
import com.rudra.myassistant.model.FriendUser

class ParticipantSplitAdapter(
    private val isSelected: (String) -> Boolean,
    private val onFriendClicked: (FriendUser) -> Unit
) : ListAdapter<FriendUser, ParticipantSplitAdapter.ParticipantViewHolder>(FriendDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return ParticipantViewHolder(v)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        val friend = getItem(position)
        holder.bind(friend, isSelected(friend.uid))
        holder.itemView.setOnClickListener { onFriendClicked(friend) }
    }

    class ParticipantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.friendName)
        private val username: TextView = itemView.findViewById(R.id.friendUsername)
        private val photo: ImageView = itemView.findViewById(R.id.friendAvatar)
        private val onlineIndicator: View? = itemView.findViewById(R.id.onlineStatusIndicator)
        private val actionIcon: ImageView? = itemView.findViewById(R.id.friendActionIcon)

        fun bind(friend: FriendUser, selected: Boolean) {
            name.text = friend.displayName
            username.text = friend.username
            
            Glide.with(itemView.context)
                .load(friend.photoUrl)
                .placeholder(R.drawable.ic_person)
                .circleCrop()
                .into(photo)

            onlineIndicator?.visibility = if (friend.isOnline) View.VISIBLE else View.GONE
            
            // Selection visual state
            itemView.setBackgroundResource(if (selected) R.drawable.bg_friend_selected else 0)
            actionIcon?.setImageResource(if (selected) R.drawable.ic_check_circle else R.drawable.ic_chevron_right)
            actionIcon?.alpha = if (selected) 1.0f else 0.4f
        }
    }

    class FriendDiffCallback : DiffUtil.ItemCallback<FriendUser>() {
        override fun areItemsTheSame(oldItem: FriendUser, newItem: FriendUser): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: FriendUser, newItem: FriendUser): Boolean {
            return oldItem == newItem
        }
    }
}
