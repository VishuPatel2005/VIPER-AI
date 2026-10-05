package com.rudra.myassistant.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rudra.myassistant.R
import com.rudra.myassistant.model.BalanceDirection
import com.rudra.myassistant.model.FriendBalance

class ExpenseListAdapter(private val onBalanceClick: (FriendBalance) -> Unit) : RecyclerView.Adapter<ExpenseListAdapter.BalanceViewHolder>() {
    private var balances = emptyList<FriendBalance>()

    fun setBalances(newBalances: List<FriendBalance>) {
        this.balances = newBalances
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BalanceViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_balance_summary, parent, false)
        return BalanceViewHolder(v)
    }

    override fun onBindViewHolder(holder: BalanceViewHolder, position: Int) {
        val summary = balances[position]
        holder.name.text = summary.friend.displayName
        
        when (summary.direction) {
            BalanceDirection.SETTLED -> {
                holder.status.text = "settled up"
                holder.status.setTextColor(0xFF8E8E93.toInt())
                holder.amount.text = "₹0"
                holder.amount.setTextColor(0xFFFFFFFF.toInt())
            }
            BalanceDirection.YOU_OWE -> {
                holder.status.text = "you owe"
                holder.status.setTextColor(0xFFFF453A.toInt())
                holder.amount.text = "₹${summary.amount.toInt()}"
                holder.amount.setTextColor(0xFFFF453A.toInt())
            }
            BalanceDirection.THEY_OWE -> {
                holder.status.text = "owes you"
                holder.status.setTextColor(0xFF32D74B.toInt())
                holder.amount.text = "₹${summary.amount.toInt()}"
                holder.amount.setTextColor(0xFF32D74B.toInt())
            }
        }

        Glide.with(holder.itemView.context)
            .load(summary.friend.photoUrl)
            .placeholder(R.drawable.ic_person)
            .circleCrop()
            .into(holder.photo)

        holder.itemView.setOnClickListener { onBalanceClick(summary) }
    }

    override fun getItemCount() = balances.size

    class BalanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tvFriendBalanceName)
        val status: TextView = itemView.findViewById(R.id.tvFriendBalanceStatus)
        val amount: TextView = itemView.findViewById(R.id.tvFriendBalanceAmount)
        val photo: ImageView = itemView.findViewById(R.id.ivFriendBalancePhoto)
    }
}
