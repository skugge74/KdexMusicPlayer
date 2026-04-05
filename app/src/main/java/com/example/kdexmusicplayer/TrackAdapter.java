package com.example.kdexmusicplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.net.Uri;
import android.content.ContentUris;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.MaterialColors;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    private List<MusicTrack> tracks;
    private List<MusicTrack> fullTracksList;
    private final OnTrackClickListener listener;
    private final OnTrackLongClickListener longClickListener;
    private String selectedTrackPath = "";

    public interface OnTrackClickListener {
        void onTrackClick(MusicTrack track, int position);
    }

    public interface OnTrackLongClickListener {
        void onTrackLongClick(MusicTrack track, int position);
    }

    public TrackAdapter(List<MusicTrack> tracks, OnTrackClickListener listener, OnTrackLongClickListener longClickListener) {
        this.tracks = tracks;
        this.fullTracksList = new ArrayList<>(tracks);
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    public void filter(String text) {
        List<MusicTrack> filteredList = new ArrayList<>();
        String query = text.toLowerCase().trim();
        for (MusicTrack track : fullTracksList) {
            boolean matches = track.getTitle().toLowerCase().contains(query) ||
                             track.getArtist().toLowerCase().contains(query);
            
            if (!matches) {
                for (String tag : track.getTags()) {
                    if (tag.contains(query)) {
                        matches = true;
                        break;
                    }
                }
            }
            
            if (matches) {
                filteredList.add(track);
            }
        }
        this.tracks = filteredList;
        notifyDataSetChanged();
    }

    public void setSelectedTrackPath(String path) {
        this.selectedTrackPath = path;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        MusicTrack track = tracks.get(position);
        holder.title.setText(track.getTitle());
        holder.artist.setText(track.getArtist());
        
        boolean isSelected = track.getFilePath().equals(selectedTrackPath);
        com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) holder.itemView;
        if (isSelected) {
            int colorPrimary = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorPrimary);
            int colorSurfaceVariant = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorSurfaceVariant);
            card.setStrokeWidth(4);
            card.setStrokeColor(colorPrimary);
            card.setCardBackgroundColor(colorSurfaceVariant);
        } else {
            int colorSurface = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorSurface);
            int colorOutline = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOutline);
            card.setStrokeWidth(2);
            card.setStrokeColor(colorOutline);
            card.setCardBackgroundColor(colorSurface);
        }

        Uri artworkUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), track.getAlbumId());
        Glide.with(holder.itemView.getContext())
                .load(artworkUri)
                .placeholder(R.drawable.ic_launcher_foreground)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.albumArt);

        holder.tagGroup.removeAllViews();
        for (String tag : track.getTags()) {
            Chip chip = new Chip(holder.itemView.getContext());
            chip.setText(tag);
            chip.setCheckable(false);
            chip.setClickable(false);
            holder.tagGroup.addView(chip);
        }

        holder.itemView.setOnClickListener(v -> listener.onTrackClick(track, position));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onTrackLongClick(track, position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    public List<MusicTrack> getTracks() {
        return tracks;
    }

    public void setTracks(List<MusicTrack> tracks) {
        this.tracks = tracks;
        this.fullTracksList = new ArrayList<>(tracks);
        notifyDataSetChanged();
    }

    public void sortAlphabetical() {
        Collections.sort(fullTracksList, (t1, t2) -> t1.getTitle().compareToIgnoreCase(t2.getTitle()));
        Collections.sort(tracks, (t1, t2) -> t1.getTitle().compareToIgnoreCase(t2.getTitle()));
        notifyDataSetChanged();
    }

    public void sortByDateAdded() {
        Collections.sort(fullTracksList, (t1, t2) -> Long.compare(t2.getDateAdded(), t1.getDateAdded())); // Descending
        Collections.sort(tracks, (t1, t2) -> Long.compare(t2.getDateAdded(), t1.getDateAdded()));
        notifyDataSetChanged();
    }

    static class TrackViewHolder extends RecyclerView.ViewHolder {
        TextView title, artist;
        ImageView albumArt;
        ChipGroup tagGroup;

        public TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.trackTitle);
            artist = itemView.findViewById(R.id.trackArtist);
            albumArt = itemView.findViewById(R.id.itemAlbumArt);
            tagGroup = itemView.findViewById(R.id.tagGroup);
        }
    }
}