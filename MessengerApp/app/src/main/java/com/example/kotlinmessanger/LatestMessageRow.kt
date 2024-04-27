package com.example.kotlinmessanger

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.latest_message_row.view.*

class LatestMessageRow(val chatMessage: ChatMessage): Item<ViewHolder>() {
    var chatPartnerUser: User? = null

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.message_textview.text = chatMessage.text

        val chatPartner: String
        if (chatMessage.fromId == FirebaseAuth.getInstance().uid)
            chatPartner = chatMessage.toId
        else
            chatPartner = chatMessage.fromId

        val ref = FirebaseDatabase.getInstance().getReference("/users/$chatPartner")
        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                chatPartnerUser = p0.getValue(User::class.java)
                viewHolder.itemView.username_textview.text = chatPartnerUser?.username

                val targetImageView = viewHolder.itemView.latest_image_view
                Picasso.get().load(chatPartnerUser?.profileImageUrl).into(targetImageView)
            }

        })
        viewHolder.itemView.username_textview.text = "..."
    }

    override fun getLayout(): Int = R.layout.latest_message_row
}
