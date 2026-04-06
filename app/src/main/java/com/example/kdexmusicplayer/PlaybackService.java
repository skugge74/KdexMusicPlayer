package com.example.kdexmusicplayer;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.CommandButton;
import androidx.media3.session.DefaultMediaNotificationProvider;
import androidx.media3.session.MediaNotification;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import com.google.common.collect.ImmutableList;

@UnstableApi
public class PlaybackService extends MediaSessionService {
    private MediaSession mediaSession = null;

    @Override
    public void onCreate() {
        super.onCreate();
        ExoPlayer player = new ExoPlayer.Builder(this).build();
        mediaSession = new MediaSession.Builder(this, player).build();

        setMediaNotificationProvider(new MediaNotification.Provider() {
            private final DefaultMediaNotificationProvider defaultProvider = 
                new DefaultMediaNotificationProvider.Builder(PlaybackService.this).build();

            @Override
            public MediaNotification createNotification(
                    MediaSession session,
                    ImmutableList<CommandButton> customLayout,
                    MediaNotification.ActionFactory actionFactory,
                    Callback callback) {
                
                // We wrap the callback to ensure our custom colors are applied
                // even after the provider updates the notification (e.g., when album art loads).
                Callback interceptedCallback = new Callback() {
                    @Override
                    public void onNotificationChanged(MediaNotification notification) {
                        applyCustomDesign(notification);
                        callback.onNotificationChanged(notification);
                    }
                };

                MediaNotification mediaNotification = defaultProvider.createNotification(
                    session, customLayout, actionFactory, interceptedCallback);
                
                applyCustomDesign(mediaNotification);
                return mediaNotification;
            }

            private void applyCustomDesign(MediaNotification mediaNotification) {
                // Set the accent color to our Vibrant Red
                int redColor = getColor(R.color.vibrant_red);
                mediaNotification.notification.color = redColor;
                
                // On Android 12+, the system often ignores the background color 
                // and picks one based on the album art or system "Dynamic Color".
                // Setting colorized=true tells the system to use our accent color instead.
                mediaNotification.notification.extras.putBoolean("android.colorized", true);
                
                // Extra metadata hints for some OS versions
                mediaNotification.notification.extras.putInt("android.progressIndeterminate", 0);
                mediaNotification.notification.extras.putBoolean("android.showWhen", false);
                
                // Force the background color for the custom view if the system is being stubborn
                mediaNotification.notification.extras.putInt("android.backgroundColor", redColor);
            }

            @Override
            public boolean handleCustomCommand(MediaSession session, String action, Bundle extras) {
                return defaultProvider.handleCustomCommand(session, action, extras);
            }
        });
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.getPlayer().release();
            mediaSession.release();
            mediaSession = null;
        }
        super.onDestroy();
    }
}