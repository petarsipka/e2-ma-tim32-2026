package com.example.slagalica.ui.leaderboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.slagalica.R;
import com.example.slagalica.data.model.LeaderboardEntry;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.EntryHolder> {

    private List<LeaderboardEntry> entries = new ArrayList<>();
    private String myUid;

    public LeaderboardAdapter(String myUid) {
        this.myUid = myUid;
    }

    public void setEntries(List<LeaderboardEntry> entries) {
        this.entries = entries;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public EntryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new EntryHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard_entry, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull EntryHolder h, int position) {
        LeaderboardEntry e = entries.get(position);
        h.rank.setText(String.valueOf(position + 1));
        h.name.setText(e.username != null ? e.username : "???");
        h.stars.setText("⭐ " + e.stars);
        h.region.setText(e.region != null ? e.region : "");
        h.leagueIcon.setText(leagueEmoji(e.leagueOrdinal));

        // Highlight current user
        if (myUid != null && myUid.equals(e.uid)) {
            h.itemView.setBackgroundColor(h.itemView.getContext().getColor(R.color.accent_soft));
        } else {
            h.itemView.setBackgroundColor(h.itemView.getContext().getColor(R.color.panel));
        }

        // Medal colors for top 3
        int rank = position + 1;
        if (rank == 1) h.rank.setTextColor(h.itemView.getContext().getColor(R.color.token));
        else if (rank == 2) h.rank.setTextColor(h.itemView.getContext().getColor(R.color.muted));
        else if (rank == 3) h.rank.setTextColor(h.itemView.getContext().getColor(R.color.league_text));
        else h.rank.setTextColor(h.itemView.getContext().getColor(R.color.text));
    }

    @Override public int getItemCount() { return entries.size(); }

    static class EntryHolder extends RecyclerView.ViewHolder {
        final TextView rank, name, stars, region;
        final TextView leagueIcon;
        EntryHolder(View itemView) {
            super(itemView);
            rank = itemView.findViewById(R.id.lbRank);
            name = itemView.findViewById(R.id.lbName);
            stars = itemView.findViewById(R.id.lbStars);
            region = itemView.findViewById(R.id.lbRegion);
            leagueIcon = itemView.findViewById(R.id.lbLeagueIcon);
        }
    }
    private String leagueEmoji(int ordinal) {
        switch (ordinal) {
            case 0: return "🥉";
            case 1: return "🥈";
            case 2: return "🥇";
            case 3: return "💎";
            case 4: return "💠";
            case 5: return "👑";
            default: return "🥉";
        }
    }
}