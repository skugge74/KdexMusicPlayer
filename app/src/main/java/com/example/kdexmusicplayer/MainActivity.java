package com.example.kdexmusicplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.appcompat.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.MultiAutoCompleteTextView;
import android.widget.EditText;
import android.text.TextUtils;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.widget.SeekBar;
import android.os.Handler;
import android.os.Looper;
import android.widget.RadioGroup;
import android.widget.ImageView;
import android.widget.Button;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.activity.OnBackPressedCallback;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import android.content.Intent;
import android.content.SharedPreferences;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.recyclerview.widget.ItemTouchHelper;
import java.util.Collections;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.media3.common.util.UnstableApi;

@UnstableApi
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private String lastLoadedMediaId = null;

    private MusicLibrary musicLibrary;
    private RecyclerView recyclerView;
    private TrackAdapter adapter;
    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;

    private TextView currentTrackTitle;
    private TextView currentTrackArtist;
    private TextView currentTime;
    private TextView totalTime;
    private SeekBar playbackSeekBar;
    private ImageButton playPauseButton;
    private ImageButton settingsButton;
    private View settingsPane;
    private ImageButton closeSettingsButton;
    private RadioGroup playbackModeGroup;
    private ImageView albumArt;
    private DrawerLayout drawerLayout;
    private PlaylistAdapter playlistAdapter;
    private RecyclerView playlistRecyclerView;
    private AppDatabase db;
    private ItemTouchHelper itemTouchHelper;
    private long currentPlaylistId = -1;
    private View searchBarContainer;
    private ImageButton searchButton;
    private ImageButton closeSearchButton;
    private SearchView searchView;

    private ChipGroup drawerTagGroup;
    private TextView currentLibraryPathText;
    private String selectedLibraryPath;
    private ActivityResultLauncher<Uri> folderPickerLauncher;

    private ImageButton shuffleButton;
    private ImageButton repeatButton;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateSeekBarTask = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        musicLibrary = new MusicLibrary();
        db = AppDatabase.getDatabase(this);
        
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        currentTrackTitle = findViewById(R.id.currentTrackTitle);
        currentTrackArtist = findViewById(R.id.currentTrackArtist);
        currentTime = findViewById(R.id.currentTime);
        totalTime = findViewById(R.id.totalTime);
        playbackSeekBar = findViewById(R.id.playbackSeekBar);
        playPauseButton = findViewById(R.id.playPauseButton);
        settingsButton = findViewById(R.id.settingsButton);
        settingsPane = findViewById(R.id.settingsPane);
        closeSettingsButton = findViewById(R.id.closeSettingsButton);
        playbackModeGroup = findViewById(R.id.playbackModeGroup);
        albumArt = findViewById(R.id.albumArt);
        drawerLayout = findViewById(R.id.drawerLayout);
        
        ImageButton drawerButton = findViewById(R.id.drawerButton);
        ImageButton sortButton = findViewById(R.id.sortButton);
        ImageButton scrollToTopButton = findViewById(R.id.scrollToTopButton);
        ImageButton scrollToBottomButton = findViewById(R.id.scrollToBottomButton);
        searchButton = findViewById(R.id.searchButton);
        searchBarContainer = findViewById(R.id.searchBarContainer);
        closeSearchButton = findViewById(R.id.closeSearchButton);
        searchView = findViewById(R.id.searchView);

        drawerButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        sortButton.setOnClickListener(v -> showSortPopup(v));
        
        searchButton.setOnClickListener(v -> {
            searchBarContainer.setVisibility(View.VISIBLE);
            searchView.setIconified(false);
            searchView.requestFocus();
        });
        
        closeSearchButton.setOnClickListener(v -> {
            searchView.setQuery("", false);
            searchBarContainer.setVisibility(View.GONE);
        });
        
        scrollToTopButton.setOnClickListener(v -> {
            if (adapter != null && adapter.getItemCount() > 0) {
                recyclerView.scrollToPosition(0);
            }
        });
        
        scrollToBottomButton.setOnClickListener(v -> {
            if (adapter != null && adapter.getItemCount() > 0) {
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            }
        });

        
        drawerTagGroup = findViewById(R.id.drawerTagGroup);
        currentLibraryPathText = findViewById(R.id.currentLibraryPathText);
        Button setLibraryPathButton = findViewById(R.id.setLibraryPathButton);
        Button clearLibraryPathButton = findViewById(R.id.clearLibraryPathButton);

        SharedPreferences prefs = getSharedPreferences("MusicPlayerPrefs", MODE_PRIVATE);
        selectedLibraryPath = prefs.getString("library_path", null);
        String lastPlayedPath = prefs.getString("last_played_path", null);
        updateLibraryPathUI();

        folderPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocumentTree(),
                uri -> {
                    if (uri != null) {
                        getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        selectedLibraryPath = uri.toString();
                        prefs.edit().putString("library_path", selectedLibraryPath).apply();
                        updateLibraryPathUI();
                        loadMusic(); // Reload with new filter
                    }
                }
        );

        setLibraryPathButton.setOnClickListener(v -> folderPickerLauncher.launch(null));
        clearLibraryPathButton.setOnClickListener(v -> {
            selectedLibraryPath = null;
            prefs.edit().remove("library_path").apply();
            updateLibraryPathUI();
            loadMusic();
        });
        
        ImageButton nextButton = findViewById(R.id.nextButton);
        ImageButton prevButton = findViewById(R.id.prevButton);
        playlistRecyclerView = findViewById(R.id.playlistRecyclerView);
        ImageButton addPlaylistButton = findViewById(R.id.addPlaylistButton);
        Button allTracksButton = findViewById(R.id.allTracksButton);
        
        setupPlaylistDrawer(addPlaylistButton, allTracksButton);
        setupDragAndDrop();

        settingsButton.setOnClickListener(v -> {
            updateSettingsPaneState();
            settingsPane.setVisibility(View.VISIBLE);
        });
        closeSettingsButton.setOnClickListener(v -> settingsPane.setVisibility(View.GONE));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (searchBarContainer.getVisibility() == View.VISIBLE) {
                    searchBarContainer.setVisibility(View.GONE);
                } else if (settingsPane.getVisibility() == View.VISIBLE) {
                    settingsPane.setVisibility(View.GONE);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        playbackModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (mediaController == null) return;
            
            if (checkedId == R.id.modeStop) {
                mediaController.setRepeatMode(Player.REPEAT_MODE_OFF);
            } else if (checkedId == R.id.modeLoopAll) {
                mediaController.setRepeatMode(Player.REPEAT_MODE_ALL);
            } else if (checkedId == R.id.modeLoopCurrent) {
                mediaController.setRepeatMode(Player.REPEAT_MODE_ONE);
            }
        });

        playbackSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaController != null) {
                    mediaController.seekTo(progress);
                    currentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateSeekBarTask);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                handler.post(updateSeekBarTask);
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    adapter.filter(newText);
                }
                return true;
            }
        });

        playPauseButton.setOnClickListener(v -> {
            if (mediaController != null) {
                if (mediaController.isPlaying()) {
                    mediaController.pause();
                } else {
                    if (mediaController.getPlaybackState() == Player.STATE_ENDED) {
                        mediaController.seekTo(0);
                    }
                    mediaController.play();
                }
            }
        });

        nextButton.setOnClickListener(v -> {
            if (mediaController != null) {
                mediaController.seekToNext();
            }
        });

        prevButton.setOnClickListener(v -> {
            if (mediaController != null) {
                mediaController.seekToPrevious();
            }
        });

        shuffleButton = findViewById(R.id.shuffleButton);
        repeatButton = findViewById(R.id.repeatButton);

        shuffleButton.setOnClickListener(v -> {
            if (mediaController != null) {
                boolean shuffleModeEnabled = !mediaController.getShuffleModeEnabled();
                mediaController.setShuffleModeEnabled(shuffleModeEnabled);
                updateUI();
            }
        });

        repeatButton.setOnClickListener(v -> {
            if (mediaController != null) {
                int mode = mediaController.getRepeatMode();
                int nextMode;
                if (mode == Player.REPEAT_MODE_OFF) nextMode = Player.REPEAT_MODE_ALL;
                else if (mode == Player.REPEAT_MODE_ALL) nextMode = Player.REPEAT_MODE_ONE;
                else nextMode = Player.REPEAT_MODE_OFF;
                mediaController.setRepeatMode(nextMode);
                updateUI();
            }
        });

        checkPermissions();
    }

    private void showSortPopup(View v) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, v);
        popup.getMenu().add("Sort by A-Z");
        popup.getMenu().add("Sort by Last Added");
        
        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Sort by A-Z")) {
                adapter.sortAlphabetical();
            } else {
                adapter.sortByDateAdded();
            }
            return true;
        });
        popup.show();
    }

    private void updateLibraryPathUI() {
        if (selectedLibraryPath == null) {
            currentLibraryPathText.setText("All device storage");
        } else {
            Uri uri = Uri.parse(selectedLibraryPath);
            String path = uri.getPath();
            if (path != null && path.contains(":")) {
                path = path.split(":")[1];
            }
            currentLibraryPathText.setText(path != null ? path : uri.getLastPathSegment());
        }
    }

    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
        permissions.add(Manifest.permission.POST_NOTIFICATIONS);

        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            loadMusic();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                loadMusic();
            } else {
                Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadMusic() {
        backgroundExecutor.execute(() -> {
            List<MusicTrack> loadedTracks = new ArrayList<>();
            ContentResolver contentResolver = getContentResolver();
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
            
            String folderPath = null;
            if (selectedLibraryPath != null) {
                Uri treeUri = Uri.parse(selectedLibraryPath);
                String path = treeUri.getPath();
                if (path != null && path.contains(":")) {
                    folderPath = path.split(":")[1];
                }
            }

            // Optimization: Get all existing tracks from DB once
            List<MusicTrack> existingTracks = db.trackDao().getAll();
            java.util.Map<String, MusicTrack> trackMap = new java.util.HashMap<>();
            for (MusicTrack t : existingTracks) {
                trackMap.put(t.getFilePath(), t);
            }

            List<MusicTrack> newTracksToInsert = new ArrayList<>();
            List<MusicTrack> tracksToUpdate = new ArrayList<>();

            try (Cursor cursor = contentResolver.query(uri, null, selection, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
                    int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                    int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                    int albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
                    int dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                    int dateAddedColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED);

                    do {
                        String dataPath = cursor.getString(dataColumn);
                        if (folderPath != null && (dataPath == null || !dataPath.contains(folderPath))) {
                            continue;
                        }

                        long id = cursor.getLong(idColumn);
                        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                        String trackUriString = contentUri.toString();
                        
                        MusicTrack track = trackMap.get(trackUriString);
                        if (track == null) {
                            String title = cursor.getString(titleColumn);
                            String artist = cursor.getString(artistColumn);
                            long albumId = cursor.getLong(albumIdColumn);
                            long dateAdded = cursor.getLong(dateAddedColumn) * 1000;
                            
                            track = new MusicTrack(title, artist, trackUriString, albumId, dateAdded);
                            newTracksToInsert.add(track);
                        } else if (track.getAlbumId() == 0) {
                            track.setAlbumId(cursor.getLong(albumIdColumn));
                            tracksToUpdate.add(track);
                        }
                        loadedTracks.add(track);
                    } while (cursor.moveToNext());
                }
            }

            // Batch database operations
            if (!newTracksToInsert.isEmpty()) {
                db.trackDao().insertAll(newTracksToInsert);
            }
            for (MusicTrack t : tracksToUpdate) {
                db.trackDao().update(t);
            }

            runOnUiThread(() -> {
                musicLibrary.getAllTracks().clear();
                musicLibrary.getAllTracks().addAll(loadedTracks);
                
                if (adapter == null) {
                    adapter = new TrackAdapter(musicLibrary.getAllTracks(), 
                        (track, position) -> playTrack(position),
                        (track, position) -> { /* Long click ignored */ },
                        (track, position) -> showEditTrackDialog(track, position));
                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.setTracks(musicLibrary.getAllTracks());
                }
                
                adapter.sortByDateAdded();

                String lastPlayedPath = getSharedPreferences("MusicPlayerPrefs", MODE_PRIVATE).getString("last_played_path", null);
                if (lastPlayedPath != null) {
                    adapter.setSelectedTrackPath(lastPlayedPath);
                    for (int i = 0; i < musicLibrary.getAllTracks().size(); i++) {
                        if (musicLibrary.getAllTracks().get(i).getFilePath().equals(lastPlayedPath)) {
                            recyclerView.scrollToPosition(i);
                            break;
                        }
                    }
                }
            });
        });
    }

    private void setupPlaylistDrawer(ImageButton addPlaylistButton, Button allTracksButton) {
        playlistRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        playlistAdapter = new PlaylistAdapter(new ArrayList<>(), this::loadPlaylist, this::showPlaylistOptionsDialog);
        playlistRecyclerView.setAdapter(playlistAdapter);

        addPlaylistButton.setOnClickListener(v -> showCreatePlaylistDialog());
        allTracksButton.setOnClickListener(v -> {
            currentPlaylistId = -1;
            loadMusic(); // Reload all tracks with default listeners
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        refreshPlaylists();
    }

    private void showPlaylistOptionsDialog(Playlist playlist) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(playlist.getName());
        String[] options = {"Rename", "Delete"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                showRenamePlaylistDialog(playlist);
            } else {
                showDeletePlaylistConfirmDialog(playlist);
            }
        });
        builder.show();
    }

    private void showRenamePlaylistDialog(Playlist playlist) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Playlist");
        final EditText input = new EditText(this);
        input.setText(playlist.getName());
        builder.setView(input);
        builder.setPositiveButton("Rename", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                db.playlistDao().updateName(playlist.getId(), newName);
                refreshPlaylists();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeletePlaylistConfirmDialog(Playlist playlist) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Playlist")
            .setMessage("Are you sure you want to delete '" + playlist.getName() + "'?")
            .setPositiveButton("Delete", (dialog, which) -> {
                db.playlistDao().delete(playlist);
                refreshPlaylists();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void refreshPlaylists() {
        List<Playlist> playlists = db.playlistDao().getAllPlaylists();
        playlistAdapter.setPlaylists(playlists);
        refreshDrawerTags();
    }

    private void refreshDrawerTags() {
        drawerTagGroup.removeAllViews();
        List<String> tags = musicLibrary.getAllUniqueTags();
        for (String tag : tags) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setCheckable(true);
            chip.setOnClickListener(v -> {
                if (((Chip)v).isChecked()) {
                    List<MusicTrack> filtered = musicLibrary.getTracksByTag(tag);
                    adapter.setTracks(filtered);
                } else {
                    adapter.setTracks(musicLibrary.getAllTracks());
                }
                drawerLayout.closeDrawer(GravityCompat.START);
            });
            drawerTagGroup.addView(chip);
        }
    }

    private void showCreatePlaylistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New Playlist");
        final EditText input = new EditText(this);
        input.setHint("Playlist Name");
        builder.setView(input);
        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                db.playlistDao().insert(new Playlist(name));
                refreshPlaylists();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void loadPlaylist(Playlist playlist) {
        currentPlaylistId = playlist.getId();
        new Thread(() -> {
            List<MusicTrack> tracks = db.playlistDao().getTracksForPlaylist(playlist.getId());
            runOnUiThread(() -> {
                if (adapter != null) {
                    adapter = new TrackAdapter(tracks,
                        (track, position) -> playTrack(position),
                        (track, position) -> { /* Long click handled by drag in playlist */ },
                        (track, position) -> showEditTrackDialog(track, position));
                    adapter.setShowDragHandle(true);
                    adapter.setOnStartDragListener(viewHolder -> itemTouchHelper.startDrag(viewHolder));
                    recyclerView.setAdapter(adapter);
                }
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(this, "Loaded: " + playlist.getName(), Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void setupDragAndDrop() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                
                if (adapter != null) {
                    List<MusicTrack> tracks = adapter.getTracks();
                    Collections.swap(tracks, fromPosition, toPosition);
                    adapter.notifyItemMoved(fromPosition, toPosition);
                    return true;
                }
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                if (currentPlaylistId != -1 && adapter != null) {
                    savePlaylistOrder();
                }
            }
        };

        itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void savePlaylistOrder() {
        if (currentPlaylistId == -1 || adapter == null) return;
        List<MusicTrack> tracks = new ArrayList<>(adapter.getTracks());
        long playlistId = currentPlaylistId;
        new Thread(() -> {
            for (int i = 0; i < tracks.size(); i++) {
                db.playlistDao().updateTrackOrder(playlistId, tracks.get(i).getFilePath(), i);
            }
        }).start();
    }

    private void showEditTrackDialog(MusicTrack track, int position) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_track_options_bottom_sheet, null);
        
        TextView title = view.findViewById(R.id.bottomSheetTrackTitle);
        TextView artist = view.findViewById(R.id.bottomSheetTrackArtist);
        ImageView albumArt = view.findViewById(R.id.bottomSheetAlbumArt);
        View btnEdit = view.findViewById(R.id.btnEditInfo);
        View btnAddPlaylist = view.findViewById(R.id.btnAddToPlaylist);
        View btnRemoveFromPlaylist = view.findViewById(R.id.btnRemoveFromPlaylist);
        View btnPlayNext = view.findViewById(R.id.btnPlayNext);
        View btnAddToQueue = view.findViewById(R.id.btnAddToQueue);

        title.setText(track.getTitle());
        artist.setText(track.getArtist());

        btnPlayNext.setOnClickListener(v -> {
            if (mediaController != null) {
                Uri artworkUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), track.getAlbumId());
                MediaMetadata metadata = new MediaMetadata.Builder()
                        .setTitle(track.getTitle())
                        .setArtist(track.getArtist())
                        .setArtworkUri(artworkUri)
                        .build();
                MediaItem mediaItem = new MediaItem.Builder()
                        .setMediaId(track.getFilePath())
                        .setUri(Uri.parse(track.getFilePath()))
                        .setMediaMetadata(metadata)
                        .build();
                
                int nextIndex = mediaController.getCurrentMediaItemIndex() + 1;
                mediaController.addMediaItem(nextIndex, mediaItem);
                Toast.makeText(this, "Playing next: " + track.getTitle(), Toast.LENGTH_SHORT).show();
            }
            bottomSheetDialog.dismiss();
        });

        btnAddToQueue.setOnClickListener(v -> {
            if (mediaController != null) {
                Uri artworkUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), track.getAlbumId());
                MediaMetadata metadata = new MediaMetadata.Builder()
                        .setTitle(track.getTitle())
                        .setArtist(track.getArtist())
                        .setArtworkUri(artworkUri)
                        .build();
                MediaItem mediaItem = new MediaItem.Builder()
                        .setMediaId(track.getFilePath())
                        .setUri(Uri.parse(track.getFilePath()))
                        .setMediaMetadata(metadata)
                        .build();
                
                mediaController.addMediaItem(mediaItem);
                Toast.makeText(this, "Added to queue: " + track.getTitle(), Toast.LENGTH_SHORT).show();
            }
            bottomSheetDialog.dismiss();
        });
        
        if (currentPlaylistId != -1) {
            btnRemoveFromPlaylist.setVisibility(View.VISIBLE);
            btnRemoveFromPlaylist.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                db.playlistDao().removeTrackFromPlaylist(currentPlaylistId, track.getFilePath());
                // Refresh the current playlist view
                Playlist p = new Playlist("");
                p.setId(currentPlaylistId);
                loadPlaylist(p);
            });
        }
        
        Uri artworkUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), track.getAlbumId());
        Glide.with(this)
                .load(artworkUri)
                .placeholder(R.drawable.ic_launcher_foreground)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(albumArt);

        btnEdit.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showActualEditDialog(track, position);
        });

        btnAddPlaylist.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showPlaylistManagementDialog(track);
        });

        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    private void showPlaylistManagementDialog(MusicTrack track) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_manage_playlists_bottom_sheet, null);
        
        TextView trackName = view.findViewById(R.id.playlistTrackName);
        RecyclerView playlistsList = view.findViewById(R.id.playlistsSelectionList);
        Button btnDone = view.findViewById(R.id.btnDonePlaylists);

        trackName.setText(track.getTitle());
        
        List<Playlist> playlists = db.playlistDao().getAllPlaylists();
        playlistsList.setLayoutManager(new LinearLayoutManager(this));
        
        playlistsList.setAdapter(new RecyclerView.Adapter<PlaylistSelectionViewHolder>() {
            @NonNull
            @Override
            public PlaylistSelectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist_selection, parent, false);
                return new PlaylistSelectionViewHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull PlaylistSelectionViewHolder holder, int position) {
                Playlist playlist = playlists.get(position);
                holder.name.setText(playlist.getName());
                boolean isChecked = db.playlistDao().isTrackInPlaylist(playlist.getId(), track.getFilePath());
                holder.checkBox.setChecked(isChecked);
                
                holder.itemView.setOnClickListener(v -> holder.checkBox.toggle());
                
                holder.checkBox.setOnCheckedChangeListener((buttonView, isNowChecked) -> {
                    new Thread(() -> {
                        if (isNowChecked) {
                            int nextOrder = db.playlistDao().getMaxOrderForPlaylist(playlist.getId()) + 1;
                            db.playlistDao().addTrackToPlaylist(new PlaylistTrack(playlist.getId(), track.getFilePath(), nextOrder));
                        } else {
                            db.playlistDao().removeTrackFromPlaylist(playlist.getId(), track.getFilePath());
                        }
                    }).start();
                });
            }

            @Override
            public int getItemCount() {
                return playlists.size();
            }
        });

        btnDone.setOnClickListener(v -> bottomSheetDialog.dismiss());
        
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    static class PlaylistSelectionViewHolder extends RecyclerView.ViewHolder {
        com.google.android.material.checkbox.MaterialCheckBox checkBox;
        TextView name;

        public PlaylistSelectionViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.playlistCheckBox);
            name = itemView.findViewById(R.id.playlistSelectionName);
        }
    }

    private void showActualEditDialog(MusicTrack track, int position) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_edit_track_bottom_sheet, null);
        
        com.google.android.material.textfield.TextInputEditText titleInput = view.findViewById(R.id.editTitleInput);
        com.google.android.material.textfield.TextInputEditText artistInput = view.findViewById(R.id.editArtistInput);
        MultiAutoCompleteTextView tagsInput = view.findViewById(R.id.editTagsInput);
        Button btnSave = view.findViewById(R.id.btnSaveTrackInfo);

        titleInput.setText(track.getTitle());
        artistInput.setText(track.getArtist());
        tagsInput.setText(TextUtils.join(", ", track.getTags()));
        
        List<String> existingTags = musicLibrary.getAllUniqueTags();
        ArrayAdapter<String> tagAutocompleteAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_dropdown_item_1line, existingTags);
        tagsInput.setAdapter(tagAutocompleteAdapter);
        tagsInput.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

        btnSave.setOnClickListener(v -> {
            String newTitle = titleInput.getText().toString().trim();
            String newArtist = artistInput.getText().toString().trim();
            String tagsString = tagsInput.getText().toString().trim();

            if (!newTitle.isEmpty() && !newArtist.isEmpty()) {
                track.setTitle(newTitle);
                track.setArtist(newArtist);
                
                track.getTags().clear();
                if (!tagsString.isEmpty()) {
                    String[] tags = tagsString.split(",");
                    for (String tag : tags) {
                        track.addTag(tag.trim());
                    }
                }
                
                db.trackDao().update(track);
                if (adapter != null) {
                    adapter.notifyItemChanged(position);
                }
                refreshDrawerTags();
                updateUI();
                bottomSheetDialog.dismiss();
            }
        });

        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    private void playTrack(int position) {
        if (mediaController == null) return;

        List<MusicTrack> currentAdapterTracks = adapter.getTracks();
        if (position < 0 || position >= currentAdapterTracks.size()) return;

        // Optimization: If the queue is already the same, just seek.
        // This is the "Compromise": Saves massive battery/CPU on large libraries.
        if (mediaController.getMediaItemCount() == currentAdapterTracks.size()) {
            MediaItem itemInController = mediaController.getMediaItemAt(position);
            if (itemInController != null && itemInController.mediaId.equals(currentAdapterTracks.get(position).getFilePath())) {
                mediaController.seekTo(position, 0);
                mediaController.play();
                return;
            }
        }

        mediaController.stop();
        mediaController.clearMediaItems();

        List<MusicTrack> playlist = new ArrayList<>(currentAdapterTracks);
        backgroundExecutor.execute(() -> {
                List<MediaItem> mediaItems = new ArrayList<>();
                for (MusicTrack t : playlist) {
                    Uri artworkUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), t.getAlbumId());
                    MediaMetadata metadata = new MediaMetadata.Builder()
                            .setTitle(t.getTitle())
                            .setArtist(t.getArtist())
                            .setArtworkUri(artworkUri)
                            .build();

                    MediaItem mediaItem = new MediaItem.Builder()
                            .setMediaId(t.getFilePath())
                            .setUri(Uri.parse(t.getFilePath()))
                            .setMediaMetadata(metadata)
                            .build();
                    mediaItems.add(mediaItem);
                }
                
                runOnUiThread(() -> {
                    mediaController.addMediaItems(mediaItems);
                    mediaController.seekTo(position, 0);
                    mediaController.prepare();
                    mediaController.play();
                    updateUI();
                });
            });
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializeController();
        handler.post(updateSeekBarTask);
    }

    private void initializeController() {
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, PlaybackService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
                mediaController.addListener(new Player.Listener() {
                    @Override
                    public void onEvents(@NonNull Player player, @NonNull Player.Events events) {
                        if (events.containsAny(
                                Player.EVENT_MEDIA_ITEM_TRANSITION,
                                Player.EVENT_PLAYBACK_STATE_CHANGED,
                                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                                Player.EVENT_IS_PLAYING_CHANGED,
                                Player.EVENT_REPEAT_MODE_CHANGED,
                                Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)) {
                            updateUI();
                        }
                    }

                    @Override
                    public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                        if (mediaItem != null && mediaItem.mediaId != null) {
                            getSharedPreferences("MusicPlayerPrefs", MODE_PRIVATE)
                                .edit().putString("last_played_path", mediaItem.mediaId).apply();
                        }
                    }
                });
                updateUI();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());
    }

    private void updateProgress() {
        if (mediaController != null && mediaController.isPlaying()) {
            int currentPosition = (int) mediaController.getCurrentPosition();
            playbackSeekBar.setProgress(currentPosition);
            currentTime.setText(formatTime(currentPosition));
        }
    }

    private void updateSettingsPaneState() {
        if (mediaController == null) return;
        
        int repeatMode = mediaController.getRepeatMode();
        if (repeatMode == Player.REPEAT_MODE_OFF) {
            playbackModeGroup.check(R.id.modeStop);
        } else if (repeatMode == Player.REPEAT_MODE_ALL) {
            playbackModeGroup.check(R.id.modeLoopAll);
        } else if (repeatMode == Player.REPEAT_MODE_ONE) {
            playbackModeGroup.check(R.id.modeLoopCurrent);
        }
    }

    private String formatTime(int milliseconds) {
        int minutes = (milliseconds / 1000) / 60;
        int seconds = (milliseconds / 1000) % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    private void updateUI() {
        if (mediaController != null) {
            playPauseButton.setImageResource(mediaController.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
            
            shuffleButton.setColorFilter(mediaController.getShuffleModeEnabled() ? 
                getColor(R.color.cat_red) : getColor(R.color.cat_text));
            
            int repeatMode = mediaController.getRepeatMode();
            if (repeatMode == Player.REPEAT_MODE_OFF) {
                repeatButton.setImageResource(R.drawable.ic_repeat);
                repeatButton.setColorFilter(getColor(R.color.cat_text));
            } else if (repeatMode == Player.REPEAT_MODE_ALL) {
                repeatButton.setImageResource(R.drawable.ic_repeat);
                repeatButton.setColorFilter(getColor(R.color.cat_red));
            } else if (repeatMode == Player.REPEAT_MODE_ONE) {
                repeatButton.setImageResource(R.drawable.ic_repeat_one);
                repeatButton.setColorFilter(getColor(R.color.cat_red));
            }

            MediaItem currentItem = mediaController.getCurrentMediaItem();
            if (currentItem != null) {
                if (adapter != null && currentItem.mediaId != null) {
                    adapter.setSelectedTrackPath(currentItem.mediaId);
                }

                // Optimization: Only update UI text and art if the track actually changed
                if (lastLoadedMediaId == null || !lastLoadedMediaId.equals(currentItem.mediaId)) {
                    lastLoadedMediaId = currentItem.mediaId;
                    
                    if (currentItem.mediaMetadata.title != null) {
                        currentTrackTitle.setText(currentItem.mediaMetadata.title);
                        currentTrackArtist.setText(currentItem.mediaMetadata.artist);

                        if (currentItem.mediaMetadata.artworkUri != null) {
                            Glide.with(this)
                                    .load(currentItem.mediaMetadata.artworkUri)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .placeholder(R.drawable.ic_launcher_foreground)
                                    .into(albumArt);
                        } else {
                            albumArt.setImageResource(R.drawable.ic_launcher_foreground);
                        }
                    }
                }
            }
            if (mediaController.getPlaybackState() == Player.STATE_READY) {
                playbackSeekBar.setMax((int) mediaController.getDuration());
                totalTime.setText(formatTime((int) mediaController.getDuration()));
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(updateSeekBarTask);
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
        }
    }
}