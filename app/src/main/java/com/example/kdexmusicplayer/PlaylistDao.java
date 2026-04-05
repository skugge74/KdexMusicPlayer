package com.example.kdexmusicplayer;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.List;

@Dao
public interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Playlist playlist);

    @Delete
    void delete(Playlist playlist);

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_tracks WHERE playlistId = :playlistId AND trackPath = :trackPath)")
    boolean isTrackInPlaylist(long playlistId, String trackPath);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addTrackToPlaylist(PlaylistTrack playlistTrack);

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackPath = :trackPath")
    void removeTrackFromPlaylist(long playlistId, String trackPath);

    @Query("SELECT * FROM playlists")
    List<Playlist> getAllPlaylists();

    @Query("UPDATE playlists SET name = :newName WHERE id = :id")
    void updateName(long id, String newName);

    @Transaction
    @Query("SELECT tracks.* FROM tracks " +
           "INNER JOIN playlist_tracks ON tracks.filePath = playlist_tracks.trackPath " +
           "WHERE playlist_tracks.playlistId = :playlistId")
    List<MusicTrack> getTracksForPlaylist(long playlistId);
}