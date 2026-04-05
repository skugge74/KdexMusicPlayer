package com.example.kdexmusicplayer;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface TrackDao {
    @Query("SELECT * FROM tracks")
    List<MusicTrack> getAll();

    @Query("SELECT * FROM tracks WHERE filePath = :path LIMIT 1")
    MusicTrack getByPath(String path);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(MusicTrack track);

    @Update
    void update(MusicTrack track);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<MusicTrack> tracks);
}