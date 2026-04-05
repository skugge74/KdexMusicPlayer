package com.example.kdexmusicplayer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MusicLibrary {
    private List<MusicTrack> allTracks;

    public MusicLibrary() {
        this.allTracks = new ArrayList<>();
    }

    public List<String> getAllUniqueTags() {
        return allTracks.stream()
                .flatMap(track -> track.getTags().stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public void addTrack(MusicTrack track) {
        allTracks.add(track);
    }

    // This is how you'll filter music by your custom tags
    public List<MusicTrack> getTracksByTag(String tag) {
        return allTracks.stream()
                .filter(track -> track.hasTag(tag))
                .collect(Collectors.toList());
    }

    public List<MusicTrack> getAllTracks() {
        return allTracks;
    }
}