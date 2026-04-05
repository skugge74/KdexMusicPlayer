package com.example.kdexmusicplayer;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import java.util.HashSet;
import java.util.Set;

@Entity(tableName = "tracks")
@TypeConverters({Converters.class})
public class MusicTrack {
    @PrimaryKey
    @NonNull
    private String filePath;
    private String title;
    private String artist;
    private long albumId;
    private long dateAdded;
    private Set<String> tags;

    public MusicTrack(String title, String artist, @NonNull String filePath, long albumId, long dateAdded) {
        this.title = title;
        this.artist = artist;
        this.filePath = filePath;
        this.albumId = albumId;
        this.dateAdded = dateAdded;
        this.tags = new HashSet<>();
    }

    public void addTag(String tag) {
        tags.add(tag.toLowerCase().trim());
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void removeTag(String tag) {
        tags.remove(tag.toLowerCase().trim());
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag.toLowerCase().trim());
    }

    // Getters
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    @NonNull
    public String getFilePath() { return filePath; }
    public long getAlbumId() { return albumId; }
    public long getDateAdded() { return dateAdded; }
    public Set<String> getTags() { return tags; }

    // Setters for Room
    public void setFilePath(@NonNull String filePath) { this.filePath = filePath; }
    public void setAlbumId(long albumId) { this.albumId = albumId; }
    public void setDateAdded(long dateAdded) { this.dateAdded = dateAdded; }
    public void setTags(Set<String> tags) { this.tags = tags; }
}