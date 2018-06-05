package org.playtube.plus.fragments.local.bookmark;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.playtube.plus.App;
import org.playtube.plus.R;
import org.playtube.plus.database.LocalItem;
import org.playtube.plus.database.stream.StreamStatisticsEntry;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.playtube.plus.history.HistoryRecordManager;
import org.playtube.plus.info_list.InfoItemDialog;
import org.playtube.plus.playlist.PlayQueue;
import org.playtube.plus.playlist.SinglePlayQueue;
import org.playtube.plus.report.UserAction;
import org.playtube.plus.util.NavigationHelper;
import org.playtube.plus.util.OnClickGesture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import icepick.State;
import io.reactivex.android.schedulers.AndroidSchedulers;

public abstract class StatisticsPlaylistFragment
        extends BaseLocalListFragment<List<StreamStatisticsEntry>, Void> {

    private View headerPlayAllButton;
    private View headerPopupButton;
    private View headerBackgroundButton;

    @State
    protected Parcelable itemsListState;

    /* Used for independent events */
    private Subscription databaseSubscription;
    private HistoryRecordManager recordManager;

    ///////////////////////////////////////////////////////////////////////////
    // Abstracts
    ///////////////////////////////////////////////////////////////////////////

    protected abstract String getName();

    protected abstract List<StreamStatisticsEntry> processResult(final List<StreamStatisticsEntry> results);

    ///////////////////////////////////////////////////////////////////////////
    // Fragment LifeCycle - Creation
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recordManager = new HistoryRecordManager(getContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist, container, false);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment LifeCycle - Views
    ///////////////////////////////////////////////////////////////////////////

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);
        setTitle(getName());
    }

    @Override
    protected View getListHeader() {
        final View headerRootLayout = activity.getLayoutInflater().inflate(R.layout.playlist_control,
                itemsList, false);
        headerPlayAllButton = headerRootLayout.findViewById(R.id.playlist_ctrl_play_all_button);
        headerPopupButton = headerRootLayout.findViewById(R.id.playlist_ctrl_play_popup_button);
        headerBackgroundButton = headerRootLayout.findViewById(R.id.playlist_ctrl_play_bg_button);
        return headerRootLayout;
    }

    @Override
    protected void initListeners() {
        super.initListeners();

        itemListAdapter.setSelectedListener(new OnClickGesture<LocalItem>() {
            @Override
            public void selected(LocalItem selectedItem) {
                if (selectedItem instanceof StreamStatisticsEntry) {
                    final StreamStatisticsEntry item = (StreamStatisticsEntry) selectedItem;
                    NavigationHelper.openVideoDetailFragment(getFragmentManager(),
                            item.serviceId, item.url, item.title);
                }
            }

            @Override
            public void held(LocalItem selectedItem) {
                if (selectedItem instanceof StreamStatisticsEntry) {
                    showStreamDialog((StreamStatisticsEntry) selectedItem);
                }
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment LifeCycle - Loading
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void startLoading(boolean forceLoad) {
        super.startLoading(forceLoad);
        recordManager.getStreamStatistics()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getHistoryObserver());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment LifeCycle - Destruction
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onPause() {
        super.onPause();
        itemsListState = itemsList.getLayoutManager().onSaveInstanceState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (itemListAdapter != null) itemListAdapter.unsetSelectedListener();
        if (headerBackgroundButton != null) headerBackgroundButton.setOnClickListener(null);
        if (headerPlayAllButton != null) headerPlayAllButton.setOnClickListener(null);
        if (headerPopupButton != null) headerPopupButton.setOnClickListener(null);

        if (databaseSubscription != null) databaseSubscription.cancel();
        databaseSubscription = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        recordManager = null;
        itemsListState = null;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Statistics Loader
    ///////////////////////////////////////////////////////////////////////////

    private Subscriber<List<StreamStatisticsEntry>> getHistoryObserver() {
        return new Subscriber<List<StreamStatisticsEntry>>() {
            @Override
            public void onSubscribe(Subscription s) {
                showLoading();

                if (databaseSubscription != null) databaseSubscription.cancel();
                databaseSubscription = s;
                databaseSubscription.request(1);
            }

            @Override
            public void onNext(List<StreamStatisticsEntry> streams) {
                handleResult(streams);
                if (databaseSubscription != null) databaseSubscription.request(1);
            }

            @Override
            public void onError(Throwable exception) {
                StatisticsPlaylistFragment.this.onError(exception);
            }

            @Override
            public void onComplete() {
            }
        };
    }

    @Override
    public void handleResult(@NonNull List<StreamStatisticsEntry> result) {
        super.handleResult(result);
        if (itemListAdapter == null) return;

        itemListAdapter.clearStreamItemList();

        if (result.isEmpty()) {
            showEmptyState();
            return;
        }

        itemListAdapter.addItems(processResult(result));
        if (itemsListState != null) {
            itemsList.getLayoutManager().onRestoreInstanceState(itemsListState);
            itemsListState = null;
        }

        headerPlayAllButton.setOnClickListener(view ->
                NavigationHelper.playOnMainPlayer(activity, getPlayQueue(false)));
        headerPopupButton.setOnClickListener(view ->
                NavigationHelper.playOnPopupPlayer(activity, getPlayQueue(false)));
        headerBackgroundButton.setOnClickListener(view ->
                NavigationHelper.playOnBackgroundPlayer(activity, getPlayQueue(true)));

        hideLoading();
    }
    ///////////////////////////////////////////////////////////////////////////
    // Fragment Error Handling
    ///////////////////////////////////////////////////////////////////////////

    @Override
    protected void resetFragment() {
        super.resetFragment();
        if (databaseSubscription != null) databaseSubscription.cancel();
    }

    @Override
    protected boolean onError(Throwable exception) {
        if (super.onError(exception)) return true;

        onUnrecoverableError(exception, UserAction.SOMETHING_ELSE,
                "none", "History Statistics", R.string.general_error);
        return true;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/

    private void showStreamDialog(final StreamStatisticsEntry item) {
        final Context context = getContext();
        final Activity activity = getActivity();
        if (context == null || context.getResources() == null || getActivity() == null) return;
        final StreamInfoItem infoItem = item.toStreamInfoItem();

        final String[] commands = new String[]{
                context.getResources().getString(R.string.enqueue_on_background),
                context.getResources().getString(R.string.enqueue_on_popup),
                context.getResources().getString(R.string.start_here_on_main),
                context.getResources().getString(R.string.start_here_on_background),
                context.getResources().getString(R.string.start_here_on_popup),
        };

        final DialogInterface.OnClickListener actions = (dialogInterface, i) -> {
            final int index = Math.max(itemListAdapter.getItemsList().indexOf(item), 0);
            switch (i) {
                case 0:
                    NavigationHelper.enqueueOnBackgroundPlayer(context, new SinglePlayQueue(infoItem));
                    break;
                case 1:
                    NavigationHelper.enqueueOnPopupPlayer(activity, new SinglePlayQueue(infoItem));
                    break;
                case 2:
                    NavigationHelper.playOnMainPlayer(context, getPlayQueue(index, false));
                    break;
                case 3:
                    NavigationHelper.playOnBackgroundPlayer(context, getPlayQueue(index, true));
                    break;
                case 4:
                    NavigationHelper.playOnPopupPlayer(activity, getPlayQueue(index, false));
                    break;
                default:
                    break;
            }
        };

        new InfoItemDialog(getActivity(), infoItem, commands, actions).show();
    }

    private PlayQueue getPlayQueue(boolean isBackgroundPlay) {
        return getPlayQueue(0, isBackgroundPlay);
    }

    private PlayQueue getPlayQueue(final int index, boolean isBackgroundPlay) {
        if (itemListAdapter == null) {
            return new SinglePlayQueue(Collections.emptyList(), 0);
        }

        final List<LocalItem> infoItems = itemListAdapter.getItemsList();
        List<StreamInfoItem> streamInfoItems = new ArrayList<>(infoItems.size());
        boolean isHasYoutube = false;
        for (final LocalItem item : infoItems) {
            if (item instanceof StreamStatisticsEntry) {
                StreamStatisticsEntry entry = (StreamStatisticsEntry) item;
                if (isBackgroundPlay && !App.isBgPlay()) {
                    if (entry.serviceId == 1) {
                        streamInfoItems.add(entry.toStreamInfoItem());
                    } else {
                        isHasYoutube = true;
                    }
                } else {
                    streamInfoItems.add(entry.toStreamInfoItem());
                }
            }
        }

        if (isHasYoutube) {
            Toast.makeText(getContext(), R.string.background_play_tips, Toast.LENGTH_LONG).show();
        }

        return new SinglePlayQueue(streamInfoItems, index);
    }
}

