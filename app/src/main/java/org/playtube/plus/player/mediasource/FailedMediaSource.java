package org.playtube.plus.player.mediasource;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.upstream.Allocator;

import org.playtube.plus.playlist.PlayQueueItem;

import java.io.IOException;

public class FailedMediaSource implements ManagedMediaSource {
    private final String TAG = "FailedMediaSource@" + Integer.toHexString(hashCode());

    private final PlayQueueItem playQueueItem;
    private final Throwable error;

    private final long retryTimestamp;

    public FailedMediaSource(@NonNull final PlayQueueItem playQueueItem,
                             @NonNull final Throwable error,
                             final long retryTimestamp) {
        this.playQueueItem = playQueueItem;
        this.error = error;
        this.retryTimestamp = retryTimestamp;
    }

    /**
     * Permanently fail the play queue item associated with this source, with no hope of retrying.
     * The error will always be propagated to ExoPlayer.
     * */
    public FailedMediaSource(@NonNull final PlayQueueItem playQueueItem,
                             @NonNull final Throwable error) {
        this.playQueueItem = playQueueItem;
        this.error = error;
        this.retryTimestamp = Long.MAX_VALUE;
    }

    public PlayQueueItem getStream() {
        return playQueueItem;
    }

    public Throwable getError() {
        return error;
    }

    private boolean canRetry() {
        return System.currentTimeMillis() >= retryTimestamp;
    }

    @Override
    public void prepareSource(ExoPlayer player, boolean isTopLevelSource, Listener listener) {
        Log.e(TAG, "Loading failed source: ", error);
    }

    @Override
    public void maybeThrowSourceInfoRefreshError() throws IOException {
        throw new IOException(error);
    }

    @Override
    public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator) {
        return null;
    }

    @Override
    public void releasePeriod(MediaPeriod mediaPeriod) {}

    @Override
    public void releaseSource() {}

    @Override
    public boolean shouldBeReplacedWith(@NonNull final PlayQueueItem newIdentity,
                                        final boolean isInterruptable) {
        return newIdentity != playQueueItem || canRetry();
    }

    @Override
    public boolean isStreamEqual(@NonNull PlayQueueItem stream) {
        return playQueueItem == stream;
    }
}
