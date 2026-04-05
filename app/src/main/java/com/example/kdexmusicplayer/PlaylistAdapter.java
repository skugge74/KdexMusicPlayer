package com.example.kdexmusicplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private List<Playlist> playlists;
    private final OnPlaylistClickListener listener;
    private final OnPlaylistLongClickListener longClickListener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    public interface OnPlaylistLongClickListener {
        void onPlaylistLongClick(Playlist playlist);
    }

    public PlaylistAdapter(List<Playlist> playlists, OnPlaylistClickListener listener, OnPlaylistLongClickListener longClickListener) {
        this.playlists = playlists;
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.name.setText(playlist.getName());
        holder.itemView.setOnClickListener(v -> listener.onPlaylistClick(playlist));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onPlaylistLongClick(playlist);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    public void setPlaylists(List<Playlist> playlists) {
        this.playlists = playlists;
        notifyDataSetChanged();
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        TextView name;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(android.R.id.text1);
        }
    }
}