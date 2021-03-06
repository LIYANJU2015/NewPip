package org.tubeplayer.plus.player.playback;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.exoplayer2.source.DynamicConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ShuffleOrder;

import org.tubeplayer.plus.player.mediasource.LoadedMediaSource;
import org.tubeplayer.plus.player.mediasource.ManagedMediaSource;
import org.tubeplayer.plus.playlist.PlayQueue;
import org.tubeplayer.plus.playlist.PlayQueueItem;
import org.tubeplayer.plus.playlist.events.MoveEvent;
import org.tubeplayer.plus.playlist.events.RemoveEvent;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.tubeplayer.plus.player.mediasource.FailedMediaSource;
import org.tubeplayer.plus.player.mediasource.PlaceholderMediaSource;
import org.tubeplayer.plus.playlist.events.PlayQueueEvent;
import org.tubeplayer.plus.playlist.events.ReorderEvent;
import org.tubeplayer.plus.util.ServiceHelper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.SerialDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.subscriptions.EmptySubscription;
import io.reactivex.subjects.PublishSubject;

public class MediaSourceManager {
    @NonNull private final String TAG = "MediaSourceManager@" + hashCode();

    /**
     * Determines how many streams before and after the current stream should be loaded.
     * The default value (1) ensures seamless playback under typical network settings.
     * <br><br>
     * The streams after the current will be loaded into the playlist timeline while the
     * streams before will only be cached for future usage.
     *
     * @see #onMediaSourceReceived(PlayQueueItem, ManagedMediaSource)
     * @see #update(int, MediaSource, Runnable)
     * */
    private final static int WINDOW_SIZE = 1;

    @NonNull private final PlaybackListener playbackListener;
    @NonNull private final PlayQueue playQueue;

    /**
     * Determines the gap time between the playback position and the playback duration which
     * the {@link #getEdgeIntervalSignal()} begins to request loading.
     *
     * @see #progressUpdateIntervalMillis
     * */
    private final long playbackNearEndGapMillis;
    /**
     * Determines the interval which the {@link #getEdgeIntervalSignal()} waits for between
     * each request for loading, once {@link #playbackNearEndGapMillis} has reached.
     * */
    private final long progressUpdateIntervalMillis;
    @NonNull private final Observable<Long> nearEndIntervalSignal;

    /**
     * Process only the last load order when receiving a stream of load orders (lessens I/O).
     * <br><br>
     * The higher it is, the less loading occurs during rapid noncritical timeline changes.
     * <br><br>
     * Not recommended to go below 100ms.
     *
     * @see #loadDebounced()
     * */
    private final long loadDebounceMillis;
    @NonNull private final Disposable debouncedLoader;
    @NonNull private final PublishSubject<Long> debouncedSignal;

    @NonNull private Subscription playQueueReactor;

    /**
     * Determines the maximum number of disposables allowed in the {@link #loaderReactor}.
     * Once exceeded, new calls to {@link #loadImmediate()} will evict all disposables in the
     * {@link #loaderReactor} in order to load a new set of items.
     *
     * @see #loadImmediate()
     * @see #maybeLoadItem(PlayQueueItem)
     * */
    private final static int MAXIMUM_LOADER_SIZE = WINDOW_SIZE * 2 + 1;
    @NonNull private final CompositeDisposable loaderReactor;
    @NonNull private final Set<PlayQueueItem> loadingItems;
    @NonNull private final SerialDisposable syncReactor;

    @NonNull private final AtomicBoolean isBlocked;

    @NonNull private DynamicConcatenatingMediaSource sources;

    public MediaSourceManager(@NonNull final PlaybackListener listener,
                              @NonNull final PlayQueue playQueue) {
        this(listener, playQueue, /*loadDebounceMillis=*/400L,
                /*playbackNearEndGapMillis=*/TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS),
                /*progressUpdateIntervalMillis*/TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS));
    }

    private MediaSourceManager(@NonNull final PlaybackListener listener,
                               @NonNull final PlayQueue playQueue,
                               final long loadDebounceMillis,
                               final long playbackNearEndGapMillis,
                               final long progressUpdateIntervalMillis) {
        if (playQueue.getBroadcastReceiver() == null) {
            throw new IllegalArgumentException("Play Queue has not been initialized.");
        }
        if (playbackNearEndGapMillis < progressUpdateIntervalMillis) {
            throw new IllegalArgumentException("Playback end gap=[" + playbackNearEndGapMillis +
                    " ms] must be longer than update interval=[ " + progressUpdateIntervalMillis +
                    " ms] for them to be useful.");
        }

        this.playbackListener = listener;
        this.playQueue = playQueue;

        this.playbackNearEndGapMillis = playbackNearEndGapMillis;
        this.progressUpdateIntervalMillis = progressUpdateIntervalMillis;
        this.nearEndIntervalSignal = getEdgeIntervalSignal();

        this.loadDebounceMillis = loadDebounceMillis;
        this.debouncedSignal = PublishSubject.create();
        this.debouncedLoader = getDebouncedLoader();

        this.playQueueReactor = EmptySubscription.INSTANCE;
        this.loaderReactor = new CompositeDisposable();
        this.syncReactor = new SerialDisposable();

        this.isBlocked = new AtomicBoolean(false);

        this.sources = new DynamicConcatenatingMediaSource();

        this.loadingItems = Collections.synchronizedSet(new HashSet<>());

        playQueue.getBroadcastReceiver()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getReactor());
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Exposed Methods
    //////////////////////////////////////////////////////////////////////////*/
    /**
     * Dispose the manager and releases all message buses and loaders.
     * */
    public void dispose() {
        if (PlayQueue.DEBUG) Log.d(TAG, "dispose() called.");

        debouncedSignal.onComplete();
        debouncedLoader.dispose();

        playQueueReactor.cancel();
        loaderReactor.dispose();
        syncReactor.dispose();
        sources.releaseSource();
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Event Reactor
    //////////////////////////////////////////////////////////////////////////*/

    private Subscriber<PlayQueueEvent> getReactor() {
        return new Subscriber<PlayQueueEvent>() {
            @Override
            public void onSubscribe(@NonNull Subscription d) {
                playQueueReactor.cancel();
                playQueueReactor = d;
                playQueueReactor.request(1);
            }

            @Override
            public void onNext(@NonNull PlayQueueEvent playQueueMessage) {
                onPlayQueueChanged(playQueueMessage);
            }

            @Override
            public void onError(@NonNull Throwable e) {}

            @Override
            public void onComplete() {}
        };
    }

    private void onPlayQueueChanged(final PlayQueueEvent event) {
        if (playQueue.isEmpty() && playQueue.isComplete()) {
            playbackListener.onPlaybackShutdown();
            return;
        }

        // Event specific action
        switch (event.type()) {
            case INIT:
            case ERROR:
                maybeBlock();
            case APPEND:
                populateSources();
                break;
            case SELECT:
                maybeRenewCurrentIndex();
                break;
            case REMOVE:
                final RemoveEvent removeEvent = (RemoveEvent) event;
                remove(removeEvent.getRemoveIndex());
                break;
            case MOVE:
                final MoveEvent moveEvent = (MoveEvent) event;
                move(moveEvent.getFromIndex(), moveEvent.getToIndex());
                break;
            case REORDER:
                // Need to move to ensure the playing index from play queue matches that of
                // the source timeline, and then window correction can take care of the rest
                final ReorderEvent reorderEvent = (ReorderEvent) event;
                move(reorderEvent.getFromSelectedIndex(), reorderEvent.getToSelectedIndex());
                break;
            case RECOVERY:
            default:
                break;
        }

        // Loading and Syncing
        switch (event.type()) {
            case INIT:
            case REORDER:
            case ERROR:
            case SELECT:
                loadImmediate(); // low frequency, critical events
                break;
            case APPEND:
            case REMOVE:
            case MOVE:
            case RECOVERY:
            default:
                loadDebounced(); // high frequency or noncritical events
                break;
        }

        if (!isPlayQueueReady()) {
            maybeBlock();
            playQueue.fetch();
        }
        playQueueReactor.request(1);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Playback Locking
    //////////////////////////////////////////////////////////////////////////*/

    private boolean isPlayQueueReady() {
        final boolean isWindowLoaded = playQueue.size() - playQueue.getIndex() > WINDOW_SIZE;
        return playQueue.isComplete() || isWindowLoaded;
    }

    private boolean isPlaybackReady() {
        if (sources.getSize() != playQueue.size()) return false;

        final ManagedMediaSource mediaSource =
                (ManagedMediaSource) sources.getMediaSource(playQueue.getIndex());
        final PlayQueueItem playQueueItem = playQueue.getItem();
        return mediaSource.isStreamEqual(playQueueItem);
    }

    private void maybeBlock() {
        if (PlayQueue.DEBUG) Log.d(TAG, "maybeBlock() called.");

        if (isBlocked.get()) return;

        playbackListener.onPlaybackBlock();
        resetSources();

        isBlocked.set(true);
    }

    private void maybeUnblock() {
        if (PlayQueue.DEBUG) Log.d(TAG, "maybeUnblock() called.");

        if (isPlayQueueReady() && isPlaybackReady() && isBlocked.get()) {
            isBlocked.set(false);
            playbackListener.onPlaybackUnblock(sources);
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Metadata Synchronization
    //////////////////////////////////////////////////////////////////////////*/

    private void maybeSync() {
        if (PlayQueue.DEBUG) Log.d(TAG, "onPlaybackSynchronize() called.");

        final PlayQueueItem currentItem = playQueue.getItem();
        if (isBlocked.get() || !isPlaybackReady() || currentItem == null) return;

        final Consumer<StreamInfo> onSuccess = info -> syncInternal(currentItem, info);
        final Consumer<Throwable> onError = throwable -> syncInternal(currentItem, null);

        final Disposable sync = currentItem.getStream()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onSuccess, onError);
        syncReactor.set(sync);
    }

    private void syncInternal(@NonNull final PlayQueueItem item,
                              @Nullable final StreamInfo info) {
        // Ensure the current item is up to date with the play queue
        if (playQueue.getItem() == item) {
            playbackListener.onPlaybackSynchronize(item, info);
        }
    }

    private void maybeSynchronizePlayer() {
        maybeUnblock();
        maybeSync();
    }

    /*//////////////////////////////////////////////////////////////////////////
    // MediaSource Loading
    //////////////////////////////////////////////////////////////////////////*/

    private Observable<Long> getEdgeIntervalSignal() {
        return Observable.interval(progressUpdateIntervalMillis, TimeUnit.MILLISECONDS)
                .filter(ignored -> playbackListener.isNearPlaybackEdge(playbackNearEndGapMillis));
    }

    private Disposable getDebouncedLoader() {
        return debouncedSignal.mergeWith(nearEndIntervalSignal)
                .debounce(loadDebounceMillis, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(timestamp -> loadImmediate());
    }

    private void loadDebounced() {
        debouncedSignal.onNext(System.currentTimeMillis());
    }

    private void loadImmediate() {
        if (PlayQueue.DEBUG) Log.d(TAG, "MediaSource - loadImmediate() called");
        // The current item has higher priority
        final int currentIndex = playQueue.getIndex();
        final PlayQueueItem currentItem = playQueue.getItem(currentIndex);
        if (currentItem == null) return;

        // Evict the items being loaded to free up memory
        if (loaderReactor.size() > MAXIMUM_LOADER_SIZE) {
            loaderReactor.clear();
            loadingItems.clear();
        }
        maybeLoadItem(currentItem);

        // The rest are just for seamless playback
        // Although timeline is not updated prior to the current index, these sources are still
        // loaded into the cache for faster retrieval at a potentially later time.
        final int leftBound = Math.max(0, currentIndex - WINDOW_SIZE);
        final int rightLimit = currentIndex + WINDOW_SIZE + 1;
        final int rightBound = Math.min(playQueue.size(), rightLimit);
        final Set<PlayQueueItem> items = new HashSet<>(
                playQueue.getStreams().subList(leftBound,rightBound));

        // Do a round robin
        final int excess = rightLimit - playQueue.size();
        if (excess >= 0) {
            items.addAll(playQueue.getStreams().subList(0, Math.min(playQueue.size(), excess)));
        }
        items.remove(currentItem);

        for (final PlayQueueItem item : items) {
            maybeLoadItem(item);
        }
    }

    private void maybeLoadItem(@NonNull final PlayQueueItem item) {
        if (PlayQueue.DEBUG) Log.d(TAG, "maybeLoadItem() called.");
        if (playQueue.indexOf(item) >= sources.getSize()) return;

        if (!loadingItems.contains(item) && isCorrectionNeeded(item)) {
            if (PlayQueue.DEBUG) Log.d(TAG, "MediaSource - Loading=[" + item.getTitle() +
                    "] with url=[" + item.getUrl() + "]");

            loadingItems.add(item);
            final Disposable loader = getLoadedMediaSource(item)
                    .observeOn(AndroidSchedulers.mainThread())
                    /* No exception handling since getLoadedMediaSource guarantees nonnull return */
                    .subscribe(mediaSource -> onMediaSourceReceived(item, mediaSource));
            loaderReactor.add(loader);
        }
    }

    private Single<ManagedMediaSource> getLoadedMediaSource(@NonNull final PlayQueueItem stream) {
        return stream.getStream().map(streamInfo -> {
            final MediaSource source = playbackListener.sourceOf(stream, streamInfo);
            if (source == null) {
                final Exception exception = new IllegalStateException(
                        "Unable to resolve source from stream info." +
                                " URL: " + stream.getUrl() +
                                ", audio count: " + streamInfo.getAudioStreams().size() +
                                ", video count: " + streamInfo.getVideoOnlyStreams().size() +
                                streamInfo.getVideoStreams().size());
                return new FailedMediaSource(stream, exception);
            }

            final long expiration = System.currentTimeMillis() +
                    ServiceHelper.getCacheExpirationMillis(streamInfo.getServiceId());
            return new LoadedMediaSource(source, stream, expiration);
        }).onErrorReturn(throwable -> new FailedMediaSource(stream, throwable));
    }

    private void onMediaSourceReceived(@NonNull final PlayQueueItem item,
                                       @NonNull final ManagedMediaSource mediaSource) {
        if (PlayQueue.DEBUG) Log.d(TAG, "MediaSource - Loaded=[" + item.getTitle() +
                "] with url=[" + item.getUrl() + "]");

        loadingItems.remove(item);

        final int itemIndex = playQueue.indexOf(item);
        // Only update the playlist timeline for items at the current index or after.
        if (itemIndex >= playQueue.getIndex() && isCorrectionNeeded(item)) {
            if (PlayQueue.DEBUG) Log.d(TAG, "MediaSource - Updating index=[" + itemIndex + "] with " +
                    "title=[" + item.getTitle() + "] at url=[" + item.getUrl() + "]");
            update(itemIndex, mediaSource, this::maybeSynchronizePlayer);
        }
    }

    /**
     * Checks if the corresponding MediaSource in {@link DynamicConcatenatingMediaSource}
     * for a given {@link PlayQueueItem} needs replacement, either due to gapless playback
     * readiness or playlist desynchronization.
     * <br><br>
     * If the given {@link PlayQueueItem} is currently being played and is already loaded,
     * then correction is not only needed if the playlist is desynchronized. Otherwise, the
     * check depends on the status (e.g. expiration or placeholder) of the
     * {@link ManagedMediaSource}.
     * */
    private boolean isCorrectionNeeded(@NonNull final PlayQueueItem item) {
        final int index = playQueue.indexOf(item);
        if (index == -1 || index >= sources.getSize()) return false;

        final ManagedMediaSource mediaSource = (ManagedMediaSource) sources.getMediaSource(index);
        return mediaSource.shouldBeReplacedWith(item,
                /*mightBeInProgress=*/index != playQueue.getIndex());
    }

    /**
     * Checks if the current playing index contains an expired {@link ManagedMediaSource}.
     * If so, the expired source is replaced by a {@link PlaceholderMediaSource} and
     * {@link #loadImmediate()} is called to reload the current item.
     * <br><br>
     * If not, then the media source at the current index is ready for playback, and
     * {@link #maybeSynchronizePlayer()} is called.
     * <br><br>
     * Under both cases, {@link #maybeSync()} will be called to ensure the listener
     * is up-to-date.
     * */
    private void maybeRenewCurrentIndex() {
        final int currentIndex = playQueue.getIndex();
        if (sources.getSize() <= currentIndex) return;

        final ManagedMediaSource currentSource =
                (ManagedMediaSource) sources.getMediaSource(currentIndex);
        final PlayQueueItem currentItem = playQueue.getItem();
        if (!currentSource.shouldBeReplacedWith(currentItem, /*canInterruptOnRenew=*/true)) {
            maybeSynchronizePlayer();
            return;
        }

        if (PlayQueue.DEBUG) Log.d(TAG, "MediaSource - Reloading currently playing, " +
                "index=[" + currentIndex + "], item=[" + currentItem.getTitle() + "]");
        update(currentIndex, new PlaceholderMediaSource(), this::loadImmediate);
    }
    /*//////////////////////////////////////////////////////////////////////////
    // MediaSource Playlist Helpers
    //////////////////////////////////////////////////////////////////////////*/

    private void resetSources() {
        if (PlayQueue.DEBUG) Log.d(TAG, "resetSources() called.");

        this.sources.releaseSource();
        this.sources = new DynamicConcatenatingMediaSource(false,
                // Shuffling is done on PlayQueue, thus no need to use ExoPlayer's shuffle order
                new ShuffleOrder.UnshuffledShuffleOrder(0));
    }

    private void populateSources() {
        if (PlayQueue.DEBUG) Log.d(TAG, "populateSources() called.");
        if (sources.getSize() >= playQueue.size()) return;

        for (int index = sources.getSize() - 1; index < playQueue.size(); index++) {
            emplace(index, new PlaceholderMediaSource());
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // MediaSource Playlist Manipulation
    //////////////////////////////////////////////////////////////////////////*/

    /**
     * Places a {@link MediaSource} into the {@link DynamicConcatenatingMediaSource}
     * with position in respect to the play queue only if no {@link MediaSource}
     * already exists at the given index.
     * */
    private synchronized void emplace(final int index, @NonNull final MediaSource source) {
        if (index < sources.getSize()) return;

        sources.addMediaSource(index, source);
    }

    /**
     * Removes a {@link MediaSource} from {@link DynamicConcatenatingMediaSource}
     * at the given index. If this index is out of bound, then the removal is ignored.
     * */
    private synchronized void remove(final int index) {
        if (index < 0 || index > sources.getSize()) return;

        sources.removeMediaSource(index);
    }

    /**
     * Moves a {@link MediaSource} in {@link DynamicConcatenatingMediaSource}
     * from the given source index to the target index. If either index is out of bound,
     * then the call is ignored.
     * */
    private synchronized void move(final int source, final int target) {
        if (source < 0 || target < 0) return;
        if (source >= sources.getSize() || target >= sources.getSize()) return;

        sources.moveMediaSource(source, target);
    }

    /**
     * Updates the {@link MediaSource} in {@link DynamicConcatenatingMediaSource}
     * at the given index with a given {@link MediaSource}. If the index is out of bound,
     * then the replacement is ignored.
     * <br><br>
     * Not recommended to use on indices LESS THAN the currently playing index, since
     * this will modify the playback timeline prior to the index and may cause desynchronization
     * on the playing item between {@link PlayQueue} and {@link DynamicConcatenatingMediaSource}.
     * */
    private synchronized void update(final int index, @NonNull final MediaSource source,
                                     @Nullable final Runnable finalizingAction) {
        if (index < 0 || index >= sources.getSize()) return;

        sources.addMediaSource(index + 1, source, () ->
                sources.removeMediaSource(index, finalizingAction));
    }
}
