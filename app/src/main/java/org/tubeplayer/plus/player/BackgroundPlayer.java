/*
 * Copyright 2017 Mauricio Colli <mauriciocolli@outlook.com>
 * BackgroundPlayer.java is part of NewPipe
 *
 * License: GPL-3.0+
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tubeplayer.plus.player;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.MediaSource;

import org.tubeplayer.plus.player.helper.LockManager;
import org.tubeplayer.plus.player.helper.PlayerHelper;
import org.tubeplayer.plus.playlist.PlayQueueItem;
import org.tubeplayer.plus.BuildConfig;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.tubeplayer.plus.player.event.PlayerEventListener;
import org.tubeplayer.plus.util.ListHelper;
import org.tubeplayer.plus.util.NavigationHelper;


/**
 * Base players joining the common properties
 *
 * @author mauriciocolli
 */
public final class BackgroundPlayer extends Service {
    private static final String TAG = "BackgroundPlayer";
    private static final boolean DEBUG = BasePlayer.DEBUG;

    public static final String ACTION_CLOSE = "com.playtube.plus.player.BackgroundPlayer.CLOSE";
    public static final String ACTION_PLAY_PAUSE = "com.playtube.plus.player.BackgroundPlayer.PLAY_PAUSE";
    public static final String ACTION_REPEAT = "com.playtube.plus.player.BackgroundPlayer.REPEAT";
    public static final String ACTION_PLAY_NEXT = "com.playtube.plus.player.BackgroundPlayer.ACTION_PLAY_NEXT";
    public static final String ACTION_PLAY_PREVIOUS = "com.playtube.plus.player.BackgroundPlayer.ACTION_PLAY_PREVIOUS";
    public static final String ACTION_FAST_REWIND = "com.playtube.plus.player.BackgroundPlayer.ACTION_FAST_REWIND";
    public static final String ACTION_FAST_FORWARD = "com.playtube.plus.player.BackgroundPlayer.ACTION_FAST_FORWARD";

    public static final String SET_IMAGE_RESOURCE_METHOD = "setImageResource";

    private BasePlayerImpl basePlayerImpl;
    private LockManager lockManager;

    /*//////////////////////////////////////////////////////////////////////////
    // Service-Activity Binder
    //////////////////////////////////////////////////////////////////////////*/

    private PlayerEventListener activityListener;
    private IBinder mBinder;

    /*//////////////////////////////////////////////////////////////////////////
    // Notification
    //////////////////////////////////////////////////////////////////////////*/

    private static final int NOTIFICATION_ID = 123789;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notBuilder;
    private RemoteViews notRemoteView;
    private RemoteViews bigNotRemoteView;
    private final String setAlphaMethodName = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) ? "setImageAlpha" : "setAlpha";

    private boolean shouldUpdateOnProgress;

    /*//////////////////////////////////////////////////////////////////////////
    // Service's LifeCycle
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "onCreate() called");
        notificationManager = ((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
        lockManager = new LockManager(this);

        basePlayerImpl = new BasePlayerImpl(this);
        basePlayerImpl.setup();

        mBinder = new PlayerServiceBinder(basePlayerImpl);
        shouldUpdateOnProgress = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "onStartCommand() called with: intent = [" + intent + "], flags = [" + flags + "], startId = [" + startId + "]");
        basePlayerImpl.handleIntent(intent);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "destroy() called");
        onClose();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Actions
    //////////////////////////////////////////////////////////////////////////*/
    private void onClose() {
        if (DEBUG) Log.d(TAG, "onClose() called");

        if (lockManager != null) {
            lockManager.releaseWifiAndCpu();
        }
        if (basePlayerImpl != null) {
            basePlayerImpl.stopActivityBinding();
            basePlayerImpl.destroy();
        }
        if (notificationManager != null) notificationManager.cancel(NOTIFICATION_ID);
        mBinder = null;
        basePlayerImpl = null;
        lockManager = null;

        stopForeground(true);
        stopSelf();
    }

    private void onScreenOnOff(boolean on) {
        if (DEBUG) Log.d(TAG, "onScreenOnOff() called with: on = [" + on + "]");
        shouldUpdateOnProgress = on;
        basePlayerImpl.triggerProgressUpdate();
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Notification
    //////////////////////////////////////////////////////////////////////////*/

    private void resetNotification() {
        notBuilder = createNotification();
    }

    private NotificationCompat.Builder createNotification() {
        notRemoteView = new RemoteViews(BuildConfig.APPLICATION_ID, org.tubeplayer.plus.R.layout.player_notification);
        bigNotRemoteView = new RemoteViews(BuildConfig.APPLICATION_ID, org.tubeplayer.plus.R.layout.player_notification_expanded);

        setupNotification(notRemoteView);
        setupNotification(bigNotRemoteView);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, getString(org.tubeplayer.plus.R.string.notification_channel_id))
                .setOngoing(true)
                .setSmallIcon(org.tubeplayer.plus.R.drawable.ic_play_notification)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCustomContentView(notRemoteView)
                .setCustomBigContentView(bigNotRemoteView);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) builder.setPriority(NotificationCompat.PRIORITY_MAX);
        return builder;
    }

    private void setupNotification(RemoteViews remoteViews) {
        if (basePlayerImpl == null) return;

        remoteViews.setTextViewText(org.tubeplayer.plus.R.id.notificationSongName, basePlayerImpl.getVideoTitle());
        remoteViews.setTextViewText(org.tubeplayer.plus.R.id.notificationArtist, basePlayerImpl.getUploaderName());

        remoteViews.setOnClickPendingIntent(org.tubeplayer.plus.R.id.notificationPlayPause,
                PendingIntent.getBroadcast(this, NOTIFICATION_ID, new Intent(ACTION_PLAY_PAUSE), PendingIntent.FLAG_UPDATE_CURRENT));
        remoteViews.setOnClickPendingIntent(org.tubeplayer.plus.R.id.notificationStop,
                PendingIntent.getBroadcast(this, NOTIFICATION_ID, new Intent(ACTION_CLOSE), PendingIntent.FLAG_UPDATE_CURRENT));
        remoteViews.setOnClickPendingIntent(org.tubeplayer.plus.R.id.notificationRepeat,
                PendingIntent.getBroadcast(this, NOTIFICATION_ID, new Intent(ACTION_REPEAT), PendingIntent.FLAG_UPDATE_CURRENT));

        // Starts background player activity -- attempts to unlock lockscreen
        final Intent intent = NavigationHelper.getBackgroundPlayerActivityIntent(this);
        remoteViews.setOnClickPendingIntent(org.tubeplayer.plus.R.id.notificationContent,
                PendingIntent.getActivity(this, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT));

        if (basePlayerImpl.playQueue != null && basePlayerImpl.playQueue.size() > 1) {
            remoteViews.setInt(org.tubeplayer.plus.R.id.notificationFRewind, SET_IMAGE_RESOURCE_METHOD, org.tubeplayer.plus.R.drawable.exo_controls_previous);
            remoteViews.setInt(org.tubeplayer.plus.R.id.notificationFForward, SET_IMAGE_RESOURCE_METHOD, org.tubeplayer.plus.R.drawable.exo_controls_next);
            remoteViews.setOnClickPendingIntent(org.tubeplayer.plus.R.id.notificationFRewind,
                    PendingIntent.getBroadcast(this, NOTIFICATION_ID, new Intent(ACTION_PLAY_PREVIOUS), PendingIntent.FLAG_UPDATE_CURRENT));
            remoteViews.setOnClickPendingIntent(org.tubeplayer.plus.R.id.notificationFForward,
                    PendingIntent.getBroadcast(this, NOTIFICATION_ID, new Intent(ACTION_PLAY_NEXT), PendingIntent.FLAG_UPDATE_CURRENT));
        } else {
            remoteViews.setInt(org.tubeplayer.plus.R.id.notificationFRewind, SET_IMAGE_RESOURCE_METHOD, org.tubeplayer.plus.R.drawable.exo_controls_rewind);
            remoteViews.setInt(org.tubeplayer.plus.R.id.notificationFForward, SET_IMAGE_RESOURCE_METHOD, org.tubeplayer.plus.R.drawable.exo_controls_fastforward);
            remoteViews.setOnClickPendingIntent(org.tubeplayer.plus.R.id.notificationFRewind,
                    PendingIntent.getBroadcast(this, NOTIFICATION_ID, new Intent(ACTION_FAST_REWIND), PendingIntent.FLAG_UPDATE_CURRENT));
            remoteViews.setOnClickPendingIntent(org.tubeplayer.plus.R.id.notificationFForward,
                    PendingIntent.getBroadcast(this, NOTIFICATION_ID, new Intent(ACTION_FAST_FORWARD), PendingIntent.FLAG_UPDATE_CURRENT));
        }

        setRepeatModeIcon(remoteViews, basePlayerImpl.getRepeatMode());
    }

    /**
     * Updates the notification, and the play/pause button in it.
     * Used for changes on the remoteView
     *
     * @param drawableId if != -1, sets the drawable with that id on the play/pause button
     */
    private synchronized void updateNotification(int drawableId) {
        //if (DEBUG) Log.d(TAG, "updateNotification() called with: drawableId = [" + drawableId + "]");
        if (notBuilder == null) return;
        if (drawableId != -1) {
            if (notRemoteView != null) notRemoteView.setImageViewResource(org.tubeplayer.plus.R.id.notificationPlayPause, drawableId);
            if (bigNotRemoteView != null) bigNotRemoteView.setImageViewResource(org.tubeplayer.plus.R.id.notificationPlayPause, drawableId);
        }
        try {
            notificationManager.notify(NOTIFICATION_ID, notBuilder.build());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void setControlsOpacity(@IntRange(from = 0, to = 255) int opacity) {
        if (notRemoteView != null) notRemoteView.setInt(org.tubeplayer.plus.R.id.notificationPlayPause, setAlphaMethodName, opacity);
        if (bigNotRemoteView != null) bigNotRemoteView.setInt(org.tubeplayer.plus.R.id.notificationPlayPause, setAlphaMethodName, opacity);
        if (notRemoteView != null) notRemoteView.setInt(org.tubeplayer.plus.R.id.notificationFForward, setAlphaMethodName, opacity);
        if (bigNotRemoteView != null) bigNotRemoteView.setInt(org.tubeplayer.plus.R.id.notificationFForward, setAlphaMethodName, opacity);
        if (notRemoteView != null) notRemoteView.setInt(org.tubeplayer.plus.R.id.notificationFRewind, setAlphaMethodName, opacity);
        if (bigNotRemoteView != null) bigNotRemoteView.setInt(org.tubeplayer.plus.R.id.notificationFRewind, setAlphaMethodName, opacity);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/

    private void setRepeatModeIcon(final RemoteViews remoteViews, final int repeatMode) {
        switch (repeatMode) {
            case Player.REPEAT_MODE_OFF:
                remoteViews.setInt(org.tubeplayer.plus.R.id.notificationRepeat, SET_IMAGE_RESOURCE_METHOD, org.tubeplayer.plus.R.drawable.exo_controls_repeat_off);
                break;
            case Player.REPEAT_MODE_ONE:
                remoteViews.setInt(org.tubeplayer.plus.R.id.notificationRepeat, SET_IMAGE_RESOURCE_METHOD, org.tubeplayer.plus.R.drawable.exo_controls_repeat_one);
                break;
            case Player.REPEAT_MODE_ALL:
                remoteViews.setInt(org.tubeplayer.plus.R.id.notificationRepeat, SET_IMAGE_RESOURCE_METHOD, org.tubeplayer.plus.R.drawable.exo_controls_repeat_all);
                break;
        }
    }
    //////////////////////////////////////////////////////////////////////////

    protected class BasePlayerImpl extends BasePlayer {

        BasePlayerImpl(Context context) {
            super(context);
        }

        @Override
        public void handleIntent(final Intent intent) {
            super.handleIntent(intent);

            resetNotification();
            if (bigNotRemoteView != null) bigNotRemoteView.setProgressBar(org.tubeplayer.plus.R.id.notificationProgressBar, 100, 0, false);
            if (notRemoteView != null) notRemoteView.setProgressBar(org.tubeplayer.plus.R.id.notificationProgressBar, 100, 0, false);
            startForeground(NOTIFICATION_ID, notBuilder.build());
        }

        @Override
        public void initThumbnail(final String url) {
            resetNotification();
            if (notRemoteView != null) notRemoteView.setImageViewResource(org.tubeplayer.plus.R.id.notificationCover, org.tubeplayer.plus.R.drawable.dummy_thumbnail);
            if (bigNotRemoteView != null) bigNotRemoteView.setImageViewResource(org.tubeplayer.plus.R.id.notificationCover, org.tubeplayer.plus.R.drawable.dummy_thumbnail);
            updateNotification(-1);
            super.initThumbnail(url);
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            super.onLoadingComplete(imageUri, view, loadedImage);

            if (loadedImage != null) {
                // rebuild notification here since remote view does not release bitmaps, causing memory leaks
                resetNotification();

                if (notRemoteView != null) notRemoteView.setImageViewBitmap(org.tubeplayer.plus.R.id.notificationCover, loadedImage);
                if (bigNotRemoteView != null) bigNotRemoteView.setImageViewBitmap(org.tubeplayer.plus.R.id.notificationCover, loadedImage);

                updateNotification(-1);
            }
        }

        @Override
        public void onPrepared(boolean playWhenReady) {
            super.onPrepared(playWhenReady);
            simpleExoPlayer.setVolume(1f);
        }

        @Override
        public void onShuffleClicked() {
            super.onShuffleClicked();
            updatePlayback();
        }

        @Override
        public void onUpdateProgress(int currentProgress, int duration, int bufferPercent) {
            updateProgress(currentProgress, duration, bufferPercent);

            if (!shouldUpdateOnProgress) return;
            resetNotification();
            if (bigNotRemoteView != null) {
                bigNotRemoteView.setProgressBar(org.tubeplayer.plus.R.id.notificationProgressBar, duration, currentProgress, false);
                bigNotRemoteView.setTextViewText(org.tubeplayer.plus.R.id.notificationTime, PlayerHelper.getTimeString(currentProgress) + " / " + PlayerHelper.getTimeString(duration));
            }
            if (notRemoteView != null) {
                notRemoteView.setProgressBar(org.tubeplayer.plus.R.id.notificationProgressBar, duration, currentProgress, false);
            }
            updateNotification(-1);
        }

        @Override
        public void onPlayPrevious() {
            super.onPlayPrevious();
            triggerProgressUpdate();
        }

        @Override
        public void onPlayNext() {
            super.onPlayNext();
            triggerProgressUpdate();
        }

        @Override
        public void destroy() {
            super.destroy();
            if (notRemoteView != null) notRemoteView.setImageViewBitmap(org.tubeplayer.plus.R.id.notificationCover, null);
            if (bigNotRemoteView != null) bigNotRemoteView.setImageViewBitmap(org.tubeplayer.plus.R.id.notificationCover, null);
        }

        /*//////////////////////////////////////////////////////////////////////////
        // ExoPlayer Listener
        //////////////////////////////////////////////////////////////////////////*/

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            super.onPlaybackParametersChanged(playbackParameters);
            updatePlayback();
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            // Disable default behavior
        }

        @Override
        public void onRepeatModeChanged(int i) {
            resetNotification();
            updateNotification(-1);
            updatePlayback();
        }

        /*//////////////////////////////////////////////////////////////////////////
        // Playback Listener
        //////////////////////////////////////////////////////////////////////////*/

        protected void onMetadataChanged(@NonNull final PlayQueueItem item,
                                         @Nullable final StreamInfo info,
                                         final int newPlayQueueIndex,
                                         final boolean hasPlayQueueItemChanged) {
            if (shouldUpdateOnProgress || hasPlayQueueItemChanged) {
                resetNotification();
                updateNotification(-1);
                updateMetadata();
            }
        }

        @Override
        @Nullable
        public MediaSource sourceOf(final PlayQueueItem item, final StreamInfo info) {
            final MediaSource liveSource = super.sourceOf(item, info);
            if (liveSource != null) return liveSource;

            final int index = ListHelper.getDefaultAudioFormat(context, info.getAudioStreams());
            if (index < 0 || index >= info.getAudioStreams().size()) return null;

            final AudioStream audio = info.getAudioStreams().get(index);
            return buildMediaSource(audio.getUrl(), PlayerHelper.cacheKeyOf(info, audio),
                    MediaFormat.getSuffixById(audio.getFormatId()));
        }

        @Override
        public void onPlaybackShutdown() {
            super.onPlaybackShutdown();
            onClose();
        }

        /*//////////////////////////////////////////////////////////////////////////
        // Activity Event Listener
        //////////////////////////////////////////////////////////////////////////*/

        /*package-private*/ void setActivityListener(PlayerEventListener listener) {
            activityListener = listener;
            updateMetadata();
            updatePlayback();
            triggerProgressUpdate();
        }

        /*package-private*/ void removeActivityListener(PlayerEventListener listener) {
            if (activityListener == listener) {
                activityListener = null;
            }
        }

        private void updateMetadata() {
            if (activityListener != null && currentInfo != null) {
                activityListener.onMetadataUpdate(currentInfo);
            }
        }

        private void updatePlayback() {
            if (activityListener != null && simpleExoPlayer != null && playQueue != null) {
                activityListener.onPlaybackUpdate(currentState, getRepeatMode(),
                        playQueue.isShuffled(), getPlaybackParameters());
            }
        }

        private void updateProgress(int currentProgress, int duration, int bufferPercent) {
            if (activityListener != null) {
                activityListener.onProgressUpdate(currentProgress, duration, bufferPercent);
            }
        }

        private void stopActivityBinding() {
            if (activityListener != null) {
                activityListener.onServiceStopped();
                activityListener = null;
            }
        }

        /*//////////////////////////////////////////////////////////////////////////
        // Broadcast Receiver
        //////////////////////////////////////////////////////////////////////////*/

        @Override
        protected void setupBroadcastReceiver(IntentFilter intentFilter) {
            super.setupBroadcastReceiver(intentFilter);
            intentFilter.addAction(ACTION_CLOSE);
            intentFilter.addAction(ACTION_PLAY_PAUSE);
            intentFilter.addAction(ACTION_REPEAT);
            intentFilter.addAction(ACTION_PLAY_PREVIOUS);
            intentFilter.addAction(ACTION_PLAY_NEXT);
            intentFilter.addAction(ACTION_FAST_REWIND);
            intentFilter.addAction(ACTION_FAST_FORWARD);

            intentFilter.addAction(Intent.ACTION_SCREEN_ON);
            intentFilter.addAction(Intent.ACTION_SCREEN_OFF);

            intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        }

        @Override
        public void onBroadcastReceived(Intent intent) {
            super.onBroadcastReceived(intent);
            if (intent == null || intent.getAction() == null) return;
            if (DEBUG) Log.d(TAG, "onBroadcastReceived() called with: intent = [" + intent + "]");
            switch (intent.getAction()) {
                case ACTION_CLOSE:
                    onClose();
                    break;
                case ACTION_PLAY_PAUSE:
                    onPlayPause();
                    break;
                case ACTION_REPEAT:
                    onRepeatClicked();
                    break;
                case ACTION_PLAY_NEXT:
                    onPlayNext();
                    break;
                case ACTION_PLAY_PREVIOUS:
                    onPlayPrevious();
                    break;
                case ACTION_FAST_FORWARD:
                    onFastForward();
                    break;
                case ACTION_FAST_REWIND:
                    onFastRewind();
                    break;
                case Intent.ACTION_SCREEN_ON:
                    onScreenOnOff(true);
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    onScreenOnOff(false);
                    break;
            }
        }

        /*//////////////////////////////////////////////////////////////////////////
        // States
        //////////////////////////////////////////////////////////////////////////*/

        @Override
        public void changeState(int state) {
            super.changeState(state);
            updatePlayback();
        }

        @Override
        public void onBlocked() {
            super.onBlocked();

            setControlsOpacity(77);
            updateNotification(-1);
        }

        @Override
        public void onPlaying() {
            super.onPlaying();

            setControlsOpacity(255);
            updateNotification(org.tubeplayer.plus.R.drawable.ic_pause_white);

            lockManager.acquireWifiAndCpu();
        }

        @Override
        public void onPaused() {
            super.onPaused();

            updateNotification(org.tubeplayer.plus.R.drawable.ic_play_arrow_white);
            if (isProgressLoopRunning()) stopProgressLoop();

            lockManager.releaseWifiAndCpu();
        }

        @Override
        public void onCompleted() {
            super.onCompleted();

            setControlsOpacity(255);

            resetNotification();
            if (bigNotRemoteView != null) bigNotRemoteView.setProgressBar(org.tubeplayer.plus.R.id.notificationProgressBar, 100, 100, false);
            if (notRemoteView != null) notRemoteView.setProgressBar(org.tubeplayer.plus.R.id.notificationProgressBar, 100, 100, false);
            updateNotification(org.tubeplayer.plus.R.drawable.ic_replay_white);

            lockManager.releaseWifiAndCpu();
        }
    }
}
