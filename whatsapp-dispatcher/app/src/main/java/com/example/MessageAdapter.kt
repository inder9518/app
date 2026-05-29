package com.example

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class MessageAdapter : ListAdapter<ContactMessage, MessageAdapter.MessageViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val item = getItem(position)
        val nextPendingItem = currentList.firstOrNull { it.status.equals("Pending", ignoreCase = true) }
        val isNextPending = nextPendingItem != null && nextPendingItem.id == item.id
        holder.bind(item, isNextPending)
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.card_view)
        private val avatarContainer: FrameLayout = itemView.findViewById(R.id.avatar_container)
        private val textSNo: TextView = itemView.findViewById(R.id.text_s_no)
        private val textCustomerName: TextView = itemView.findViewById(R.id.text_customer_name)
        private val textPhoneNumber: TextView = itemView.findViewById(R.id.text_phone_number)
        private val textCustomMessage: TextView = itemView.findViewById(R.id.text_custom_message)
        private val statusBadge: TextView = itemView.findViewById(R.id.status_badge)

        fun bind(item: ContactMessage, isNextPending: Boolean) {
            val context = itemView.context
            
            // Extract Initials from name if available, else sNo
            val initials = if (item.customerName.trim().isNotEmpty()) {
                val parts = item.customerName.trim().split(Regex("\\s+"))
                if (parts.size >= 2) {
                    "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
                } else {
                    item.customerName.trim().take(2).uppercase()
                }
            } else {
                item.sNo.ifEmpty { (bindingAdapterPosition + 1).toString() }
            }

            textSNo.text = initials
            textCustomerName.text = item.customerName.ifEmpty { "No Name" }
            textPhoneNumber.text = item.phoneNumber
            textCustomMessage.text = item.customMessage

            val density = context.resources.displayMetrics.density

            // Style conditionally based on whether this is the next item active for dispatching
            if (isNextPending) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.accent_light))
                cardView.strokeColor = ContextCompat.getColor(context, R.color.primary)
                cardView.strokeWidth = (2 * density).toInt()
                cardView.cardElevation = 4 * density

                avatarContainer.setBackgroundResource(R.drawable.circle_avatar_active_bg)
                textSNo.setTextColor(ContextCompat.getColor(context, R.color.white))
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_bg))
                cardView.strokeColor = ContextCompat.getColor(context, R.color.border_color)
                cardView.strokeWidth = (1 * density).toInt()
                cardView.cardElevation = 0f

                avatarContainer.setBackgroundResource(R.drawable.circle_avatar_bg)
                textSNo.setTextColor(ContextCompat.getColor(context, R.color.primary))
            }

            // Status Badge Coloring
            if (item.status.equals("Sent", ignoreCase = true)) {
                statusBadge.text = context.getString(R.string.status_sent)
                statusBadge.setBackgroundResource(R.drawable.badge_sent_bg)
                statusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_sent_text))
            } else {
                statusBadge.text = context.getString(R.string.status_pending)
                if (isNextPending) {
                    statusBadge.setBackgroundResource(R.drawable.badge_pending_bg)
                    statusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_pending_text))
                } else {
                    // For typical non-active pending items in list
                    statusBadge.setBackgroundResource(R.drawable.msg_text_bg)
                    statusBadge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                }
            }
        }
    }

    private class ContactDiffCallback : DiffUtil.ItemCallback<ContactMessage>() {
        override fun areItemsTheSame(oldItem: ContactMessage, newItem: ContactMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ContactMessage, newItem: ContactMessage): Boolean {
            return oldItem == newItem
        }
    }
}
