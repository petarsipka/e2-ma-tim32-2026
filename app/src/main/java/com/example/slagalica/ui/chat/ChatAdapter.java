package com.example.slagalica.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.slagalica.R;
import com.example.slagalica.data.model.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ME = 1;
    private static final int TYPE_OTHER = 2;

    private final List<ChatMessage> messages = new ArrayList<>();
    private final String myUid;

    public ChatAdapter(String myUid) {
        this.myUid = myUid;
    }

    public void setMessages(List<ChatMessage> msgs) {
        messages.clear();
        messages.addAll(msgs);
        notifyDataSetChanged();
    }

    @Override public int getItemViewType(int position) {
        return myUid.equals(messages.get(position).senderUid) ? TYPE_ME : TYPE_OTHER;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ME) {
            return new MsgHolder(inflater.inflate(R.layout.item_chat_message_me, parent, false));
        } else {
            return new MsgHolder(inflater.inflate(R.layout.item_chat_message_other, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((MsgHolder) holder).bind(messages.get(position));
    }

    @Override public int getItemCount() { return messages.size(); }

    static class MsgHolder extends RecyclerView.ViewHolder {
        final TextView tvName, tvText, tvTime;

        MsgHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.chatMsgName);
            tvText = itemView.findViewById(R.id.chatMsgText);
            tvTime = itemView.findViewById(R.id.chatMsgTime);
        }

        void bind(ChatMessage msg) {
            tvName.setText(msg.senderName);
            tvText.setText(msg.text);
            tvTime.setText(formatTime(msg.timestamp));
        }

        private String formatTime(long ts) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(ts));
        }
    }
}