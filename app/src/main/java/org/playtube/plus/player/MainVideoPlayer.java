/*
 * Copyright 2017 Mauricio Colli <mauriciocolli@outlook.com>
 * MainVideoPlayer.java is part of NewPipe
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

package org.playtube.plus.player;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.ads.Ad;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.SubtitleView;

import org.playtube.plus.App;
import org.playtube.plus.player.helper.PlaybackParameterDialog;
import org.playtube.plus.player.helper.PlayerHelper;
import org.playtube.plus.playlist.PlayQueue;
import org.playtube.plus.playlist.PlayQueueItem;
import org.playtube.plus.playlist.PlayQueueItemTouchCallback;
import org.playtube.plus.util.AnimationUtils;
import org.playtube.plus.util.FBAdUtils;
import org.playtube.plus.util.ListHelper;
import org.playtube.plus.util.NavigationHelper;
import org.playtube.plus.util.PermissionHelper;
import org.playtube.plus.util.StateSaver;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.playtube.plus.fragments.OnScrollBelowItemsListener;
import org.playtube.plus.playlist.PlayQueueItemBuilder;
import org.playtube.plus.playlist.PlayQueueItemHolder;
import org.playtube.plus.util.Constants;

import java.util.List;
import java.util.Queue;
import java.util.UUID;

import static org.playtube.plus.player.BasePlayer.STATE_PLAYING;
import static org.playtube.plus.player.VideoPlayer.DEFAULT_CONTROLS_DURATION;
import static org.playtube.plus.player.VideoPlayer.DEFAULT_CONTROLS_HIDE_TIME;
import static org.playtube.plus.util.AnimationUtils.animateView;

/**
 * Activity Player implementing VideoPlayer
 *
 * @author mauriciocolli
 */
public final class MainVideoPlayer extends AppCompatActivity
        implements StateSaver.WriteRead, PlaybackParameterDialog.Callback {
    private static final String TAG = ".MainVideoPlayer";
    private static final boolean DEBUG = BasePlayer.DEBUG;

    private GestureDetector gestureDetector;

    private boolean activityPaused;
    private VideoPlayerImpl playerImpl;

    private SharedPreferences defaultPreferences;

    @Nullable private StateSaver.SavedState savedState;

    /*//////////////////////////////////////////////////////////////////////////
    // Activity LifeCycle
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.d(TAG, "onCreate() called with: savedInstanceState = [" + savedInstanceState + "]");
        defaultPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) getWindow().setStatusBarColor(Color.BLACK);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        hideSystemUi();
        setContentView(org.playtube.plus.R.layout.activity_main_player);
        playerImpl = new VideoPlayerImpl(this);
        playerImpl.setup(findViewById(android.R.id.content));

        if (savedInstanceState != null && savedInstanceState.get(StateSaver.KEY_SAVED_STATE) != null) {
            return; // We have saved states, stop here to restore it
        }

        final Intent intent = getIntent();
        if (intent != null) {
            playerImpl.handleIntent(intent);
        } else {
            Toast.makeText(this, org.playtube.plus.R.string.general_error, Toast.LENGTH_SHORT).show();
            finish();
        }

        if (App.isBgPlay()) {
            FBAdUtils.get().interstitialLoad(Constants.INTERSTITIAL_AD, new FBAdUtils.FBInterstitialAdListener() {
                @Override
                public void onInterstitialDismissed(Ad ad) {
                    super.onInterstitialDismissed(ad);
                    FBAdUtils.get().destoryInterstitial();
                }
            });
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        savedState = StateSaver.tryToRestore(bundle, this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (DEBUG) Log.d(TAG, "onNewIntent() called with: intent = [" + intent + "]");
        super.onNewIntent(intent);
        playerImpl.handleIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG) Log.d(TAG, "onResume() called");
        if (playerImpl.getPlayer() != null && activityPaused && playerImpl.wasPlaying()
                && !playerImpl.isPlaying()) {
            playerImpl.onPlay();
        }
        activityPaused = false;

        if(globalScreenOrientationLocked()) {
            boolean lastOrientationWasLandscape
                    = defaultPreferences.getBoolean(getString(org.playtube.plus.R.string.last_orientation_landscape_key), false);
            setLandscape(lastOrientationWasLandscape);
        }
    }

    @Override
    public void onBackPressed() {
        if (DEBUG) Log.d(TAG, "onBackPressed() called");
        super.onBackPressed();
        if (playerImpl.isPlaying()) playerImpl.getPlayer().setPlayWhenReady(false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (playerImpl.isSomePopupMenuVisible()) {
            playerImpl.getQualityPopupMenu().dismiss();
            playerImpl.getPlaybackSpeedPopupMenu().dismiss();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DEBUG) Log.d(TAG, "onPause() called");

        if (playerImpl != null && playerImpl.getPlayer() != null && !activityPaused) {
            playerImpl.wasPlaying = playerImpl.isPlaying();
            playerImpl.onPause();
        }
        activityPaused = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (playerImpl == null) return;

        playerImpl.setRecovery();
        savedState = StateSaver.tryToSave(isChangingConfigurations(), savedState,
                outState, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.d(TAG, "onDestroy() called");
        if (playerImpl != null) playerImpl.destroy();

        if (App.isBgPlay()) {
            if (FBAdUtils.get().isInterstitialLoaded()) {
                FBAdUtils.get().showInterstitial();
            }
            FBAdUtils.get().destoryInterstitial();
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // State Saving
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public String generateSuffix() {
        return "." + UUID.randomUUID().toString() + ".player";
    }

    @Override
    public void writeTo(Queue<Object> objectsToSave) {
        if (objectsToSave == null) return;
        objectsToSave.add(playerImpl.getPlayQueue());
        objectsToSave.add(playerImpl.getRepeatMode());
        objectsToSave.add(playerImpl.getPlaybackSpeed());
        objectsToSave.add(playerImpl.getPlaybackPitch());
        objectsToSave.add(playerImpl.getPlaybackQuality());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void readFrom(@NonNull Queue<Object> savedObjects) throws Exception {
        @NonNull final PlayQueue queue = (PlayQueue) savedObjects.poll();
        final int repeatMode = (int) savedObjects.poll();
        final float playbackSpeed = (float) savedObjects.poll();
        final float playbackPitch = (float) savedObjects.poll();
        @NonNull final String playbackQuality = (String) savedObjects.poll();

        playerImpl.setPlaybackQuality(playbackQuality);
        playerImpl.initPlayback(queue, repeatMode, playbackSpeed, playbackPitch);

        StateSaver.onDestroy(savedState);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // View
    //////////////////////////////////////////////////////////////////////////*/

    /**
     * Prior to Kitkat, hiding system ui causes the player view to be overlaid and require two
     * clicks to get rid of that invisible overlay. By showing the system UI on actions/events,
     * that overlay is removed and the player view is put to the foreground.
     *
     * Post Kitkat, navbar and status bar can be pulled out by swiping the edge of
     * screen, therefore, we can do nothing or hide the UI on actions/events.
     * */
    private void changeSystemUi() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            showSystemUi();
        } else {
            hideSystemUi();
        }
    }

    private void showSystemUi() {
        if (DEBUG) Log.d(TAG, "showSystemUi() called");
        if (playerImpl != null && playerImpl.queueVisible) return;

        final int visibility;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        } else {
            visibility = View.STATUS_BAR_VISIBLE;
        }
        getWindow().getDecorView().setSystemUiVisibility(visibility);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void hideSystemUi() {
        if (DEBUG) Log.d(TAG, "hideSystemUi() called");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            int visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                visibility |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }
            getWindow().getDecorView().setSystemUiVisibility(visibility);
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void toggleOrientation() {
        setLandscape(!isLandscape());
        defaultPreferences.edit()
                .putBoolean(getString(org.playtube.plus.R.string.last_orientation_landscape_key), !isLandscape())
                .apply();
    }

    private boolean isLandscape() {
        return getResources().getDisplayMetrics().heightPixels < getResources().getDisplayMetrics().widthPixels;
    }

    private void setLandscape(boolean v) {
        setRequestedOrientation(v
                ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
    }

    private boolean globalScreenOrientationLocked() {
        // 1: Screen orientation changes using acelerometer
        // 0: Screen orientatino is locked
        return !(android.provider.Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1);
    }

    protected void setRepeatModeButton(final ImageButton imageButton, final int repeatMode) {
        switch (repeatMode) {
            case Player.REPEAT_MODE_OFF:
                imageButton.setImageResource(org.playtube.plus.R.drawable.exo_controls_repeat_off);
                break;
            case Player.REPEAT_MODE_ONE:
                imageButton.setImageResource(org.playtube.plus.R.drawable.exo_controls_repeat_one);
                break;
            case Player.REPEAT_MODE_ALL:
                imageButton.setImageResource(org.playtube.plus.R.drawable.exo_controls_repeat_all);
                break;
        }
    }

    protected void setShuffleButton(final ImageButton shuffleButton, final boolean shuffled) {
        final int shuffleAlpha = shuffled ? 255 : 77;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            shuffleButton.setImageAlpha(shuffleAlpha);
        } else {
            shuffleButton.setAlpha(shuffleAlpha);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Playback Parameters Listener
    ////////////////////////////////////////////////////////////////////////////

    @Override
    public void onPlaybackParameterChanged(float playbackTempo, float playbackPitch) {
        if (playerImpl != null) playerImpl.setPlaybackParameters(playbackTempo, playbackPitch);
    }

    ///////////////////////////////////////////////////////////////////////////

    @SuppressWarnings({"unused", "WeakerAccess"})
    private class VideoPlayerImpl extends VideoPlayer {
        private TextView titleTextView;
        private TextView channelTextView;
        private TextView volumeTextView;
        private TextView brightnessTextView;
        private ImageButton queueButton;
        private ImageButton repeatButton;
        private ImageButton shuffleButton;

        private ImageButton playPauseButton;
        private ImageButton playPreviousButton;
        private ImageButton playNextButton;

        private RelativeLayout queueLayout;
        private ImageButton itemsListCloseButton;
        private RecyclerView itemsList;
        private ItemTouchHelper itemTouchHelper;

        private boolean queueVisible;

        private ImageButton moreOptionsButton;
        private ImageButton toggleOrientationButton;
        private ImageButton switchPopupButton;
        private ImageButton switchBackgroundButton;

        private RelativeLayout windowRootLayout;
        private View secondaryControls;

        VideoPlayerImpl(final Context context) {
            super("VideoPlayerImpl" + MainVideoPlayer.TAG, context);
        }

        @Override
        public void onBroadcastReceived(Intent intent) {
            super.onBroadcastReceived(intent);
            if (intent == null || intent.getAction() == null) return;
            if (DEBUG) Log.d(TAG, "onBroadcastReceived() called with: intent = [" + intent + "]");
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_OFF:
                    if (!App.isBgPlay()) {
                        onPlayPause();
                    }
                    break;
            }
        }

        private View youtubeIconIV;
        private View musicLogoIV;

        @Override
        public void initViews(View rootView) {
            super.initViews(rootView);
            this.titleTextView = rootView.findViewById(org.playtube.plus.R.id.titleTextView);
            this.channelTextView = rootView.findViewById(org.playtube.plus.R.id.channelTextView);
            this.volumeTextView = rootView.findViewById(org.playtube.plus.R.id.volumeTextView);
            this.brightnessTextView = rootView.findViewById(org.playtube.plus.R.id.brightnessTextView);
            this.queueButton = rootView.findViewById(org.playtube.plus.R.id.queueButton);
            this.repeatButton = rootView.findViewById(org.playtube.plus.R.id.repeatButton);
            this.shuffleButton = rootView.findViewById(org.playtube.plus.R.id.shuffleButton);

            this.playPauseButton = rootView.findViewById(org.playtube.plus.R.id.playPauseButton);
            this.playPreviousButton = rootView.findViewById(org.playtube.plus.R.id.playPreviousButton);
            this.playNextButton = rootView.findViewById(org.playtube.plus.R.id.playNextButton);

            this.moreOptionsButton = rootView.findViewById(org.playtube.plus.R.id.moreOptionsButton);
            this.secondaryControls = rootView.findViewById(org.playtube.plus.R.id.secondaryControls);
            this.toggleOrientationButton = rootView.findViewById(org.playtube.plus.R.id.toggleOrientation);
            this.switchBackgroundButton = rootView.findViewById(org.playtube.plus.R.id.switchBackground);
            this.switchPopupButton = rootView.findViewById(org.playtube.plus.R.id.switchPopup);

            this.queueLayout = findViewById(org.playtube.plus.R.id.playQueuePanel);
            this.itemsListCloseButton = findViewById(org.playtube.plus.R.id.playQueueClose);
            this.itemsList = findViewById(org.playtube.plus.R.id.playQueue);

            this.windowRootLayout = rootView.findViewById(org.playtube.plus.R.id.playbackWindowRoot);
            // Prior to Kitkat, there is no way of setting translucent navbar programmatically.
            // Thus, fit system windows is opted instead.
            // See https://stackoverflow.com/questions/29069070/completely-transparent-status-bar-and-navigation-bar-on-lollipop
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                windowRootLayout.setFitsSystemWindows(false);
                windowRootLayout.invalidate();
            }

            titleTextView.setSelected(true);
            channelTextView.setSelected(true);

            getRootView().setKeepScreenOn(true);

            musicLogoIV = rootView.findViewById(org.playtube.plus.R.id.music_logo_iv);
            youtubeIconIV = rootView.findViewById(org.playtube.plus.R.id.youtube_icon_iv);
            youtubeIconIV.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    try {
                        if (playerImpl == null) {
                            return;
                        }
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(playerImpl.getVideoUrl()));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        protected void setupSubtitleView(@NonNull SubtitleView view,
                                         @NonNull String captionSizeKey) {
            final float captionRatioInverse;
            if (captionSizeKey.equals(getString(org.playtube.plus.R.string.smaller_caption_size_key))) {
                captionRatioInverse = 22f;
            } else if (captionSizeKey.equals(getString(org.playtube.plus.R.string.larger_caption_size_key))) {
                captionRatioInverse = 18f;
            } else {
                captionRatioInverse = 20f;
            }

            final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            final int minimumLength = Math.min(metrics.heightPixels, metrics.widthPixels);
            view.setFixedTextSize(TypedValue.COMPLEX_UNIT_PX,
                    (float) minimumLength / captionRatioInverse);
        }

        @Override
        public void initListeners() {
            super.initListeners();

            MySimpleOnGestureListener listener = new MySimpleOnGestureListener();
            gestureDetector = new GestureDetector(context, listener);
            gestureDetector.setIsLongpressEnabled(false);
            getRootView().setOnTouchListener(listener);

            queueButton.setOnClickListener(this);
            repeatButton.setOnClickListener(this);
            shuffleButton.setOnClickListener(this);

            playPauseButton.setOnClickListener(this);
            playPreviousButton.setOnClickListener(this);
            playNextButton.setOnClickListener(this);

            moreOptionsButton.setOnClickListener(this);
            toggleOrientationButton.setOnClickListener(this);
            switchBackgroundButton.setOnClickListener(this);
            switchPopupButton.setOnClickListener(this);
        }

        /*//////////////////////////////////////////////////////////////////////////
        // ExoPlayer Video Listener
        //////////////////////////////////////////////////////////////////////////*/

        @Override
        public void onRepeatModeChanged(int i) {
            super.onRepeatModeChanged(i);
            updatePlaybackButtons();
        }

        @Override
        public void onShuffleClicked() {
            super.onShuffleClicked();
            updatePlaybackButtons();
        }

        /*//////////////////////////////////////////////////////////////////////////
        // Playback Listener
        //////////////////////////////////////////////////////////////////////////*/

        protected void onMetadataChanged(@NonNull final PlayQueueItem item,
                                         @Nullable final StreamInfo info,
                                         final int newPlayQueueIndex,
                                         final boolean hasPlayQueueItemChanged) {
            super.onMetadataChanged(item, info, newPlayQueueIndex, false);

            titleTextView.setText(getVideoTitle());
            channelTextView.setText(getUploaderName());

            if (info != null && info.getServiceId() == 0 && !App.isBgPlay()) {
                switchBackgroundButton.setVisibility(View.GONE);
            } else {
                switchBackgroundButton.setVisibility(View.VISIBLE);
            }

            if (info != null && info.getServiceId() == 0) {
                youtubeIconIV.setVisibility(View.VISIBLE);
                musicLogoIV.setVisibility(View.GONE);
            } else {
                youtubeIconIV.setVisibility(View.GONE);
                musicLogoIV.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPlaybackShutdown() {
            super.onPlaybackShutdown();
            finish();
        }

        /*//////////////////////////////////////////////////////////////////////////
        // Player Overrides
        //////////////////////////////////////////////////////////////////////////*/

        @Override
        public void onFullScreenButtonClicked() {
            super.onFullScreenButtonClicked();

            if (DEBUG) Log.d(TAG, "onFullScreenButtonClicked() called");
            if (simpleExoPlayer == null) return;

            if (!PermissionHelper.isPopupEnabled(context)) {
                PermissionHelper.showPopupEnablementToast(context);
                return;
            }

            setRecovery();
            final Intent intent = NavigationHelper.getPlayerIntent(
                    context,
                    PopupVideoPlayer.class,
                    this.getPlayQueue(),
                    this.getRepeatMode(),
                    this.getPlaybackSpeed(),
                    this.getPlaybackPitch(),
                    this.getPlaybackQuality()
            );
            context.startService(intent);

            ((View) getControlAnimationView().getParent()).setVisibility(View.GONE);
            destroy();
            finish();
        }

        public void onPlayBackgroundButtonClicked() {
            if (DEBUG) Log.d(TAG, "onPlayBackgroundButtonClicked() called");
            if (playerImpl.getPlayer() == null) return;

            setRecovery();
            final Intent intent = NavigationHelper.getPlayerIntent(
                    context,
                    BackgroundPlayer.class,
                    this.getPlayQueue(),
                    this.getRepeatMode(),
                    this.getPlaybackSpeed(),
                    this.getPlaybackPitch(),
                    this.getPlaybackQuality()
            );
            context.startService(intent);

            ((View) getControlAnimationView().getParent()).setVisibility(View.GONE);
            destroy();
            finish();
        }


        @Override
        public void onClick(View v) {
            super.onClick(v);
            if (v.getId() == playPauseButton.getId()) {
                onPlayPause();

            } else if (v.getId() == playPreviousButton.getId()) {
                onPlayPrevious();

            } else if (v.getId() == playNextButton.getId()) {
                onPlayNext();

            } else if (v.getId() == queueButton.getId()) {
                onQueueClicked();
                return;
            } else if (v.getId() == repeatButton.getId()) {
                onRepeatClicked();
                return;
            } else if (v.getId() == shuffleButton.getId()) {
                onShuffleClicked();
                return;
            } else if (v.getId() == moreOptionsButton.getId()) {
                onMoreOptionsClicked();

            } else if (v.getId() == toggleOrientationButton.getId()) {
                onScreenRotationClicked();

            } else if (v.getId() == switchPopupButton.getId()) {
                onFullScreenButtonClicked();

            } else if (v.getId() == switchBackgroundButton.getId()) {
                onPlayBackgroundButtonClicked();

            }

            if (getCurrentState() != STATE_COMPLETED) {
                getControlsVisibilityHandler().removeCallbacksAndMessages(null);
                AnimationUtils.animateView(getControlsRoot(), true, DEFAULT_CONTROLS_DURATION, 0, () -> {
                    if (getCurrentState() == STATE_PLAYING && !isSomePopupMenuVisible()) {
                        hideControls(DEFAULT_CONTROLS_DURATION, DEFAULT_CONTROLS_HIDE_TIME);
                    }
                });
            }
        }

        private void onQueueClicked() {
            queueVisible = true;
            hideSystemUi();

            buildQueue();
            updatePlaybackButtons();

            getControlsRoot().setVisibility(View.INVISIBLE);
            animateView(queueLayout, AnimationUtils.Type.SLIDE_AND_ALPHA, /*visible=*/true,
                    DEFAULT_CONTROLS_DURATION);

            itemsList.scrollToPosition(playQueue.getIndex());
        }

        private void onQueueClosed() {
            animateView(queueLayout, AnimationUtils.Type.SLIDE_AND_ALPHA, /*visible=*/false,
                    DEFAULT_CONTROLS_DURATION);
            queueVisible = false;
        }

        private void onMoreOptionsClicked() {
            if (DEBUG) Log.d(TAG, "onMoreOptionsClicked() called");

            final boolean isMoreControlsVisible = secondaryControls.getVisibility() == View.VISIBLE;

            AnimationUtils.animateRotation(moreOptionsButton, DEFAULT_CONTROLS_DURATION,
                    isMoreControlsVisible ? 0 : 180);
            animateView(secondaryControls, AnimationUtils.Type.SLIDE_AND_ALPHA, !isMoreControlsVisible,
                    DEFAULT_CONTROLS_DURATION);
            showControls(DEFAULT_CONTROLS_DURATION);
        }

        private void onScreenRotationClicked() {
            if (DEBUG) Log.d(TAG, "onScreenRotationClicked() called");
            toggleOrientation();
            showControlsThenHide();
        }

        @Override
        public void onPlaybackSpeedClicked() {
            PlaybackParameterDialog.newInstance(getPlaybackSpeed(), getPlaybackPitch())
                    .show(getSupportFragmentManager(), TAG);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            try {
                super.onStopTrackingTouch(seekBar);
                if (wasPlaying()) showControlsThenHide();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDismiss(PopupMenu menu) {
            super.onDismiss(menu);
            if (isPlaying()) hideControls(DEFAULT_CONTROLS_DURATION, 0);
            hideSystemUi();
        }

        @Override
        protected int nextResizeMode(int currentResizeMode) {
            switch (currentResizeMode) {
                case AspectRatioFrameLayout.RESIZE_MODE_FIT:
                    return AspectRatioFrameLayout.RESIZE_MODE_FILL;
                case AspectRatioFrameLayout.RESIZE_MODE_FILL:
                    return AspectRatioFrameLayout.RESIZE_MODE_ZOOM;
                default:
                    return AspectRatioFrameLayout.RESIZE_MODE_FIT;
            }
        }

        @Override
        protected int getDefaultResolutionIndex(final List<VideoStream> sortedVideos) {
            return ListHelper.getDefaultResolutionIndex(context, sortedVideos);
        }

        @Override
        protected int getOverrideResolutionIndex(final List<VideoStream> sortedVideos,
                                                 final String playbackQuality) {
            return ListHelper.getDefaultResolutionIndex(context, sortedVideos, playbackQuality);
        }

        /*//////////////////////////////////////////////////////////////////////////
        // States
        //////////////////////////////////////////////////////////////////////////*/

        private void animatePlayButtons(final boolean show, final int duration) {
            AnimationUtils.animateView(playPauseButton, AnimationUtils.Type.SCALE_AND_ALPHA, show, duration);
            AnimationUtils.animateView(playPreviousButton, AnimationUtils.Type.SCALE_AND_ALPHA, show, duration);
            AnimationUtils.animateView(playNextButton, AnimationUtils.Type.SCALE_AND_ALPHA, show, duration);
        }

        @Override
        public void onBlocked() {
            super.onBlocked();
            playPauseButton.setImageResource(org.playtube.plus.R.drawable.ic_pause_white);
            animatePlayButtons(false, 100);
            getRootView().setKeepScreenOn(true);
        }

        @Override
        public void onBuffering() {
            super.onBuffering();
            animatePlayButtons(false, 100);
            getRootView().setKeepScreenOn(true);
        }

        @Override
        public void onPlaying() {
            super.onPlaying();
            AnimationUtils.animateView(playPauseButton, AnimationUtils.Type.SCALE_AND_ALPHA, false, 80, 0, () -> {
                playPauseButton.setImageResource(org.playtube.plus.R.drawable.ic_pause_white);
                animatePlayButtons(true, 200);
            });

            getRootView().setKeepScreenOn(true);
        }

        @Override
        public void onPaused() {
            super.onPaused();
            AnimationUtils.animateView(playPauseButton, AnimationUtils.Type.SCALE_AND_ALPHA, false, 80, 0, () -> {
                playPauseButton.setImageResource(org.playtube.plus.R.drawable.ic_play_arrow_white);
                animatePlayButtons(true, 200);
            });

            changeSystemUi();
            getRootView().setKeepScreenOn(false);
        }

        @Override
        public void onPausedSeek() {
            super.onPausedSeek();
            animatePlayButtons(false, 100);
            getRootView().setKeepScreenOn(true);
        }


        @Override
        public void onCompleted() {
            AnimationUtils.animateView(playPauseButton, AnimationUtils.Type.SCALE_AND_ALPHA, false, 0, 0, () -> {
                playPauseButton.setImageResource(org.playtube.plus.R.drawable.ic_replay_white);
                animatePlayButtons(true, DEFAULT_CONTROLS_DURATION);
            });

            getRootView().setKeepScreenOn(false);
            super.onCompleted();
        }

        /*//////////////////////////////////////////////////////////////////////////
        // Utils
        //////////////////////////////////////////////////////////////////////////*/

        @Override
        public void showControlsThenHide() {
            if (queueVisible) return;

            super.showControlsThenHide();
        }

        @Override
        public void showControls(long duration) {
            if (queueVisible) return;

            super.showControls(duration);
        }

        @Override
        public void hideControls(final long duration, long delay) {
            if (DEBUG) Log.d(TAG, "hideControls() called with: delay = [" + delay + "]");
            getControlsVisibilityHandler().removeCallbacksAndMessages(null);
            getControlsVisibilityHandler().postDelayed(() ->
                    AnimationUtils.animateView(getControlsRoot(), false, duration, 0,
                            MainVideoPlayer.this::hideSystemUi),
                    /*delayMillis=*/delay
            );
        }

        private void updatePlaybackButtons() {
            if (repeatButton == null || shuffleButton == null ||
                    simpleExoPlayer == null || playQueue == null) return;

            setRepeatModeButton(repeatButton, getRepeatMode());
            setShuffleButton(shuffleButton, playQueue.isShuffled());
        }

        private void buildQueue() {
            itemsList.setAdapter(playQueueAdapter);
            itemsList.setClickable(true);
            itemsList.setLongClickable(true);

            itemsList.clearOnScrollListeners();
            itemsList.addOnScrollListener(getQueueScrollListener());

            itemTouchHelper = new ItemTouchHelper(getItemTouchCallback());
            itemTouchHelper.attachToRecyclerView(itemsList);

            playQueueAdapter.setSelectedListener(getOnSelectedListener());

            itemsListCloseButton.setOnClickListener(view -> onQueueClosed());
        }

        private OnScrollBelowItemsListener getQueueScrollListener() {
            return new OnScrollBelowItemsListener() {
                @Override
                public void onScrolledDown(RecyclerView recyclerView) {
                    if (playQueue != null && !playQueue.isComplete()) {
                        playQueue.fetch();
                    } else if (itemsList != null) {
                        itemsList.clearOnScrollListeners();
                    }
                }
            };
        }

        private ItemTouchHelper.SimpleCallback getItemTouchCallback() {
            return new PlayQueueItemTouchCallback() {
                @Override
                public void onMove(int sourceIndex, int targetIndex) {
                    if (playQueue != null) playQueue.move(sourceIndex, targetIndex);
                }
            };
        }

        private PlayQueueItemBuilder.OnSelectedListener getOnSelectedListener() {
            return new PlayQueueItemBuilder.OnSelectedListener() {
                @Override
                public void selected(PlayQueueItem item, View view) {
                    onSelected(item);
                }

                @Override
                public void held(PlayQueueItem item, View view) {
                    final int index = playQueue.indexOf(item);
                    if (index != -1) playQueue.remove(index);
                }

                @Override
                public void onStartDrag(PlayQueueItemHolder viewHolder) {
                    if (itemTouchHelper != null) itemTouchHelper.startDrag(viewHolder);
                }
            };
        }

        ///////////////////////////////////////////////////////////////////////////
        // Getters
        ///////////////////////////////////////////////////////////////////////////

        public TextView getTitleTextView() {
            return titleTextView;
        }

        public TextView getChannelTextView() {
            return channelTextView;
        }

        public TextView getVolumeTextView() {
            return volumeTextView;
        }

        public TextView getBrightnessTextView() {
            return brightnessTextView;
        }

        public ImageButton getRepeatButton() {
            return repeatButton;
        }

        public ImageButton getPlayPauseButton() {
            return playPauseButton;
        }
    }

    private class MySimpleOnGestureListener extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener {
        private boolean isMoving;

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (DEBUG) Log.d(TAG, "onDoubleTap() called with: e = [" + e + "]" + "rawXy = " + e.getRawX() + ", " + e.getRawY() + ", xy = " + e.getX() + ", " + e.getY());
            if (!playerImpl.isPlaying()) return false;

            if (e.getX() > playerImpl.getRootView().getWidth() / 2) {
                playerImpl.onFastForward();
            } else {
                playerImpl.onFastRewind();
            }

            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (DEBUG) Log.d(TAG, "onSingleTapConfirmed() called with: e = [" + e + "]");
            if (playerImpl.getCurrentState() == BasePlayer.STATE_BLOCKED) return true;

            if (playerImpl.isControlsVisible()) {
                playerImpl.hideControls(150, 0);
            } else {
                playerImpl.showControlsThenHide();
                changeSystemUi();
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            if (DEBUG) Log.d(TAG, "onDown() called with: e = [" + e + "]");

            return super.onDown(e);
        }

        private final boolean isPlayerGestureEnabled = PlayerHelper.isPlayerGestureEnabled(getApplicationContext());

        private final float stepsBrightness = 15, stepBrightness = (1f / stepsBrightness), minBrightness = .01f;
        private float currentBrightness = .5f;

        private int currentVolume, maxVolume = playerImpl.getAudioReactor().getMaxVolume();
        private final float stepsVolume = 15, stepVolume = (float) Math.ceil(maxVolume / stepsVolume), minVolume = 0;

        private final String brightnessUnicode = new String(Character.toChars(0x2600));
        private final String volumeUnicode = new String(Character.toChars(0x1F508));

        private final int MOVEMENT_THRESHOLD = 40;
        private final int eventsThreshold = 8;
        private boolean triggered = false;
        private int eventsNum;

        // TODO: Improve video gesture controls
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (!isPlayerGestureEnabled) return false;

            //noinspection PointlessBooleanExpression
            if (DEBUG && false) Log.d(TAG, "MainVideoPlayer.onScroll = " +
                    ", e1.getRaw = [" + e1.getRawX() + ", " + e1.getRawY() + "]" +
                    ", e2.getRaw = [" + e2.getRawX() + ", " + e2.getRawY() + "]" +
                    ", distanceXy = [" + distanceX + ", " + distanceY + "]");
            float abs = Math.abs(e2.getY() - e1.getY());
            if (!triggered) {
                triggered = abs > MOVEMENT_THRESHOLD;
                return false;
            }

            if (eventsNum++ % eventsThreshold != 0 || playerImpl.getCurrentState() == BasePlayer.STATE_COMPLETED) return false;
            isMoving = true;
//            boolean up = !((e2.getY() - e1.getY()) > 0) && distanceY > 0; // Android's origin point is on top
            boolean up = distanceY > 0;


            if (e1.getX() > playerImpl.getRootView().getWidth() / 2) {
                double floor = Math.floor(up ? stepVolume : -stepVolume);
                currentVolume = (int) (playerImpl.getAudioReactor().getVolume() + floor);
                if (currentVolume >= maxVolume) currentVolume = maxVolume;
                if (currentVolume <= minVolume) currentVolume = (int) minVolume;
                playerImpl.getAudioReactor().setVolume(currentVolume);

                currentVolume = playerImpl.getAudioReactor().getVolume();
                if (DEBUG) Log.d(TAG, "onScroll().volumeControl, currentVolume = " + currentVolume);
                final String volumeText = volumeUnicode + " " + Math.round((((float) currentVolume) / maxVolume) * 100) + "%";
                playerImpl.getVolumeTextView().setText(volumeText);

                if (playerImpl.getVolumeTextView().getVisibility() != View.VISIBLE) AnimationUtils.animateView(playerImpl.getVolumeTextView(), true, 200);
                if (playerImpl.getBrightnessTextView().getVisibility() == View.VISIBLE) playerImpl.getBrightnessTextView().setVisibility(View.GONE);
            } else {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                currentBrightness += up ? stepBrightness : -stepBrightness;
                if (currentBrightness >= 1f) currentBrightness = 1f;
                if (currentBrightness <= minBrightness) currentBrightness = minBrightness;

                lp.screenBrightness = currentBrightness;
                getWindow().setAttributes(lp);
                if (DEBUG) Log.d(TAG, "onScroll().brightnessControl, currentBrightness = " + currentBrightness);
                int brightnessNormalized = Math.round(currentBrightness * 100);

                final String brightnessText = brightnessUnicode + " " + (brightnessNormalized == 1 ? 0 : brightnessNormalized) + "%";
                playerImpl.getBrightnessTextView().setText(brightnessText);

                if (playerImpl.getBrightnessTextView().getVisibility() != View.VISIBLE) AnimationUtils.animateView(playerImpl.getBrightnessTextView(), true, 200);
                if (playerImpl.getVolumeTextView().getVisibility() == View.VISIBLE) playerImpl.getVolumeTextView().setVisibility(View.GONE);
            }
            return true;
        }

        private void onScrollEnd() {
            if (DEBUG) Log.d(TAG, "onScrollEnd() called");
            triggered = false;
            eventsNum = 0;
            /* if (playerImpl.getVolumeTextView().getVisibility() == View.VISIBLE) playerImpl.getVolumeTextView().setVisibility(View.GONE);
            if (playerImpl.getBrightnessTextView().getVisibility() == View.VISIBLE) playerImpl.getBrightnessTextView().setVisibility(View.GONE);*/
            if (playerImpl.getVolumeTextView().getVisibility() == View.VISIBLE) {
                AnimationUtils.animateView(playerImpl.getVolumeTextView(), false, 200, 200);
            }
            if (playerImpl.getBrightnessTextView().getVisibility() == View.VISIBLE) {
                AnimationUtils.animateView(playerImpl.getBrightnessTextView(), false, 200, 200);
            }

            if (playerImpl.isControlsVisible() && playerImpl.getCurrentState() == STATE_PLAYING) {
                playerImpl.hideControls(DEFAULT_CONTROLS_DURATION, DEFAULT_CONTROLS_HIDE_TIME);
            }
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            //noinspection PointlessBooleanExpression
            if (DEBUG && false) Log.d(TAG, "onTouch() called with: v = [" + v + "], event = [" + event + "]");
            gestureDetector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP && isMoving) {
                isMoving = false;
                onScrollEnd();
            }
            return true;
        }

    }
}
